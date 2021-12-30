package edu.stanford.hivdb.sars2.drdb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
// import java.util.Set;
import java.util.Set;
import java.util.stream.Collectors;

import edu.stanford.hivdb.mutations.AAMutation;
import edu.stanford.hivdb.mutations.Mutation;
// import edu.stanford.hivdb.mutations.GenePosition;
import edu.stanford.hivdb.mutations.MutationSet;
import edu.stanford.hivdb.sars2.SARS2;
import edu.stanford.hivdb.viruses.Gene;

public abstract class SuscResult {

	private static final double PARTIAL_RESIST_FOLD = 3;
	private static final double RESIST_FOLD = 10;

	private static final Set<Gene<SARS2>> INCLUDE_GENES;
	protected static final Set<Mutation<SARS2>> EXCLUDE_MUTATIONS;
	private static final List<Set<Mutation<SARS2>>> RANGE_DELETIONS;
	
	static {
		SARS2 sars2 = SARS2.getInstance();
		Gene<SARS2> spikeGene = sars2.getGene("SARS2S");

		INCLUDE_GENES = Set.of(spikeGene);
		
		EXCLUDE_MUTATIONS = Set.of(
			new AAMutation<>(spikeGene, 614, 'G')
		);

		RANGE_DELETIONS = List.of(
			Set.of(
				new AAMutation<>(spikeGene, 69, '-'),
				new AAMutation<>(spikeGene, 70, '-')
			),
			Set.of(
				new AAMutation<>(spikeGene, 141, '-'),
				new AAMutation<>(spikeGene, 142, '-'),
				new AAMutation<>(spikeGene, 143, '-'),
				new AAMutation<>(spikeGene, 144, '-'),
				new AAMutation<>(spikeGene, 145, '-'),
				new AAMutation<>(spikeGene, 146, '-')
			),
			Set.of(
				new AAMutation<>(spikeGene, 242, '-'),
				new AAMutation<>(spikeGene, 243, '-'),
				new AAMutation<>(spikeGene, 244, '-')
			)
		);
	}
	
	public static Map<Mutation<SARS2>, List<SuscResult>> buildSuscResultSearchTree(List<SuscResult> suscResults) {
		Map<Mutation<SARS2>, List<SuscResult>> tree = new HashMap<>();
		for (SuscResult sr : suscResults) {
			for (Mutation<SARS2> mut : sr.getComparableIsolateMutations()) {
				if (!tree.containsKey(mut)) {
					tree.put(mut, new ArrayList<>());
				}
				tree.get(mut).add(sr);
			}
		}
		return tree;
	}
	
	public static List<BoundSuscResult> query(Map<Mutation<SARS2>, List<SuscResult>> tree, MutationSet<SARS2> queryMuts) {
		Map<SuscResult, Integer> counter = new HashMap<>();
		Set<Mutation<SARS2>> splittedQueryMuts = queryMuts.getSplitted();
		int numQueryMuts = splittedQueryMuts.size(); 
		for (Mutation<SARS2> mut : splittedQueryMuts) {
			if (!tree.containsKey(mut)) {
				continue;
			}
			for (SuscResult sr : tree.get(mut)) {
				if (!counter.containsKey(sr)) {
					counter.put(sr, 0);
				}
				counter.put(sr, counter.get(sr) + 1);
			}
		}
		List<BoundSuscResult> results = new ArrayList<>();
		for (Entry<SuscResult, Integer> pair : counter.entrySet()) {
			IsolateMatchType matchType;
			SuscResult sr = pair.getKey();
			int numIsoMuts = sr.getComparableIsolateMutations().getSplitted().size();
			int numSharedMuts = pair.getValue();
			int numIsoOnlyMuts = numIsoMuts - numSharedMuts;
			int numQueryOnlyMuts = numQueryMuts - numSharedMuts;
			if (numIsoOnlyMuts == 0 && numQueryOnlyMuts == 0) {
				matchType = IsolateMatchType.EQUAL;
			}
			else if (numIsoOnlyMuts > 0 && numQueryOnlyMuts == 0) {
				matchType = IsolateMatchType.SUPERSET;
			}
			else if (numIsoOnlyMuts == 0 && numQueryOnlyMuts > 0) {
				matchType = IsolateMatchType.SUBSET;
			}
			else { // numIsoOnlyMuts > 0 || numQueryOnlyMuts > 0
				matchType = IsolateMatchType.OVERLAP;
			}

			results.add(new BoundSuscResult(
				matchType,
				numIsoOnlyMuts,
				numQueryOnlyMuts,
				sr
			));
			
		}
		
		return results;
	}
	
	public enum IsolateMatchType {
		EQUAL,     // isolate mutation set equals to query mutation set
		SUPERSET,  //                      is the superset of query mutation set
		SUBSET,    //                      is the subset of query mutation set
		OVERLAP,   //                      overlaps with query mutation set
		MISMATCH   //                      mismatches to query mutation set
	}
	
