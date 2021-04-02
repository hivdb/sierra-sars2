package edu.stanford.hivdb.sars2.drdb;

import java.util.Collections;
import java.util.List;

public class SuscSummaryByAntibodyClass extends WithCumulativeSuscResults {
	private String antibodyClass;
	private List<SuscResult> items;
	
	public SuscSummaryByAntibodyClass(
		String antibodyClass,
		List<SuscResult> items
	) {
		this.antibodyClass = antibodyClass;
		this.items = Collections.unmodifiableList(items);
	}
	
	public String getAntibodyClass() { return antibodyClass; }
	@Override
	public List<SuscResult> getItems() { return items; }
	
}