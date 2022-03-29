package edu.stanford.hivdb.sars2.drdb;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import edu.stanford.hivdb.mutations.Mutation;
import edu.stanford.hivdb.mutations.MutationSet;
import edu.stanford.hivdb.sars2.SARS2;

public class ConvPlasmaSuscResult extends SuscResult {

	private static final Map<String, Map<Mutation<SARS2>, List<SuscResult>>> searchTrees = new HashMap<>();

	private final String infectedVarName;
	private final String cumulativeGroup;

	private static void prepareSearchTree(String drdbVersion) {
		if (searchTrees.containsKey(drdbVersion)) {
			return;
		}
		DRDB drdb = DRDB.getInstance(drdbVersion);
		List<SuscResult> allSuscResults = (
			drdb
			.queryAllSuscResultsForConvPlasma()
			.stream()
			.map(d -> new ConvPlasmaSuscResult(drdbVersion, d))
			.collect(Collectors.toList())
		);
		searchTrees.put(drdbVersion, SuscResult.buildSuscResultSearchTree(allSuscResults));
	}
	
	public static List<BoundSuscResult> query(String drdbVersion, MutationSet<SARS2> queryMuts) {
		prepareSearchTree(drdbVersion);
		final MutationSet<SARS2> finalQueryMuts = prepareQueryMutations(queryMuts);
		return SuscResult.query(drdbVersion, searchTrees.get(drdbVersion), finalQueryMuts);
	}
	
	private ConvPlasmaSuscResult(
		String drdbVersion,
		Map<String, Object> suscData
	) {
		super(drdbVersion, suscData);

		this.infectedVarName = (String) suscData.get("infectedVarName");
		this.cumulativeGroup = (String) suscData.get("cumulativeGroup");
	}
	
	public String getInfectedVarName() {
		return infectedVarName;
	}
	
	public String getCumulativeGroup() {
		return cumulativeGroup;
	}

}
