package org.sagebionetworks;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.SynapseClientImpl;
import org.sagebionetworks.client.SynapseProfileProxy;
import org.sagebionetworks.client.exceptions.SynapseConflictingUpdateException;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.evaluation.model.BatchUploadResponse;
import org.sagebionetworks.evaluation.model.Submission;
import org.sagebionetworks.evaluation.model.SubmissionBundle;
import org.sagebionetworks.evaluation.model.SubmissionStatus;
import org.sagebionetworks.evaluation.model.SubmissionStatusBatch;
import org.sagebionetworks.evaluation.model.SubmissionStatusEnum;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.annotation.Annotations;
import org.sagebionetworks.repo.model.annotation.DoubleAnnotation;
import org.sagebionetworks.repo.model.annotation.LongAnnotation;
import org.sagebionetworks.repo.model.annotation.StringAnnotation;
import org.sagebionetworks.repo.model.message.MessageToUser;


/**
 * Olfaction scoring application
 * 
 */
//
// TODO add try-catch around each submission, so no submission breaks the app'
//
// TODO implement per-subchallenge limits (300 for SC1, 30 for SC2 in LB phase; 
// TODO 1 and 1 for final phase; subsequent sub's are rejected)
//
// TODO implement 4 notifications:  (1) validation failure (include output of validation script), 
// (2) exceeding submission limit, (3) scoring failure, (4) success
//
// TODO:  map scoring metrics to leader board
// 
public class OlfactionChallengeScoring {    
    // the page size can be bigger, we do this just to demonstrate pagination
    private static int PAGE_SIZE = 20;
    
    // the batch size can be bigger, we do this just to demonstrate batching
    private static int BATCH_SIZE = 20;
        
	private static Properties properties = null;

    private SynapseClient synapseAdmin;
    
    public enum SUBCHALLENGE {
    	SUBCHALLENGE_1,
    	SUBCHALLENGE_2
    }
    
    /*
     * NOTE: The returned file is a TEMPORARY file, slated for deletion when the process exits.
     */
    public static File writeResourceToFile(String name, File directory) throws IOException {
    	File result;
    	if (directory==null) {
    		result =File.createTempFile((new File(name)).getName(), "");
    	} else {
    		result =File.createTempFile((new File(name)).getName(), "", directory);
    	}
	    result.deleteOnExit();
    	InputStream in = OlfactionChallengeScoring.class.getClassLoader().getResourceAsStream(name);
    	if (in==null) throw new IllegalArgumentException("Cannot find "+name+" on class path.");
   		OutputStream out = null;
   	    try {
   	    	out = new FileOutputStream(result);
    		IOUtils.copy(in, out);
    		out.close();
    		out = null;
    	} finally {
    		in.close();
    		if (out!=null) out.close();
    	}
    	return result;
    }
    
    private static Random random = new Random();

    /*
     * @param name:  the file name in src/main/resources
     * @param inputFile: the input file for the script
     * @param params: the params to pass, after perl <script file> <inputFile> <output file>
     */
    public static String executePerlScript(String name, String[] params, File outputFile, File workingDirectory) throws IOException {
       	File scriptFile = writeResourceToFile(name, workingDirectory);
		
   	    String[] commandAndParams = new String[params.length+2];
   	    int i=0;
  	    commandAndParams[i++] = "perl";
  	    commandAndParams[i++] = scriptFile.getAbsolutePath();
   	    if (commandAndParams.length!=i+params.length) throw new IllegalStateException();
   	    System.arraycopy(params, 0, commandAndParams, i, params.length);
   	    String[] envp = new String[0];
   	    Process process = Runtime.getRuntime().exec(commandAndParams, envp, workingDirectory);
   	    try {
   	    	process.waitFor();
   	    } catch (InterruptedException e) {
   	    	throw new RuntimeException(e);
   	    }
   	    int exitValue = process.exitValue();
   	    ByteArrayOutputStream resultOS = new ByteArrayOutputStream();
   	    String output = null;
   	    try {
   	    	if (exitValue==0) {
   	    		IOUtils.copy(process.getInputStream(), resultOS);
   	    	} else {
  	    		IOUtils.copy(process.getErrorStream(), resultOS);
   	    	}
   	    	resultOS.close();
   	    	output = new String(resultOS.toByteArray(), "UTF-8");
   	    } finally {
   	    	if (resultOS!=null) resultOS.close();
   	    }
   	    if (exitValue!=0) {
   	    	throw new RuntimeException(output);
   	    }
   	    
		System.out.println(output);
		String result;
		FileInputStream fis = new FileInputStream(outputFile);
		try {
			result =  IOUtils.toString(fis);
		} finally {
			fis.close();
		}
		return result;
    }
    
