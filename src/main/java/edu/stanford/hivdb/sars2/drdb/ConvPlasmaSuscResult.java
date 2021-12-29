package edu.stanford.hivdb.sars2.drdb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import edu.stanford.hivdb.mutations.MutationSet;
import edu.stanford.hivdb.sars2.SARS2;

public class ConvPlasmaSuscResult extends SuscResult {

	private static final Map<String, Map<MutationSet<SARS2>, List<ConvPlasmaSuscResult>>> singletons = new HashMap<>();

	private final String infectedIsoName;
	private final String cumulativeGroup;

	private static void prepareSingletons(String drdbVersion) {
		if (singletons.containsKey(drdbVersion)) {
			return;
		}
		DRDB drdb = DRDB.getInstance(drdbVersion);
		Map<MutationSet<SARS2>, List<ConvPlasmaSuscResult>> allSuscResults = (
			drdb
			.queryAllSuscResultsForConvPlasma()
			.stream()
			.map(d -> new ConvPlasmaSuscResult(drdbVersion, d))
			.collect(Collectors.groupingBy(d -> d.getComparableIsolateMutations()))
		);
		singletons.put(drdbVersion, allSuscResults);
	}
	
	public static List<BoundSuscResult> query(String drdbVersion, MutationSet<SARS2> queryMuts) {
		prepareSingletons(drdbVersion);
		final MutationSet<SARS2> finalQueryMuts = prepareQueryMutations(queryMuts);
		
		List<BoundSuscResult> results = new ArrayList<>();
		for (Entry<MutationSet<SARS2>, List<ConvPlasmaSuscResult>> pair : singletons.get(drdbVersion).entrySet()) {
				IsolateMatchType matchType = SuscResult.calcMatchType(pair.getKey(), finalQueryMuts); 
				if (matchType == IsolateMatchType.MISMATCH) {
					continue;
				}
				for (SuscResult sr : pair.getValue()) {
					results.add(new BoundSuscResult(matchType, finalQueryMuts, sr));
				}
		}
		return results;
	}
	
	private ConvPlasmaSuscResult(
		String drdbVersion,
		Map<String, Object> suscData
	) {
		super(drdbVersion, suscData);

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
