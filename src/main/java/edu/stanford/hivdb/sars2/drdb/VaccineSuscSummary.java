package edu.stanford.hivdb.sars2.drdb;

import java.util.List;

import edu.stanford.hivdb.mutations.MutationSet;
import edu.stanford.hivdb.sars2.SARS2;

public class VaccineSuscSummary extends SuscSummary {
	private final String vaccineName;
	private final Integer vaccinePriority;
	private final String vaccineType;

	public VaccineSuscSummary(
		String vaccineName,
		List<BoundSuscResult> items,
		MutationSet<SARS2> queryMuts,
		String lastUpdate,
		String drdbVersion
	) {
		super(items, queryMuts, lastUpdate, drdbVersion);
		this.vaccineName = vaccineName;
		BoundSuscResult firstItem = items.get(0);
		this.vaccinePriority = firstItem.getVaccinePriority();
		this.vaccineType = firstItem.getVaccineType();
	}

	public String getVaccineName() { return vaccineName; }
	public Integer getVaccinePriority() { return vaccinePriority; }
	public String getVaccineType() { return vaccineType; }
}