    public static String validate(SUBCHALLENGE subchallenge, File inputFile, String phase) throws IOException {
		// USAGE:perl DREAM_Olfactory_S1_validation.pl  <input file to validate> <output file> <flag L for Leaderboard or F for final submission>  
	    String scriptName;
	    
	    switch (subchallenge) {
	    case SUBCHALLENGE_1:
	    	scriptName = "DREAM_Olfactory_S1_validation.pl";
	    	break;
	    case SUBCHALLENGE_2:
	    	scriptName = "DREAM_Olfactory_S2_validation.pl";
	    	break;
	    default:
	    	throw new IllegalArgumentException(subchallenge.name());
	    }
	    if (!(phase.equals("L") || phase.equals("F"))) throw new IllegalArgumentException(phase);
		File outputFile = File.createTempFile("scriptOutput", ".txt");//new File(workingDirectory, outputFileName);
  	    //String outputFileName = "scriptOutput"+Math.abs(random.nextLong())+".txt";
		outputFile.deleteOnExit();
		File workingDirectory = outputFile.getParentFile();;
	    // note, the validation file expects the input file PATH but the output file NAME
 		return OlfactionChallengeScoring.executePerlScript(scriptName, 
 				new String[]{inputFile.getAbsolutePath(), outputFile.getName(), phase}, outputFile, workingDirectory);
    }
    
    public static String score(SUBCHALLENGE subchallenge, File inputFile, File goldStandard) throws IOException {
	    String scriptName;
	    
	    switch (subchallenge) {
	    case SUBCHALLENGE_1:
	    	scriptName = "DREAM_Olfaction_scoring_Q1.pl";
	    	break;
	    case SUBCHALLENGE_2:
	    	scriptName = "DREAM_Olfaction_scoring_Q2.pl";
	    	break;
	    default:
	    	throw new IllegalArgumentException(subchallenge.name());
	    }
		File outputFile = File.createTempFile("scriptOutput", ".txt");//new File(workingDirectory, outputFileName);
  	    //String outputFileName = "scriptOutput"+Math.abs(random.nextLong())+".txt";
		outputFile.deleteOnExit();
		File workingDirectory = outputFile.getParentFile();
		if (!workingDirectory.getAbsolutePath().equals(inputFile.getParentFile().getAbsolutePath())) throw new RuntimeException();
	    // NOTE: Perl script expects input file NAME, output file NAME (not PATH).  Both must be in working directory
	    // However, gold standard is to be a file PATH
		return OlfactionChallengeScoring.executePerlScript(scriptName, 
				new String[]{inputFile.getName(), outputFile.getName(), 
				goldStandard.getAbsolutePath()}, outputFile, workingDirectory);
    }
    
    public static void main( String[] args ) throws Exception {
   		OlfactionChallengeScoring sct = new OlfactionChallengeScoring();
   	    try {
   	    	// validate and score subchallenge 1
    		sct.validate(SUBCHALLENGE.SUBCHALLENGE_1, "3154769");
       		sct.score(SUBCHALLENGE.SUBCHALLENGE_1, "3154769");
       		
   	    	// validate and score subchallenge 2
    		sct.validate(SUBCHALLENGE.SUBCHALLENGE_2, "3154771");
       		sct.score(SUBCHALLENGE.SUBCHALLENGE_2, "3154771");
       		
       		// TODO for final round, do we just zip up submitted files for manual scoring?
   	} finally {
    	}
    }
    
