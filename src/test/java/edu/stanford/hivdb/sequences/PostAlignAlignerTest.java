package edu.stanford.hivdb.sequences;
import static org.junit.Assert.*;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import com.google.common.collect.Lists;

import edu.stanford.hivdb.mutations.MutationSet;
import edu.stanford.hivdb.sars2.SARS2;
import edu.stanford.hivdb.viruses.Gene;
import edu.stanford.hivdb.viruses.Strain;

public class PostAlignAlignerTest {
	
	private static SARS2 sars2 = SARS2.getInstance();
	private static Strain<SARS2> sars2Strain = sars2.getStrain("SARS2");
	private static Gene<SARS2> RDRP = sars2.getGene("SARS2RdRP");
	private static Gene<SARS2> SPIKE = sars2.getGene("SARS2S");
	
	@Test
	public void testExecute() {
		PostAlignAligner<SARS2> postAlign = new PostAlignAligner<>(sars2);
		Map<Gene<SARS2>, List<Map<String, ?>>> results = postAlign.execute(
			Lists.newArrayList(
				new Sequence(
					"TestSeq description",
					"TCAGCTGATGCACAATCGTTTTTAAACGGGTTTGCGGTGTAAGTGCAGCCCGT" +
					"CTTACACCGTGCGGCACAGGCACTAGTACTGATGTCGTATATAGGGCTTTTGA" +
					"CATCTACAATGATAAAGTAGCTGGTTTTGNNNNNNNNNNNNNNNNNNNNNNNN" +
					"ATGTTTGTTTTTCTTGTTTTATTGCCACTAGTCTCTAGTCAGTGTGTTAATCT" +
					"TACAACCAGAACTCAATTACCCCCTGCATACACTAATTCTTTCACACGTGGTG" +
					"TTTATTACCCTGACAAAGTTTTCAGATCCTCAGTTTTACATTNNNNNNNNNNN"
				)
			)
		).get(sars2Strain);
		Set<Gene<SARS2>> expectedGenes = new HashSet<>();
		expectedGenes.add(RDRP);
		expectedGenes.add(SPIKE);
		assertEquals(expectedGenes, results.keySet());
		assertEquals(1, results.get(RDRP).size());
		assertEquals(1, results.get(SPIKE).size());
		assertEquals("TestSeq description", results.get(RDRP).get(0).get("Name"));
		assertEquals("TestSeq description", results.get(SPIKE).get(0).get("Name"));
		assertEquals(45, ((List<?>) ((Map<?, ?>) results.get(RDRP).get(0).get("Report")).get("AlignedSites")).size());
		assertEquals(49, ((List<?>) ((Map<?, ?>) results.get(SPIKE).get(0).get("Report")).get("AlignedSites")).size());
	}
	
	@Test
	public void testAlignTooShort() {
		Aligner<SARS2> postAlign = Aligner.getInstance(sars2);
		assertTrue(postAlign instanceof PostAlignAligner);
		AlignedSequence<SARS2> alignResult = postAlign.align(
			new Sequence(
				"TestSeq description",
				"TCAGCTGATGCACAATCGTTTTTAAACGGGTTTGCGGTGTAAGTGCAGCCCGT" +
				"CTTACACCGTGCGGCACAGGCACTAGTACTGATGTCGTATATAGGGCTTTTGA" +
				"CATCTACAATGATAAAGTAGCTGGTTTTGNNNNNNNNNNNNNNNNNNNNNNNN" +
				"ATGTTTGTTTTTCTTGTTTTATTGCCACTAGTCTCTAGTCAGTGTGTTAATCT" +
				"TACAACCAGAACTCAATTACCCCCTGCATACACTAATTCTTTCACACGTGGTG" +
				"TTTATTACCCTGACAAAGTTTTCAGATCCTCAGTTTTACATTNNNNNNNNNNN"
			)
		);
		assertEquals(
			"CRITICAL: There were no Protease, Reverse Transcriptase, or Integrase genes found, refuse to process.",
			alignResult.getValidationResults().get(0).toString()
		);
		assertEquals(Collections.emptyList(), alignResult.getAvailableGenes());
	}
	
	@Test
	public void testAlign() {
		Sequence testSeq = Sequence.fromGenbank("LR824523");
		PostAlignAligner<SARS2> postAlign = new PostAlignAligner<>(sars2);
		AlignedSequence<SARS2> alignResult = postAlign.align(testSeq);
		List<Gene<SARS2>> expectedGenes = Lists.newArrayList(RDRP, SPIKE);
		assertEquals(expectedGenes, alignResult.getAvailableGenes());

		AlignedGeneSeq<SARS2> rdrpGeneAlignedSeq = alignResult.getAlignedGeneSequence(RDRP);
		assertEquals(1, rdrpGeneAlignedSeq.getFirstAA());
		assertEquals(932, rdrpGeneAlignedSeq.getLastAA());
		MutationSet<SARS2> expectedRdRPMutations = MutationSet.parseString(RDRP, "P323L");
		MutationSet<SARS2> rdrpMutations = rdrpGeneAlignedSeq.getMutations();
		assertEquals(expectedRdRPMutations, rdrpMutations);
		assertEquals(Collections.emptyList(), rdrpGeneAlignedSeq.getFrameShifts());

		AlignedGeneSeq<SARS2> sGeneAlignedSeq = alignResult.getAlignedGeneSequence(SPIKE);
		assertEquals(1, sGeneAlignedSeq.getFirstAA());
		assertEquals(1273, sGeneAlignedSeq.getLastAA());
		MutationSet<SARS2> expectedSMutations = MutationSet.parseString(
			SPIKE, "V289VG, D290*DEFLVY, C291CGW, A292ADE, L293LIMRS, F318FL, D614G"
		); 
		MutationSet<SARS2> sMutations = sGeneAlignedSeq.getMutations();
		assertEquals(expectedSMutations, sMutations);
		assertEquals(Collections.emptyList(), rdrpGeneAlignedSeq.getFrameShifts());
	}

}
