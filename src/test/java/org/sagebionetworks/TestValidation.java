package org.sagebionetworks;
import java.io.File;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;


public class TestValidation {
	
	private static final boolean VERBOSE = false;
	
	private static void o(Object s) {if (VERBOSE) System.out.println(s);}

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void test() throws Exception {
		File inputFile = OlfactionChallengeScoring.writeResourceToFile("Test_LB_S1/low_val.txt");
		String result = OlfactionChallengeScoring.validate(OlfactionChallengeScoring.SUBCHALLENGE.SUBCHALLENGE_1, inputFile, "L");
		o("\n========\n");
		o(result);
		o("\n========\n");
		assertTrue(result.contains("NOT_OK"));
	}

}
