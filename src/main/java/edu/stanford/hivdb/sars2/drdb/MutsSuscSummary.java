package edu.stanford.hivdb.sars2.drdb;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import edu.stanford.hivdb.mutations.MutationSet;
import edu.stanford.hivdb.sars2.SARS2;
import edu.stanford.hivdb.sars2.drdb.SuscResult.IsolateMatchType;

public class MutsSuscSummary extends SuscSummary {
	private final MutationSet<SARS2> mutations;
	private transient Set<Isolate> hitIsolates;
	private transient IsolateMatchType matchType;
	private transient Integer numIsolateOnlyMutations;
	private transient Integer numQueryOnlyMutations;

	public MutsSuscSummary(
		MutationSet<SARS2> mutations,
		List<SuscResult> suscResults,
		String lastUpdate
	) {
		super(suscResults, lastUpdate);
		this.mutations = mutations;
	}

	public MutationSet<SARS2> getMutations() {
		return mutations;
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
			matchType = getItems().get(0).getMatchType();
		}
		return matchType;
	}
	
	public Integer getNumIsolateOnlyMutations() {
		if (numIsolateOnlyMutations == null) {
			numIsolateOnlyMutations = getItems().get(0).getNumIsolateOnlyMutations();
		}
		return numIsolateOnlyMutations;
	}

	public Integer getNumQueryOnlyMutations() {
		if (numQueryOnlyMutations == null) {
			numQueryOnlyMutations = getItems().get(0).getNumQueryOnlyMutations();
		}
		return numQueryOnlyMutations;
	}
	
}