package edu.stanford.hivdb.sars2.drdb;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.collections4.map.LRUMap;
import org.apache.commons.lang3.tuple.Pair;

import edu.stanford.hivdb.mutations.AAMutation;
import edu.stanford.hivdb.mutations.GenePosition;
import edu.stanford.hivdb.mutations.Mutation;
import edu.stanford.hivdb.mutations.MutationSet;
import edu.stanford.hivdb.sars2.SARS2;
import edu.stanford.hivdb.viruses.Gene;

public class DRDB {
	
	private static final int MAX_ENTRIES = 20;
	private static final String LIST_JOIN_UNIQ = "$#\u0008#$";
	private static final String QUOTED_LIST_JOIN_UNIQ = Pattern.quote(LIST_JOIN_UNIQ);
	private static final String COVID_DRDB_RESURL_PREFIX = "https://s3-us-west-2.amazonaws.com/cms.hivdb.org/chiro-prod/downloads/covid-drdb";

	private static final Map<String, DRDB> singletons = Collections.synchronizedMap(new LRUMap<String, DRDB>(MAX_ENTRIES));
	
	public static <T> Map<String, Map<String, T>> initVersionalSingletons() {
		return Collections.synchronizedMap(new LRUMap<String, Map<String, T>>(MAX_ENTRIES));
	}
	
	public static <T> void addVersionToVersionalSingletons(
		String version,
		Map<String, Map<String, T>> singletons,
		Function<DRDB, Map<String, T>> getInstances
	) {
		if (!singletons.containsKey(version)) {
			DRDB drdb = DRDB.getInstance(version);
			singletons.put(version, Collections.unmodifiableMap(getInstances.apply(drdb)));
		}
	}
	
	/**
	 * Predicate if input mutation is a key mutation
	 * 
	 * @param mutation
	 * @return
	 */
	public Predicate<Mutation<SARS2>> isKeyMutation = mutation -> {
		return queryAllKeyMutations()
			.stream()
			.anyMatch(
				mutSet -> mutSet.hasSharedAAMutation(mutation)
			);
	};
	
	public static DRDB getInstance(String version) {
		String resourcePath = String.format("%s/%s.db", COVID_DRDB_RESURL_PREFIX, version);
		if (!singletons.containsKey(resourcePath)) {
			DRDB instance = new DRDB(resourcePath);
			singletons.put(resourcePath, instance);
		}
		return singletons.get(resourcePath);
	}
	
	private final SARS2 virusIns;
	private final Connection conn;
	private transient Set<MutationSet<SARS2>> allKeyMutations;
	
	private DRDB(String resourcePath) {
		virusIns = SARS2.getInstance();
		try {
			Class.forName("org.sqlite.JDBC");
		} catch (ClassNotFoundException e1) {
			throw new RuntimeException(e1);
		}
		try {
			conn = DriverManager.getConnection("jdbc:sqlite::resource:" + resourcePath);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			throw new RuntimeException(e);
		}
	}
	