	protected static MutationSet<SARS2> prepareQueryMutations(MutationSet<SARS2> muts) {
		// no unseq region
		muts = muts.filterBy(mut -> !mut.isUnsequenced());
	
		// filter genes
		muts = muts.filterBy(mut -> INCLUDE_GENES.contains(mut.getGene()));
	
		// remove excluded mutations
		muts = muts.subtractsBy(EXCLUDE_MUTATIONS);
		
		// query all deletions in the range if at least one exists
		for (Set<Mutation<SARS2>> rangeDel : RANGE_DELETIONS) {
			if (!muts.intersectsWith(rangeDel).isEmpty()) {
				muts = muts.mergesWith(rangeDel);
			}
		}		
		
		// remove refAA from mixtures
		muts = new MutationSet<>(
			muts.getSplitted()
			.stream()
			.filter(mut -> mut.getAAsWithoutReference().length() > 0)
			.collect(Collectors.toList())
		);
		
		return muts;
	}

	private String calcResistanceLevel() {
		if (fbResistanceLevel == null) {
			if (ineffective != null && (ineffective.equals("both") || ineffective.equals("control"))) {
				return "undetermined";
			}
			if (foldCmp != null && foldCmp.equals("<")) {
				if (fold != null && fold <= PARTIAL_RESIST_FOLD) {
					return "susceptible";
				}
				else if (fold != null && fold <= RESIST_FOLD) {
					return "lt-resistant";
				}
				else {
					return "undetermined";
				}
			}
			else if (foldCmp != null && foldCmp.equals(">")) {
				if (fold != null && fold >= RESIST_FOLD) {
					return "resistant";
				}
				else if (fold != null && fold >= PARTIAL_RESIST_FOLD) {
					return "gt-partial-resistance";
				}
				else {
					return "undetermined";
				}
			}
			else if (cumulativeCount == 1) {
				if (fold != null && fold < PARTIAL_RESIST_FOLD) {
					return "susceptible";
				}
				else if (fold != null && fold < RESIST_FOLD) {
					return "partial-resistance";
				}
				else {
					return "resistant";
				}
			}
			else {
				return "undetermined";
			}
		}
		else {
			return fbResistanceLevel;
		}
	}

	private final String drdbVersion;
	private final String refName;
	private final String rxName;
	private final String controlIsoName;
	private final String isoName;
	private final String section;
	private final String assayName;
	private final String foldCmp;
	private final Double fold;
	private final String fbResistanceLevel;
	private final String ineffective;
	private final Integer cumulativeCount;
	
	private transient Article reference;
	private transient String resistanceLevel;
	private transient Isolate controlIsolate;
	private transient Isolate isolate;
	private transient MutationSet<SARS2> comparableIsolateMutations;

	protected SuscResult(
		String drdbVersion,
		Map<String, Object> suscData
	) {
		this.drdbVersion = drdbVersion;
		
		refName = (String) suscData.get("refName");
		rxName = (String) suscData.get("rxName");
		controlIsoName = (String) suscData.get("controlIsoName");
		isoName = (String) suscData.get("isoName");
		assayName = (String) suscData.get("assayName");
		section = (String) suscData.get("section");
		foldCmp = (String) suscData.get("foldCmp");
		fold = (Double) suscData.get("fold");
		fbResistanceLevel = (String) suscData.get("fbResistanceLevel");
		ineffective = (String) suscData.get("ineffective");
		cumulativeCount = (Integer) suscData.get("cumulativeCount");
	}

	public String getResistanceLevel() {
		if (resistanceLevel == null) {
			resistanceLevel = calcResistanceLevel();
		}
		return resistanceLevel;
	}

	public Article getReference() {
		if (reference == null) {
			reference = Article.getInstance(drdbVersion, refName);
		}
		return reference;
	}

	public Isolate getControlIsolate() {
		if (controlIsolate == null) {
			controlIsolate = Isolate.getInstance(drdbVersion, controlIsoName);
		}
		return controlIsolate;
	}

	public Isolate getIsolate() {
		if (isolate == null) {
			isolate = Isolate.getInstance(drdbVersion, isoName);
		}
		return isolate;
	}

	public String getRefName() { return refName; }
	public String getRxName() { return rxName; }
	public String getAssayName() { return assayName; }
	public String getSection() { return section; }
	public String getFoldCmp() { return foldCmp; }
	public Double getFold() { return fold; }
	public String getIneffective() { return ineffective; }
	public Integer getCumulativeCount() { return cumulativeCount; }

	/**
	 * Internal use only for providing comparable isolate mutations (v.s. queryMuts)
	 * of SuscSummary
	 * 
	 * @return MutationSet
	 */
	protected MutationSet<SARS2> getComparableIsolateMutations() {
		if (comparableIsolateMutations == null) {
			comparableIsolateMutations = prepareQueryMutations(getIsolate().getMutations());
		}
		return comparableIsolateMutations;
	}
	
}
