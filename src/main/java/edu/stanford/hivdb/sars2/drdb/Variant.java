package edu.stanford.hivdb.sars2.drdb;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Variant {

	private final static Map<String, Map<String, Variant>> singletons = DRDB.initVersionalSingletons();

	private static void updateSingletons(String drdbVersion) {
		DRDB.addVersionToVersionalSingletons(drdbVersion, singletons, drdb -> {
			List<Map<String, Object>> allVVs = drdb.queryAllVariants();
			return (
				allVVs.stream()
				.map(vv -> new Variant(drdb, vv))
				.collect(Collectors.toMap(
					vs -> vs.getName(),
					vs -> vs,
					(vs1, vs2) -> vs1,
					LinkedHashMap::new
				))
			);
		});
	}

	public static Variant getInstance(String drdbVersion, String varName) {
		updateSingletons(drdbVersion);
		return singletons.get(drdbVersion).get(varName);
	}

	public static Collection<Variant> getAllInstances(String drdbVersion) {
		updateSingletons(drdbVersion);
		return singletons.get(drdbVersion).values();
	}

	private final String varName;
	private final Boolean asWildtype;

	private Variant(DRDB drdb, Map<String, Object> refData) {
		varName = (String) refData.get("varName");
		asWildtype = (Boolean) refData.get("asWildtype");
	}

	public String name() { return varName; }
	public String getName() { return varName; }
	public Boolean asWildtype() { return asWildtype; }
	
	@Override
	public String toString() {
		return varName;
	}

}