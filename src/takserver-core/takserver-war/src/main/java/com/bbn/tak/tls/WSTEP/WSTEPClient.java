package com.bbn.tak.tls.WSTEP;

import com.bbn.marti.remote.exception.TakException;

import com.sun.xml.ws.developer.JAXWSProperties;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.StringReader;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.*;
import javax.net.ssl.*;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.soap.*;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Dispatch;
import javax.xml.ws.Service;
import javax.xml.ws.soap.SOAPBinding;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import sun.security.pkcs.PKCS7;


/**
 * WS-Trust X.509v3 Token Enrollment Extensions (WSTEP) client
 */
public class WSTEPClient {

	private static class TrustAllHosts implements HostnameVerifier {
		public boolean verify(String hostname, SSLSession session) {
			return true;
		}
	}

	public static final Logger logger = LoggerFactory.getLogger(WSTEPClient.class);

	public static X509Certificate[] submitCSR(String CSR, String TemplateName, String svcUrl,
											  String username, String password,
											  String truststore, String truststorePassword, String tlsContext) {
		try {
			//
			// create the web service client
			//
			String namespace = "http://schemas.microsoft.com/windows/pki/2009/01/enrollment";
			QName serviceInfo = new QName(namespace, "SecurityTokenService");
			QName portName = new QName(namespace, "WSHttpBinding_ISecurityTokenService");
			String portAddress = svcUrl + "/CES";
			Service sts = Service.create(null, serviceInfo);
			sts.addPort(portName, SOAPBinding.SOAP12HTTP_BINDING, portAddress);
			Dispatch<SOAPMessage> dispatch = sts.createDispatch(portName, SOAPMessage.class, Service.Mode.MESSAGE);
			MessageFactory messageFactory = MessageFactory.newInstance(SOAPConstants.SOAP_1_2_PROTOCOL);
			SOAPFactory soapFactory = SOAPFactory.newInstance();

			//
			// create a new ssl context with our truststore
			//
			TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(
					TrustManagerFactory.getDefaultAlgorithm());
			KeyStore keyStore = KeyStore.getInstance("JKS");
			keyStore.load(new FileInputStream(truststore), truststorePassword.toCharArray());
			trustManagerFactory.init(keyStore);
			SSLContext sslContext = SSLContext.getInstance(tlsContext);
			sslContext.init(null, trustManagerFactory.getTrustManagers(), new java.security.SecureRandom());

			// update the web service context with our custom socket factory. this causes us to pull in com.sun.xml.ws
			// and therefore jaxws-rt.jar and it's dependencies
			dispatch.getRequestContext().put(JAXWSProperties.SSL_SOCKET_FACTORY, sslContext.getSocketFactory());

			// update the web service context with the endpoint address. this allows us to pass in null as the
			// wsdl location in Service.create.. but we still need to point to the endpoint
			dispatch.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, portAddress);

			// disable hostname verification, we still verify the host by requiring the trust store to be loaded
			// in advance. This just relaxes the requirements that the cert's CN match it's hostname
			dispatch.getRequestContext().put(JAXWSProperties.HOSTNAME_VERIFIER, new TrustAllHosts());

			// create a new message
			SOAPMessage request = messageFactory.createMessage();

			// create the message header
			SOAPHeader header = request.getSOAPHeader();

			//
			// add header elements
			//
			SOAPElement to = soapFactory.createElement(
					"To", "", "http://www.w3.org/2005/08/addressing");
			to.addTextNode(portAddress);
			header.addChildElement(to);

			SOAPElement action = soapFactory.createElement(
					"Action", "", "http://www.w3.org/2005/08/addressing");
			action.addTextNode(
					"http://schemas.microsoft.com/windows/pki/2009/01/enrollment/RST/wstep");
			header.addChildElement(action);

			SOAPElement messageID = soapFactory.createElement(
					"MessageID", "", "http://www.w3.org/2005/08/addressing");
			messageID.addTextNode("uuid:" + UUID.randomUUID());
			header.addChildElement(messageID);

			//
			// add username/password to the header
			//
			SOAPElement security = header.addChildElement(
					"Security", "wsse",
					"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd");
			SOAPElement usernameToken = security.addChildElement(
					"UsernameToken", "wsse");
			SOAPElement usernameSoap = usernameToken.addChildElement(
					"Username", "wsse");
			usernameSoap.addTextNode(username);
			SOAPElement passwordSoap = usernameToken.addChildElement(
					"Password", "wsse");
			passwordSoap.addTextNode(password);
			header.addChildElement(security);

			// create the message body
			SOAPBody soapBody = request.getSOAPBody();

			//
			// create the RequestSecurityToken payload
			//
			QName requestSecurityToken = new QName(
					"http://docs.oasis-open.org/ws-sx/ws-trust/200512",
					"RequestSecurityToken", "");
			SOAPBodyElement payload = soapBody.addBodyElement(requestSecurityToken);
			payload.addChildElement("TokenType").addTextNode(
					"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-x509-token-profile-1.0#X509v3");
			payload.addChildElement("RequestType").addTextNode(
					"http://docs.oasis-open.org/ws-sx/ws-trust/200512/Issue");

			//
			// add the CSR
			//
			SOAPElement binarySecurityToken = soapFactory.createElement(
					"BinarySecurityToken", "",
					"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd");
			binarySecurityToken.setAttribute(
					"ValueType", "http://schemas.microsoft.com/windows/pki/2009/01/enrollment#PKCS10");
			binarySecurityToken.setAttribute(
					"EncodingType",
					"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd#base64binary");
			binarySecurityToken.addNamespaceDeclaration(
					"a", "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd");
			binarySecurityToken.addTextNode(CSR);
			payload.addChildElement(binarySecurityToken);

			//
			// create the AdditionalContext element
			//
			QName additionalContextName = new QName(
					"http://schemas.xmlsoap.org/ws/2006/12/authorization",
					"AdditionalContext", "");
			SOAPBodyElement additionalContext = soapBody.addBodyElement(additionalContextName);
			payload.addChildElement(additionalContext);

			//
			// set the template name
			//
			SOAPElement certificateTemplate = additionalContext.addChildElement("ContextItem");
			certificateTemplate.setAttribute("Name", "CertificateTemplate");
			SOAPElement Value = additionalContext.addChildElement("Value");
			Value.setTextContent(TemplateName);
			certificateTemplate.addChildElement(Value);
			additionalContext.addChildElement(certificateTemplate);

			//
			// set the remote server name
			//
			SOAPElement rmd = additionalContext.addChildElement("ContextItem");
			rmd.setAttribute("Name", "rmd");
			SOAPElement Value3 = additionalContext.addChildElement("Value");
			Value3.setTextContent(portAddress);
			rmd.addChildElement(Value3);
			additionalContext.addChildElement(rmd);

			// send the request
			logger.info("sending certificate signing request");
			SOAPMessage response = dispatch.invoke(request);
			logger.info("received certificate signing response");

			// parse the response
			X509Certificate[] chain = parseResponse(response);
			return chain;

		} catch (Exception e) {
			logger.error("exception in submitCSR!", e);
			return null;
		}
	}

	public static X509Certificate[] parseResponse(SOAPMessage response) {
		try {
			//
			// read the response into a document
			//
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			response.writeTo(baos);
			DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
			Document document = documentBuilder.parse(new InputSource(new StringReader(baos.toString())));
			document.getDocumentElement().normalize();

			//
			// get the RequestSecurityTokenResponse node
			//
			NodeList requestSecurityTokenResponses = document.getElementsByTagName("RequestSecurityTokenResponse");
			if (requestSecurityTokenResponses.getLength() != 1) {
				throw new TakException("Found more than 1 RequestSecurityTokenResponse!");
			}
			Node requestSecurityTokenResponse = requestSecurityTokenResponses.item(0);

			//
			// the response will contain 2 BinarySecurityToken. the first is a pkcs7 file containing the
			// full trust chain followed by the signed certificate. the second is the signed certificate PEM
			// we need the full chain so we're only concerned with the first
			//
			Element requestSecurityTokenElement = (Element)requestSecurityTokenResponse;
			NodeList nodeList = requestSecurityTokenElement.getElementsByTagName("BinarySecurityToken");
			if (nodeList.getLength() != 2) {
				throw new TakException("Found more than 2 BinarySecurityTokens!");
			}
			Element element = (Element) nodeList.item(0);
			NodeList cert = element.getChildNodes();

			//
			// load pkcs7 and extract certificate chain
			//
			String encodedPkcs7 = ((Node)cert.item(0)).getNodeValue();
			byte[] bytes = Base64.decodeBase64(encodedPkcs7.getBytes("UTF-8"));
			PKCS7 pkcs7 = new PKCS7(bytes);
			return pkcs7.getCertificates();

		} catch (Exception e) {
			logger.error("exception in parseResponse!", e);
			return null;
		}
	}
}