	protected <T> List<T> queryAll(String sql, Function<ResultSet, T> processor) {
		try (
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql)
		) {
			List<T> results = new ArrayList<>();
			while (rs.next()) {
				results.add(processor.apply(rs));
			}			
			return results;
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
	
	protected <T, R> Map<T, List<R>> groupAll(String sql, Function<R, T> classifier, Function<ResultSet, R> processor) {
		return (
			queryAll(sql, processor).stream()
			.collect(
				Collectors.groupingBy(
					classifier,
					LinkedHashMap::new,
					Collectors.toList()
				)
			)
		);
	}
	
	protected <T, R> Map<T, R> queryAll(String sql, Function<R, T> keyGetter, Function<ResultSet, R> processor) {
		return (
			queryAll(sql, processor).stream()
			.collect(
				Collectors.toMap(
					keyGetter,
					one -> one,
					(a, b) -> {
						throw new RuntimeException(
							String.format(
								"Conflict records was detected: %s and %s",
								a, b
							)
						);
					},
					LinkedHashMap::new
				)
			)
		);
	}
	
	protected <T> List<T> querySuscResults(
		MutationSet<SARS2> mutations,
		String columns,
		String joins,
		String where,
		Function<ResultSet, T> processor
	) {
		List<String> mutQueryList = new ArrayList<>();
		for (GenePosition<SARS2> genePos : mutations.getPositions()) {
			mutQueryList.add(String.format(
				"(M.gene = '%s' AND M.position = '%d')",
				genePos.getAbstractGene(),
				genePos.getPosition()
			));
		}
		String mutQuery = mutQueryList.size() > 0 ? String.join(" OR ", mutQueryList) : " false ";
		
		return queryAll(
			"SELECT " +
			columns +
			"  FROM susc_results S " +
			joins +
			"  WHERE EXISTS(" +
			"    SELECT 1 FROM variant_mutations M " +
			"    WHERE S.variant_name = M.variant_name AND (" +
			mutQuery +
			"  )) AND " +
			// exclude results that are ineffective to control
			"  (ineffective == 'experimental' OR ineffective IS NULL) AND " +
			where,
			processor
		);
	}
	
	public List<Map<String, Object>> queryAllArticles() {
		return queryAll(
			"SELECT ref_name, doi, url, first_author, year " +
			"FROM articles",
			rs -> {
				try {
					Map<String, Object> result = new LinkedHashMap<>();
					result.put("refName", rs.getString("ref_name"));
					result.put("doi", rs.getString("doi"));
					result.put("url", rs.getString("url"));
					result.put("firstAuthor", rs.getString("first_author"));
					result.put("year", rs.getInt("year"));
					return result;
				}
				catch(SQLException e) {
					throw new RuntimeException(e);
				}
			}
		);
	}
	
	public Set<MutationSet<SARS2>> queryAllKeyMutations() {
		if (allKeyMutations == null) {
			allKeyMutations = Collections.unmodifiableSet(new TreeSet<>(queryAll(
				"SELECT gene, position, position_end, amino_acid FROM key_mutations",
				rs -> {
					try {
						Gene<SARS2> gene = virusIns.getMainStrain().getGene(rs.getString("gene"));
						int position = rs.getInt("position");
						int posEnd = rs.getInt("position_end");
						char[] aminoAcid = rs.getString("amino_acid")
							.replaceAll("^del$", "-")
							.replaceAll("^ins$", "_")
							.replaceAll("^stop$", "*")
							.toCharArray();
						if (posEnd == 0) {
							posEnd = position;
						}
						List<Mutation<SARS2>> mutations = new ArrayList<>();
						for (int pos = position; pos <= posEnd; pos ++) {
							mutations.add(new AAMutation<>(gene, pos, aminoAcid));
						}
						return new MutationSet<>(mutations);

					}
					catch (SQLException e) {
						throw new RuntimeException(e);
					}
				}
			)));
		}
		return allKeyMutations;
	}
	
	public List<Map<String, Object>> queryAllVirusVariants() {
		List<Map<String, Object>> variants = queryAll(
			"SELECT variant_name, display_name FROM virus_variants",
			rs -> {
				try {
					Map<String, Object> result = new LinkedHashMap<>();
					result.put("variantName", rs.getString("variant_name"));
					result.put("displayName", rs.getString("display_name"));
					return result;
				}
				catch(SQLException e) {
					throw new RuntimeException(e);
				}
			}
		);
		SARS2 sars2 = SARS2.getInstance();
		Map<String, List<Pair<String, Mutation<SARS2>>>> mutations = groupAll(
			"SELECT variant_name, gene, position, amino_acid " +
			"FROM variant_mutations WHERE gene='S'",
			r -> r.getLeft(),
			rs -> {
				try {
					return Pair.of(
						rs.getString("variant_name"),
						new AAMutation<>(
							sars2.getMainStrain().getGene(rs.getString("gene")),
							rs.getInt("position"),
							(rs.getString("amino_acid")
							 .replaceAll("^del$", "-")
							 .replaceAll("^ins$", "_")
							 .replaceAll("^stop$", "*")
							 .toCharArray())
						)
					);
				}
				catch(SQLException e) {
					throw new RuntimeException(e);
				}
			}
		);
		for (Map<String, Object> variant : variants) {
			variant.put(
				"mutations",
				mutations.getOrDefault(
					(String) variant.get("variantName"),
					Collections.emptyList()
				)
				.stream()
				.map(r -> r.getRight())
				.collect(Collectors.toList())
			);
		}
		return variants;
	}
	
	public List<Map<String, Object>> queryAllAntibodies() {
		List<Map<String, Object>> antibodies = queryAll(
			"SELECT ab_name, abbreviation_name, availability, priority, visibility " +
			"FROM antibodies",
			rs -> {
				try {
					Map<String, Object> result = new LinkedHashMap<>();
					result.put("abName", rs.getString("ab_name"));
					result.put("abbrName", rs.getString("abbreviation_name"));
					result.put("availability", rs.getString("availability"));
					result.put("priority", rs.getInt("priority"));
					result.put("visibility", rs.getInt("visibility") == 1);
					return result;
				}
				catch (SQLException e) {
					throw new RuntimeException(e);
				}
			}
		);
		Map<String, Map<String, Object>> targets = queryAll(
			"SELECT ab_name, target, class " +
			"FROM antibody_targets WHERE source='structure'",
			r -> (String) r.get("abName"),
			rs -> {
				try {
					Map<String, Object> result = new LinkedHashMap<>();
					result.put("abName", rs.getString("ab_name"));
					result.put("abTarget", rs.getString("target"));
					result.put("abClass", rs.getString("class"));
					return result;
				}
				catch (SQLException e) {
					throw new RuntimeException(e);
				}
			}
		);
		Map<String, List<Map<String, Object>>> synonyms = groupAll(
			"SELECT ab_name, synonym " +
			"FROM antibody_synonyms",
			r -> (String) r.get("abName"),
			rs -> {
				try {
					Map<String, Object> result = new LinkedHashMap<>();
					result.put("abName", rs.getString("ab_name"));
					result.put("synonym", rs.getString("synonym"));
					return result;
				}
				catch (SQLException e) {
					throw new RuntimeException(e);
				}
			}
		);
		for (Map<String, Object> ab : antibodies) {
			ab.put(
				"target", 
				targets.getOrDefault(
					ab.get("abName"), Collections.emptyMap()
				)
			);
			ab.put(
				"synonyms",
				synonyms.getOrDefault(
					ab.get("abName"), Collections.emptyList()
				)
			);
		}
		return antibodies;
	}
	
	public List<Map<String, Object>> querySuscResultsForAntibodies(MutationSet<SARS2> mutations) {
		List<Map<String, Object>> results = querySuscResults(
			mutations,
			
			/* columns = */
			"S.ref_name, " +
			"A.doi, " +
			"A.url, " +
			"S.rx_name, " +
			"S.control_variant_name, " +
			"S.variant_name, " +
			"ordinal_number, " +
			"assay, " +
			"section, " +
			"fold_cmp, " +
			"fold, " +
			"S.ineffective, " +
			"resistance_level, " +
			"cumulative_count, " +

			"(SELECT GROUP_CONCAT(RXMAB.ab_name, '" + LIST_JOIN_UNIQ + "') " +
			"  FROM rx_antibodies RXMAB" +
			"  WHERE S.ref_name = RXMAB.ref_name AND S.rx_name = RXMAB.rx_name" +
			"  ORDER BY RXMAB.ab_name" +
			") AS ab_names",

			/* joins = */
			"JOIN articles A ON S.ref_name = A.ref_name",

			/* where = */
			"EXISTS(" +
			"  SELECT 1 FROM rx_antibodies RXMAB" +
			"  WHERE S.ref_name = RXMAB.ref_name AND S.rx_name = RXMAB.rx_name" +
			")",
			/* processor = */
			rs -> {
				try {
					Map<String, Object> result = new LinkedHashMap<>();
					String foldCmp = rs.getString("fold_cmp");
					Double fold = rs.getDouble("fold");
					result.put("refName", rs.getString("ref_name"));
					result.put("refDOI", rs.getString("doi"));
					result.put("refURL", rs.getString("url"));
					result.put("rxName", rs.getString("rx_name"));
					result.put("abNames", Set.of(rs.getString("ab_names").split(QUOTED_LIST_JOIN_UNIQ)));
					result.put("controlVariantName", rs.getString("control_variant_name"));
					result.put("variantName", rs.getString("variant_name"));
					result.put("assay", rs.getString("assay"));
					result.put("section", rs.getString("section"));
					result.put("ordinalNumber", rs.getInt("ordinal_number"));
					result.put("foldCmp", foldCmp);
					result.put("fold", fold);
					result.put("ineffective", rs.getString("ineffective"));
					result.put("fbResistanceLevel", rs.getString("resistance_level"));
					result.put("cumulativeCount", rs.getInt("cumulative_count"));
					return result;
				}
				catch (SQLException e) {
					throw new RuntimeException(e);
				}
			}
		);
		return results;
	}

	public List<Map<String, Object>> querySuscResultsForConvPlasma(MutationSet<SARS2> mutations) {
		List<Map<String, Object>> results = querySuscResults(
			mutations,
			
			/* columns = */
			"S.ref_name, " +
			"A.doi, " +
			"A.url, " +
			"S.rx_name, " +
			"S.control_variant_name, " +
			"S.variant_name, " +
			"ordinal_number, " +
			"assay, " +
			"section, " +
			"fold_cmp, " +
			"fold, " +
			"S.ineffective, " +
			"resistance_level, " +
			"cumulative_count, " +
			"RXCP.infection, " +
			"RXCP.cumulative_group ",
			
			/* joins = */
			"JOIN rx_conv_plasma RXCP ON S.ref_name = RXCP.ref_name AND S.rx_name = RXCP.rx_name " +
			"JOIN articles A ON S.ref_name = A.ref_name",

			/* where = */
			"TRUE",
			/* processor = */
			rs -> {
				try {
					Map<String, Object> result = new LinkedHashMap<>();
					String foldCmp = rs.getString("fold_cmp");
					Double fold = rs.getDouble("fold");
					result.put("refName", rs.getString("ref_name"));
					result.put("refDOI", rs.getString("doi"));
					result.put("refURL", rs.getString("url"));
					result.put("rxName", rs.getString("rx_name"));
					result.put("controlVariantName", rs.getString("control_variant_name"));
					result.put("variantName", rs.getString("variant_name"));
					result.put("assay", rs.getString("assay"));
					result.put("section", rs.getString("section"));
					result.put("ordinalNumber", rs.getInt("ordinal_number"));
					result.put("foldCmp", foldCmp);
					result.put("fold", fold);
					result.put("ineffective", rs.getString("ineffective"));
					result.put("fbResistanceLevel", rs.getString("resistance_level"));
					result.put("cumulativeCount", rs.getInt("cumulative_count"));
					result.put("infection", rs.getString("infection"));
					result.put("cumulativeGroup", rs.getString("cumulative_group"));
					return result;
				}
				catch (SQLException e) {
					throw new RuntimeException(e);
				}
			}
		);
		return results;
	}

	public List<Map<String, Object>> querySuscResultsForImmuPlasma(MutationSet<SARS2> mutations) {
		List<Map<String, Object>> results = querySuscResults(
			mutations,
			
			/* columns = */
			"S.ref_name, " +
			"A.doi, " +
			"A.url, " +
			"S.rx_name, " +
			"S.control_variant_name, " +
			"S.variant_name, " +
			"assay, " +
			"section, " +
			"ordinal_number, " +
			"fold_cmp, " +
			"fold, " +
			"S.ineffective, " +
			"resistance_level, " +
			"cumulative_count, " +
			"RXIP.cumulative_group, " +
			"RXIP.vaccine_name ",
			
			/* joins = */
			"JOIN rx_immu_plasma RXIP ON S.ref_name = RXIP.ref_name AND S.rx_name = RXIP.rx_name " +
			"JOIN articles A ON S.ref_name = A.ref_name",

			/* where = */
			"TRUE",
			/* processor = */
			rs -> {
				try {
					Map<String, Object> result = new LinkedHashMap<>();
					String foldCmp = rs.getString("fold_cmp");
					Double fold = rs.getDouble("fold");
					result.put("refName", rs.getString("ref_name"));
					result.put("refDOI", rs.getString("doi"));
					result.put("refURL", rs.getString("url"));
					result.put("rxName", rs.getString("rx_name"));
					result.put("vaccineName", rs.getString("vaccine_name"));
					result.put("controlVariantName", rs.getString("control_variant_name"));
					result.put("variantName", rs.getString("variant_name"));
					result.put("assay", rs.getString("assay"));
					result.put("section", rs.getString("section"));
					result.put("ordinalNumber", rs.getInt("ordinal_number"));
					result.put("foldCmp", foldCmp);
					result.put("fold", fold);
					result.put("ineffective", rs.getString("ineffective"));
					result.put("fbResistanceLevel", rs.getString("resistance_level"));
					result.put("cumulativeCount", rs.getInt("cumulative_count"));
					result.put("cumulativeGroup", rs.getString("cumulative_group"));
					return result;
				}
				catch (SQLException e) {
					throw new RuntimeException(e);
				}
			}
		);
		return results;
	}

}
