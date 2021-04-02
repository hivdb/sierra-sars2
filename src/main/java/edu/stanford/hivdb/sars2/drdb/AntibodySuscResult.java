package edu.stanford.hivdb.sars2.drdb;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import edu.stanford.hivdb.mutations.MutationSet;
import edu.stanford.hivdb.sars2.SARS2;

public class AntibodySuscResult extends SuscResult {

	private final String drdbVersion;
	private final Set<String> abNames;
	
	private transient Set<Antibody> antibodies;
	
	public static List<AntibodySuscResult> query(String drdbVersion, MutationSet<SARS2> queryMuts) {
		DRDB drdb = DRDB.getInstance(drdbVersion);
		final MutationSet<SARS2> realQueryMuts = queryMuts.filterBy(mut -> !mut.isUnsequenced());
		List<AntibodySuscResult> results = (
			drdb
			.querySuscResultsForAntibodies(queryMuts)
			.stream()
			.map(d -> new AntibodySuscResult(drdbVersion, realQueryMuts, d))
			.collect(Collectors.toList())
		);
		
		return results;
	}
	
	private AntibodySuscResult(
		String drdbVersion,
		MutationSet<SARS2> queryMuts,
		Map<String, Object> suscData
	) {
		super(drdbVersion, queryMuts, suscData);

		@SuppressWarnings("unchecked")
		Set<String> abNames = (Set<String>) suscData.get("abNames");
		this.abNames = abNames;
		this.drdbVersion = drdbVersion;

	}

	public Set<Antibody> getAntibodies() {
		if (antibodies == null) {
			antibodies = (
				abNames.stream()
				.map(abName -> Antibody.getInstance(drdbVersion, abName))
				.collect(Collectors.toSet())
			);
		}
		return antibodies;
	}

}
