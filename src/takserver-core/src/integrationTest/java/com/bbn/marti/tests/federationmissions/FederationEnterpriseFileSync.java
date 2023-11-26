package com.bbn.marti.tests.federationmissions;

import com.bbn.marti.test.shared.data.servers.ImmutableServerProfiles;
import org.junit.AfterClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

/**
 * Created on 10/28/15.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class FederationEnterpriseFileSync extends FederationConfigurationA {

	@Test(timeout = LONG_TIMEOUT)
	public void a_setupEnvironment() {
		innerSetupEnvironment(FederationEnterpriseFileSync.class);
	}

	@Test(timeout = LONG_TIMEOUT)
	public void b_addFile() {
		dataAUploadFileHash = engine.fileAdd(admin, "testData.jpg", dataA);
		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	@Test(timeout = SHORT_TIMEOUT)
	public void c_downloadFile() {
		engine.fileDownload(existingMember, dataAUploadFileHash);
	}

//	@Test(timeout = SHORT_TIMEOUT)
//	public void d_deleteFile() {
//		engine.fileDelete(admin, dataAUploadFileHash);
//		try {
//			Thread.sleep(10000);
//		} catch (InterruptedException e) {
//			throw new RuntimeException(e);
//		}
//	}
//
//	@Test(timeout = SHORT_TIMEOUT)
//	public void e_downloadDeletedFile() {
//		engine.fileDownload(existingMember, dataAUploadFileHash);
//	}
//
	@Test(timeout=360000)
	public void zzz_teardown() {
		try {
			engine.stopServers(getServers());
		} catch (Exception e) {
//			 It doesn't matter if we run into issues here, so print and move on
			System.err.println(e.getMessage());
		}
	}

//	@AfterClass
//	public static void zzz_teardown() {
//		try {
//			engine.stopServers(getServers());
//		} catch (Exception e) {
//			 It doesn't matter if we run into issues here, so print and move on
//			System.err.println(e.getMessage());
//		}
//	}
}
