package edu.stanford.hivdb.sars2;

import static org.junit.Assert.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import edu.stanford.hivdb.mutations.MutationSet;

public class DRDBTest {

	private final String drdbVer = "20210325";
	private final SARS2 virusIns = SARS2.getInstance();
	private final DRDB drdbObj = DRDB.getInstance(drdbVer);

	@Test
	public void testQuerySuscResultsForAntibodies() {
		MutationSet<SARS2> mutations = MutationSet.parseString(
			virusIns, "S:484K"
		);
		List<Map<String, Object>> results = drdbObj.querySuscResultsForAntibodies(mutations);
		assertTrue(results.size() > 330);
		assertEquals(
			Set.of(
				"refName", "refDOI", "refURL", "rxName",
				"abNames", "controlVariantName", "variantName", "assay",
				"section", "ordinalNumber", "foldCmp", "fold", "ineffective",
				"fbResistanceLevel", "cumulativeCount"
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
				"refName", "refDOI", "refURL", "rxName",
				"controlVariantName", "variantName", "mutations", "assay",
				"section", "ordinalNumber", "foldCmp", "fold", "ineffective",
				"resistanceLevel", "cumulativeCount", "cumulativeGroup"
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
				"refName", "refDOI", "refURL", "rxName", "vaccineName",
				"controlVariantName", "variantName", "mutations", "assay",
				"section", "ordinalNumber", "foldCmp", "fold", "ineffective",
				"resistanceLevel", "cumulativeCount", "cumulativeGroup"
			),
			results.get(0).keySet()
		);
	}

	@Test
	public void testAntibodies() {
		assertTrue(Antibody.getAllInstances(drdbVer).size() >= 227);
	}
	
	@Test
	public void testArticles() {
		assertTrue(Article.getAllInstances(drdbVer).size() >= 55);
	}

	@Test
	public void testVirusStrains() {
		assertTrue(VirusVariant.getAllInstances(drdbVer).size() >= 55);
	}

}
