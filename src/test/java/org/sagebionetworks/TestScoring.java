package org.sagebionetworks;

import static org.junit.Assert.*;

import org.junit.Test;

public class TestScoring {
	
	private static final String entityBundle = "{\"entity\":{\"concreteType\":\"org.sagebionetworks.repo.model.FileEntity\",\"versionLabel\":\"1\",\"etag\":\"00000000-0000-0000-0000-000000000000\",\"accessControlList\":\"/repo/v1/entity/syn3155531/acl\",\"versionUrl\":\"/repo/v1/entity/syn3155531/version/1\",\"modifiedBy\":\"273960\",\"entityType\":\"org.sagebionetworks.repo.model.FileEntity\",\"uri\":\"/repo/v1/entity/syn3155531\",\"id\":\"syn3155531\",\"createdOn\":\"2015-01-27T18:24:38.322Z\",\"versions\":\"/repo/v1/entity/syn3155531/version\",\"modifiedOn\":\"2015-01-27T18:24:38.322Z\",\"parentId\":\"syn3135021\",\"createdBy\":\"273960\",\"name\":\"2015Jan27_s1_ok.txt\",\"annotations\":\"/repo/v1/entity/syn3155531/annotations\",\"versionNumber\":1,\"dataFileHandleId\":\"911048\"},\"fileHandles\":[{\"id\":\"911048\",\"createdOn\":\"2015-01-27T18:24:37.000Z\",\"previewId\":\"911049\",\"concreteType\":\"org.sagebionetworks.repo.model.file.S3FileHandle\",\"contentSize\":1672444,\"etag\":\"7f65bd4c-ba80-47f0-b4e4-fab86dc906d1\",\"createdBy\":\"273960\",\"fileName\":\"2015Jan27_s1_ok.txt\",\"contentType\":\"text/plain\",\"contentMd5\":\"80945d32ec9d71b5ee808caf15918b6f\",\"bucketName\":\"proddata.sagebase.org\",\"key\":\"273960/7b01d135-2b19-41a0-b0df-2efbb9129565/2015Jan27_s1_ok.txt\"},{\"id\":\"911049\",\"createdOn\":\"2015-01-27T18:24:37.000Z\",\"concreteType\":\"org.sagebionetworks.repo.model.file.PreviewFileHandle\",\"contentSize\":1500,\"etag\":\"a997d3d0-9e8b-4495-8f3c-c0ae3166dc19\",\"createdBy\":\"273960\",\"fileName\":\"preview.txt\",\"contentType\":\"text/plain\",\"bucketName\":\"proddata.sagebase.org\",\"key\":\"273960/a87b2581-91c0-45cd-b2b5-53f728cb763e\"}],\"annotations\":{\"id\":\"syn3155531\",\"creationDate\":\"1422383078322\",\"stringAnnotations\":{},\"dateAnnotations\":{},\"etag\":\"00000000-0000-0000-0000-000000000000\",\"doubleAnnotations\":{},\"longAnnotations\":{},\"blobAnnotations\":{},\"uri\":\"/entity/syn3155531/annotations\"},\"entityType\":\"org.sagebionetworks.repo.model.FileEntity\"}";

	@Test
	public void testExtractNameFromEntityBundle() {
		assertEquals("2015Jan27_s1_ok.txt", OlfactionChallengeScoring.getEntityNameFromBundle(entityBundle));
	}

}
