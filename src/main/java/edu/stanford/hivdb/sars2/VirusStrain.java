package edu.stanford.hivdb.sars2;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import edu.stanford.hivdb.mutations.Mutation;
import edu.stanford.hivdb.mutations.MutationSet;

public class VirusStrain {

	private final static Map<String, VirusStrain> singletons;

	static {
		SARS2 sars2 = SARS2.getInstance();
		List<Map<String, Object>> allRefs = sars2.getDRDBObj().queryAllVirusStrains();
		Map<String, VirusStrain> localSingletons = (
			allRefs.stream()
			.map(VirusStrain::new)
			.collect(Collectors.toMap(
				vs -> vs.getName(),
				vs -> vs,
				(vs1, vs2) -> vs1,
				LinkedHashMap::new
			))
		);
		singletons = Collections.unmodifiableMap(localSingletons);
	}

	public static VirusStrain getInstance(String refName) {
		return singletons.get(refName);
	}

	public static Collection<VirusStrain> getAllInstances() {
		return singletons.values();
	}

	private final String strainName;
	private final MutationSet<SARS2> mutations;

	private VirusStrain(Map<String, Object> refData) {
		strainName = (String) refData.get("strainName");

		@SuppressWarnings("unchecked")
		List<Mutation<SARS2>> mutList = (List<Mutation<SARS2>>) refData.get("mutations");

		mutations = new MutationSet<>(mutList);
	}

	public String name() { return strainName; }
	public String getName() { return strainName; }

	public MutationSet<SARS2> getMutations() { return mutations; }

	@Override
	public String toString() {
		return strainName;
	}

}