package edu.stanford.hivdb.sars2;

import static org.junit.Assert.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import edu.stanford.hivdb.mutations.MutationSet;
import edu.stanford.hivdb.sars2.drdb.Antibody;
import edu.stanford.hivdb.sars2.drdb.Article;
import edu.stanford.hivdb.sars2.drdb.DRDB;
import edu.stanford.hivdb.sars2.drdb.Isolate;

public class DRDBTest {

	private final String drdbVer = "20210512-slim";
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
				"abNames", "controlIsoName", "isoName", "assay",
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
		List<Map<String, Object>> results = drdbObj.querySuscResultsForConvPlasma(mutations);
		assertTrue(results.size() > 3);
		assertEquals(
			Set.of(
				"refName", "refDOI", "refURL", "rxName",
				"controlIsoName", "isoName", "assay",
				"section", "ordinalNumber", "foldCmp", "fold", "ineffective",
				"fbResistanceLevel", "cumulativeCount", "infection",
				"cumulativeGroup"
			),
			results.get(0).keySet()
		);
	}

	@Test
	public void testQuerySuscResultsForImmuPlasma() {
		MutationSet<SARS2> mutations = MutationSet.parseString(
			virusIns, "S:452R"
		);
		List<Map<String, Object>> results = drdbObj.querySuscResultsForVaccPlasma(mutations);
		assertTrue(results.size() > 14);
		assertEquals(
			Set.of(
				"refName", "refDOI", "refURL", "rxName",
				"vaccineName", "vaccinePriority", "vaccineType",
				"controlIsoName", "isoName", "assay",
				"section", "ordinalNumber", "foldCmp", "fold", "ineffective",
				"fbResistanceLevel", "cumulativeCount", "cumulativeGroup"
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
	public void testIsolate() {
		assertTrue(Isolate.getAllInstances(drdbVer).size() >= 55);
		Isolate isolate = Isolate.getInstance(drdbVer, "B.1.1.7 Spike");
		assertEquals("B.1.1.7 Spike", isolate.getName());
		assertEquals("B.1.1.7", isolate.getVariantName());
	}
	
}
