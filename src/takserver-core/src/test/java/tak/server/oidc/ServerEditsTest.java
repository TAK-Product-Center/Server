package tak.server.oidc;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.StringWriter;

import javax.xml.namespace.QName;

import org.junit.Test;

import com.bbn.marti.config.CertificateConfig;
import com.bbn.marti.config.NameEntries;
import com.bbn.tak.tls.CertManagerApi.CertificateConfigResponse;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.Marshaller;
import tak.server.ServerConfiguration;

public class ServerEditsTest {

    @Test
    public void redirectsLegacyExternalAuthWhenTakLoginIsDisabled() {
        assertTrue(ServerConfiguration.shouldRedirectToExternalLogin(false, true, true));
    }

    @Test
    public void keepsTakLoginForRegisteredOidcProvider() {
        assertFalse(ServerConfiguration.shouldRedirectToExternalLogin(false, true, false));
    }

    @Test
    public void keepsTakLoginWhenExplicitlyConfigured() {
        assertFalse(ServerConfiguration.shouldRedirectToExternalLogin(true, true, true));
    }

    @Test
    public void doesNotRedirectWithoutExternalAuthConfiguration() {
        assertFalse(ServerConfiguration.shouldRedirectToExternalLogin(false, false, true));
    }

    @Test
    public void certificateConfigResponseAddsIdentityWithoutDroppingExistingFields() throws Exception {
        CertificateConfig config = new CertificateConfig();
        config.setNameEntries(new NameEntries());
        CertificateConfigResponse response = new CertificateConfigResponse(config, "alice@example.test");

        Marshaller marshaller = JAXBContext.newInstance(CertificateConfigResponse.class).createMarshaller();
        StringWriter writer = new StringWriter();
        QName rootName = new QName("com.bbn.marti.config", "certificateConfig");
        marshaller.marshal(new JAXBElement<>(rootName, CertificateConfigResponse.class, response), writer);

        String xml = writer.toString();
        assertTrue(xml.contains("nameEntries"));
        assertTrue(xml.contains("<identity>alice@example.test</identity>"));
    }
}
