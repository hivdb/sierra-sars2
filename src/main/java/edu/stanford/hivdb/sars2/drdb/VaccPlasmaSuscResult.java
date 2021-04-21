package edu.stanford.hivdb.sars2.drdb;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import edu.stanford.hivdb.mutations.MutationSet;
import edu.stanford.hivdb.sars2.SARS2;

public class VaccPlasmaSuscResult extends SuscResult {

	private final String vaccineName;
	private final String cumulativeGroup;
	
	public static List<VaccPlasmaSuscResult> query(String drdbVersion, MutationSet<SARS2> queryMuts) {
		DRDB drdb = DRDB.getInstance(drdbVersion);
		final MutationSet<SARS2> finalQueryMuts = prepareQueryMutations(queryMuts);
		List<VaccPlasmaSuscResult> results = (
			drdb
			.querySuscResultsForImmuPlasma(finalQueryMuts)
			.stream()
			.map(d -> new VaccPlasmaSuscResult(drdbVersion, finalQueryMuts, d))
			.collect(Collectors.toList())
		);
		
		return results;
	}
	
	private VaccPlasmaSuscResult(
		String drdbVersion,
		MutationSet<SARS2> queryMuts,
		Map<String, Object> suscData
	) {
		super(drdbVersion, queryMuts, suscData);

		this.vaccineName = (String) suscData.get("vaccineName");
		this.cumulativeGroup = (String) suscData.get("cumulativeGroup");
	}
	
	public String getVaccineName() {
		return vaccineName;
	}
	
	public String getCumulativeGroup() {
		return cumulativeGroup;
	}

}
