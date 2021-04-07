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
	private transient List<KeyMutsSuscSummary> itemsByKeyMuts;
	private transient List<VaccineSuscSummary> itemsByVaccine;
	
	public static SuscSummary queryAntibodySuscSummary(String drdbVersion, MutationSet<SARS2> queryMuts) {
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
		return new SuscSummary(results);
	}
	
	public static SuscSummary queryConvPlasmaSuscSummary(String drdbVersion, MutationSet<SARS2> queryMuts) {
		queryMuts = queryMuts.filterBy(mut -> !mut.isUnsequenced());
		List<SuscResult> results = (
			ConvPlasmaSuscResult.query(drdbVersion, queryMuts)
			.stream()
			.filter(r -> (
				r.getVirusVariant().hasKeyMutations() &&
				r.isEveryVariantKeyMutationHit()
			))
			.collect(Collectors.toList())
		);
		return new SuscSummary(results);
	}

	public static SuscSummary queryVaccPlasmaSuscSummary(String drdbVersion, MutationSet<SARS2> queryMuts) {
		queryMuts = queryMuts.filterBy(mut -> !mut.isUnsequenced());
		List<SuscResult> results = (
			VaccPlasmaSuscResult.query(drdbVersion, queryMuts)
			.stream()
			.filter(r -> (
				r.getVirusVariant().hasKeyMutations() &&
				r.isEveryVariantKeyMutationHit()
			))
			.collect(Collectors.toList())
		);
		return new SuscSummary(results);
	}
	
	protected SuscSummary(List<SuscResult> items) {
		this.items = Collections.unmodifiableList(items);
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
	
	public List<KeyMutsSuscSummary> getItemsByKeyMutations() {
		if (itemsByKeyMuts == null) {
			
			Map<MutationSet<SARS2>, List<SuscResult>> byMutations = (
				items.stream()
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

			List<KeyMutsSuscSummary> summaryResults = byMutations.entrySet()
				.stream()
				.map(entry -> new KeyMutsSuscSummary(
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
			
			itemsByKeyMuts = Collections.unmodifiableList(summaryResults);
		}
		return itemsByKeyMuts;
	}
}