package edu.stanford.hivdb.sars2;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public class Antibody {

	private final static Map<String, Map<String, Antibody>> singletons = DRDB.initVersionalSingletons();
	
	private static void updateSingletons(String drdbVersion) {
		DRDB.addVersionToVersionalSingletons(
			drdbVersion, singletons, drdb -> {
				List<Map<String, Object>> allABs = drdb.queryAllAntibodies();
				return (
					allABs.stream()
					.map(Antibody::new)
					.collect(Collectors.toMap(
						ab -> ab.getName(),
						ab -> ab,
						(ab1, ab2) -> ab1,
						LinkedHashMap::new
					))
				);
			}
		);
	}
	
	public static Antibody getInstance(String drdbVersion, String abName) {
		updateSingletons(drdbVersion);
		return singletons.get(drdbVersion).get(abName);
	}
	
	public static Collection<Antibody> getAllInstances(String drdbVersion) {
		updateSingletons(drdbVersion);
		return singletons.get(drdbVersion).values();
	}
	
	private final String abName;
	private final String pdbID;
	private final String abbrName;
	private final String availability;
	private final String abTarget;
	private final String abClass;
	private final List<String> synonyms;
	
	private Antibody(Map<String, Object> abData) {
		abName = (String) abData.get("abName");
		pdbID = (String) abData.get("pdbID");
		abbrName = (String) abData.get("abbrName");
		availability = (String) abData.get("availability");
		Map<?, ?> targetData = (Map<?, ?>) abData.get("target");
		abTarget = (String) targetData.get("abTarget");
		abClass = (String) targetData.get("abClass");
		synonyms = Collections.unmodifiableList(
			((List<?>) abData.get("synonyms"))
			.stream()
			.map(syn -> (String) ((Map<?, ?>) syn).get("synonym"))
			.collect(Collectors.toList())
		);
	}
	
	public String name() { return abName; }
	public String getName() { return abName; }
	public String getAbbrName() { return abbrName; }
	
	public String getPDB() { return pdbID; }
	public String getAvailability() { return availability; }

	public String getTarget() { return abTarget; }
	public String getAntibodyClass() { return abClass; }
	public List<String> getSynonyms() { return synonyms; }
	
	@Override
	public String toString() {
		return abName;
	}
	
}
