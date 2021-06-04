package edu.stanford.hivdb.sars2.drdb;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import edu.stanford.hivdb.mutations.MutationSet;
import edu.stanford.hivdb.sars2.SARS2;

public class ConvPlasmaSuscResult extends SuscResult {

	private static final Map<String, List<Map<String, Object>>> rawResults = new HashMap<>();

	private final String infectedIsoName;
	private final String cumulativeGroup;

	private static void prepareRawResults(String drdbVersion) {
		if (rawResults.containsKey(drdbVersion)) {
			return;
		}
		DRDB drdb = DRDB.getInstance(drdbVersion);
		List<Map<String, Object>> allSuscResults = (
			drdb
			.queryAllSuscResultsForConvPlasma()
		);
		rawResults.put(drdbVersion, allSuscResults);
	}
	
	public static List<ConvPlasmaSuscResult> query(String drdbVersion, MutationSet<SARS2> queryMuts) {
		prepareRawResults(drdbVersion);
		final MutationSet<SARS2> finalQueryMuts = prepareQueryMutations(queryMuts);
		List<ConvPlasmaSuscResult> results = (
			rawResults.get(drdbVersion)
			.stream()
			.map(d -> new ConvPlasmaSuscResult(drdbVersion, finalQueryMuts, d))
			.filter(d -> d.getMatchType() != IsolateMatchType.MISMATCH)
			.collect(Collectors.toList())
		);
		
		return results;
	}
	
	private ConvPlasmaSuscResult(
		String drdbVersion,
		MutationSet<SARS2> queryMuts,
		Map<String, Object> suscData
	) {
		super(drdbVersion, queryMuts, suscData);

		this.infectedIsoName = (String) suscData.get("infection");
		this.cumulativeGroup = (String) suscData.get("cumulativeGroup");
	}
	
	public String getInfectedIsoName() {
		return infectedIsoName;
	}
	
	public String getCumulativeGroup() {
		return cumulativeGroup;
	}

}
