package edu.stanford.hivdb.sars2;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import edu.stanford.hivdb.mutations.MutationSet;
import edu.stanford.hivdb.viruses.Gene;

public class DRDB {
	
	private static final double PARTIAL_RESIST_FOLD = 3;
	private static final double RESIST_FOLD = 10;
	private static final String LIST_JOIN_UNIQ = "$#\u0008#$";
	private static final String QUOTED_LIST_JOIN_UNIQ = Pattern.quote(LIST_JOIN_UNIQ);
	
	private final SARS2 virusIns;
	private final Connection conn;
	
	public DRDB(String resourcePath) {
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
		try (
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(
				"SELECT " +
				columns +
				// "  ref_name, rx_name, strain_name, fold_cmp, " +
				// "  fold, cumulative_count, date_added " +
				"  FROM susc_results S " +
				joins +
				"  WHERE EXISTS(" +
				"    SELECT 1 FROM strain_mutations M " +
				"    WHERE S.strain_name = M.strain_name AND (" +
				genePosQuery +
				"  )) AND " +
				where
			)
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
	
	public List<Map<String, ?>> querySuscResultsForAntibodies(MutationSet<SARS2> mutations) {
		List<Map<String, ?>> results = querySuscResults(
			mutations,
			
			/* columns = */
			"S.ref_name, " +
			"A.doi, " +
			"A.url, " +
			"S.rx_name, " +
			"S.control_strain_name, " +
			"S.strain_name, " +
			"ordinal_number, " +
			"section, " +
			"fold_cmp, " +
			"fold, " +
			"S.ineffective, " +
			"resistance_level, " +
			"cumulative_count, " +

			"(SELECT GROUP_CONCAT(RXMAB.ab_name, '" + LIST_JOIN_UNIQ + "') " +
			"  FROM rx_antibodies RXMAB" +
			"  WHERE S.ref_name = RXMAb.ref_name AND S.rx_name = RXMAB.rx_name" +
			"  ORDER BY RXMAB.ab_name" +
			") AS ab_names, " +
			
			"(SELECT GROUP_CONCAT(SMUT.gene || ':' || SMUT.position || SMUT.amino_acid, '" + LIST_JOIN_UNIQ + "') " +
			"  FROM strain_mutations SMUT" +
			"  WHERE S.strain_name = SMUT.strain_name" +
			"  ORDER BY SMUT.gene, SMUT.position, SMUT.amino_acid" +
			") AS mutations",
			
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
					String fbLevel = rs.getString("resistance_level");
					result.put("refName", rs.getString("ref_name"));
					result.put("refDOI", rs.getString("doi"));
					result.put("refURL", rs.getString("url"));
					result.put("rxName", rs.getString("rx_name"));
					result.put("antibodies", rs.getString("ab_names").split(QUOTED_LIST_JOIN_UNIQ));
					result.put("controlStrainName", rs.getString("control_strain_name"));
					result.put("strainName", rs.getString("strain_name"));
					result.put("mutations", rs.getString("mutations").split(QUOTED_LIST_JOIN_UNIQ));
					result.put("section", rs.getString("section"));
					result.put("ordinalNumber", rs.getInt("ordinal_number"));
					result.put("foldCmp", foldCmp);
					result.put("fold", fold);
					result.put("ineffective", rs.getString("ineffective"));
					result.put("resistanceLevel", calcResistanceLevel(foldCmp, fold, fbLevel));
					result.put("cumulativeCount", rs.getString("cumulative_count"));
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
			"S.control_strain_name, " +
			"S.strain_name, " +
			"section, " +
			"ordinal_number, " +
			"fold_cmp, " +
			"fold, " +
			"S.ineffective, " +
			"resistance_level, " +
			"cumulative_count, " +
			"RXCP.cumulative_group, " +
			
			"(SELECT GROUP_CONCAT(SMUT.gene || ':' || SMUT.position || SMUT.amino_acid, '" + LIST_JOIN_UNIQ + "') " +
			"  FROM strain_mutations SMUT" +
			"  WHERE S.strain_name = SMUT.strain_name" +
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
					result.put("controlStrainName", rs.getString("control_strain_name"));
					result.put("strainName", rs.getString("strain_name"));
					result.put("mutations", rs.getString("mutations").split(QUOTED_LIST_JOIN_UNIQ));
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
			"S.control_strain_name, " +
			"S.strain_name, " +
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
			"  FROM strain_mutations SMUT" +
			"  WHERE S.strain_name = SMUT.strain_name" +
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
					result.put("controlStrainName", rs.getString("control_strain_name"));
					result.put("strainName", rs.getString("strain_name"));
					result.put("mutations", rs.getString("mutations").split(QUOTED_LIST_JOIN_UNIQ));
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
