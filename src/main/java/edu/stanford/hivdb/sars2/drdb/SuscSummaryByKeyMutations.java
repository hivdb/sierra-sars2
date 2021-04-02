package edu.stanford.hivdb.sars2.drdb;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import edu.stanford.hivdb.mutations.MutationSet;
import edu.stanford.hivdb.sars2.SARS2;

public class SuscSummaryByKeyMutations extends WithCumulativeSuscResults {
	private final MutationSet<SARS2> mutations;
	private final List<SuscResult> items;
	private transient Integer numKeyMutGroups;
	private transient Set<VirusVariant> hitVariants;

	public SuscSummaryByKeyMutations(MutationSet<SARS2> mutations, List<SuscResult> suscResults) {
		this.mutations = mutations;
		this.items = suscResults;
	}

	public MutationSet<SARS2> getMutations() {
		return mutations;
	}

	public Set<VirusVariant> getHitVariants() {
		if (hitVariants == null) {
			hitVariants = Collections
					.unmodifiableSet(items.stream().map(sr -> sr.getVirusVariant()).collect(Collectors.toSet()));
		}
		return hitVariants;
	}

	public Set<MutationSet<SARS2>> getKeyMutGroups() {
		return items.get(0).getVirusVariant().getKeyMutationGroups();
	}

	public int getNumKeyMutGroups() {
		if (numKeyMutGroups == null) {
			numKeyMutGroups = items.get(0).getVirusVariant().getNumKeyMutationGroups();
		}
		return numKeyMutGroups;
	}

	@Override
	public List<SuscResult> getItems() {
		return items;
	}
}