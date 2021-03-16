package edu.stanford.hivdb.sars2;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
			int aMiss = a.getNumMissMutations();
			int bMiss = b.getNumMissMutations();
			if (aMiss == bMiss) {
				int aHit = a.getNumHitMutations();
				int bHit = b.getNumHitMutations();
				return bHit - aHit;  // descending order
			}
			else {
				return aMiss - bMiss;
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
		return queryMuts.intersectsWith(getVirusStrain().getMutations());
	}
	
	public Integer getNumHitMutations() {
		return getHitMutations().size();
	}
	
	public MutationSet<SARS2> getMissMutations() {
		MutationSet<SARS2> otherMuts = getVirusStrain().getMutations();
		return (
			queryMuts
			.subtractsBy(otherMuts)
			.mergesWith(
				otherMuts
				.subtractsBy(queryMuts)
			)
		);
	}
	
	public Integer getNumMissMutations() {
		return getMissMutations().size();
	}
}
