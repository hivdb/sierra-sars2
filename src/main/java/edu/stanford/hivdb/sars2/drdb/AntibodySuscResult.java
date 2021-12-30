package edu.stanford.hivdb.sars2.drdb;

import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import edu.stanford.hivdb.mutations.Mutation;
import edu.stanford.hivdb.mutations.MutationSet;
import edu.stanford.hivdb.sars2.SARS2;

public class AntibodySuscResult extends SuscResult {

	private static final Map<String, Map<Mutation<SARS2>, List<SuscResult>>> searchTrees = new HashMap<>();

	private final String drdbVersion;
	private final Set<String> abNames;
	
	private transient Set<Antibody> antibodies;

	private static void prepareSearchTree(String drdbVersion) {
		if (searchTrees.containsKey(drdbVersion)) {
			return;
		}
		DRDB drdb = DRDB.getInstance(drdbVersion);
		List<SuscResult> allSuscResults = (
			drdb
			.queryAllSuscResultsForAntibodies()
			.stream()
			.map(d -> new AntibodySuscResult(drdbVersion, d))
			.collect(Collectors.toList())
		);
		searchTrees.put(drdbVersion, SuscResult.buildSuscResultSearchTree(allSuscResults));
	}
	
	public static List<BoundSuscResult> query(String drdbVersion, MutationSet<SARS2> queryMuts) {
		prepareSearchTree(drdbVersion);
		final MutationSet<SARS2> finalQueryMuts = prepareQueryMutations(queryMuts);
		return SuscResult.query(searchTrees.get(drdbVersion), finalQueryMuts);
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