    public OlfactionChallengeScoring() throws SynapseException {
    	synapseAdmin = createSynapseClient();
    	String adminUserName = getProperty("ADMIN_USERNAME");
    	String adminPassword = getProperty("ADMIN_PASSWORD");
    	synapseAdmin.login(adminUserName, adminPassword);
    }
    
    /**
     * This demonstrates a lightweight validation step
     * 
     * Beyond simple validation, other criteria could be checked, e.g.
     * whether the user has exceeded a limit on the number of submissions
     * permitted in a given time period
     * 
     * @throws SynapseException
     */
    public void validate(SUBCHALLENGE subchallenge, String evaluationId) throws SynapseException, IOException {
    	List<SubmissionStatus> statusesToUpdate = new ArrayList<SubmissionStatus>();
    	long total = Integer.MAX_VALUE;
       	for (int offset=0; offset<total; offset+=PAGE_SIZE) {
       			// get the newly RECEIVED Submissions
       		PaginatedResults<SubmissionBundle> submissionPGs = 
       			synapseAdmin.getAllSubmissionBundlesByStatus(evaluationId, SubmissionStatusEnum.RECEIVED, offset, PAGE_SIZE);
        	total = (int)submissionPGs.getTotalNumberOfResults();
        	List<SubmissionBundle> page = submissionPGs.getResults();
        	for (int i=0; i<page.size(); i++) {
        		SubmissionBundle bundle = page.get(i);
        		Submission sub = bundle.getSubmission();
       			File temp = downloadSubmissionFile(sub);
       			temp.deleteOnExit();
       			String validationResult = validate(subchallenge,temp, "L"/*"leader board" phase*/);
       			boolean fileIsOK = !validationResult.contains("NOT_OK");
       			
       			SubmissionStatusEnum newStatus = null;
       			if (fileIsOK) {
       				newStatus = SubmissionStatusEnum.VALIDATED;
       			} else {
       				newStatus = SubmissionStatusEnum.INVALID;
       				// send the user an email message to let them know
       				// TODO in the message be clear about which sub challenge the submission was sent to
       				sendMessage(sub.getUserId(), SUB_ACK_SUBJECT, SUB_ACK_INVALID+"\n"+validationResult);
       			}
           		SubmissionStatus status = bundle.getSubmissionStatus();
           		status.setStatus(newStatus);
           	    statusesToUpdate.add(status);
        	}
       	}
       	// we can update all the statuses in a batch
       	updateSubmissionStatusBatch(evaluationId, statusesToUpdate);
    }
    
    private static final String SUB_ACK_SUBJECT = "Submission Acknowledgment";
    private static final String SUB_ACK_INVALID = "Your submission is invalid. Please try again.";
       
    private void sendMessage(String userId, String subject, String body) throws SynapseException {
    	MessageToUser messageMetadata = new MessageToUser();
    	messageMetadata.setRecipients(Collections.singleton(userId));
    	messageMetadata.setSubject(subject);
    	synapseAdmin.sendStringMessage(messageMetadata, body);
    }
    
