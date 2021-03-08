package edu.stanford.hivdb.sars2;

import static org.junit.Assert.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import edu.stanford.hivdb.mutations.MutationSet;

public class DRDBTest {

	private final SARS2 virusIns = SARS2.getInstance();
	private final DRDB drdbObj = SARS2.getInstance().getDRDBObj();

	@Test
	public void testQuerySuscResultsForAntibodies() {
		MutationSet<SARS2> mutations = MutationSet.parseString(
			virusIns, "S:484K"
		);
		List<Map<String, ?>> results = drdbObj.querySuscResultsForAntibodies(mutations);
		assertTrue(results.size() > 330);
		assertEquals(
			Set.of(
				"refName", "refDOI", "refURL", "rxName", "antibodies", "strainName",
				"mutations", "section", "ordinalNumber", "foldCmp", "fold",
				"resistanceLevel", "cumulativeCount"
			),
			results.get(0).keySet()
		);
	}

	@Test
	public void testQuerySuscResultsForConvPlasma() {
		MutationSet<SARS2> mutations = MutationSet.parseString(
			virusIns, "S:452R"
		);
		List<Map<String, ?>> results = drdbObj.querySuscResultsForConvPlasma(mutations);
		assertTrue(results.size() > 3);
		assertEquals(
			Set.of(
				"refName", "refDOI", "refURL", "rxName", "strainName", "mutations",
				"section", "ordinalNumber", "foldCmp", "fold", "resistanceLevel",
				"cumulativeCount", "cumulativeGroup"
			),
			results.get(0).keySet()
		);
	}

	@Test
	public void testQuerySuscResultsForImmuPlasma() {
		MutationSet<SARS2> mutations = MutationSet.parseString(
			virusIns, "S:452R"
		);
		List<Map<String, ?>> results = drdbObj.querySuscResultsForImmuPlasma(mutations);
		assertTrue(results.size() > 14);
		assertEquals(
			Set.of(
				"refName", "refDOI", "refURL", "rxName", "strainName", "mutations",
				"section", "ordinalNumber", "foldCmp", "fold", "resistanceLevel",
				"cumulativeCount", "cumulativeGroup", "vaccineName"
			),
			results.get(0).keySet()
		);
	}

}
