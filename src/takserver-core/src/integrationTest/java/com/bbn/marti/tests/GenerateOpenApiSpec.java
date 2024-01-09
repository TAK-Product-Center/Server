package com.bbn.marti.tests;

import com.bbn.marti.test.shared.AbstractTestClass;
import com.bbn.marti.test.shared.data.users.AbstractUser;
import com.bbn.marti.test.shared.data.generated.ImmutableConnections;
import com.bbn.marti.test.shared.data.generated.ImmutableUsers;
import com.bbn.marti.test.shared.data.servers.ImmutableServerProfiles;
import org.junit.Assert;
import org.junit.Test;


public class GenerateOpenApiSpec extends AbstractTestClass {

    private static final String className = "GenerateOpenApiSpec";
    protected static final AbstractUser admin = ImmutableUsers.s0_authstcp_authwssuser012_012f;

    @Test(timeout = 600000)
    public void generateOpenApiSpec() {
        try {
            String sessionIdentifier = initTestMethod();

            engine.offlineAddUsersAndConnectionsIfNecessary(ImmutableUsers.s0_authssl_authuser12_012f);

            engine.startServer(ImmutableServerProfiles.SERVER_0, sessionIdentifier);

            engine.connectClientsAndVerify(true,
                    ImmutableUsers.s0_authssl_authuser12_012f);

            engine.openApiSpecGet(admin);

        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        } finally {
            engine.stopServers(ImmutableServerProfiles.SERVER_0);
        }
    }

}
