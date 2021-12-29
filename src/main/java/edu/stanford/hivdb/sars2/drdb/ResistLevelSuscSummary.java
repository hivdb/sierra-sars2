package edu.stanford.hivdb.sars2.drdb;

import java.util.List;

public class ResistLevelSuscSummary extends SuscSummary {
	private String resistanceLevel;

	public ResistLevelSuscSummary(
		String resistanceLevel,
		List<BoundSuscResult> items,
		String lastUpdate
	) {
		super(items, lastUpdate);
		this.resistanceLevel = resistanceLevel;
	}

	public String getResistanceLevel() { return resistanceLevel; }
}