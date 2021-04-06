package edu.stanford.hivdb.sars2;

import static org.junit.Assert.*;

import java.util.List;
import org.junit.Test;


import edu.stanford.hivdb.mutations.MutationSet;
import edu.stanford.hivdb.sars2.SARS2MutationComment.SARS2BoundMutationComment;

public class SARS2MutationCommentTest {
	
	private final SARS2 sars2 = SARS2.getInstance();
	
	@Test
	public void testQueryMutations() {
		MutationSet<SARS2> qMuts = sars2.newMutationSet("S:484K, S:501Y, S:417N, S:614G, S:69del");
		List<SARS2BoundMutationComment> results = SARS2MutationComment.query("20210406", qMuts);
		assertTrue(results.size() == 5);
	}
	
}
