package edu.stanford.hivdb.sars2.drdb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import edu.stanford.hivdb.mutations.MutationSet;
import edu.stanford.hivdb.sars2.SARS2;

public class VaccPlasmaSuscResult extends SuscResult {

	private static final Map<String, Map<MutationSet<SARS2>, List<VaccPlasmaSuscResult>>> singletons = new HashMap<>();

	private final String vaccineName;
	private final Integer vaccinePriority;
	private final String vaccineType;
	private final String cumulativeGroup;
	
	private static void prepareSingletons(String drdbVersion) {
		if (singletons.containsKey(drdbVersion)) {
			return;
		}
		DRDB drdb = DRDB.getInstance(drdbVersion);
		Map<MutationSet<SARS2>, List<VaccPlasmaSuscResult>> allSuscResults = (
			drdb
			.queryAllSuscResultsForVaccPlasma()
			.stream()
			.map(d -> new VaccPlasmaSuscResult(drdbVersion, d))
			.collect(Collectors.groupingBy(d -> d.getComparableIsolateMutations()))
		);
		singletons.put(drdbVersion, allSuscResults);
	}
	
	public static List<BoundSuscResult> query(String drdbVersion, MutationSet<SARS2> queryMuts) {
		prepareSingletons(drdbVersion);
		final MutationSet<SARS2> finalQueryMuts = prepareQueryMutations(queryMuts);
		
		List<BoundSuscResult> results = new ArrayList<>();
		for (Entry<MutationSet<SARS2>, List<VaccPlasmaSuscResult>> pair : singletons.get(drdbVersion).entrySet()) {
				IsolateMatchType matchType = SuscResult.calcMatchType(pair.getKey(), finalQueryMuts); 
				if (matchType == IsolateMatchType.MISMATCH) {
					continue;
				}
				for (SuscResult sr : pair.getValue()) {
					results.add(new BoundSuscResult(matchType, finalQueryMuts, sr));
				}
		}
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
