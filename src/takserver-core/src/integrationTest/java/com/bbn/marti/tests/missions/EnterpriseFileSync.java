package com.bbn.marti.tests.missions;

import com.bbn.marti.test.shared.data.servers.ImmutableServerProfiles;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

/**
 * Created on 10/28/15.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class EnterpriseFileSync extends AbstractConfigurationA {

	protected ImmutableServerProfiles[] getServers() {
		return new ImmutableServerProfiles[]{ImmutableServerProfiles.SERVER_0, ImmutableServerProfiles.SERVER_1, ImmutableServerProfiles.SERVER_2};
	}

	@Test(timeout = LONG_TIMEOUT)
	public void a_setupEnvironment() {
		innerSetupEnvironment(EnterpriseFileSync.class);
	}

	@Test(timeout = SHORT_TIMEOUT)
	public void b_addFile() {
		dataAUploadFileHash = engine.fileAdd(admin, "testData.jpg", dataA);
	}

	@Test(timeout = SHORT_TIMEOUT)
	public void c_downloadFile() {
		engine.fileDownload(existingMember, dataAUploadFileHash);
	}

	@Test(timeout = SHORT_TIMEOUT)
	public void d_deleteFile() {
		engine.fileDelete(admin, dataAUploadFileHash);
	}

	@Test(timeout = SHORT_TIMEOUT)
	public void e_downloadDeletedFile() {
		engine.fileDownload(existingMember, dataAUploadFileHash);
	}

	@Test(timeout=SHORT_TIMEOUT)
	public void zzz_teardown() {
		try {
			engine.stopServers(getServers());
		} catch (Exception e) {
			// It doesn't matter if we run into issues here, so print and move on
			System.err.println(e.getMessage());
		}
	}
}
