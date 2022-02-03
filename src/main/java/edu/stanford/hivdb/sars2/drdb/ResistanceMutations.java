package edu.stanford.hivdb.sars2.drdb;

import java.util.HashMap;
import java.util.Map;

import edu.stanford.hivdb.mutations.MutationSet;
import edu.stanford.hivdb.sars2.SARS2;

public class ResistanceMutations {

	private static final Map<String, MutationSet<SARS2>> RESISTANCE_MUTATIONS = new HashMap<>();

	public static MutationSet<SARS2> get(String drdbVersion) {
		if (!RESISTANCE_MUTATIONS.containsKey(drdbVersion)) {
			DRDB drdb = DRDB.getInstance(drdbVersion);
			RESISTANCE_MUTATIONS.put(drdbVersion, drdb.queryResistanceMutations());
		}
		return RESISTANCE_MUTATIONS.get(drdbVersion);
	}

}