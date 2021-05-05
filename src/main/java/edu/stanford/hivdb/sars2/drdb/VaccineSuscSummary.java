package edu.stanford.hivdb.sars2.drdb;

import java.util.List;

public class VaccineSuscSummary extends SuscSummary {
	private final String vaccineName;

	public VaccineSuscSummary(
		String vaccineName,
		List<SuscResult> items,
		String lastUpdate
	) {
		super(items, lastUpdate);
		this.vaccineName = vaccineName;
	}

	public String getVaccineName() { return vaccineName; }
}