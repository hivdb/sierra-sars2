package edu.stanford.hivdb.sars2.drdb;

import java.util.Set;

import edu.stanford.hivdb.mutations.MutationSet;
import edu.stanford.hivdb.sars2.SARS2;
import edu.stanford.hivdb.sars2.drdb.SuscResult.IsolateMatchType;

public class BoundSuscResult {
	
	private final IsolateMatchType matchType;
	private final Integer numIsoOnlyMuts;
	private final Integer numQueryOnlyMuts;
	private final Integer numIsoOnlyResistMuts;
	private final Integer numQueryOnlyResistMuts;
	private final SuscResult suscResult;
	
	protected BoundSuscResult(
		IsolateMatchType matchType,
		Integer numIsoOnlyMuts,
		Integer numQueryOnlyMuts,
		Integer numIsoOnlyResistMuts,
		Integer numQueryOnlyResistMuts,
		SuscResult suscResult
	) {
		this.matchType = matchType;
		this.numIsoOnlyMuts = numIsoOnlyMuts;
		this.numQueryOnlyMuts = numQueryOnlyMuts;
		this.numIsoOnlyResistMuts = numIsoOnlyResistMuts;
		this.numQueryOnlyResistMuts = numQueryOnlyResistMuts;
		this.suscResult = suscResult;
	}
	
	public IsolateMatchType getMatchType() { return matchType; }
	public SuscResult getSuscResult() { return suscResult; }

	public boolean isAntibody() {
		return suscResult instanceof AntibodySuscResult;
	}
	
	public boolean isConvPlasma() {
		return suscResult instanceof ConvPlasmaSuscResult;
	}
	
	public boolean isVaccPlasma() {
		return suscResult instanceof VaccPlasmaSuscResult;
	}
	
	public String getResistanceLevel() { return suscResult.getResistanceLevel(); }
	public Article getReference() { return suscResult.getReference(); }
	public Isolate getControlIsolate() { return suscResult.getControlIsolate(); }
	public Variant getVariant() { return suscResult.getVariant(); }
	public Isolate getIsolate() { return suscResult.getIsolate(); }
	public String getRefName() { return suscResult.getRefName(); }
	public String getRxName() { return suscResult.getRxName(); }
	public String getAssayName() { return suscResult.getAssayName(); }
	public String getSection() { return suscResult.getSection(); }
	public String getFoldCmp() { return suscResult.getFoldCmp(); }
	public Double getFold() { return suscResult.getFold(); }
	public String getIneffective() { return suscResult.getIneffective(); }
	public Integer getCumulativeCount() { return suscResult.getCumulativeCount(); }
	public MutationSet<SARS2> getComparableIsolateMutations() { return suscResult.getComparableIsolateMutations(); }
	
	public Integer getNumIsolateOnlyMutations() { return numIsoOnlyMuts; } 
	
	public Integer getNumQueryOnlyMutations() { return numQueryOnlyMuts; }

	public Integer getNumIsolateOnlyDRMs() { return numIsoOnlyResistMuts; } 
	
	public Integer getNumQueryOnlyDRMs() { return numQueryOnlyResistMuts; }
	
	public Set<Antibody> getAntibodies() {
		return ((AntibodySuscResult) suscResult).getAntibodies();
	}
	
	public String getInfectedVarName() {
		return ((ConvPlasmaSuscResult) suscResult).getInfectedVarName();
	}
	
	public Integer getVaccinePriority() {
		return ((VaccPlasmaSuscResult) suscResult).getVaccinePriority();
	}

	public String getVaccineName() {
		return ((VaccPlasmaSuscResult) suscResult).getVaccineName();
	}
	
	public String getVaccineType() {
		return ((VaccPlasmaSuscResult) suscResult).getVaccineType();
	}
}