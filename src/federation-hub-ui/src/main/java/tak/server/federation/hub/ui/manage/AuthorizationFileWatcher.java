package tak.server.federation.hub.ui.manage;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.*;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tak.server.federation.hub.ui.FederationHubUIConfig;

public class AuthorizationFileWatcher {
    private static final Logger logger = LoggerFactory.getLogger(AuthorizationFileWatcher.class);

    private Thread thread;
    private WatchService watchService;

    private File file;

    private FederationHubUIConfig fedHubConfig;

    private List<AuthorizedUser> userList;

    public AuthorizationFileWatcher(FederationHubUIConfig fedHubConfig) {
        this.fedHubConfig = fedHubConfig;

        URL resource = getClass().getResource(fedHubConfig.getAuthUsers());
        if (resource != null)
            this.file = new File(resource.getFile());
        else
            this.file = FileSystems.getDefault().getPath(
                fedHubConfig.getAuthUsers()).toFile();

        logger.info("Monitoring for authorized users in file " + this.file.getAbsolutePath());
        this.userList = readAuthorizedUsers();
    }

    public boolean userAuthorized(String username, String fingerprint) {
        return this.userList.contains(new AuthorizedUser(username, fingerprint));
    }

    private List<AuthorizedUser> readAuthorizedUsers() {
        ObjectMapper om = new ObjectMapper(new YAMLFactory());
        AuthorizedUsers authUsers;
        if (this.file.exists()) {
            try {
                authUsers = om.readValue(this.file, AuthorizedUsers.class);
            } catch (IOException e) {
                logger.error("Can't read authorized user file " + fedHubConfig.getAuthUsers() + ": " + e);
                authUsers = new AuthorizedUsers();
            }
        } else {
            logger.info("Authorized user file does not yet exist: " + fedHubConfig.getAuthUsers());
            authUsers = new AuthorizedUsers();
        }
        return authUsers.getUsers();
    }

    public void start() throws IOException {
        this.watchService = FileSystems.getDefault().newWatchService();
        Path p = this.file.toPath();

        Path parent;
        if (p.getParent() == null) {
            File absolute = new File(this.file.getAbsolutePath());
            parent = absolute.toPath().getParent();
        } else {
            parent = p.getParent();
        }
        parent.register(this.watchService,
            StandardWatchEventKinds.ENTRY_MODIFY,
            StandardWatchEventKinds.ENTRY_CREATE,
            StandardWatchEventKinds.ENTRY_DELETE);

        this.thread = new Thread(() -> {
            while (true) {
                WatchKey wk = null;
                try {
                    wk = this.watchService.take();
                    Thread.sleep(500);
                    for (WatchEvent<?> event : wk.pollEvents()) {
                        Path changed = parent.resolve((Path)event.context());
                        if (Files.exists(changed) && Files.isSameFile(changed, p)) {
                            logger.info("Authorized user file changed");
                            this.userList = readAuthorizedUsers();
                            break;
                        }
                    }
                } catch (InterruptedException e) {
                    logger.error("Stop watching authorized user file");
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    logger.error("Error while reloading cert", e);
                } finally {
                    if (wk != null) {
                        wk.reset();
                    }
                }
            }
        });
        this.thread.start();
    }

    public void stop() {
        this.thread.interrupt();
        try {
            this.watchService.close();
        } catch (IOException e) {
            logger.error("Error closing watch service", e);
        }
    }
}
