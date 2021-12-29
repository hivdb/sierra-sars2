package edu.stanford.hivdb.sars2.drdb;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import edu.stanford.hivdb.mutations.MutationSet;
import edu.stanford.hivdb.sars2.SARS2;

public class AntibodySuscResult extends SuscResult {

	private static final Map<String, Map<MutationSet<SARS2>, List<AntibodySuscResult>>> singletons = new HashMap<>();

	private final String drdbVersion;
	private final Set<String> abNames;
	
	private transient Set<Antibody> antibodies;

	private static void prepareSingletons(String drdbVersion) {
		if (singletons.containsKey(drdbVersion)) {
			return;
		}
		DRDB drdb = DRDB.getInstance(drdbVersion);
		Map<MutationSet<SARS2>, List<AntibodySuscResult>> allSuscResults = (
			drdb
			.queryAllSuscResultsForAntibodies()
			.stream()
			.map(d -> new AntibodySuscResult(drdbVersion, d))
			.collect(Collectors.groupingBy(d -> d.getComparableIsolateMutations()))
		);
		singletons.put(drdbVersion, allSuscResults);
	}
	
	public static List<BoundSuscResult> query(String drdbVersion, MutationSet<SARS2> queryMuts) {
		prepareSingletons(drdbVersion);
		final MutationSet<SARS2> finalQueryMuts = prepareQueryMutations(queryMuts);
		
		List<BoundSuscResult> results = new ArrayList<>();
		for (Entry<MutationSet<SARS2>, List<AntibodySuscResult>> pair : singletons.get(drdbVersion).entrySet()) {
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
	
	private AntibodySuscResult(
		String drdbVersion,
		Map<String, Object> suscData
	) {
		super(drdbVersion, suscData);

		@SuppressWarnings("unchecked")
		Set<String> abNames = (Set<String>) suscData.get("abNames");
		this.abNames = abNames;
		this.drdbVersion = drdbVersion;

	}

	public Set<Antibody> getAntibodies() {
		if (antibodies == null) {
			antibodies = (
				abNames.stream()
				.map(abName -> Antibody.getInstance(drdbVersion, abName))
				.collect(Collectors.toSet())
			);
		}
		return antibodies;
	}

}
