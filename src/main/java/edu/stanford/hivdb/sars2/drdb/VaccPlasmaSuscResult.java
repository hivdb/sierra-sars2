package edu.stanford.hivdb.sars2.drdb;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import edu.stanford.hivdb.mutations.MutationSet;
import edu.stanford.hivdb.sars2.SARS2;

public class VaccPlasmaSuscResult extends SuscResult {

	private static final Map<String, List<Map<String, Object>>> rawResults = new HashMap<>();

	private final String vaccineName;
	private final Integer vaccinePriority;
	private final String vaccineType;
	private final String cumulativeGroup;
	
	private static void prepareRawResults(String drdbVersion) {
		if (rawResults.containsKey(drdbVersion)) {
			return;
		}
		DRDB drdb = DRDB.getInstance(drdbVersion);
		List<Map<String, Object>> allSuscResults = (
			drdb
			.queryAllSuscResultsForVaccPlasma()
		);
		rawResults.put(drdbVersion, allSuscResults);
	}
	
	public static List<VaccPlasmaSuscResult> query(String drdbVersion, MutationSet<SARS2> queryMuts) {
		prepareRawResults(drdbVersion);
		final MutationSet<SARS2> finalQueryMuts = prepareQueryMutations(queryMuts);
		List<VaccPlasmaSuscResult> results = (
			rawResults.get(drdbVersion)
			.stream()
			.map(d -> new VaccPlasmaSuscResult(drdbVersion, finalQueryMuts, d))
			.filter(d -> d.getMatchType() != IsolateMatchType.MISMATCH)
			.sorted((a, b) -> a.getVaccinePriority() - b.getVaccinePriority())
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
