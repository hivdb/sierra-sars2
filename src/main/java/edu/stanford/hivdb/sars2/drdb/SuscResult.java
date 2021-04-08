package edu.stanford.hivdb.sars2.drdb;

import java.util.Map;
// import java.util.Set;

// import edu.stanford.hivdb.mutations.GenePosition;
import edu.stanford.hivdb.mutations.MutationSet;
import edu.stanford.hivdb.sars2.SARS2;

public abstract class SuscResult {

	private static final double PARTIAL_RESIST_FOLD = 3;
	private static final double RESIST_FOLD = 10;

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
	private transient MutationSet<SARS2> hitMutations;
	// private transient MutationSet<SARS2> missMutations;
	// private transient Set<GenePosition<SARS2>> hitPositions;
	// private transient Set<GenePosition<SARS2>> missPositions;
	
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
	
	public MutationSet<SARS2> getHitMutations() {
		if (hitMutations == null) {
			hitMutations = getVirusVariant().getHitMutations(queryMuts);
		}
		return hitMutations;
	}
	
	public MutationSet<SARS2> getHitKeyMutations() {
		return getVirusVariant().getHitKeyMutations(queryMuts);
	}
	
	public Integer getNumHitKeyMutationGroups() {
		return getVirusVariant().getNumHitKeyMutationGroups(queryMuts);
	}
	
	public boolean isEveryVariantKeyMutationHit() {
		return getVirusVariant().isEveryVariantKeyMutationHit(queryMuts);
	}
	
	public boolean isAllKeyMutationsMatched() {
		return getVirusVariant().isAllKeyMutationsMatched(queryMuts);
	}
	
	/*public Set<GenePosition<SARS2>> getHitPositions() {
		if (hitPositions == null) {
			hitPositions = getVirusVariant().getHitPositions(queryMuts);
		}
		return hitPositions;
	}
	
	public Integer getNumHitKeyMutations() {
		return getHitKeyMutations().size();
	}
	
	public Integer getNumHitMutations() {
		return getHitMutations().size();
	}
	
	public Integer getNumHitPositions() {
		return getHitPositions().size();
	}
	
	public MutationSet<SARS2> getMissMutations() {
		if (missMutations == null) {
			missMutations = getVirusVariant().getMissMutations(queryMuts);
		}
		return missMutations;
	}
	
	public Set<GenePosition<SARS2>> getMissPositions() {
		if (missPositions == null) {
			missPositions = getVirusVariant().getMissPositions(queryMuts);
		}
		return missPositions;
	}
	
	public Integer getNumMissMutations() {
		return getMissMutations().size();
	}
	
	public Integer getNumMissPositions() {
		return getMissPositions().size();
	} */
}
