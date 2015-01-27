package org.sagebionetworks;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Before;
import org.junit.Test;


public class TestPerlIntegration {
	
	private static final boolean VERBOSE = false;
	
	private static void o(Object s) {if (VERBOSE) System.out.println(s);}

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testSubchallenge1Validation() throws Exception {
		File inputFile;
		String result;
		
		inputFile = OlfactionChallengeScoring.writeResourceToFile("Test_LB_S1/low_val.txt", null);
		result = OlfactionChallengeScoring.validate(OlfactionChallengeScoring.SUBCHALLENGE.SUBCHALLENGE_1, inputFile, "L");
		o("\n====Test_LB_S1/low_val.txt====\n"+result+"\n========\n");
		assertTrue(result.contains("NOT_OK"));
		
		inputFile = OlfactionChallengeScoring.writeResourceToFile("Test_LB_S1/repeats.txt", null);
		result = OlfactionChallengeScoring.validate(OlfactionChallengeScoring.SUBCHALLENGE.SUBCHALLENGE_1, inputFile, "L");
		o("\n========\n"+result+"\n========\n");
		assertTrue(result.contains("NOT_OK"));
		
		inputFile = OlfactionChallengeScoring.writeResourceToFile("Test_LB_S1/short.txt", null);
		result = OlfactionChallengeScoring.validate(OlfactionChallengeScoring.SUBCHALLENGE.SUBCHALLENGE_1, inputFile, "L");
		o("\n========\n"+result+"\n========\n");
		assertTrue(result.contains("NOT_OK"));
		
		inputFile = OlfactionChallengeScoring.writeResourceToFile("Test_LB_S1/test_ok.txt", null);
		result = OlfactionChallengeScoring.validate(OlfactionChallengeScoring.SUBCHALLENGE.SUBCHALLENGE_1, inputFile, "L");
		o("\n========\n"+result+"\n========\n");
		assertFalse(result.contains("NOT_OK"));
	}

	@Test
	public void testSubchallenge1Scoring() throws Exception {
		File inputFile;
		File goldStandardFile; 
		String result;
		
		inputFile = OlfactionChallengeScoring.writeResourceToFile("Test_LB_S1/test_ok.txt", null);
		goldStandardFile = OlfactionChallengeScoring.writeResourceToFile("Test_LB_S1/test_ok.txt", null); // not really a gold standard...
		result = OlfactionChallengeScoring.score(OlfactionChallengeScoring.SUBCHALLENGE.SUBCHALLENGE_1, inputFile, goldStandardFile);
		o("\n========\n"+result+"\n========\n");
		assertFalse(result.contains("NOT_OK"));
	}

}
