package edu.stanford.hivdb.sars2.drdb;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import edu.stanford.hivdb.mutations.MutationSet;
import edu.stanford.hivdb.sars2.SARS2;

public class SuscSummary {
	
	protected static List<SuscSummaryByRLevel> groupByRLevel(List<SuscResult> suscResults) {
		Map<String, List<SuscResult>> byRLevel = (
			suscResults.stream()
			.collect(Collectors.groupingBy(
				sr -> sr.getResistanceLevel(),
				LinkedHashMap::new,
				Collectors.toList()
			))
		);
		
		return byRLevel.entrySet()
			.stream()
			.map(entry -> new SuscSummaryByRLevel(
				entry.getKey(),
				entry.getValue()
			))
			.collect(Collectors.toList());
		
	}

	protected static List<SuscSummaryByAntibodyClass> groupByAntibodyClass(List<SuscResult> suscResults) {
		Map<String, List<SuscResult>> byAntibodyClass = (
			suscResults.stream()
			.filter(sr -> (
				(sr instanceof AntibodySuscResult) &&
				((AntibodySuscResult) sr).getAntibodies().size() == 1 &&
				((AntibodySuscResult) sr).getAntibodies()
				.stream()
				.allMatch(ab -> ab.getAntibodyClass() != null)
			))
			.collect(Collectors.groupingBy(
				sr -> ((AntibodySuscResult) sr).getAntibodies().iterator().next().getAntibodyClass(),
				LinkedHashMap::new,
				Collectors.toList()
			))
		);
		
		return byAntibodyClass.entrySet()
			.stream()
			.map(entry -> new SuscSummaryByAntibodyClass(
				entry.getKey(),
				entry.getValue()
			))
			.collect(Collectors.toList());
	}
	
	protected static List<SuscSummaryByAntibody> groupByAntibody(List<SuscResult> suscResults) {
		Map<Set<Antibody>, List<SuscResult>> byAntibodies = (
			suscResults.stream()
			.filter(sr -> (
				(sr instanceof AntibodySuscResult) &&
				((AntibodySuscResult) sr).getAntibodies()
				.stream()
				.allMatch(ab -> ab.getVisibility())
			))
			.collect(Collectors.groupingBy(
				sr -> ((AntibodySuscResult) sr).getAntibodies(),
				LinkedHashMap::new,
				Collectors.toList()
			))
		);
		
		return byAntibodies.entrySet()
			.stream()
			.map(entry -> new SuscSummaryByAntibody(
				entry.getKey(),
				entry.getValue()
			))
			.collect(Collectors.toList());
	}
	
	protected static List<SuscSummaryByKeyMutations> groupByKeyMutations(
		List<SuscResult> results
	) {
		Map<MutationSet<SARS2>, List<SuscResult>> byMutations = (
			results.stream()
			.collect(Collectors.groupingBy(
				sr -> sr.getVirusVariant().getKeyMutationGroups(),
				LinkedHashMap::new,
				Collectors.toList()
			))
			.values()
			.stream()
			.collect(Collectors.toMap(
				srs -> new MutationSet<>(
					srs.stream()
					.map(sr -> sr.getVirusVariant().getKeyMutations().getSplitted())
					.flatMap(Set::stream)
					.collect(Collectors.toSet())
				),
				srs -> srs,
				(a, b) -> {
					throw new RuntimeException("Key mutations conflict");
				},
				LinkedHashMap::new
			))
		);

		List<SuscSummaryByKeyMutations> summaryResults = byMutations.entrySet()
			.stream()
			.map(entry -> new SuscSummaryByKeyMutations(
				entry.getKey(),
				entry.getValue()
			))
			.collect(Collectors.toList());
		summaryResults.sort((a, b) -> {
			// sorting order: [numKeyMutGroups, mutations]
			int aNumKeyMutGroups = a.getNumKeyMutGroups();
			int bNumKeyMutGroups = b.getNumKeyMutGroups();
			int cmp = Math.min(2, aNumKeyMutGroups) - Math.min(2, bNumKeyMutGroups); 
			if (cmp != 0) {
				return cmp;
			}
			
			MutationSet<SARS2> aMuts = a.getMutations();
			MutationSet<SARS2> bMuts = b.getMutations();
			cmp = aMuts.compareTo(bMuts);
			return cmp;
		});
		return summaryResults;
	}
	
	public static List<SuscSummaryByKeyMutations> getAntibodySuscSummaryItems(String drdbVersion, MutationSet<SARS2> queryMuts) {
		queryMuts = queryMuts.filterBy(mut -> !mut.isUnsequenced());
		List<SuscResult> results = (
			AntibodySuscResult.query(drdbVersion, queryMuts)
			.stream()
			.filter(r -> (
				(
					r.getVirusVariant().hasKeyMutations() &&
					r.isEveryVariantKeyMutationHit()
				) &&
				r.getAntibodies().stream().allMatch(
					ab -> (
						ab.getVisibility() ||
						ab.getAntibodyClass() != null		
					)
				)
			))
			.collect(Collectors.toList())
		);
		return groupByKeyMutations(results);
	}

}
