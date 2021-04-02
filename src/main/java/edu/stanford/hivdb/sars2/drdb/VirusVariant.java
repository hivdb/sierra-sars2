package edu.stanford.hivdb.sars2.drdb;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.Sets;

import edu.stanford.hivdb.mutations.GenePosition;
import edu.stanford.hivdb.mutations.Mutation;
import edu.stanford.hivdb.mutations.MutationSet;
import edu.stanford.hivdb.sars2.SARS2;

public class VirusVariant {

	private final static Map<String, Map<String, VirusVariant>> singletons = DRDB.initVersionalSingletons();

	private static void updateSingletons(String drdbVersion) {
		DRDB.addVersionToVersionalSingletons(drdbVersion, singletons, drdb -> {
			List<Map<String, Object>> allVVs = drdb.queryAllVirusVariants();
			return (
				allVVs.stream()
				.map(vv -> new VirusVariant(drdb, vv))
				.collect(Collectors.toMap(
					vs -> vs.getName(),
					vs -> vs,
					(vs1, vs2) -> vs1,
					LinkedHashMap::new
				))
			);
		});
	}

	public static VirusVariant getInstance(String drdbVersion, String variantName) {
		updateSingletons(drdbVersion);
		return singletons.get(drdbVersion).get(variantName);
	}

	public static Collection<VirusVariant> getAllInstances(String drdbVersion) {
		updateSingletons(drdbVersion);
		return singletons.get(drdbVersion).values();
	}

	private final DRDB drdb;
	private final String variantName;
	private final String displayName;
	private final MutationSet<SARS2> mutations;
	private transient Set<MutationSet<SARS2>> keyMutationSearchGroups;

	private VirusVariant(DRDB drdb, Map<String, Object> refData) {
		this.drdb = drdb;
		variantName = (String) refData.get("variantName");
		displayName = (String) refData.get("displayName");

		@SuppressWarnings("unchecked")
		List<Mutation<SARS2>> mutList = (List<Mutation<SARS2>>) refData.get("mutations");

		mutations = new MutationSet<>(mutList);
	}

	public String name() { return variantName; }
	public String getName() { return variantName; }
	public String getDisplayName() { return displayName; }

	public MutationSet<SARS2> getMutations() { return mutations; }
	public MutationSet<SARS2> getKeyMutations() {
		Set<MutationSet<SARS2>> allKeyMutations = drdb.queryAllKeyMutations();
		return new MutationSet<>(
			mutations.stream()
			.filter(mut -> allKeyMutations.stream().anyMatch(
				mutSet -> mutSet.hasSharedAAMutation(mut)
			))
			.collect(Collectors.toList())
		);
	}

	public boolean hasKeyMutations() {
		return !getKeyMutationGroups().isEmpty();
	}
	
	public Set<MutationSet<SARS2>> getKeyMutationGroups() {
		if (keyMutationSearchGroups == null) {
			keyMutationSearchGroups = Collections.unmodifiableSet(
				drdb.queryAllKeyMutations()
				.stream()
				.filter(mutSet -> !mutSet.intersectsWith(mutations).isEmpty())
				.collect(Collectors.toSet())
			);
		}
		return keyMutationSearchGroups;
	}
	
	public int getNumKeyMutationGroups() {
		return getKeyMutationGroups().size();
	}
	
	public MutationSet<SARS2> getHitMutations(MutationSet<SARS2> queryMuts) {
		return queryMuts.intersectsWith(mutations);
	}
	
	public MutationSet<SARS2> getHitKeyMutations(MutationSet<SARS2> queryMuts) {
		Set<MutationSet<SARS2>> keyMutationGroups = getKeyMutationGroups();
		return new MutationSet<>(
			queryMuts.stream()
			.filter(mut -> (
				keyMutationGroups.stream()
				.anyMatch(
					mutSet -> mutSet.hasSharedAAMutation(mut)
				)
			))
			.collect(Collectors.toList())
		);
	}
	
	public Integer getNumHitKeyMutationGroups(MutationSet<SARS2> queryMuts) {
		return Long.valueOf(
			getKeyMutationGroups()
			.stream()
			.filter(mutSet -> !mutSet.intersectsWith(queryMuts).isEmpty())
			.count()
		).intValue();
	}
	
	public boolean isEveryVariantKeyMutationHit(MutationSet<SARS2> queryMuts) {
		Set<MutationSet<SARS2>> keyMutationGroups = getKeyMutationGroups();
		return keyMutationGroups.stream()
			.allMatch(mutSet -> !mutSet.intersectsWith(queryMuts).isEmpty());
	}	
	
	public boolean isAllKeyMutationsMatched(MutationSet<SARS2> queryMuts) {
		return getKeyMutationGroups().equals(
			drdb.queryAllKeyMutations()
			.stream()
			.filter(mutSet -> !mutSet.intersectsWith(queryMuts).isEmpty())
			.collect(Collectors.toSet())
		);
	}
	
	public Set<GenePosition<SARS2>> getHitPositions(MutationSet<SARS2> queryMuts) {
		
		return Sets.intersection(
			mutations.getPositions(),
			queryMuts.getPositions()
		);
	}
	
	public MutationSet<SARS2> getMissMutations(MutationSet<SARS2> queryMuts) {
		
		return new MutationSet<>(
			Sets.symmetricDifference(
				mutations.getSplitted(),
				queryMuts.getSplitted()
			)
		);
	}
	
	public Set<GenePosition<SARS2>> getMissPositions(MutationSet<SARS2> queryMuts) {
		
		return Sets.symmetricDifference(
			mutations.getPositions(),
			queryMuts.getPositions()
		);
	}
	
	@Override
	public String toString() {
		return variantName;
	}

}