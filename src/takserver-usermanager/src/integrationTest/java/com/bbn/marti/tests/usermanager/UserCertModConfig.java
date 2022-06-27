package com.bbn.marti.tests.usermanager;

import com.bbn.marti.test.shared.data.servers.AbstractServerProfile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;

/**
 * Created on 9/25/17.
 */
public class UserCertModConfig {
    private final UserManagerTestEngine umte;
    private final AbstractServerProfile server;
    private final ModMode modMode;
    private final String username;
    private final Boolean delete;
    private final String password;
    private final String certpath;
    private final Boolean administrator;
    private final String fingerprint;
    private final String[] group;

    public UserCertModConfig(@NotNull UserManagerTestEngine umte, @NotNull AbstractServerProfile server, @NotNull ModMode modMode, @Nullable String username,
                             @Nullable Boolean delete, @Nullable String password, @Nullable String certpath,
                             @Nullable Boolean administrator, @Nullable String fingerprint, @Nullable String... group) {
        this.umte = umte;
        this.server = server;
        this.modMode = modMode;
        this.username = username;
        this.delete = delete;
        this.password = password;
        this.certpath = certpath;
        this.administrator = administrator;
        this.fingerprint = fingerprint;
        this.group = group;
    }

    public void execute() {
        switch (modMode) {
            case CERT_MOD:
                Assert.assertNotNull(certpath);
                umte.certmod(server, certpath, delete, password, administrator, fingerprint, group);
                break;

            case USER_MOD:
                Assert.assertNotNull(username);
                umte.usermod(server, username, delete, password, certpath, administrator, fingerprint, group);
                break;

            default:
                throw new RuntimeException("Unexpected mod mode '" + modMode.name() + "'!");
        }
    }
}
