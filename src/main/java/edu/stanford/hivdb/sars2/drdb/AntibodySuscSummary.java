package edu.stanford.hivdb.sars2.drdb;

import java.util.List;
import java.util.Set;

import edu.stanford.hivdb.mutations.MutationSet;
import edu.stanford.hivdb.sars2.SARS2;

public class AntibodySuscSummary extends SuscSummary {
	private Set<Antibody> antibodies;

	protected AntibodySuscSummary(
		Set<Antibody> antibodies,
		List<BoundSuscResult> items,
		MutationSet<SARS2> queryMuts,
		String lastUpdate,
		String drdbVersion
	) {
		super(items, queryMuts, lastUpdate, drdbVersion);
		this.antibodies = antibodies;
	}

	public Set<Antibody> getAntibodies() { return antibodies; }

}