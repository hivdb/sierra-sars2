package edu.stanford.hivdb.sequences;

import static org.junit.Assert.*;

import org.junit.Test;

import edu.stanford.hivdb.mutations.CodonMutation;
import edu.stanford.hivdb.mutations.Mutation;
import edu.stanford.hivdb.mutations.MutationSet;
import edu.stanford.hivdb.viruses.Gene;
import edu.stanford.hivdb.sars2.SARS2;

public class GeneRegionsTest {

	@Test
	public void testAdjacentUnseq() {
		SARS2 sars2 = SARS2.getInstance();
		Gene<SARS2> spike = sars2.getGene("SARS2S");
		Mutation<SARS2> p1247 = new CodonMutation<>(spike, 1247, "X", "NAA");
		MutationSet<SARS2> muts = new MutationSet<>(
			new CodonMutation<>(spike, 1245, "X", "NAA"),
			new CodonMutation<>(spike, 1246, "X", "NNN"),
			p1247
		);
		GeneRegions<SARS2> unseqs = GeneRegions.newUnsequencedRegions(spike, 1, 1273, muts);
		assertEquals(1, unseqs.getRegions().size());
		assertEquals(Long.valueOf(1246L), unseqs.getRegions().get(0).getPosStart());
		assertEquals(Long.valueOf(1247L), unseqs.getRegions().get(0).getPosEnd());
	}
	
	@Test
	public void testPartialGeneUnseq() {
		SARS2 sars2 = SARS2.getInstance();
		Gene<SARS2> spike = sars2.getGene("SARS2S");
		MutationSet<SARS2> muts = new MutationSet<>(
			new CodonMutation<>(spike, 1245, "X", "NAA"),
			new CodonMutation<>(spike, 1246, "X", "NNN")
		);
		GeneRegions<SARS2> unseqs = GeneRegions.newUnsequencedRegions(spike, 1, 1246, muts);
		assertEquals(1, unseqs.getRegions().size());
		assertEquals(Long.valueOf(1246L), unseqs.getRegions().get(0).getPosStart());
		assertEquals(Long.valueOf(1273L), unseqs.getRegions().get(0).getPosEnd());
	}
}
