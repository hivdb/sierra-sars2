package edu.stanford.hivdb.seqreads;

import static org.junit.Assert.*;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

public class CutoffCalculatorTest {
	
	@Test
	public void testMergeCodons() {
		List<Integer> codonA = "ACG".chars().mapToObj(i -> i).collect(Collectors.toList());
		List<Integer> codonB = "AGG".chars().mapToObj(i -> i).collect(Collectors.toList());
		List<Integer> codonC = CutoffCalculator.mergeCodon(codonA, codonB);
		assertEquals(List.of(65, -1, 71), codonC);
	}

}