    /**
     * Note: There are two types of scoring, that in which each submission is scored alone and that
     * in which the entire set of submissions is rescored whenever a new one arrives. 
     * 
     * @throws SynapseException
     */
    public void score(SUBCHALLENGE subchallenge, String evaluationId) throws SynapseException, IOException {
    	long startTime = System.currentTimeMillis();
    	List<SubmissionStatus> statusesToUpdate = new ArrayList<SubmissionStatus>();
    	long total = Integer.MAX_VALUE;
    	File goldStandard = null;
       	for (int offset=0; offset<total; offset+=PAGE_SIZE) {
       		PaginatedResults<SubmissionBundle> submissionPGs = null;
       		// alternatively just get the unscored submissions in the Evaluation
       		// here we get the ones that the 'validation' step (above) marked as validated
       		submissionPGs = synapseAdmin.getAllSubmissionBundlesByStatus(evaluationId, SubmissionStatusEnum.VALIDATED, offset, PAGE_SIZE);
        	total = (int)submissionPGs.getTotalNumberOfResults();
        	List<SubmissionBundle> page = submissionPGs.getResults();
        	for (int i=0; i<page.size(); i++) {
        		SubmissionBundle bundle = page.get(i);
        		Submission sub = bundle.getSubmission();
        		// at least once, download file and make sure it's correct
    			File temp = downloadSubmissionFile(sub);
    			if (goldStandard==null) {
    				goldStandard = File.createTempFile("goldStd", ".txt");
    				goldStandard.deleteOnExit();
    				synapseAdmin.downloadFromFileEntityCurrentVersion(getProperty("GS_ENTITY_ID_L_"+subchallenge), goldStandard);
    			}
    			
        		double score = 0d;
        		SubmissionStatus status = bundle.getSubmissionStatus();
        		try {
    				String s = score(subchallenge, temp, goldStandard);
    				// TODO map all output values from the script into submission annotations
           			status.setStatus(SubmissionStatusEnum.SCORED);
           			score = 100d;
        		} catch (Exception e) {
           			status.setStatus(SubmissionStatusEnum.REJECTED);
           			score = 0d;
        		}

        		Annotations annotations = status.getAnnotations();
        		if (annotations==null) {
        			annotations=new Annotations();
        			status.setAnnotations(annotations);
        		}
       		    addAnnotations(
    					annotations, 
    					sub.getSubmitterAlias(),
    					sub.getCreatedOn().getTime(),
    					score,
    					sub.getName()
    					);
    			statusesToUpdate.add(status);
        	}
       	}
       	
       	System.out.println("Retrieved "+total+" submissions for scoring.");
       	
       	updateSubmissionStatusBatch(evaluationId, statusesToUpdate);
       	
       	System.out.println("Scored "+statusesToUpdate.size()+" submissions.");
       	long delta = System.currentTimeMillis() - startTime;
       	System.out.println("Elapsed time for running scoring app: "+formatInterval(delta));
    }
    
    private static final int BATCH_UPLOAD_RETRY_COUNT = 3;
    
    private void updateSubmissionStatusBatch(String evaluationId, List<SubmissionStatus> statusesToUpdate) throws SynapseException {
       	// now we have a batch of statuses to update
    	for (int retry=0; retry<BATCH_UPLOAD_RETRY_COUNT; retry++) {
    		try {
		       	String batchToken = null;
		       	for (int offset=0; offset<statusesToUpdate.size(); offset+=BATCH_SIZE) {
		       		SubmissionStatusBatch updateBatch = new SubmissionStatusBatch();
		       		List<SubmissionStatus> batch = new ArrayList<SubmissionStatus>();
		       		for (int i=0; i<BATCH_SIZE && offset+i<statusesToUpdate.size(); i++) {
		       			batch.add(statusesToUpdate.get(offset+i));
		       		}
		       		updateBatch.setStatuses(batch);
		       		boolean isFirstBatch = (offset==0);
		       		updateBatch.setIsFirstBatch(isFirstBatch);
		       		boolean isLastBatch = (offset+BATCH_SIZE)>=statusesToUpdate.size();
		       		updateBatch.setIsLastBatch(isLastBatch);
		       		updateBatch.setBatchToken(batchToken);
		       		BatchUploadResponse response = 
		       				synapseAdmin.updateSubmissionStatusBatch(evaluationId, updateBatch);
		       		batchToken = response.getNextUploadToken();
		       	}
		       	break; // success!
    		} catch (SynapseConflictingUpdateException e) {
    			// we collided with someone else access the Evaluation.  Will retry!
    		}
    	}
    }
    
    private File downloadSubmissionFile(Submission submission) throws SynapseException, IOException {
		String fileHandleId = getFileHandleIdFromEntityBundle(submission.getEntityBundleJSON());
		File temp = File.createTempFile("temp", null);
		synapseAdmin.downloadFromSubmission(submission.getId(), fileHandleId, temp);
		return temp;
    }
    
