package edu.stanford.hivdb.sars2;

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
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.collections4.map.LRUMap;
import org.apache.commons.lang3.tuple.Pair;

import edu.stanford.hivdb.mutations.AAMutation;
import edu.stanford.hivdb.mutations.Mutation;
import edu.stanford.hivdb.mutations.MutationSet;
import edu.stanford.hivdb.viruses.Gene;

public class DRDB {
	
	private static final int MAX_ENTRIES = 20;
	private static final double PARTIAL_RESIST_FOLD = 3;
	private static final double RESIST_FOLD = 10;
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
		Map<Gene<SARS2>, MutationSet<SARS2>> mutsByGene = mutations.groupByGene();
		String genePosQuery = (
			mutsByGene.entrySet()
			.stream()
			//.filter(e -> e.getValue().size() > 0)
			.filter(e -> (
				// resistance data are currently only available for Spike gene
				e.getKey() == virusIns.getGene("SARS2S") &&
				e.getValue().size() > 0
			))
			.map(e -> String.format(
				"(M.gene = '%s' AND M.position IN (%s))",
				e.getKey().getAbstractGene().replace("'", "''"),
				e.getValue()
				.getPositions()
				.stream()
				.map(gpos -> String.valueOf(gpos.getPosition()))
				.collect(Collectors.joining(","))
			))
			.collect(Collectors.joining(" OR "))
		);
		if (genePosQuery.length() == 0) {
			genePosQuery = "false";
		}
		return queryAll(
			"SELECT " +
			columns +
			// "  ref_name, rx_name, variant_name, fold_cmp, " +
			// "  fold, cumulative_count, date_added " +
			"  FROM susc_results S " +
			joins +
			"  WHERE EXISTS(" +
			"    SELECT 1 FROM variant_mutations M " +
			"    WHERE S.variant_name = M.variant_name AND (" +
			genePosQuery +
			"  )) AND " +
			// exclude results that are ineffective to control
			"  (ineffective == 'experimental' OR ineffective IS NULL) AND " +
			where,
			processor
		);
	}
	
	private String calcResistanceLevel(String foldCmp, Double fold, String fallbackLevel) {
		if (fallbackLevel == null) {
			if (foldCmp.equals("<") || foldCmp.equals("~") || foldCmp.equals("=")) {
				if (fold <= PARTIAL_RESIST_FOLD) {
					return "susceptible";
				}
				else if (fold <= RESIST_FOLD) {
					return "partial-resistance";
				}
				else {
					return "resistance";
				}
			}
			else {  // foldCmp.equals(">")
				if (fold > RESIST_FOLD) {
					return "resistance";
				}
				else if (fold > PARTIAL_RESIST_FOLD) {
					return "gt-partial-resistance";
				}
				else {
					return "gt-susceptible";
				}
			}
		}
		else {
			return fallbackLevel;
		}
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
	
	public List<Map<String, Object>> queryAllVirusVariants() {
		List<Map<String, Object>> variants = queryAll(
			"SELECT variant_name FROM virus_variants",
			rs -> {
				try {
					Map<String, Object> result = new LinkedHashMap<>();
					result.put("variantName", rs.getString("variant_name"));
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
			"SELECT ab_name, pdb_id, abbreviation_name, availability " +
			"FROM antibodies",
			rs -> {
				try {
					Map<String, Object> result = new LinkedHashMap<>();
					result.put("abName", rs.getString("ab_name"));
					result.put("pdbID", rs.getString("pdb_id"));
					result.put("abbrName", rs.getString("abbreviation_name"));
					result.put("availability", rs.getString("availability"));
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
					result.put("abNames", List.of(rs.getString("ab_names").split(QUOTED_LIST_JOIN_UNIQ)));
					result.put("controlVariantName", rs.getString("control_variant_name"));
					result.put("variantName", rs.getString("variant_name"));
					result.put("assay", rs.getString("assay"));
					result.put("section", rs.getString("section"));
					result.put("ordinalNumber", rs.getInt("ordinal_number"));
					result.put("foldCmp", foldCmp);
					result.put("fold", fold);
					result.put("ineffective", rs.getString("ineffective"));
					result.put("fbResistanceLevel", rs.getString("resistance_level"));
					// result.put("resistanceLevel", calcResistanceLevel(foldCmp, fold, fbLevel));
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

	public List<Map<String, ?>> querySuscResultsForConvPlasma(MutationSet<SARS2> mutations) {
		List<Map<String, ?>> results = querySuscResults(
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
			"RXCP.cumulative_group, " +
			
			"(SELECT GROUP_CONCAT(SMUT.gene || ':' || SMUT.position || SMUT.amino_acid, '" + LIST_JOIN_UNIQ + "') " +
			"  FROM variant_mutations SMUT" +
			"  WHERE S.variant_name = SMUT.variant_name" +
			"  ORDER BY SMUT.gene, SMUT.position, SMUT.amino_acid" +
			") AS mutations",
			
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
					String fbLevel = rs.getString("resistance_level");
					result.put("refName", rs.getString("ref_name"));
					result.put("refDOI", rs.getString("doi"));
					result.put("refURL", rs.getString("url"));
					result.put("rxName", rs.getString("rx_name"));
					result.put("controlVariantName", rs.getString("control_variant_name"));
					result.put("variantName", rs.getString("variant_name"));
					result.put("mutations", rs.getString("mutations").split(QUOTED_LIST_JOIN_UNIQ));
					result.put("assay", rs.getString("assay"));
					result.put("section", rs.getString("section"));
					result.put("ordinalNumber", rs.getString("ordinal_number"));
					result.put("foldCmp", foldCmp);
					result.put("fold", fold);
					result.put("ineffective", rs.getString("ineffective"));
					result.put("resistanceLevel", calcResistanceLevel(foldCmp, fold, fbLevel));
					result.put("cumulativeCount", rs.getString("cumulative_count"));
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

	public List<Map<String, ?>> querySuscResultsForImmuPlasma(MutationSet<SARS2> mutations) {
		List<Map<String, ?>> results = querySuscResults(
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
			"RXIP.vaccine_name, " +
			
			"(SELECT GROUP_CONCAT(SMUT.gene || ':' || SMUT.position || SMUT.amino_acid, '" + LIST_JOIN_UNIQ + "') " +
			"  FROM variant_mutations SMUT" +
			"  WHERE S.variant_name = SMUT.variant_name" +
			"  ORDER BY SMUT.gene, SMUT.position, SMUT.amino_acid" +
			") AS mutations",
			
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
					String fbLevel = rs.getString("resistance_level");
					result.put("refName", rs.getString("ref_name"));
					result.put("refDOI", rs.getString("doi"));
					result.put("refURL", rs.getString("url"));
					result.put("rxName", rs.getString("rx_name"));
					result.put("vaccineName", rs.getString("vaccine_name"));
					result.put("controlVariantName", rs.getString("control_variant_name"));
					result.put("variantName", rs.getString("variant_name"));
					result.put("mutations", rs.getString("mutations").split(QUOTED_LIST_JOIN_UNIQ));
					result.put("assay", rs.getString("assay"));
					result.put("section", rs.getString("section"));
					result.put("ordinalNumber", rs.getString("ordinal_number"));
					result.put("foldCmp", foldCmp);
					result.put("fold", fold);
					result.put("ineffective", rs.getString("ineffective"));
					result.put("resistanceLevel", calcResistanceLevel(foldCmp, fold, fbLevel));
					result.put("cumulativeCount", rs.getString("cumulative_count"));
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
