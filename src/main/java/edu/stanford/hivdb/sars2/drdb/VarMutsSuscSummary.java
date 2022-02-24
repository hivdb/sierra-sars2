package edu.stanford.hivdb.sars2.drdb;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import edu.stanford.hivdb.mutations.MutationSet;
import edu.stanford.hivdb.sars2.SARS2;
import edu.stanford.hivdb.sars2.drdb.SuscResult.IsolateMatchType;
import edu.stanford.hivdb.viruses.Gene;

public class VarMutsSuscSummary extends SuscSummary {

	private static final Integer MAX_NUM_MISS = 100;
	private static final Gene<SARS2> SPIKE = SARS2.getInstance().getMainStrain().getGene("S");

	private final Variant variant;
	private final MutationSet<SARS2> mutations;
	private transient Set<Isolate> hitIsolates;
	private transient IsolateMatchType matchType;
	private transient Integer numDiffMutations;
	private transient Integer numDiffDRMs;
	private Integer displayOrder;
	
	static List<VarMutsSuscSummary> decideDisplayPriority(List <VarMutsSuscSummary> items) {
		if (items.size() == 0) {
			return Collections.emptyList();
		}
		LinkedHashSet<IsolateMatchType> matchTypes = items.stream()
			.filter(item -> item.getNumDiffMutations() <= MAX_NUM_MISS)
			.map(item -> item.getIsolateMatchType())
			.collect(Collectors.toCollection(LinkedHashSet::new));
		IsolateMatchType defaultType = matchTypes.stream().findFirst().get();
		Set<IsolateMatchType> expandableTypes = matchTypes.stream()
			.skip(1).limit(2).collect(Collectors.toSet());
		List<VarMutsSuscSummary> results = new ArrayList<>();
		Integer subsetMaxNumDiff = 0;
		Integer subsetMinNumDiff = Integer.MAX_VALUE;
		Integer overlapMinNumDiff = Integer.MAX_VALUE;
		boolean hasOverlap = false;

		for (VarMutsSuscSummary item : items) {

			if (item.getNumDiffMutations() <= MAX_NUM_MISS) {
				Integer displayOrder = null;
				Integer numDiff = item.getNumDiffDRMs();
				IsolateMatchType matchType = item.getIsolateMatchType();
				if (matchType == IsolateMatchType.SUBSET) {
					subsetMaxNumDiff = subsetMaxNumDiff > numDiff ? subsetMaxNumDiff : numDiff;
					subsetMinNumDiff = subsetMinNumDiff < numDiff ? subsetMinNumDiff : numDiff;
				}
			
				if (matchType == defaultType) {
					displayOrder = 0;
				}
				else if (expandableTypes.contains(matchType)) {
					displayOrder = 1;
				}
				if (displayOrder != null && matchType == IsolateMatchType.OVERLAP) {
					if (numDiff >= subsetMaxNumDiff) {
						displayOrder = null;
					}
					else {
						hasOverlap = true;
						overlapMinNumDiff = overlapMinNumDiff < numDiff ? overlapMinNumDiff : numDiff;
					}
				}
				item.displayOrder = displayOrder;
			}

			results.add(item);
		}
		
		if (hasOverlap && defaultType == IsolateMatchType.SUBSET) {
			// check if we should switch place of SUBSET and some OVERLAP
			// when some OVERLAP have general better results than SUBSET
			if (overlapMinNumDiff < subsetMinNumDiff) {
				for (VarMutsSuscSummary item : results) {
					Integer displayOrder = item.displayOrder;
					Integer numDiff = item.getNumDiffDRMs();
					if (displayOrder == null) {
						continue;
					}
					else if (
						displayOrder == 1 &&
						numDiff < subsetMinNumDiff &&
						numDiff - overlapMinNumDiff < subsetMinNumDiff - numDiff
					) {
						item.displayOrder = 0;
					}
					else if (displayOrder == 0) {
						item.displayOrder = 1;
					}
				}
				defaultType = IsolateMatchType.OVERLAP;
			}
		}
		
		if (
			defaultType == IsolateMatchType.SUBSET &&
			subsetMaxNumDiff - subsetMinNumDiff > 3 &&
			subsetMinNumDiff < 4
		) {
			for (VarMutsSuscSummary item : results) {
				if (item.getIsolateMatchType() == IsolateMatchType.SUBSET) {
					// subsetMinNumMiss is close to EQUAL,
					// hide imperfect matches by default
					Integer numDiff = item.getNumDiffDRMs();
					item.displayOrder = numDiff > subsetMinNumDiff ? 1 : 0;
				}
			}
		}
		
		results.sort(
			(itemA, itemB) -> {
				if (itemB.displayOrder == null) {
					return -1;
				}
				if (itemA.displayOrder == null) {
					return 1;
				}
				int cmp = itemA.displayOrder.compareTo(itemB.displayOrder);
				if (cmp != 0) {
					return cmp;
				}
				cmp = itemA.getNumDiffDRMs().compareTo(itemB.getNumDiffDRMs());
				if (cmp != 0) {
					return cmp;
				}
				cmp = itemA.getNumDiffMutations().compareTo(itemB.getNumDiffMutations());
				if (cmp != 0) {
					return cmp;
				}
				return itemA.getMutations().compareTo(itemB.getMutations());
			}
		);
		return results;
	}
	

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
		this.displayOrder = null;
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
	
	public MutationSet<SARS2> getVariantMatchingMutations() {
		if (variant == null) {
			return null;
		}
		return queryMuts
			.filterBy(mut -> mut.getGene() == SPIKE && !mut.isUnsequenced())
			.intersectsWith(mutations)
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
	
	public Integer getNumDiffMutations() {
		if (numDiffMutations == null) {
			numDiffMutations = getItems().stream()
				.map(item -> item.getNumIsolateOnlyMutations() + item.getNumQueryOnlyMutations())
				.min(Integer::compare)
				.get();
		}
		return numDiffMutations;
	}
	
	public Integer getNumDiffDRMs() {
		if (numDiffDRMs == null) {
			numDiffDRMs = getItems().stream()
				.map(item -> item.getNumIsolateOnlyDRMs() + item.getNumQueryOnlyDRMs())
				.min(Integer::compare)
				.get();
		}
		return numDiffDRMs;
	}
	
	public Integer getDisplayOrder() {
		return displayOrder;
	}
	
}