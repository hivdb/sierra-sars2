package edu.stanford.hivdb.sars2.drdb;

import java.util.Collections;
import java.util.List;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

public abstract class WithCumulativeSuscResults {
	private transient DescriptiveStatistics cumulativeFold;
	private transient Integer cumulativeCount;
	private transient List<SuscSummaryByAntibody> itemsByAntibody;
	private transient List<SuscSummaryByAntibodyClass> itemsByAntibodyClass;
	private transient List<SuscSummaryByRLevel> itemsByResistLevel;
	private transient List<SuscSummaryByKeyMutations> itemsByKeyMuts;
	
	abstract public List<SuscResult> getItems();
	
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

	public List<SuscSummaryByAntibody> getItemsByAntibody() {
		if (itemsByAntibody == null) {
			itemsByAntibody = Collections.unmodifiableList(SuscSummary.groupByAntibody(getItems()));
		}
		return itemsByAntibody;
	}
	public List<SuscSummaryByAntibodyClass> getItemsByAntibodyClass() {
		if (itemsByAntibodyClass == null) {
			itemsByAntibodyClass = Collections.unmodifiableList(SuscSummary.groupByAntibodyClass(getItems()));
		}
		return itemsByAntibodyClass;
	}
	public List<SuscSummaryByRLevel> getItemsByResistLevel() {
		if (itemsByResistLevel == null) {
			itemsByResistLevel = Collections.unmodifiableList(SuscSummary.groupByRLevel(getItems()));
		}
		return itemsByResistLevel;
	}
	public List<SuscSummaryByKeyMutations> getItemsByKeyMutations() {
		if (itemsByKeyMuts == null) {
			itemsByKeyMuts = Collections.unmodifiableList(SuscSummary.groupByKeyMutations(getItems()));
		}
		return itemsByKeyMuts;
	}
}