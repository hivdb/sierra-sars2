package edu.stanford.hivdb.sars2.drdb;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import edu.stanford.hivdb.mutations.Mutation;
import edu.stanford.hivdb.mutations.MutationSet;
import edu.stanford.hivdb.sars2.SARS2;

public class Isolate {

	private final static Map<String, Map<String, Isolate>> singletons = DRDB.initVersionalSingletons();

	private static void updateSingletons(String drdbVersion) {
		DRDB.addVersionToVersionalSingletons(drdbVersion, singletons, drdb -> {
			List<Map<String, Object>> allVVs = drdb.queryAllIsolates();
			return (
				allVVs.stream()
				.map(vv -> new Isolate(drdb, vv))
				.collect(Collectors.toMap(
					vs -> vs.getName(),
					vs -> vs,
					(vs1, vs2) -> vs1,
					LinkedHashMap::new
				))
			);
		});
	}

	public static Isolate getInstance(String drdbVersion, String isoName) {
		updateSingletons(drdbVersion);
		return singletons.get(drdbVersion).get(isoName);
	}

	public static Collection<Isolate> getAllInstances(String drdbVersion) {
		updateSingletons(drdbVersion);
		return singletons.get(drdbVersion).values();
	}

	private final String isoName;
	private final String varName;
	private final MutationSet<SARS2> mutations;

	private Isolate(DRDB drdb, Map<String, Object> refData) {
		isoName = (String) refData.get("isoName");
		varName = (String) refData.get("varName");

		@SuppressWarnings("unchecked")
		List<Mutation<SARS2>> mutList = (List<Mutation<SARS2>>) refData.get("mutations");

		mutations = new MutationSet<>(mutList);
	}

	public String name() { return isoName; }
	public String getName() { return isoName; }
	public String getVariantName() { return varName; }

	public MutationSet<SARS2> getMutations() { return mutations; }
	
	public MutationSet<SARS2> getHitMutations(MutationSet<SARS2> queryMuts) {
		return queryMuts.intersectsWith(mutations);
	}
	
	@Override
	public String toString() {
		return isoName;
	}

}