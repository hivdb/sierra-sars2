package edu.stanford.hivdb.sars2.drdb;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
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
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import org.apache.commons.collections4.map.LRUMap;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.tuple.Pair;

import edu.stanford.hivdb.mutations.AAMutation;
import edu.stanford.hivdb.mutations.Mutation;
import edu.stanford.hivdb.sars2.SARS2;

public class DRDB {
	
	private static final int MAX_ENTRIES = 20;
	private static final String LIST_JOIN_UNIQ = "$#\u0008#$";
	private static final String QUOTED_LIST_JOIN_UNIQ = Pattern.quote(LIST_JOIN_UNIQ);
	private static final String COVID_DRDB_RESURL_PREFIX;
	
	static {
		COVID_DRDB_RESURL_PREFIX = "https://s3-us-west-2.amazonaws.com/cms.hivdb.org/covid-drdb";
	}

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
		String resourcePath = String.format("%s/covid-drdb-%s.db", COVID_DRDB_RESURL_PREFIX, version);
		if (!singletons.containsKey(resourcePath)) {
			DRDB instance = new DRDB(resourcePath);
			singletons.put(resourcePath, instance);
		}
		return singletons.get(resourcePath);
	}
	
	private final Connection conn;
	private String lastUpdate;
	
	private DRDB(String resourcePath) {
		try {
			Class.forName("org.sqlite.JDBC");
		} catch (ClassNotFoundException e1) {
			throw new RuntimeException(e1);
		}
		File targetDir = new File("/tmp/drdb-payload");
		if (!targetDir.exists()) {
			targetDir.mkdirs();
		}
    File targetFile = new File("/tmp/drdb-payload/" + FilenameUtils.getName(resourcePath));
		try {
			URL url = new URL(resourcePath);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestProperty("Accept-Encoding", "gzip, deflate");
			InputStream dbStream;
      if ("gzip".equals(conn.getContentEncoding())) {
         dbStream = new GZIPInputStream(conn.getInputStream());
      }
      else {
         dbStream = conn.getInputStream();
      }
      Files.copy(dbStream, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		try {
			conn = DriverManager.getConnection("jdbc:sqlite:" + targetFile.getAbsolutePath());
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

	protected <T> List<T> queryAllSuscResults(
		String columns,
		String joins,
		String where,
		Function<ResultSet, T> processor
	) {
		return queryAll(
			"SELECT " +
			columns +
			"  FROM susc_results S " +
			joins +
			"  WHERE " +
			// exclude results of SARS-CoV, WIV1, B and B.1
			"  NOT EXISTS (" +
			"    SELECT 1 FROM isolates I " +
			"    WHERE S.iso_name=I.iso_name AND I.var_name IN ('SARS-CoV', 'WIV1', 'B', 'B.1')" +
			"  ) AND " +
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
	
	public List<Map<String, Object>> queryAllVariants() {
		List<Map<String, Object>> variants = queryAll(
			"SELECT var_name, as_wildtype FROM variants",
			rs -> {
				try {
					Map<String, Object> result = new LinkedHashMap<>();
					result.put("varName", rs.getString("var_name"));
					result.put("asWildtype", rs.getBoolean("as_wildtype"));
					return result;
				}
				catch (SQLException e) {
					throw new RuntimeException(e);
				}
			}
		);
		return variants;
	}
	
	public List<Map<String, Object>> queryAllIsolates() {
		List<Map<String, Object>> isolates = queryAll(
			"SELECT iso_name, var_name FROM isolates",
			rs -> {
				try {
					Map<String, Object> result = new LinkedHashMap<>();
					result.put("isoName", rs.getString("iso_name"));
					result.put("varName", rs.getString("var_name"));
					return result;
				}
				catch(SQLException e) {
					throw new RuntimeException(e);
				}
			}
		);
		SARS2 sars2 = SARS2.getInstance();
		Map<String, List<Pair<String, Mutation<SARS2>>>> mutations = groupAll(
			"SELECT iso_name, gene, position, amino_acid " +
			"FROM isolate_mutations WHERE gene='S'",
			r -> r.getLeft(),
			rs -> {
				try {
					return Pair.of(
						rs.getString("iso_name"),
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
		for (Map<String, Object> isolate : isolates) {
			isolate.put(
				"mutations",
				mutations.getOrDefault(
					(String) isolate.get("isoName"),
					Collections.emptyList()
				)
				.stream()
				.map(r -> r.getRight())
				.collect(Collectors.toList())
			);
		}
		return isolates;
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
	
	public List<Map<String, Object>> queryAllSuscResultsForAntibodies() {
		List<Map<String, Object>> results = queryAllSuscResults(
			/* columns = */
			"S.ref_name, " +
			"A.doi, " +
			"A.url, " +
			"S.rx_name, " +
			"S.control_iso_name, " +
			"S.iso_name, " +
			"assay_name, " +
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
			"  SELECT 1 FROM rx_antibodies RXMAB, antibodies MAB" +
			"  WHERE" +
			"    S.ref_name = RXMAB.ref_name AND" +
			"    S.rx_name = RXMAB.rx_name AND" +
			"    RXMAB.ab_name = MAB.ab_name AND" +
			"    MAB.visibility = 1" +
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
					result.put("controlIsoName", rs.getString("control_iso_name"));
					result.put("isoName", rs.getString("iso_name"));
					result.put("assayName", rs.getString("assay_name"));
					result.put("section", rs.getString("section"));
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

	public List<Map<String, Object>> queryAllSuscResultsForConvPlasma() {
		List<Map<String, Object>> results = queryAllSuscResults(
			/* columns = */
			"S.ref_name, " +
			"A.doi, " +
			"A.url, " +
			"S.rx_name, " +
			"S.control_iso_name, " +
			"S.iso_name, " +
			"assay_name, " +
			"section, " +
			"fold_cmp, " +
			"fold, " +
			"S.ineffective, " +
			"resistance_level, " +
			"cumulative_count, " +
			"RXCP.infected_var_name, " +
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
					result.put("controlIsoName", rs.getString("control_iso_name"));
					result.put("isoName", rs.getString("iso_name"));
					result.put("assayName", rs.getString("assay_name"));
					result.put("section", rs.getString("section"));
					result.put("foldCmp", foldCmp);
					result.put("fold", fold);
					result.put("ineffective", rs.getString("ineffective"));
					result.put("fbResistanceLevel", rs.getString("resistance_level"));
					result.put("cumulativeCount", rs.getInt("cumulative_count"));
					result.put("infectedVarName", rs.getString("infected_var_name"));
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
	
	public String queryLastUpdate() {
		if (this.lastUpdate == null) {
			this.lastUpdate = queryAll(
				"SELECT last_update FROM last_update WHERE scope='global'",
				rs -> {
					try {
						return rs.getString("last_update");
					}
					catch (SQLException e) {
						throw new RuntimeException(e);
					}
				}
			).get(0);
		}
		return this.lastUpdate;
	}

	public List<Map<String, Object>> queryAllSuscResultsForVaccPlasma() {
		List<Map<String, Object>> results = queryAllSuscResults(
			/* columns = */
			"S.ref_name, " +
			"A.doi, " +
			"A.url, " +
			"S.rx_name, " +
			"S.control_iso_name, " +
			"S.iso_name, " +
			"assay_name, " +
			"section, " +
			"fold_cmp, " +
			"fold, " +
			"S.ineffective, " +
			"resistance_level, " +
			"cumulative_count, " +
			"RXVP.cumulative_group, " +
			"RXVP.vaccine_name, " +
			"V.priority AS vaccine_priority, " +
			"V.vaccine_type",
			
			/* joins = */
			"JOIN rx_vacc_plasma RXVP ON S.ref_name = RXVP.ref_name AND S.rx_name = RXVP.rx_name " +
			"JOIN articles A ON S.ref_name = A.ref_name " +
			"JOIN vaccines V ON RXVP.vaccine_name = V.vaccine_name",

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
					result.put("vaccinePriority", rs.getInt("vaccine_priority"));
					result.put("vaccineType", rs.getString("vaccine_type"));
					result.put("controlIsoName", rs.getString("control_iso_name"));
					result.put("isoName", rs.getString("iso_name"));
					result.put("assayName", rs.getString("assay_name"));
					result.put("section", rs.getString("section"));
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
