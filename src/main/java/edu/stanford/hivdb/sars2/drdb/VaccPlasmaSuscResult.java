package edu.stanford.hivdb.sars2.drdb;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import edu.stanford.hivdb.mutations.Mutation;
import edu.stanford.hivdb.mutations.MutationSet;
import edu.stanford.hivdb.sars2.SARS2;

public class VaccPlasmaSuscResult extends SuscResult {

	private static final Map<String, Map<Mutation<SARS2>, List<SuscResult>>> searchTrees = new HashMap<>();

	private final String vaccineName;
	private final Integer vaccinePriority;
	private final String vaccineType;
	private final String cumulativeGroup;
	
	private static void prepareSearchTree(String drdbVersion) {
		if (searchTrees.containsKey(drdbVersion)) {
			return;
		}
		DRDB drdb = DRDB.getInstance(drdbVersion);
		List<SuscResult> allSuscResults = (
			drdb
			.queryAllSuscResultsForVaccPlasma()
			.stream()
			.map(d -> new VaccPlasmaSuscResult(drdbVersion, d))
			.collect(Collectors.toList())
		);
		searchTrees.put(drdbVersion, SuscResult.buildSuscResultSearchTree(allSuscResults));
	}
	
	public static List<BoundSuscResult> query(String drdbVersion, MutationSet<SARS2> queryMuts) {
		prepareSearchTree(drdbVersion);
		final MutationSet<SARS2> finalQueryMuts = prepareQueryMutations(queryMuts);
		List<BoundSuscResult> results = SuscResult.query(searchTrees.get(drdbVersion), finalQueryMuts);
		results.sort((a, b) -> (
			((VaccPlasmaSuscResult) a.getSuscResult()).getVaccinePriority() - 
			((VaccPlasmaSuscResult) b.getSuscResult()).getVaccinePriority()
		));
		return results;
	}
	
	private VaccPlasmaSuscResult(
		String drdbVersion,
		Map<String, Object> suscData
	) {
		super(drdbVersion, suscData);

		this.vaccineName = (String) suscData.get("vaccineName");
		this.vaccinePriority = (Integer) suscData.get("vaccinePriority");
		this.vaccineType = (String) suscData.get("vaccineType");
		this.cumulativeGroup = (String) suscData.get("cumulativeGroup");
	}
	
	public String getVaccineName() {
		return vaccineName;
	}
	
	public Integer getVaccinePriority() {
		return vaccinePriority;
	}
	
	public String getVaccineType() {
		return vaccineType;
	}
	
	public String getCumulativeGroup() {
		return cumulativeGroup;
	}

}
