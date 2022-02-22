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
	private transient Integer numIsolateOnlyMutations;
	private transient Integer numQueryOnlyMutations;
	private Integer displayOrder;
	
	static List<VarMutsSuscSummary> decideDisplayPriority(List <VarMutsSuscSummary> items) {
		if (items.size() == 0) {
			return Collections.emptyList();
		}
		LinkedHashSet<IsolateMatchType> matchTypes = items.stream()
			.map(item -> item.getIsolateMatchType())
			.collect(Collectors.toCollection(LinkedHashSet::new));
		IsolateMatchType defaultType = matchTypes.stream().findFirst().get();
		Set<IsolateMatchType> expandableTypes = matchTypes.stream()
			.skip(1).limit(2).collect(Collectors.toSet());
		List<VarMutsSuscSummary> results = new ArrayList<>();
		Integer subsetMaxNumMiss = 0;
		Integer subsetMinNumMiss = Integer.MAX_VALUE;
		Integer overlapMinNumMiss = Integer.MAX_VALUE;
		boolean hasOverlap = false;

		for (VarMutsSuscSummary item : items) {
			IsolateMatchType matchType = item.getIsolateMatchType();
			Integer numMiss = item.getNumMissMutations();
			Integer displayOrder = null;

			if (matchType == IsolateMatchType.SUBSET) {
				subsetMaxNumMiss = subsetMaxNumMiss > numMiss ? subsetMaxNumMiss : numMiss;
				subsetMinNumMiss = subsetMinNumMiss < numMiss ? subsetMinNumMiss : numMiss;
			}
			
			if (numMiss > MAX_NUM_MISS) {
				displayOrder = null;
			}
			else if (matchType == defaultType) {
				displayOrder = 0;
			}
			else if (expandableTypes.contains(matchType)) {
				displayOrder = 1;
			}
			if (displayOrder != null && matchType == IsolateMatchType.OVERLAP) {
				if (numMiss >= subsetMaxNumMiss) {
					displayOrder = null;
				}
				else {
					hasOverlap = true;
					overlapMinNumMiss = overlapMinNumMiss < numMiss ? overlapMinNumMiss : numMiss;
				}
			}
			item.displayOrder = displayOrder;
			results.add(item);
		}
		
		if (hasOverlap && defaultType == IsolateMatchType.SUBSET) {
			// check if we should switch place of SUBSET and some OVERLAP
			// when some OVERLAP have general better results than SUBSET
			if (overlapMinNumMiss < subsetMinNumMiss) {
				for (VarMutsSuscSummary item : results) {
					Integer displayOrder = item.displayOrder;
					Integer numMiss = item.getNumMissMutations();
					if (displayOrder == null) {
						continue;
					}
					else if (
						displayOrder == 1 &&
						numMiss < subsetMinNumMiss &&
						numMiss - overlapMinNumMiss < subsetMinNumMiss - numMiss
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
			subsetMaxNumMiss - subsetMinNumMiss > 3 &&
			subsetMinNumMiss < 4
		) {
			for (VarMutsSuscSummary item : results) {
				if (item.getIsolateMatchType() == IsolateMatchType.SUBSET) {
					// subsetMinNumMiss is close to EQUAL,
					// hide imperfect matches by default
					Integer numMiss = item.getNumMissMutations();
					item.displayOrder = numMiss > subsetMinNumMiss ? 1 : 0;
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
				cmp = itemA.getNumMissMutations().compareTo(itemB.getNumMissMutations());
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
	
	public Integer getNumMissMutations() {
		return getNumIsolateOnlyMutations() + getNumQueryOnlyMutations();
	}
	
	public Integer getDisplayOrder() {
		return displayOrder;
	}
	
}