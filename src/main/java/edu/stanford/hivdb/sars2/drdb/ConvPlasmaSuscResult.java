package edu.stanford.hivdb.sars2.drdb;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import edu.stanford.hivdb.mutations.MutationSet;
import edu.stanford.hivdb.sars2.SARS2;

public class ConvPlasmaSuscResult extends SuscResult {

	private final String infection;
	private final String cumulativeGroup;
	
	public static List<ConvPlasmaSuscResult> query(String drdbVersion, MutationSet<SARS2> queryMuts) {
		DRDB drdb = DRDB.getInstance(drdbVersion);
		final MutationSet<SARS2> finalQueryMuts = prepareQueryMutations(queryMuts);
		List<ConvPlasmaSuscResult> results = (
			drdb
			.querySuscResultsForConvPlasma(finalQueryMuts)
			.stream()
			.map(d -> new ConvPlasmaSuscResult(drdbVersion, finalQueryMuts, d))
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

		this.infection = (String) suscData.get("infection");
		this.cumulativeGroup = (String) suscData.get("cumulativeGroup");
	}
	
	public String getInfection() {
		return infection;
	}
	
	public String getCumulativeGroup() {
		return cumulativeGroup;
	}

}
