package edu.stanford.hivdb.sars2.drdb;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import edu.stanford.hivdb.mutations.MutationSet;
import edu.stanford.hivdb.sars2.SARS2;

public class SuscSummary {
	private List<SuscResult> items;
	private transient DescriptiveStatistics cumulativeFold;
	private transient Integer cumulativeCount;
	private transient List<AntibodySuscSummary> itemsByAntibody;
	private transient List<AntibodyClassSuscSummary> itemsByAntibodyClass;
	private transient List<ResistLevelSuscSummary> itemsByResistLevel;
	private transient List<MutsSuscSummary> itemsByKeyMuts;
	private transient List<VaccineSuscSummary> itemsByVaccine;
	
	public static SuscSummary queryAntibodySuscSummary(String drdbVersion, MutationSet<SARS2> queryMuts) {
		List<SuscResult> results = (
			AntibodySuscResult.query(drdbVersion, queryMuts)
			.stream()
			.filter(r -> (
				r.getAntibodies().stream().allMatch(
					ab -> (
						ab.getVisibility() ||
						ab.getAntibodyClass() != null		
					)
				)
			))
			.collect(Collectors.toList())
		);
		return new SuscSummary(results);
	}
	
	public static SuscSummary queryConvPlasmaSuscSummary(String drdbVersion, MutationSet<SARS2> queryMuts) {
		List<SuscResult> results = (
			ConvPlasmaSuscResult.query(drdbVersion, queryMuts)
			.stream()
			.collect(Collectors.toList())
		);
		return new SuscSummary(results);
	}

	public static SuscSummary queryVaccPlasmaSuscSummary(String drdbVersion, MutationSet<SARS2> queryMuts) {
		List<SuscResult> results = (
			VaccPlasmaSuscResult.query(drdbVersion, queryMuts)
			.stream()
			.collect(Collectors.toList())
		);
		return new SuscSummary(results);
	}
	
	protected SuscSummary(List<SuscResult> items) {
		this.items = Collections.unmodifiableList(
			items.stream()
			.sorted((itemA, itemB) -> {
				int cmp = itemA.getMatchType().compareTo(itemB.getMatchType());
				if (cmp != 0) { return cmp; }
				int cmpVariantOnly = itemA.getNumVariantOnlyMutations() - itemB.getNumVariantOnlyMutations();
				int cmpQueryOnly = itemA.getNumQueryOnlyMutations() - itemB.getNumQueryOnlyMutations();
				cmp = cmpVariantOnly + cmpQueryOnly;
				if (cmp != 0) { return cmp; }
				if (cmpVariantOnly != 0) { return cmpVariantOnly; }
				if (cmpQueryOnly != 0) { return cmpQueryOnly; }
				return itemA.getComparableVariantMutations().compareTo(itemB.getComparableVariantMutations());
			})
			.collect(Collectors.toList())
		);
	}
	
	public List<SuscResult> getItems() { return items; }

	public Set<Article> getReferences() {
		return (
			items.stream()
			.map(item -> item.getReference())
			.collect(Collectors.toSet())
		);
	}
	
	public DescriptiveStatistics getCumulativeFold() {
		if (cumulativeFold == null) {
			double[] allFolds = (
				getItems().stream()
				.map(sr -> Collections.nCopies(sr.getCumulativeCount(), sr.getFold()))
				.flatMap(List::stream)
				.filter(fold -> fold != null)
				.mapToDouble(Number::doubleValue)
				.toArray()
			);
			cumulativeFold = new DescriptiveStatistics(allFolds);
		}
		return cumulativeFold;
	}
	
	public Integer getCumulativeCount() {
		if (cumulativeCount == null) {
			cumulativeCount = (
				getItems().stream()
				.mapToInt(
					sr -> sr.getCumulativeCount()
				)
				.sum()
			);
		}
		return cumulativeCount;
	}

