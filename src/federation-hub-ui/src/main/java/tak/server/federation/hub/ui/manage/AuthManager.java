package tak.server.federation.hub.ui.manage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.security.MessageDigest;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import jakarta.xml.bind.DatatypeConverter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tak.server.federation.hub.ui.FederationHubUIConfig;

/* Configuration program to authorize users to access UI. */
public class AuthManager {
    private static final Logger logger = LoggerFactory.getLogger(AuthManager.class);

    private static final String USAGE_STRING =
        "Usage: java -jar AuthManager.java PATH_TO_CERT [PATH_TO_AUTH_USERS]\n\n" +
        "  PATH_TO_CERT := path to certificate of client to authorize (required)\n\n" +
        "  PATH_TO_AUTH_USERS := path to file to add authorized user to (optional)\n" +
        "    defaults to: " + FederationHubUIConfig.AUTH_USER_FILE_DEFAULT;

    public static void main(String[] args) {
        if (args.length < 1 || args.length > 2) {
            System.err.println("Incorrect number of parameters");
            System.err.println(USAGE_STRING);
            return;
        }

        String authFile;
        if (args.length == 2)
            authFile = args[1];
        else
            authFile = FederationHubUIConfig.AUTH_USER_FILE_DEFAULT;

        /* Check resources first. */
        Path file;
        URL resource = AuthManager.class.getClassLoader().getResource(args[0]);
        if (resource != null) {
            try {
                URI uri = resource.toURI();
                Map<String, String> env = new HashMap<>();
                env.put("create", "true");
                FileSystem fs = FileSystems.newFileSystem(uri, env);
                file = Paths.get(uri);
            } catch (URISyntaxException | IOException e) {
                logger.error("Error retrieving file: " + e);
                return;
            }
        } else {
            /* Use argument as path instead. */
            file = FileSystems.getDefault().getPath(args[0]);
        }

        if (!Files.isRegularFile(file)) {
            System.err.println("Provided file " + args[0] + " does not exist or is not regular file");
            return;
        }

        X509Certificate cert;
        try {
            cert = getCertificate(file.toString());
        } catch (CertificateException | FileNotFoundException e) {
            System.err.println("Could not get certificate from " + file.toString() + ": " + e);
            return;
        }

        String username;
        try {
            username = getCertificateUserName(cert);
            if (username == null)
                return;
        } catch (InvalidNameException e) {
            System.err.println("Could not get username from certificate: " + e);
            return;
        }

        String fingerprint;
        try {
           fingerprint = getCertificateFingerprint(cert);
        } catch (CertificateException | NoSuchAlgorithmException e) {
            System.err.println("Could not get fingerprint of certificate: " + e);
            return;
        }

        addUser(authFile, username, fingerprint);
    }

    private static X509Certificate getCertificate(String filepath)
            throws CertificateException, FileNotFoundException {
        InputStream fileInputStream = new FileInputStream(filepath);
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        return (X509Certificate)certificateFactory.generateCertificate(fileInputStream);
    }

    public static String getCertificateFingerprint(X509Certificate certificate)
            throws CertificateException, NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-512");
        String fingerprint = DatatypeConverter.printHexBinary(
            md.digest(certificate.getEncoded()));

        StringBuilder fpBuilder = new StringBuilder();

        for (int i = 0; i < fingerprint.length(); i++) {
            if (i > 0 && i % 2 == 0) {
                fpBuilder.append(':');
            }
            fpBuilder.append(Character.toUpperCase(fingerprint.charAt(i)));
        }
        return fpBuilder.toString();
    }

    private static String getCN(String dn) throws InvalidNameException {
        if (dn == null || dn.isEmpty()) {
            throw new IllegalArgumentException("Empty DN");
        }

        LdapName ldapName = new LdapName(dn);

        for (Rdn rdn : ldapName.getRdns()) {
            if (rdn.getType().equalsIgnoreCase("CN")) {
                return rdn.getValue().toString();
            }
        }

        System.err.println("Invalid certificate; no CN found in domain name " + dn);
        return null;
    }

    public static String getCertificateUserName(X509Certificate certificate)
            throws InvalidNameException {
        return getCN(certificate.getSubjectX500Principal().getName());
    }

	private static void addUser(String authFile, String username, String fingerprint) {
        ObjectMapper om = new ObjectMapper(new YAMLFactory());
        AuthorizedUsers authUsers;

        /* Get list of existing authorized users. */
        URL resource = AuthManager.class.getResource(authFile);
        File file;
        if (resource != null)
            file = new File(resource.getFile());
        else
            file = FileSystems.getDefault().getPath(authFile).toFile();

        if (file.exists()) {
            try {
                authUsers = om.readValue(file, AuthorizedUsers.class);
            } catch (IOException e) {
                System.err.println("Can't read authorized user file " + authFile + "; creating a new one: " + e);
                authUsers = new AuthorizedUsers();
            }
        } else {
            System.err.println("Can't find authorized user file " + authFile + "; creating a new one");
            authUsers = new AuthorizedUsers();
        }

        /* Add new authorized user. */
        List<AuthorizedUser> userList = authUsers.getUsers();
        AuthorizedUser newUser = new AuthorizedUser(username, fingerprint);
        if (userList.contains(newUser)) {
            System.out.println("User is already authorized:\n" +
                "\tUsername: " + username + "\n" +
                "\tFingerprint: " + fingerprint);
            return;
        }
        userList.add(newUser);

        /* Write back new list. */
        try {
            om.writeValue(file, authUsers);
        } catch (IOException e) {
            System.err.println("Can't write authorized user file " + authFile + ": " + e);
            return;
        }

        System.out.println("Successfully added authorized user:\n" +
            "\tUsername: " + username + "\n" +
            "\tFingerprint: " + fingerprint + "\n\n" +
            "Written to " + authFile + ".");
    }
}
