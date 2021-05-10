package edu.stanford.hivdb.sars2.drdb;

import java.util.List;

public class VaccineSuscSummary extends SuscSummary {
	private final String vaccineName;
	private final Integer vaccinePriority;
	private final String vaccineType;

	public VaccineSuscSummary(
		String vaccineName,
		List<SuscResult> items,
		String lastUpdate
	) {
		super(items, lastUpdate);
		this.vaccineName = vaccineName;
		VaccPlasmaSuscResult firstItem = ((VaccPlasmaSuscResult) items.get(0));
		this.vaccinePriority = firstItem.getVaccinePriority();
		this.vaccineType = firstItem.getVaccineType();
	}

	public String getVaccineName() { return vaccineName; }
	public Integer getVaccinePriority() { return vaccinePriority; }
	public String getVaccineType() { return vaccineType; }
}