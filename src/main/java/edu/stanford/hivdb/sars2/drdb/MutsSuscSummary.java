package edu.stanford.hivdb.sars2.drdb;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import edu.stanford.hivdb.mutations.MutationSet;
import edu.stanford.hivdb.sars2.SARS2;
import edu.stanford.hivdb.sars2.drdb.SuscResult.VariantMatchType;

public class MutsSuscSummary extends SuscSummary {
	private final MutationSet<SARS2> mutations;
	private transient Set<VirusVariant> hitVariants;
	private transient VariantMatchType matchType;
	private transient Integer numVariantOnlyMutations;
	private transient Integer numQueryOnlyMutations;

	public MutsSuscSummary(MutationSet<SARS2> mutations, List<SuscResult> suscResults) {
		super(suscResults);
		this.mutations = mutations;
	}

	public MutationSet<SARS2> getMutations() {
		return mutations;
	}

	public Set<VirusVariant> getHitVariants() {
		if (hitVariants == null) {
			hitVariants = Collections
				.unmodifiableSet(
					getItems().stream()
					.map(sr -> sr.getVirusVariant())
					.collect(Collectors.toSet())
				);
		}
		return hitVariants;
	}
	
	public VariantMatchType getVariantMatchType() {
		if (matchType == null) {
			matchType = getItems().get(0).getMatchType();
		}
		return matchType;
	}
	
	public Integer getNumVariantOnlyMutations() {
		if (numVariantOnlyMutations == null) {
			numVariantOnlyMutations = getItems().get(0).getNumVariantOnlyMutations();
		}
		return numVariantOnlyMutations;
	}

	public Integer getNumQueryOnlyMutations() {
		if (numQueryOnlyMutations == null) {
			numQueryOnlyMutations = getItems().get(0).getNumQueryOnlyMutations();
		}
		return numQueryOnlyMutations;
	}
	
}