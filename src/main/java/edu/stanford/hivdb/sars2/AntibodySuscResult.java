package edu.stanford.hivdb.sars2;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import edu.stanford.hivdb.mutations.GenePosition;
import edu.stanford.hivdb.mutations.Mutation;
import edu.stanford.hivdb.mutations.MutationSet;

public class AntibodySuscResult {

	private static final double PARTIAL_RESIST_FOLD = 3;
	private static final double RESIST_FOLD = 10;

	private static String calcResistanceLevel(String foldCmp, Double fold, String fallbackLevel) {
		if (fallbackLevel == null) {
			if (foldCmp.equals("<") || foldCmp.equals("~") || foldCmp.equals("=")) {
				if (fold <= PARTIAL_RESIST_FOLD) {
					return "susceptible";
				}
				else if (fold <= RESIST_FOLD) {
					return "partial-resistance";
				}
				else {
					return "resistant";
				}
			}
			else {  // foldCmp.equals(">")
				if (fold > RESIST_FOLD) {
					return "resistant";
				}
				else if (fold > PARTIAL_RESIST_FOLD) {
					return "gt-partial-resistance";
				}
				else {
					return "gt-susceptible";
				}
			}
		}
		else {
			return fallbackLevel;
		}
	}
	private final MutationSet<SARS2> queryMuts;

	private final String refName;
	private final String rxName;
	private final List<String> abNames;
	private final String controlStrainName;
	private final String strainName;
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
	private transient List<Antibody> antibodies;
	private transient VirusStrain controlVirusStrain;
	private transient VirusStrain virusStrain;
	private transient MutationSet<SARS2> hitMutations;
	private transient MutationSet<SARS2> missMutations;
	private transient Set<GenePosition<SARS2>> hitPositions;
	private transient Set<GenePosition<SARS2>> missPositions;
	
	public static List<AntibodySuscResult> query(MutationSet<SARS2> queryMuts) {
		SARS2 sars2 = SARS2.getInstance();
		final MutationSet<SARS2> realQueryMuts = queryMuts.filterBy(mut -> !mut.isUnsequenced());
		List<AntibodySuscResult> results = (
			sars2.getDRDBObj()
			.querySuscResultsForAntibodies(queryMuts)
			.stream()
			.map(d -> new AntibodySuscResult(realQueryMuts, d))
			.collect(Collectors.toList())
		);
		
		results.sort((a, b) -> {
			// sorting order: [numMissPos, numMissMut, -numHitMut, -numHitPos]
			int aMissPos = a.getNumMissPositions();
			int bMissPos = b.getNumMissPositions();
			if (aMissPos == bMissPos) {
				int aMissMut = a.getNumMissMutations();
				int bMissMut = b.getNumMissMutations();
				if (aMissMut == bMissMut) {
					int aHitMut = a.getNumHitMutations();
					int bHitMut = b.getNumHitMutations();
					if (aHitMut == bHitMut) {
						int aHitPos = a.getNumHitPositions();
						int bHitPos = b.getNumHitPositions();
						return bHitPos - aHitPos;  // descending order
					}
					return bHitMut - aHitMut;  // descending order
				}
				return aMissMut - bMissMut;
			}
			else {
				return aMissPos - bMissPos;
			}
		});
		
		return results;
	}
	
	private AntibodySuscResult(MutationSet<SARS2> queryMuts, Map<String, Object> suscData) {
		this.queryMuts = queryMuts;
		
		refName = (String) suscData.get("refName");
		rxName = (String) suscData.get("rxName");

		@SuppressWarnings("unchecked")
		List<String> abNames = (List<String>) suscData.get("abNames");
		this.abNames = abNames;

		controlStrainName = (String) suscData.get("controlStrainName");
		strainName = (String) suscData.get("strainName");
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
			resistanceLevel = calcResistanceLevel(foldCmp, fold, fbResistanceLevel);
		}
		return resistanceLevel;
	}

	public Article getReference() {
		if (reference == null) {
			reference = Article.getInstance(refName);
		}
		return reference;
	}

	public List<Antibody> getAntibodies() {
		if (antibodies == null) {
			antibodies = (
				abNames.stream()
				.map(abName -> Antibody.getInstance(abName))
				.collect(Collectors.toList())
			);
		}
		return antibodies;
	}

	public VirusStrain getControlVirusStrain() {
		if (controlVirusStrain == null) {
			controlVirusStrain = VirusStrain.getInstance(controlStrainName);
		}
		return controlVirusStrain;
	}

	public VirusStrain getVirusStrain() {
		if (virusStrain == null) {
			virusStrain = VirusStrain.getInstance(strainName);
		}
		return virusStrain;
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
			hitMutations = queryMuts.intersectsWith(getVirusStrain().getMutations());
		}
		return hitMutations;
	}
	
	public Set<GenePosition<SARS2>> getHitPositions() {
		if (hitPositions == null) {
			hitPositions = (
				queryMuts.stream()
				.map(Mutation::getGenePosition)
				.collect(Collectors.toCollection(TreeSet::new))
			);
	
			Set<GenePosition<SARS2>> strainPos = (
				getVirusStrain().getMutations().stream()
				.map(Mutation::getGenePosition)
				.collect(Collectors.toCollection(TreeSet::new))
			);
			hitPositions.retainAll(strainPos);
		}
		return hitPositions;
	}
	
	public Integer getNumHitMutations() {
		return getHitMutations().size();
	}
	
	public Integer getNumHitPositions() {
		return getHitPositions().size();
	}
	
	public MutationSet<SARS2> getMissMutations() {
		if (missMutations == null) {
			MutationSet<SARS2> otherMuts = getVirusStrain().getMutations();
			missMutations = (
				queryMuts
				.subtractsBy(otherMuts)
				.mergesWith(
					otherMuts
					.subtractsBy(queryMuts)
				)
			);
		}
		return missMutations;
	}
	
	public Set<GenePosition<SARS2>> getMissPositions() {
		if (missPositions == null) {
			Set<GenePosition<SARS2>> queryPos = (
				queryMuts.stream()
				.map(Mutation::getGenePosition)
				.collect(Collectors.toCollection(TreeSet::new))
			);
	
			Set<GenePosition<SARS2>> strainPos = (
				getVirusStrain().getMutations().stream()
				.map(Mutation::getGenePosition)
				.collect(Collectors.toCollection(TreeSet::new))
			);
			missPositions = new TreeSet<>();
			missPositions.addAll(queryPos);
			missPositions.addAll(strainPos);
			queryPos.retainAll(strainPos);
			missPositions.removeAll(queryPos);
		}
		return missPositions;
	}
	
	public Integer getNumMissMutations() {
		return getMissMutations().size();
	}
	
	public Integer getNumMissPositions() {
		return getMissPositions().size();
	}
}
