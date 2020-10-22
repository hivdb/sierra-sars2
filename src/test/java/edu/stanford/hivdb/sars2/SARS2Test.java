package edu.stanford.hivdb.sars2;

import static org.junit.Assert.*;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

import com.google.common.collect.Lists;

import edu.stanford.hivdb.graphql.SierraSchema;
import edu.stanford.hivdb.mutations.FrameShift;
import edu.stanford.hivdb.mutations.Mutation;
import edu.stanford.hivdb.mutations.MutationSet;
import edu.stanford.hivdb.sequences.AlignedSequence;
import edu.stanford.hivdb.sequences.Aligner;
import edu.stanford.hivdb.sequences.Sequence;
import edu.stanford.hivdb.utilities.FastaUtils;
import edu.stanford.hivdb.viruses.Gene;
import graphql.GraphQL;
import graphql.schema.GraphQLSchema;

public class SARS2Test {
	
	private final SARS2 virusIns = SARS2.getInstance();

	@Test
	public void testConstructor() {
		assertNotNull(virusIns);
	}
	
	@Test
	public void testGetStrains() {
		assertEquals("SARS2", virusIns.getStrain("SARS2").getName());
		assertEquals(1, virusIns.getStrains().size());
	}
	
	@Test
	public void testGetGenes() {
		List<Gene<SARS2>> genes = new ArrayList<>(virusIns.getGenes(virusIns.getStrain("SARS2")));
		assertEquals("RdRP", genes.get(0).getAbstractGene());
		assertEquals("S", genes.get(1).getAbstractGene());
		assertEquals("SARS2RdRP", genes.get(0).getName());
		assertEquals("SARS2S", genes.get(1).getName());
		assertEquals(2L, virusIns.getGenes(virusIns.getMainStrain()).size());
	}
	
	@Test
	public void testAlignment() {
		ClassLoader classLoader = getClass().getClassLoader();
		InputStream input = classLoader.getResourceAsStream("EPI_ISL_455161.fas");
		Sequence seq = FastaUtils.readStream(input).get(0);
		
		AlignedSequence<SARS2> alignedSeq = Aligner.getInstance(virusIns).align(seq);
		assertEquals(0.0, alignedSeq.getMixturePcnt(), 1e-5);
		assertEquals(
			Lists.newArrayList(FrameShift.createDeletion(virusIns.getGene("SARS2RdRP"), 10, 1)),
			alignedSeq.getFrameShifts());
		assertEquals(
			MutationSet.parseString(virusIns, Lists.newArrayList("RdRP:R10X", "RdRP:P323L")),
			alignedSeq.getAlignedGeneSequence("RdRP").getMutations());
		assertEquals(
			MutationSet.parseString(virusIns, "S:D614G"),
			alignedSeq.getAlignedGeneSequence("S").getMutations());
		assertEquals(
			1, alignedSeq.getAlignedGeneSequence("RdRP").getFirstAA());
		assertEquals(
			932, alignedSeq.getAlignedGeneSequence("RdRP").getLastAA());
		assertEquals(
			1, alignedSeq.getAlignedGeneSequence("S").getFirstAA());
		assertEquals(
			1273, alignedSeq.getAlignedGeneSequence("S").getLastAA());
	}
	
	@Test
	public void testBuildGraphQLSchema() {
		GraphQLSchema schema = SierraSchema.makeSchema(SARS2.getInstance());
		GraphQL.newGraphQL(schema).build();
	}
	
	@Test
	public void testUnusualMutation() {
		Mutation<SARS2> mutation = virusIns.parseMutationString("RdRP:164V");
		assertTrue(mutation.isUnusual());
	}
	
}
