package edu.stanford.hivdb.sars2.drdb;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import edu.stanford.hivdb.mutations.MutationSet;
import edu.stanford.hivdb.sars2.SARS2;
import edu.stanford.hivdb.sars2.drdb.SuscResult.IsolateMatchType;
import edu.stanford.hivdb.viruses.Gene;

public class VarMutsSuscSummary extends SuscSummary {

	private static final Gene<SARS2> SPIKE = SARS2.getInstance().getMainStrain().getGene("S");

	private final Variant variant;
	private final MutationSet<SARS2> mutations;
	private transient Set<Isolate> hitIsolates;
	private transient IsolateMatchType matchType;
	private transient Integer numIsolateOnlyMutations;
	private transient Integer numQueryOnlyMutations;

	public VarMutsSuscSummary(
		Variant variant,
		MutationSet<SARS2> mutations,
		List<BoundSuscResult> suscResults,
		MutationSet<SARS2> queryMuts,
		String lastUpdate,
		String drdbVersion
	) {
		super(suscResults, queryMuts, lastUpdate, drdbVersion);
		this.variant = variant;
		this.mutations = mutations;
	}
	
	public Variant getVariant() {
		return variant;
	}

	public MutationSet<SARS2> getMutations() {
		return mutations;
	}
	
	public MutationSet<SARS2> getVariantExtraMutations() {
		if (variant == null) {
			return null;
		}
		return mutations
			.filterBy(mut -> mut.getGene() == SPIKE)
			.subtractsBy(
				queryMuts.filterBy(mut -> !mut.isUnsequenced())
			)
			.subtractsBy(SuscResult.EXCLUDE_MUTATIONS);
	}
	
	public MutationSet<SARS2> getVariantMissingMutations() {
		if (variant == null) {
			return null;
		}
		return queryMuts
			.filterBy(mut -> mut.getGene() == SPIKE && !mut.isUnsequenced())
			.subtractsBy(mutations)
			.subtractsBy(SuscResult.EXCLUDE_MUTATIONS);
	}
	
	public Set<Isolate> getHitIsolates() {
		if (hitIsolates == null) {
			hitIsolates = Collections
				.unmodifiableSet(
					getItems().stream()
					.map(sr -> sr.getIsolate())
					.collect(Collectors.toSet())
				);
		}
		return hitIsolates;
	}
	
	public IsolateMatchType getIsolateMatchType() {
		if (matchType == null) {
			matchType = getFirstItem().getMatchType();
		}
		return matchType;
	}
	
	public Integer getNumIsolateOnlyMutations() {
		if (numIsolateOnlyMutations == null) {
			numIsolateOnlyMutations = getFirstItem().getNumIsolateOnlyMutations();
		}
		return numIsolateOnlyMutations;
	}

	public Integer getNumQueryOnlyMutations() {
		if (numQueryOnlyMutations == null) {
			numQueryOnlyMutations = getFirstItem().getNumQueryOnlyMutations();
		}
		return numQueryOnlyMutations;
	}
	
}