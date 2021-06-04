package edu.stanford.hivdb.sars2.drdb;

import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import edu.stanford.hivdb.mutations.MutationSet;
import edu.stanford.hivdb.sars2.SARS2;

public class AntibodySuscResult extends SuscResult {

	private static final Map<String, List<Map<String, Object>>> rawResults = new HashMap<>();

	private final String drdbVersion;
	private final Set<String> abNames;
	
	private transient Set<Antibody> antibodies;

	private static void prepareRawResults(String drdbVersion) {
		if (rawResults.containsKey(drdbVersion)) {
			return;
		}
		DRDB drdb = DRDB.getInstance(drdbVersion);
		List<Map<String, Object>> allSuscResults = (
			drdb
			.queryAllSuscResultsForAntibodies()
		);
		rawResults.put(drdbVersion, allSuscResults);
	}
	
	
	public static List<AntibodySuscResult> query(String drdbVersion, MutationSet<SARS2> queryMuts) {
		prepareRawResults(drdbVersion);
		final MutationSet<SARS2> finalQueryMuts = prepareQueryMutations(queryMuts);
		List<AntibodySuscResult> results = (
			rawResults.get(drdbVersion)
			.stream()
			.map(d -> new AntibodySuscResult(drdbVersion, finalQueryMuts, d))
			.filter(d -> d.getMatchType() != IsolateMatchType.MISMATCH)
			.collect(Collectors.toList())
		);
		
		return results;
	}
	
	private AntibodySuscResult(
		String drdbVersion,
		MutationSet<SARS2> queryMuts,
		Map<String, Object> suscData
	) {
		super(drdbVersion, queryMuts, suscData);

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
