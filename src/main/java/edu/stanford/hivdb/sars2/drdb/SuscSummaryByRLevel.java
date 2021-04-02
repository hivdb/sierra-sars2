package edu.stanford.hivdb.sars2.drdb;

import java.util.Collections;
import java.util.List;

public class SuscSummaryByRLevel extends WithCumulativeSuscResults {
	private String resistanceLevel;
	private List<SuscResult> items;

	public SuscSummaryByRLevel(
		String resistanceLevel,
		List<SuscResult> items
	) {
		this.resistanceLevel = resistanceLevel;
		this.items = Collections.unmodifiableList(items);
	}

	public String getResistanceLevel() { return resistanceLevel; }
	@Override
	public List<SuscResult> getItems() { return items; }
}