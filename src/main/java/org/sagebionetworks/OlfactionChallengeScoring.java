package org.sagebionetworks;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
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
import org.sagebionetworks.repo.model.query.QueryTableResults;
import org.sagebionetworks.repo.model.query.Row;


/**
 * Olfaction scoring application
 * 
 */
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
    
    
    private static final String[] SUB_CHALLENGE_1_METRICS = new String[]{"avg intensity", "avg valence", "avg 19 other", "avg of Z-scores"};
    private static final int SUB_CHALLENGE_1_QUOTA = 2; //300;
    private static final String[] SUB_CHALLENGE_2_METRICS = new String[]{"intensity val" ,  "valence val" ,  "19 other val" , "intensity sigma" ,  "valence sigma" ,  "19 other sigma", "avg of Z-scores"};
    private static final int SUB_CHALLENGE_2_QUOTA = 2; //30;

    public static void main( String[] args ) throws Exception {
   		OlfactionChallengeScoring sct = new OlfactionChallengeScoring();
   		
   		sct.checkLeaderBoard();

    	// validate and score subchallenge 1
		sct.validate(SUBCHALLENGE.SUBCHALLENGE_1, "3154769");
   		sct.score(SUBCHALLENGE.SUBCHALLENGE_1, "3154769", SUB_CHALLENGE_1_QUOTA);
   		
    	// validate and score subchallenge 2
		sct.validate(SUBCHALLENGE.SUBCHALLENGE_2, "3154771");
   		sct.score(SUBCHALLENGE.SUBCHALLENGE_2, "3154771", SUB_CHALLENGE_2_QUOTA);
   		
   		// TODO for final round, do we just zip up submitted files for manual scoring?

    }
    
    public void checkLeaderBoard() throws Exception {
    	QueryTableResults qtr = synapseAdmin.queryEvaluation("select * from evaluation_3154769");
    	System.out.println("Headers: "+qtr.getHeaders());
    	for (Row row : qtr.getRows()) {
    		for (String value : row.getValues()) {
    			System.out.print(value+"\t");
    		}
    		System.out.println("");
    	}
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
    
    /*
     * @param name:  the file name in src/main/resources
     * @param params: the params to pass, after perl <script file> 
     * @param outputFile: the file into which the script is to write its results
     * @param workingDirectory: the working directory for the process (returned by 'cwd' in Perl)
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
		File outputFile = File.createTempFile("scriptOutput", ".txt");
		outputFile.deleteOnExit();
		File workingDirectory = outputFile.getParentFile();;
	    // note, the validation file expects the input file PATH but the output file NAME
 		return OlfactionChallengeScoring.executePerlScript(scriptName, 
 				new String[]{inputFile.getAbsolutePath(), outputFile.getName(), phase}, outputFile, workingDirectory);
    }
    
    public static Map<String,Double> score(SUBCHALLENGE subchallenge, File inputFile, File goldStandard) throws IOException {
	    String scriptName;
	    String[] metrics;
	    switch (subchallenge) {
	    case SUBCHALLENGE_1:
	    	scriptName = "DREAM_Olfaction_scoring_Q1.pl";
	    	metrics = SUB_CHALLENGE_1_METRICS;
	    	break;
	    case SUBCHALLENGE_2:
	    	scriptName = "DREAM_Olfaction_scoring_Q2.pl";
	    	metrics = SUB_CHALLENGE_2_METRICS;
	    	break;
	    default:
	    	throw new IllegalArgumentException(subchallenge.name());
	    }
		File outputFile = File.createTempFile("scriptOutput", ".txt");
		outputFile.deleteOnExit();
		File workingDirectory = outputFile.getParentFile();
		if (!workingDirectory.getAbsolutePath().equals(inputFile.getParentFile().getAbsolutePath())) throw new RuntimeException();
	    // NOTE: Perl script expects input file NAME, output file NAME (not PATH).  Both must be in working directory
	    // However, gold standard is to be a file PATH
		String scriptOutput = OlfactionChallengeScoring.executePerlScript(scriptName, 
				new String[]{inputFile.getName(), outputFile.getName(), 
				goldStandard.getAbsolutePath()}, outputFile, workingDirectory);
		String[] lines = scriptOutput.trim().split("[\n|\r|\r\n]");
		if (lines.length!=2) throw new RuntimeException("Expected 2 lines but found "+lines.length);
		String[] header = lines[0].split("\t");
		if (header.length!=metrics.length) throw new 
			RuntimeException("Expected "+metrics.length+" metrics but found "+header.length);
		for (int i=0; i<header.length; i++) {
			if (!header[i].trim().equalsIgnoreCase(metrics[i])) throw new 
			RuntimeException("Expected "+metrics[i]+" but found "+header[i]);
		}
		String[] valueStrings = lines[1].split("\t");
		if (valueStrings.length!=metrics.length) throw new 
			RuntimeException("Expected "+metrics.length+" metrics but found "+valueStrings.length);
		Map<String,Double> result = new HashMap<String,Double>();
		for (int i=0; i<valueStrings.length; i++) {
			double value;
			if (valueStrings[i]==null || valueStrings[i].length()==0) {
				value = 0d;
			} else {
				value = Double.parseDouble(valueStrings[i]);
			}
			result.put(metrics[i], value);
		}
		return result;
    }
    
     public OlfactionChallengeScoring() throws SynapseException {
    	synapseAdmin = createSynapseClient();
    	String adminUserName = getProperty("ADMIN_USERNAME");
    	String adminPassword = getProperty("ADMIN_PASSWORD");
    	synapseAdmin.login(adminUserName, adminPassword);
    }
    
    private static final DateFormat DF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
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
		String subChallengeString = subchallenge==SUBCHALLENGE.SUBCHALLENGE_1 ? "Sub-Challenge 1": "Sub-Challenge 2";
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
       			boolean fileIsOK = true;
       			String validationResult = "";
       			try {
       				validationResult = validate(subchallenge,temp, "L"/*"leader board" phase*/);
       				fileIsOK = !validationResult.contains("NOT_OK");
       			} catch (Exception e) {
       				fileIsOK = false;
       				validationResult = "Unable to validate submission. Please contact Challenge administration.";
       			}
       			
       			SubmissionStatusEnum newStatus = null;
       			if (fileIsOK) {
       				newStatus = SubmissionStatusEnum.VALIDATED;
       			} else {
       				newStatus = SubmissionStatusEnum.INVALID;
       				// send the user an email message to let them know
       				sendMessage(sub.getUserId(), "Submission Failure", 
       						"Dear Participant:\nYour submission to  Olfaction "+subChallengeString+", received on "+
       						DF.format(sub.getCreatedOn())+", was invalid.  Please try again.  Details are given below.\n"+
       						"Sincerely,\nChallenge Administration\n\n"+validationResult);
       			}
           		SubmissionStatus status = bundle.getSubmissionStatus();
           		status.setStatus(newStatus);
           	    statusesToUpdate.add(status);
        	}
       	}
       	// we can update all the statuses in a batch
       	updateSubmissionStatusBatch(evaluationId, statusesToUpdate);
    }
       
    private void sendMessage(String userId, String subject, String body) throws SynapseException {
    	MessageToUser messageMetadata = new MessageToUser();
    	messageMetadata.setRecipients(Collections.singleton(userId));
    	messageMetadata.setSubject(subject);
    	synapseAdmin.sendStringMessage(messageMetadata, body);
    }
    
    private int getSubmissionCount(String userId, String evaluationId) throws SynapseException {
    	QueryTableResults qtr = synapseAdmin.queryEvaluation("select * from evaluation_"+evaluationId+" where userId==\""+
    			userId+"\" and status==\"SCORED\" limit 1 offset 0");
    	return qtr.getTotalNumberOfResults().intValue();
    }
    
    /**
     * Note: There are two types of scoring, that in which each submission is scored alone and that
     * in which the entire set of submissions is rescored whenever a new one arrives. 
     * 
     * @throws SynapseException
     */
    public void score(SUBCHALLENGE subchallenge, String evaluationId, int submissionQuota) throws SynapseException, IOException {
    	long startTime = System.currentTimeMillis();
    	List<SubmissionStatus> statusesToUpdate = new ArrayList<SubmissionStatus>();
    	long total = Integer.MAX_VALUE;
    	File goldStandard = null;
    	Map<String,Integer> submissionsPerUser = new HashMap<String,Integer>();
       	Map<String,List<Date>> overQuota = new HashMap<String,List<Date>>();
       	Map<String,List<Date>> scoringFailed = new HashMap<String,List<Date>>();
          		String subChallengeString = subchallenge==SUBCHALLENGE.SUBCHALLENGE_1 ? "Sub-Challenge 1": "Sub-Challenge 2";
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
           		SubmissionStatus status = bundle.getSubmissionStatus();
        		
        		// have we exceeded the quota?
        		Integer submissionCount = submissionsPerUser.get(sub.getUserId());
        		if (submissionCount==null) {
        			submissionCount = getSubmissionCount(sub.getUserId(), evaluationId);
        			submissionsPerUser.put(sub.getUserId(), submissionCount);
        		}
         		if (submissionCount>=submissionQuota) {
           			List<Date> overQuotaList = overQuota.get(sub.getUserId());
           			if (overQuotaList==null) {
           				overQuotaList = new ArrayList<Date>();
           				overQuota.put(sub.getUserId(), overQuotaList);
           			}
           			overQuotaList.add(sub.getCreatedOn());
         			status.setStatus(SubmissionStatusEnum.REJECTED);
          			statusesToUpdate.add(status);
      				continue;
        		}
        		
        		// at least once, download file and make sure it's correct
    			File temp = downloadSubmissionFile(sub);
    			if (goldStandard==null) {
    				goldStandard = File.createTempFile("goldStd", ".txt");
    				goldStandard.deleteOnExit();
    				synapseAdmin.downloadFromFileEntityCurrentVersion(getProperty("GS_ENTITY_ID_L_"+subchallenge), goldStandard);
    			}
    			Map<String,Double> metrics;
         		try {
        			metrics = score(subchallenge, temp, goldStandard);
           			status.setStatus(SubmissionStatusEnum.SCORED);
           			submissionsPerUser.put(sub.getUserId(), submissionCount+1);
        		} catch (Exception e) {
           			status.setStatus(SubmissionStatusEnum.REJECTED);
           			List<Date> rejectedList = scoringFailed.get(sub.getUserId());
           			if (rejectedList==null) {
           				rejectedList = new ArrayList<Date>();
           				scoringFailed.put(sub.getUserId(), rejectedList);
           			}
           			rejectedList.add(sub.getCreatedOn());
           			metrics = new HashMap<String,Double>();
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
    					metrics,
    					sub.getName()
    					);
    			statusesToUpdate.add(status);
        	}
       	}
       	
       	System.out.println("Retrieved "+total+" submissions for scoring.");
       	
       	updateSubmissionStatusBatch(evaluationId, statusesToUpdate);
       	
       	
       	for (String rejectedUser : scoringFailed.keySet()) {
       		List<Date> rejectedTimeStamps = scoringFailed.get(rejectedUser);
       		String message;
       		if (rejectedTimeStamps.size()==1) {
    			message = "Dear Participant:\nYour submission to Olfaction Prediction "+subChallengeString+", received on "+
   						DF.format(rejectedTimeStamps.get(0))+", was rejected.  Please try again or contact us for help.\n"+
   						"Sincerely,\nChallenge Administration";
      		} else {
           		StringBuilder sb = new StringBuilder();
           		boolean firstTime = true;
           		for (Date d: rejectedTimeStamps) {
           			if (firstTime) firstTime=false; else sb.append(", ");
           			sb.append(DF.format(d));
           		}
           		message = "Dear Participant:\nYour submissions to Olfaction Prediction "+subChallengeString+", received on "+
   						sb+", were rejected.  Please try again or contact us for help.\n"+
   						"Sincerely,\nChallenge Administration";
      		}
			sendMessage(rejectedUser, "Submission Scoring Failure", message);
       	}
       	
       	for (String rejectedUser : overQuota.keySet()) {
       		List<Date> rejectedTimeStamps = overQuota.get(rejectedUser);
       		String message;
       		if (rejectedTimeStamps.size()==1) {
    			message = "Dear Participant:\nYour submission to Olfaction Prediction "+subChallengeString+", received on "+
    					DF.format(rejectedTimeStamps.get(0))+", exceeds the submission limit of "+submissionQuota+
   						".  If you have questions, please contact contact us.\n"+
   						"Sincerely,\nChallenge Administration";
   				
     		} else {
           		StringBuilder sb = new StringBuilder();
           		boolean firstTime = true;
           		for (Date d: rejectedTimeStamps) {
           			if (firstTime) firstTime=false; else sb.append(", ");
           			sb.append(DF.format(d));
           		}
           		message = "Dear Participant:\nYour submissions to Olfaction Prediction "+subChallengeString+", received on "+
    					sb+", exceed the submission limit of "+submissionQuota+
   						".  If you have questions, please contact contact us.\n"+
   						"Sincerely,\nChallenge Administration";
      		}
			sendMessage(rejectedUser, "Submissions Exceed Quota", message);
       	}
       	
       	
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
    		Map<String,Double> metrics,
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
		
		
		List<DoubleAnnotation> das = a.getDoubleAnnos();
		if (das==null) {
			das = new ArrayList<DoubleAnnotation>();
			a.setDoubleAnnos(das);
		}
		for (String metricName : metrics.keySet()) {
			DoubleAnnotation da = new DoubleAnnotation();
			da.setIsPrivate(false);
			da.setKey(metricName);
			da.setValue(metrics.get(metricName));
			das.add(da);
		}
		
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
