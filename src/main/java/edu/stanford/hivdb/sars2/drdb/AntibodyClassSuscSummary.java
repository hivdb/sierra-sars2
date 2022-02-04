package edu.stanford.hivdb.sars2.drdb;

import java.util.List;

import edu.stanford.hivdb.mutations.MutationSet;
import edu.stanford.hivdb.sars2.SARS2;

public class AntibodyClassSuscSummary extends SuscSummary {
	private String antibodyClass;
	
	public AntibodyClassSuscSummary(
		String antibodyClass,
		List<BoundSuscResult> items,
		MutationSet<SARS2> queryMuts,
		String lastUpdate,
		String drdbVersion
	) {
		super(items, queryMuts, lastUpdate, drdbVersion);
		this.antibodyClass = antibodyClass;
	}
	
	public String getAntibodyClass() { return antibodyClass; }
	
}