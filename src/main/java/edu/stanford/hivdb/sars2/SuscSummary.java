package edu.stanford.hivdb.sars2;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import edu.stanford.hivdb.mutations.MutationSet;

public class SuscSummary {

	public static class SuscSummaryByRLevel {
		private String resistanceLevel;
		private List<AntibodySuscResult> items;
		private transient Integer cumulativeCount;

		public SuscSummaryByRLevel(
			String resistanceLevel,
			List<AntibodySuscResult> items
		) {
			this.resistanceLevel = resistanceLevel;
			this.items = Collections.unmodifiableList(items);
		}

		public Integer getCumulativeCount() {
			if (cumulativeCount == null) {
				cumulativeCount = (
					items.stream()
					.mapToInt(
						sr -> sr.getCumulativeCount()
					)
					.sum()
				);
			}
			return cumulativeCount;
		}

		public String getResistanceLevel() { return resistanceLevel; }
		public List<AntibodySuscResult> getItems() { return items; }
	}

	public static class SuscSummaryByAntibody {
		private List<Antibody> antibodies;
		private List<SuscSummaryByRLevel> items;

		public SuscSummaryByAntibody(
			List<Antibody> antibodies,
			List<SuscSummaryByRLevel> items
		) {
			this.antibodies = antibodies;
			this.items = Collections.unmodifiableList(items);
		}

		public List<Antibody> getAntibodies() { return antibodies; }
		public List<SuscSummaryByRLevel> getItems() { return items; }

	}
	
	public static class SuscSummaryByAntibodyClass {
		private String antibodyClass;
		private List<SuscSummaryByRLevel> items;
		
		public SuscSummaryByAntibodyClass(
			String antibodyClass,
			List<SuscSummaryByRLevel> items
		) {
			this.antibodyClass = antibodyClass;
			this.items = Collections.unmodifiableList(items);
		}
		
		public String getAntibodyClass() { return antibodyClass; }
		public List<SuscSummaryByRLevel> getItems() { return items; }
		
	}
	
	public static class SuscSummaryByMutationSet {
		private MutationSet<SARS2> mutations;
		private MutationSet<SARS2> hitMutations;
		private List<SuscSummaryByAntibody> itemsByAntibody;
		private List<SuscSummaryByAntibodyClass> itemsByAntibodyClass;

		public SuscSummaryByMutationSet(
			MutationSet<SARS2> queryMuts,
			MutationSet<SARS2> mutations,
			List<SuscSummaryByAntibody> itemsByAntibody,
			List<SuscSummaryByAntibodyClass> itemsByAntibodyClass
		) {
			this.mutations = mutations;
			this.hitMutations = queryMuts.intersectsWith(mutations);
			this.itemsByAntibody = Collections.unmodifiableList(itemsByAntibody);
			this.itemsByAntibodyClass = Collections.unmodifiableList(itemsByAntibodyClass);
		}
		
		public MutationSet<SARS2> getMutations() { return mutations; }
		public MutationSet<SARS2> getHitMutations() { return hitMutations; }
		public List<SuscSummaryByAntibody> getItemsByAntibody() { return itemsByAntibody; }
		public List<SuscSummaryByAntibodyClass> getItemsByAntibodyClass() { return itemsByAntibodyClass; }
	}

	private static List<SuscSummaryByRLevel> groupByRLevel(List<AntibodySuscResult> suscResults) {
		Map<String, List<AntibodySuscResult>> byRLevel = (
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
				entry.getKey(), entry.getValue()
			))
			.collect(Collectors.toList());
		
	}

	private static List<SuscSummaryByAntibodyClass> groupByAntibodyClass(List<AntibodySuscResult> suscResults) {
		Map<String, List<AntibodySuscResult>> byAntibodyClass = (
			suscResults.stream()
			.filter(sr -> (
				sr.getAntibodies().size() == 1 &&
				sr.getAntibodies()
				.stream()
				.allMatch(ab -> ab.getAntibodyClass() != null)
			))
			.collect(Collectors.groupingBy(
				sr -> sr.getAntibodies().get(0).getAntibodyClass(),
				LinkedHashMap::new,
				Collectors.toList()
			))
		);
		
		return byAntibodyClass.entrySet()
			.stream()
			.map(entry -> new SuscSummaryByAntibodyClass(
				entry.getKey(),
				groupByRLevel(entry.getValue())
			))
			.collect(Collectors.toList());
	}
	
	private static List<SuscSummaryByAntibody> groupByAntibody(List<AntibodySuscResult> suscResults) {
		Map<String, List<AntibodySuscResult>> byAntibodies = (
			suscResults.stream()
			.filter(sr -> (
				sr.getAntibodies()
				.stream()
				.allMatch(ab -> ab.getAvailability() != null)
			))
			.collect(Collectors.groupingBy(
				sr -> {
					return sr.getAntibodies()
					.stream()
					.map(ab -> ab.getName())
					.sorted()
					.collect(Collectors.joining("+"));
				},
				LinkedHashMap::new,
				Collectors.toList()
			))
		);
		
		return byAntibodies.entrySet()
			.stream()
			.map(entry -> new SuscSummaryByAntibody(
				entry.getValue().get(0).getAntibodies(),
				groupByRLevel(entry.getValue())
			))
			.collect(Collectors.toList());
	}
	
	private static List<SuscSummaryByMutationSet> groupByMutationSet(
		MutationSet<SARS2> queryMuts,
		List<AntibodySuscResult> results
	) {
		Map<MutationSet<SARS2>, List<AntibodySuscResult>> byMutations = (
			results.stream()
			.collect(Collectors.groupingBy(
				sr -> sr.getVirusStrain().getMutations(),
				LinkedHashMap::new,
				Collectors.toList()
			))
		);
		
		return byMutations.entrySet()
			.stream()
			.map(entry -> new SuscSummaryByMutationSet(
				queryMuts,
				entry.getKey(),
				groupByAntibody(entry.getValue()),
				groupByAntibodyClass(entry.getValue())
			))
			.collect(Collectors.toList());
	}		
	
	public static List<SuscSummaryByMutationSet> getAntibodySuscSummaryItems(MutationSet<SARS2> queryMuts) {
		queryMuts = queryMuts.filterBy(mut -> !mut.isUnsequenced());
		List<AntibodySuscResult> results = (
			AntibodySuscResult.query(queryMuts)
			.stream()
			.filter(r -> (
				r.getAntibodies().stream().allMatch(
					ab -> (
						ab.getAvailability() != null ||
						ab.getAntibodyClass() != null		
					)
				)
			))
			.collect(Collectors.toList())
		);
		return groupByMutationSet(queryMuts, results);
	}

}
