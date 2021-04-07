package edu.stanford.hivdb.sars2.drdb;

import java.util.List;
import java.util.Set;

public class AntibodySuscSummary extends SuscSummary {
	private Set<Antibody> antibodies;

	protected AntibodySuscSummary(
		Set<Antibody> antibodies,
		List<SuscResult> items
	) {
		super(items);
		this.antibodies = antibodies;
	}

	public Set<Antibody> getAntibodies() { return antibodies; }

}