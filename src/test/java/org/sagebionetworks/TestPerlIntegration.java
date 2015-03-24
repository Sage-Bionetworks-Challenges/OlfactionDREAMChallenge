package org.sagebionetworks;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.OlfactionChallengeScoring.PHASE;


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
		result = OlfactionChallengeScoring.validate(OlfactionChallengeScoring.SUBCHALLENGE.SUBCHALLENGE_1, inputFile, PHASE.L);
		o("\n====Test_LB_S1/low_val.txt====\n"+result+"\n========\n");
		assertTrue(result.contains("NOT_OK"));
		
		inputFile = OlfactionChallengeScoring.writeResourceToFile("Test_LB_S1/repeats.txt", null);
		result = OlfactionChallengeScoring.validate(OlfactionChallengeScoring.SUBCHALLENGE.SUBCHALLENGE_1, inputFile, PHASE.L);
		o("\n========\n"+result+"\n========\n");
		assertTrue(result.contains("NOT_OK"));
		
		inputFile = OlfactionChallengeScoring.writeResourceToFile("Test_LB_S1/short.txt", null);
		result = OlfactionChallengeScoring.validate(OlfactionChallengeScoring.SUBCHALLENGE.SUBCHALLENGE_1, inputFile, PHASE.L);
		o("\n========\n"+result+"\n========\n");
		assertTrue(result.contains("NOT_OK"));
		
		inputFile = OlfactionChallengeScoring.writeResourceToFile("Test_LB_S1/errors_low_val.txt", null);
		result = OlfactionChallengeScoring.validate(OlfactionChallengeScoring.SUBCHALLENGE.SUBCHALLENGE_1, inputFile, PHASE.L);
		o("\n========\n"+result+"\n========\n");
		assertTrue(result.contains("NOT_OK"));
		
		inputFile = OlfactionChallengeScoring.writeResourceToFile("Test_LB_S1/errors_ok.txt", null);
		result = OlfactionChallengeScoring.validate(OlfactionChallengeScoring.SUBCHALLENGE.SUBCHALLENGE_1, inputFile, PHASE.L);
		o("\n========\n"+result+"\n========\n");
		assertTrue(result.contains("NOT_OK"));
		
		inputFile = OlfactionChallengeScoring.writeResourceToFile("Test_LB_S1/errors_repeats.txt", null);
		result = OlfactionChallengeScoring.validate(OlfactionChallengeScoring.SUBCHALLENGE.SUBCHALLENGE_1, inputFile, PHASE.L);
		o("\n========\n"+result+"\n========\n");
		assertTrue(result.contains("NOT_OK"));
		
		inputFile = OlfactionChallengeScoring.writeResourceToFile("Test_LB_S1/errors_short.txt", null);
		result = OlfactionChallengeScoring.validate(OlfactionChallengeScoring.SUBCHALLENGE.SUBCHALLENGE_1, inputFile, PHASE.L);
		o("\n========\n"+result+"\n========\n");
		assertTrue(result.contains("NOT_OK"));
		
		inputFile = OlfactionChallengeScoring.writeResourceToFile("Test_LB_S1/test_ok.txt", null);
		result = OlfactionChallengeScoring.validate(OlfactionChallengeScoring.SUBCHALLENGE.SUBCHALLENGE_1, inputFile, PHASE.L);
		o("\n========\n"+result+"\n========\n");
		assertFalse(result.contains("NOT_OK"));
	}
	
	@Test
	public void testSubchallenge1ValidationFINAL() throws Exception {
		File inputFile;
		String result;
		
		inputFile = OlfactionChallengeScoring.writeResourceToFile("Test_LB_S1/test_ok_FINAL.txt", null);
		result = OlfactionChallengeScoring.validate(OlfactionChallengeScoring.SUBCHALLENGE.SUBCHALLENGE_1, inputFile, PHASE.F);
		o("\n========\n"+result+"\n========\n");
		assertFalse(result.contains("NOT_OK"));
	}
		

	
	@Test
	public void testS1ValidationInfiniteLoop() throws Exception {
		File inputFile;
		String result;
		
		inputFile = OlfactionChallengeScoring.writeResourceToFile("Test_LB_S1/S1_InfiniteLoop.tsv", null);
		result = OlfactionChallengeScoring.validate(OlfactionChallengeScoring.SUBCHALLENGE.SUBCHALLENGE_1, inputFile, PHASE.L);
		o("\n====Test_LB_S1/S1_InfiniteLoop.tsv====\n"+result+"\n========\n");
		assertTrue(result.contains("NOT_OK"));
		
	}

	@Test
	public void testSubchallenge1Scoring() throws Exception {
		File inputFile;
		File goldStandardFile; 
		Map<String,Double> result;
		
		inputFile = OlfactionChallengeScoring.writeResourceToFile("Test_LB_S1/test_ok.txt", null);
		goldStandardFile = OlfactionChallengeScoring.writeResourceToFile("fakeGoldStandardLBs1.txt", null);
		result = OlfactionChallengeScoring.score(OlfactionChallengeScoring.SUBCHALLENGE.SUBCHALLENGE_1, inputFile, goldStandardFile);
		assertEquals(4, result.size());
	}


	@Test
	public void testSubchallenge2Validation() throws Exception {
		File inputFile;
		String result;
				
		inputFile = OlfactionChallengeScoring.writeResourceToFile("Test_LB_S2/errors_LB_s2_ok.txt", null);
		result = OlfactionChallengeScoring.validate(OlfactionChallengeScoring.SUBCHALLENGE.SUBCHALLENGE_2, inputFile, PHASE.L);
		o("\n========\n"+result+"\n========\n");
		assertTrue(result.contains("NOT_OK"));
		
		inputFile = OlfactionChallengeScoring.writeResourceToFile("Test_LB_S2/test_LB_s2_ok.txt", null);
		result = OlfactionChallengeScoring.validate(OlfactionChallengeScoring.SUBCHALLENGE.SUBCHALLENGE_2, inputFile, PHASE.L);
		o("\n========\n"+result+"\n========\n");
		assertFalse(result.contains("NOT_OK"));
	}

	@Test
	public void testSubchallenge2ValidationFINAL() throws Exception {
		File inputFile;
		String result;
				
		inputFile = OlfactionChallengeScoring.writeResourceToFile("Test_LB_S2/test_s2_ok_FINAL.txt", null);
		result = OlfactionChallengeScoring.validate(OlfactionChallengeScoring.SUBCHALLENGE.SUBCHALLENGE_2, inputFile, PHASE.F);
		o("\n========\n"+result+"\n========\n");
		assertFalse(result.contains("NOT_OK"));
	}

	@Test
	public void testSubchallenge2Scoring() throws Exception {
		File inputFile;
		File goldStandardFile; 
		Map<String,Double> result;
		
		inputFile = OlfactionChallengeScoring.writeResourceToFile("Test_LB_S2/test_LB_s2_ok.txt", null);
		goldStandardFile = OlfactionChallengeScoring.writeResourceToFile("fakeGoldStandardLBs2.txt", null);
		result = OlfactionChallengeScoring.score(OlfactionChallengeScoring.SUBCHALLENGE.SUBCHALLENGE_2, inputFile, goldStandardFile);
		assertEquals(7, result.size());
	}
}
