package edu.stanford.hivdb.sars2.drdb;

import java.util.List;

public class AntibodyClassSuscSummary extends SuscSummary {
	private String antibodyClass;
	
	public AntibodyClassSuscSummary(
		String antibodyClass,
		List<SuscResult> items
	) {
		super(items);
		this.antibodyClass = antibodyClass;
	}
	
	public String getAntibodyClass() { return antibodyClass; }
	
}