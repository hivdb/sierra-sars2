package edu.stanford.hivdb.sars2.drdb;

import java.util.List;

import edu.stanford.hivdb.mutations.MutationSet;
import edu.stanford.hivdb.sars2.SARS2;

public class ResistLevelSuscSummary extends SuscSummary {
	private String resistanceLevel;

	public ResistLevelSuscSummary(
		String resistanceLevel,
		List<BoundSuscResult> items,
		MutationSet<SARS2> queryMuts,
		String lastUpdate,
		String drdbVersion
	) {
		super(items, queryMuts, lastUpdate, drdbVersion);
		this.resistanceLevel = resistanceLevel;
	}

	public String getResistanceLevel() { return resistanceLevel; }
}