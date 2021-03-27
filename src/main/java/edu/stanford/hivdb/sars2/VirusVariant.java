package edu.stanford.hivdb.sars2;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import edu.stanford.hivdb.mutations.Mutation;
import edu.stanford.hivdb.mutations.MutationSet;

public class VirusVariant {

	private final static Map<String, Map<String, VirusVariant>> singletons = DRDB.initVersionalSingletons();

	private static void updateSingletons(String drdbVersion) {
		DRDB.addVersionToVersionalSingletons(drdbVersion, singletons, drdb -> {
			List<Map<String, Object>> allRefs = drdb.queryAllVirusVariants();
			return (
				allRefs.stream()
				.map(VirusVariant::new)
				.collect(Collectors.toMap(
					vs -> vs.getName(),
					vs -> vs,
					(vs1, vs2) -> vs1,
					LinkedHashMap::new
				))
			);
		});
	}

	public static VirusVariant getInstance(String drdbVersion, String refName) {
		updateSingletons(drdbVersion);
		return singletons.get(drdbVersion).get(refName);
	}

	public static Collection<VirusVariant> getAllInstances(String drdbVersion) {
		updateSingletons(drdbVersion);
		return singletons.get(drdbVersion).values();
	}

	private final String variantName;
	private final MutationSet<SARS2> mutations;

	private VirusVariant(Map<String, Object> refData) {
		variantName = (String) refData.get("variantName");

		@SuppressWarnings("unchecked")
		List<Mutation<SARS2>> mutList = (List<Mutation<SARS2>>) refData.get("mutations");

		mutations = new MutationSet<>(mutList);
	}

	public String name() { return variantName; }
	public String getName() { return variantName; }

	public MutationSet<SARS2> getMutations() { return mutations; }

	@Override
	public String toString() {
		return variantName;
	}

}