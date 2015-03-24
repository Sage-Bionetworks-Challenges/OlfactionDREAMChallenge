package org.sagebionetworks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.sagebionetworks.util.Pair;

import com.google.common.io.Files;

public class TestAddToZip {
	
	private static final String TEST_CONTENT = "my dog has fleas";

	@Test
	public void testAddToZip() throws Exception {
		File tempDir = Files.createTempDir();
		File zipFile = OlfactionChallengeScoring.writeResourceToFile("sub1_final.zip", tempDir);
		File fileToAdd = File.createTempFile("foo", "txt");
		try (InputStream is = new ByteArrayInputStream(TEST_CONTENT.getBytes())) {
			try (OutputStream os = new FileOutputStream(fileToAdd)) {
				IOUtils.copy(is, os);
			}
		}
		String name = "testAddToZip.txt";
		Pair<File,String> fileToAddWithName = new Pair<>(fileToAdd, name);
		OlfactionChallengeScoring.addFilesToZip(zipFile, Collections.singletonList(fileToAddWithName));
		
		boolean foundIt = false;
		try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
			ZipEntry ze = null;
			while(true) {
				ze = zis.getNextEntry();
				if (ze==null) break;
				if (name.equals(ze.getName())) {
					foundIt = true;
					try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
						IOUtils.copy(zis, baos);
						assertEquals(TEST_CONTENT, new String(baos.toByteArray()));
					}
				}
			}
		}
		assertTrue(foundIt);
	}

}