    private static String getFileHandleIdFromEntityBundle(String s) {
    	try {
	    	JSONObject bundle = new JSONObject(s);
	    	JSONArray fileHandles = (JSONArray)bundle.get("fileHandles");
	    	for (int i=0; i<fileHandles.length(); i++) {
	    		JSONObject fileHandle = fileHandles.getJSONObject(i);
	    		if (!fileHandle.get("concreteType").equals("org.sagebionetworks.repo.model.file.PreviewFileHandle")) {
	    			return (String)fileHandle.get("id");
	    		}
	    	}
	    	throw new IllegalArgumentException("File has no file handle ID");
    	} catch (JSONException e) {
    		throw new RuntimeException(e);
    	}
    }
    
    private static String formatInterval(final long l) {
        final long hr = TimeUnit.MILLISECONDS.toHours(l);
        final long min = TimeUnit.MILLISECONDS.toMinutes(l - TimeUnit.HOURS.toMillis(hr));
        final long sec = TimeUnit.MILLISECONDS.toSeconds(l - TimeUnit.HOURS.toMillis(hr) - TimeUnit.MINUTES.toMillis(min));
        final long ms = TimeUnit.MILLISECONDS.toMillis(l - TimeUnit.HOURS.toMillis(hr) - TimeUnit.MINUTES.toMillis(min) - TimeUnit.SECONDS.toMillis(sec));
        return String.format("%02dh:%02dm:%02d.%03ds", hr, min, sec, ms);
    }
    
    private static void addAnnotations(
    		Annotations a, 
    		String alias,
    		long createOn,
    		double score,
    		String description
    		) {
		List<StringAnnotation> sas = a.getStringAnnos();
		if (sas==null) {
			sas = new ArrayList<StringAnnotation>();
			a.setStringAnnos(sas);
		}
		{
			StringAnnotation sa = new StringAnnotation();
			sa.setIsPrivate(false);
			sa.setKey("Team");
			sa.setValue(alias);
			sas.add(sa);
		}
		{
			StringAnnotation sa = new StringAnnotation();
			sa.setIsPrivate(false);
			sa.setKey("Description");
			sa.setValue(description);
			sas.add(sa);
		}
		DoubleAnnotation da = new DoubleAnnotation();
		da.setIsPrivate(false);
		da.setKey("Score");
		da.setValue(score);
		List<DoubleAnnotation> das = a.getDoubleAnnos();
		if (das==null) {
			das = new ArrayList<DoubleAnnotation>();
			a.setDoubleAnnos(das);
		}
		das.add(da);
		LongAnnotation la = new LongAnnotation();
		la.setIsPrivate(false);
		la.setKey("createdOnPublic");
		la.setValue(createOn);
		List<LongAnnotation> las = a.getLongAnnos();
		if (las==null) {
			las = new ArrayList<LongAnnotation>();
			a.setLongAnnos(las);
		}
		las.add(la);   	
    }
    
    
	public static void initProperties() {
		if (properties!=null) return;
		properties = new Properties();
		InputStream is = null;
    	try {
    		is = OlfactionChallengeScoring.class.getClassLoader().getResourceAsStream("global.properties");
    		properties.load(is);
    	} catch (IOException e) {
    		throw new RuntimeException(e);
    	} finally {
    		if (is!=null) try {
    			is.close();
    		} catch (IOException e) {
    			throw new RuntimeException(e);
    		}
    	}
   }
	
	public static String getProperty(String key) {
		initProperties();
		String commandlineOption = System.getProperty(key);
		if (commandlineOption!=null) return commandlineOption;
		String embeddedProperty = properties.getProperty(key);
		if (embeddedProperty!=null) return embeddedProperty;
		throw new RuntimeException("Cannot find value for "+key);
	}	
	  
	private static SynapseClient createSynapseClient() {
		SynapseClientImpl scIntern = new SynapseClientImpl();
		scIntern.setAuthEndpoint("https://repo-prod.prod.sagebase.org/auth/v1");
		scIntern.setRepositoryEndpoint("https://repo-prod.prod.sagebase.org/repo/v1");
		scIntern.setFileEndpoint("https://repo-prod.prod.sagebase.org/file/v1");
		return SynapseProfileProxy.createProfileProxy(scIntern);
	}

}
