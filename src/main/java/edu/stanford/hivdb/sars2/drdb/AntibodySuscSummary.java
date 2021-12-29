package edu.stanford.hivdb.sars2.drdb;

import java.util.List;
import java.util.Set;

public class AntibodySuscSummary extends SuscSummary {
	private Set<Antibody> antibodies;

	protected AntibodySuscSummary(
		Set<Antibody> antibodies,
		List<BoundSuscResult> items,
		String lastUpdate
	) {
		super(items, lastUpdate);
		this.antibodies = antibodies;
	}

	public Set<Antibody> getAntibodies() { return antibodies; }

}