package edu.stanford.hivdb.sars2.drdb;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class SuscSummaryByAntibody extends WithCumulativeSuscResults {
	private Set<Antibody> antibodies;
	private List<SuscResult> items;

	public SuscSummaryByAntibody(
		Set<Antibody> antibodies,
		List<SuscResult> items
	) {
		this.antibodies = antibodies;
		this.items = Collections.unmodifiableList(items);
	}

	public Set<Antibody> getAntibodies() { return antibodies; }
	@Override
	public List<SuscResult> getItems() { return items; }

}