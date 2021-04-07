package edu.stanford.hivdb.sars2.drdb;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import edu.stanford.hivdb.mutations.MutationSet;
import edu.stanford.hivdb.sars2.SARS2;

public class KeyMutsSuscSummary extends SuscSummary {
	private final MutationSet<SARS2> mutations;
	private transient Integer numKeyMutGroups;
	private transient Set<VirusVariant> hitVariants;

	public KeyMutsSuscSummary(MutationSet<SARS2> mutations, List<SuscResult> suscResults) {
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

	public Set<MutationSet<SARS2>> getKeyMutGroups() {
		return getItems().get(0).getVirusVariant().getKeyMutationGroups();
	}

	public int getNumKeyMutGroups() {
		if (numKeyMutGroups == null) {
			numKeyMutGroups = getItems().get(0).getVirusVariant().getNumKeyMutationGroups();
		}
		return numKeyMutGroups;
	}

}