	public List<AntibodySuscSummary> getItemsByAntibody() {
		if (itemsByAntibody == null) {
			Map<Set<Antibody>, List<SuscResult>> byAntibodies = (
				items.stream()
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
			
			List<AntibodySuscSummary> summaryResults = byAntibodies.entrySet()
				.stream()
				.map(entry -> new AntibodySuscSummary(
					entry.getKey(),
					entry.getValue()
				))
				.collect(Collectors.toList());

			itemsByAntibody = Collections.unmodifiableList(summaryResults);
		}
		return itemsByAntibody;
	}
	public List<AntibodyClassSuscSummary> getItemsByAntibodyClass() {
		if (itemsByAntibodyClass == null) {
			
			
			Map<String, List<SuscResult>> byAntibodyClass = (
				items.stream()
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
			
			List<AntibodyClassSuscSummary> summaryResults = byAntibodyClass.entrySet()
				.stream()
				.map(entry -> new AntibodyClassSuscSummary(
					entry.getKey(),
					entry.getValue()
				))
				.collect(Collectors.toList());
			
			
			itemsByAntibodyClass = Collections.unmodifiableList(summaryResults);
		}
		return itemsByAntibodyClass;
	}
	
	public List<ResistLevelSuscSummary> getItemsByResistLevel() {
		if (itemsByResistLevel == null) {
			
			Map<String, List<SuscResult>> byRLevel = (
				items.stream()
				.collect(Collectors.groupingBy(
					sr -> sr.getResistanceLevel(),
					LinkedHashMap::new,
					Collectors.toList()
				))
			);
			
			List<ResistLevelSuscSummary> summaryResults = byRLevel.entrySet()
				.stream()
				.map(entry -> new ResistLevelSuscSummary(
					entry.getKey(),
					entry.getValue()
				))
				.collect(Collectors.toList());
			
			itemsByResistLevel = Collections.unmodifiableList(summaryResults);
		}
		return itemsByResistLevel;
	}
	
	public List<VaccineSuscSummary> getItemsByVaccine() {
		if (itemsByVaccine == null) {
			Map<String, List<SuscResult>> byVaccine = (
				items.stream()
				.collect(Collectors.groupingBy(
					sr -> ((VaccPlasmaSuscResult) sr).getVaccineName(),
					LinkedHashMap::new,
					Collectors.toList()
				))
			);
			
			List<VaccineSuscSummary> summaryResults = byVaccine.entrySet()
				.stream()
				.map(entry -> new VaccineSuscSummary(
					entry.getKey(),
					entry.getValue()
				))
				.collect(Collectors.toList());
			
			itemsByVaccine = Collections.unmodifiableList(summaryResults);
		}
		return itemsByVaccine;
	}
	
	public List<MutsSuscSummary> getItemsByMutations() {
		if (itemsByKeyMuts == null) {
			Map<MutationSet<SARS2>, List<SuscResult>> byMutations = (
				items.stream()
				.collect(Collectors.groupingBy(
					sr -> sr.getComparableVariantMutations(),
					LinkedHashMap::new,
					Collectors.toList()
				))
				.values()
				.stream()
				.collect(Collectors.toMap(
					srs -> new MutationSet<>(
						srs.stream()
						// We only display mutations that really exist in the variant;
						// this is useful when not all range of a deletion is covered
						// by the variant
						.map(sr -> sr.getVirusVariant().getMutations().getSplitted())
						.flatMap(Set::stream)
						.collect(Collectors.toSet())
					// except for excluded mutations such as D614G
					).subtractsBy(SuscResult.EXCLUDE_MUTATIONS),
					srs -> srs,
					(a, b) -> {
						throw new RuntimeException("Key mutations conflict");
					},
					LinkedHashMap::new
				))
			);

			List<MutsSuscSummary> summaryResults = byMutations.entrySet()
				.stream()
				.map(entry -> new MutsSuscSummary(
					entry.getKey(),
					entry.getValue()
				))
				.collect(Collectors.toList());

			itemsByKeyMuts = Collections.unmodifiableList(summaryResults);
		}
		return itemsByKeyMuts;
	}
}