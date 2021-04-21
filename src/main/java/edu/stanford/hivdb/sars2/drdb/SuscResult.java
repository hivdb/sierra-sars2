package edu.stanford.hivdb.sars2.drdb;

import java.util.List;
import java.util.Map;
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
	
	public enum VariantMatchType {
		EQUAL,     // variant mutation set equals to query mutation set
		SUPERSET,  //                      is the superset of query mutation set
		SUBSET,    //                      is the subset of query mutation set
		OVERLAP    //                      overlaps with query mutation set
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
			if (foldCmp.equals("<")) {
				if (fold <= PARTIAL_RESIST_FOLD) {
					return "susceptible";
				}
				else if (fold <= RESIST_FOLD) {
					return "lt-resistant";
				}
				else {
					return "undetermined";
				}
			}
			else if (foldCmp.equals(">")) {
				if (fold >= RESIST_FOLD) {
					return "resistant";
				}
				else if (fold >= PARTIAL_RESIST_FOLD) {
					return "gt-partial-resistance";
				}
				else {
					return "undetermined";
				}
			}
			else if (cumulativeCount == 1) {
				if (fold < PARTIAL_RESIST_FOLD) {
					return "susceptible";
				}
				else if (fold < RESIST_FOLD) {
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

	private final MutationSet<SARS2> queryMuts;

	private final String drdbVersion;
	private final String refName;
	private final String rxName;
	private final String controlVariantName;
	private final String variantName;
	private final Integer ordinalNumber;
	private final String section;
	private final String assay;
	private final String foldCmp;
	private final Double fold;
	private final String fbResistanceLevel;
	private final String ineffective;
	private final Integer cumulativeCount;
	
	private transient Article reference;
	private transient String resistanceLevel;
	private transient VirusVariant controlVirusVariant;
	private transient VirusVariant virusVariant;
	private transient MutationSet<SARS2> comparableVariantMutations;

	protected SuscResult(
		String drdbVersion,
		MutationSet<SARS2> queryMuts,
		Map<String, Object> suscData
	) {
		this.drdbVersion = drdbVersion;
		this.queryMuts = queryMuts;
		
		refName = (String) suscData.get("refName");
		rxName = (String) suscData.get("rxName");
		controlVariantName = (String) suscData.get("controlVariantName");
		variantName = (String) suscData.get("variantName");
		ordinalNumber = (Integer) suscData.get("ordinalNumber");
		assay = (String) suscData.get("assay");
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

	public VirusVariant getControlVirusVariant() {
		if (controlVirusVariant == null) {
			controlVirusVariant = VirusVariant.getInstance(drdbVersion, controlVariantName);
		}
		return controlVirusVariant;
	}

	public VirusVariant getVirusVariant() {
		if (virusVariant == null) {
			virusVariant = VirusVariant.getInstance(drdbVersion, variantName);
		}
		return virusVariant;
	}

	public String getRefName() { return refName; }
	public String getRxName() { return rxName; }
	public Integer getOrdinalNumber() { return ordinalNumber; }
	public String getAssay() { return assay; }
	public String getSection() { return section; }
	public String getFoldCmp() { return foldCmp; }
	public Double getFold() { return fold; }
	public String getIneffective() { return ineffective; }
	public Integer getCumulativeCount() { return cumulativeCount; }

	public VariantMatchType getMatchType() {
		MutationSet<SARS2> varMutations = getComparableVariantMutations();
		if (varMutations.equals(queryMuts)) {
			return VariantMatchType.EQUAL;
		}
		else if (varMutations.containsAll(queryMuts)) {
			return VariantMatchType.SUPERSET;
		}
		else if (queryMuts.containsAll(varMutations)) {
			return VariantMatchType.SUBSET;
		}
		return VariantMatchType.OVERLAP;
	}
	
	/**
	 * Internal use only for providing comparable variant mutations (v.s. queryMuts)
	 * of SuscSummary
	 * 
	 * @return MutationSet
	 */
	protected MutationSet<SARS2> getComparableVariantMutations() {
		if (comparableVariantMutations == null) {
			comparableVariantMutations = prepareQueryMutations(getVirusVariant().getMutations());
		}
		return comparableVariantMutations;
	}
	
	public Integer getNumVariantOnlyMutations() {
		MutationSet<SARS2> variantOnlyMutations = getComparableVariantMutations().subtractsBy(queryMuts);
		Integer numVariantOnlyMuts = variantOnlyMutations.getSplitted().size();
		for (Set<Mutation<SARS2>> rangeDel : RANGE_DELETIONS) {
			if (!variantOnlyMutations.intersectsWith(rangeDel).isEmpty()) {
				numVariantOnlyMuts -= rangeDel.size() - 1;
			}
		}
		return numVariantOnlyMuts;
	}
	
	public Integer getNumQueryOnlyMutations() {
		MutationSet<SARS2> queryOnlyMutations = queryMuts.subtractsBy(getComparableVariantMutations());
		Integer numQueryOnlyMuts = queryOnlyMutations.getSplitted().size();
		for (Set<Mutation<SARS2>> rangeDel : RANGE_DELETIONS) {
			if (!queryOnlyMutations.intersectsWith(rangeDel).isEmpty()) {
				numQueryOnlyMuts -= rangeDel.size() - 1;
			}
		}
		return numQueryOnlyMuts;
	}
	
}
