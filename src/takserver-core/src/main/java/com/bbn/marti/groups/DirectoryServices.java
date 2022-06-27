

package com.bbn.marti.groups;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NameNotFoundException;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.AttributeInUseException;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.NoSuchAttributeException;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

public class DirectoryServices {

	public String getUserDNByCertAttributes(String serialNumber, String issuerDN) throws Exception {
		
		//Validate arguments
		if (!isSerialNumberValid(serialNumber)) {
			throw new Exception("Invalid serialNumber argument to UserManager.getUserDNByCertAttributes.");			
		}
		
		if (!isIssuerDNValid(issuerDN)) {
			throw new Exception("Invalid issuerDN argument to UserManager.getUserDNByCertAttributes.");
		}
			
		String userDN = null;
		
		List<String> queryResults = new ArrayList<String>();

        DirContext context = null;
        
		try {
			Object[] args = new Object[2];

			args[0] = userClass;
			args[1] = constructCertificateMappingValue(serialNumber, issuerDN);
			
			SearchControls searchControls = new SearchControls();

			searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
			String[] attributeNames = new String[1];
			attributeNames[0] = DN_ATTR_NM;

			searchControls.setReturningAttributes(attributeNames);
			
	    	//Need to add some exception handlers here for pooling
			context = getContext();
	    	
	    	NamingEnumeration<SearchResult> namingEnumeration = context.search(userBaseRDN, userFilter, args, searchControls);
			context.close();
	    	
			while (namingEnumeration != null && namingEnumeration.hasMore()) {
				SearchResult searchResult = namingEnumeration.next();
				queryResults.add((String) searchResult.getAttributes().get(DN_ATTR_NM).get());
			}
		} catch (NamingException e) {
			throw new Exception("Unexpected naming exception in UserManager.getUserDNByCertAttributes.");
		} finally {
			if (context != null) {
				try {
					//close is idempotent, so ok to close if already closed
					context.close();
				} catch (NamingException e) {
					//Log
				}
			}
		}

		if (queryResults.size() != 1) {
			throw new Exception("User LDAP query using certificate attributes did not return exactly one result.");
		} else {
			userDN = queryResults.get(0);
		}

		return userDN;
	}
	
	public boolean addUserCertificateMapping(String userID, String serialNumber, String issuerDN) throws Exception {
		
		boolean result = true;
		
		//Validate arguments
		if (userID == null || userID.trim().length() == 0) {
			throw new Exception("Missing userID in UserManager.addUserCertificateMapping.");
		}
		
		if (!isSerialNumberValid(serialNumber)) {
			throw new Exception("Invalid serialNumber argument to UserManager.addUserCertificateMapping.");			
		}
		
		if (!isIssuerDNValid(issuerDN)) {
			throw new Exception("Invalid issuerDN argument to UserManager.addUserCertificateMapping.");
		}

		DirContext context = null;
		
		try {
			//Need to add some exception handlers here for pooling
			context = getContext();
			
			NameParser nameParser = context.getNameParser(connectionUrl);
			Name dn = nameParser.parse("CN=" + userID + "," + userBaseRDN);

			Attributes attributes = new BasicAttributes();
			Attribute attribute = new BasicAttribute(certificateMappingAttribute);
			attribute.add(constructCertificateMappingValue(serialNumber, issuerDN));
			attributes.put(attribute);
			
			context.modifyAttributes(dn, DirContext.ADD_ATTRIBUTE, attributes);
		} catch (AttributeInUseException e1) {
			//The attribute name-value pair already exists; we'll just log this
			//exception for the time being so that this method is idempotent
		} catch (NameNotFoundException e2) {
			throw new Exception("Could not locate userID entry in LDAP to update certificate mapping.");
		} catch (NamingException e4) {
			throw new Exception("Unexpected naming exception in UserManager.updateUserCertificateMapping.");
		} finally {
			if (context != null) {
				try {
					//close is idempotent, so ok to close if already closed
					context.close();
				} catch (NamingException e) {
					//Log
				}
			}			
		}
		
		return result;
	}

	public boolean removeUserCertificateMapping(String userID, String serialNumber, String issuerDN) throws Exception {
		
		boolean result = true;
		
		//Validate arguments
		if (userID == null || userID.trim().length() == 0) {
			throw new Exception("Missing userID in UserManager.removeUserCertificateMapping.");
		}
		
		if (!isSerialNumberValid(serialNumber)) {
			throw new Exception("Invalid serialNumber argument to UserManager.removeUserCertificateMapping.");			
		}
		
		if (!isIssuerDNValid(issuerDN)) {
			throw new Exception("Invalid issuerDN argument to UserManager.removeUserCertificateMapping.");
		}

		DirContext context = null;
		
		try {
			context = getContext();
			
			NameParser nameParser = context.getNameParser(connectionUrl);
			Name dn = nameParser.parse("CN=" + userID + "," + userBaseRDN);

			Attributes attributes = new BasicAttributes();
			Attribute attribute = new BasicAttribute(certificateMappingAttribute);
			attribute.add(constructCertificateMappingValue(serialNumber, issuerDN));
			attributes.put(attribute);
			
			context.modifyAttributes(dn, DirContext.REMOVE_ATTRIBUTE, attributes);
		} catch (NoSuchAttributeException e1) {
			//Provided attribute (name-value pair) could not be found; we
			//can treat this idempotently for now
		} catch (NameNotFoundException e2) {
			throw new Exception("Could not locate userID entry in LDAP to remove certificate mapping.");
		} catch (NamingException e4) {
			throw new Exception("Unexpected naming exception in UserManager.removeUserCertificateMapping.");
		} finally {
			if (context != null) {
				try {
					//close is idempotent, so ok to close if already closed
					context.close();
				} catch (NamingException e) {
					//Log
				}
			}			
		}
		
		return result;
	}

	private DirContext getContext() throws NamingException {
		
    	DirContext result = null;
    	
		Hashtable<String, Object> properties = new Hashtable<String, Object>();
    	
    	properties.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
    	properties.put(Context.PROVIDER_URL, connectionUrl);
        properties.put(Context.SECURITY_AUTHENTICATION, ldapSecurityType);
        properties.put(Context.SECURITY_PRINCIPAL, serviceAccountDN);
        properties.put(Context.SECURITY_CREDENTIALS, serviceAccountCredential);
        
        //Request connection pooling.
        //This should work in current TAK installations, but should be revisited in
        //future if multiple synchronized ldap servers are used with some form of load balancing
        //(either simple round-robin or weighted service records and hostless (dn-based) JNDI connections)
        //Some of this can become more of an issue in clustered JVM environments 
        //with code that makes certain general types of updates where the ldap servers aren't synchronized yet.
        properties.put(CONNECTION_POOLING, "true");
        
        //Important to specify a timeout otherwise code will hang (or wait for tcp timeout) when pool is empty
        properties.put(CONNECTION_TIMEOUT, "5000");
        
        //Important connection details set at the JVM level
        //See here: http://docs.oracle.com/javase/jndi/tutorial/ldap/connect/config.html
        
		result = new InitialDirContext(properties);

		return result;

	}
	
	private boolean isSerialNumberValid(String serialNumber) {
		
		boolean result = false;
		
		//Per rfc5280, conforming CAs should ensure serial numbers are positive numbers no greater than 20 octets.
		//Non-conforming CAs may include negatives, which we'll allow
		if (serialNumber != null && serialNumber.trim().length() > 0) {
			if (serialNumber.replaceFirst("^(-?[0-9]{1,49})", "").length() == 0) {
				result = true;
			}
		}

		return result;
	}
	
	private boolean isIssuerDNValid(String issuerDN) {
		
		boolean result = false;

		if (issuerDN != null && issuerDN.trim().length() > 0) {
			//DNs may contain practically any characters, though some have to be escaped *within* each component
			//*as the DN is built up* so they don't conflict with separator characters. 
			//This can't be done here reliably. The search itself below is protected of JNDI and LDAP injection
			//attacks by way of using parameterized filters with the DirContext search method.
			//To escape components before calling this method (if the DN is being created by the application),
			//see https://www.owasp.org/index.php/Preventing_LDAP_Injection_in_Java. Bear in mind that the DN is being
			//used to search a string field (serialNumber) that is not itself a DN attribute.
			result = true;
		}

		return result;
	}
		
	/**
	 * Constructs the value that tracks a certificate mapping for a user.
	 * 
	 * NOTE: Make sure you validate inputs upstream of this call.
	 * 
	 * @param serialNumber String serialNumber of certificate
	 * @param issuerDN String DN of certificate's issuing CA
	 * @return
	 */
	private String constructCertificateMappingValue(String serialNumber, String issuerDN) {
		
    	return "SN=" + serialNumber + "," + "IDN=" + issuerDN;
    	
	}
	
	//THESE WILL BE REMOVED AND RETRIEVED MOSTLY FROM CONFIG
	private static final String certificateMappingAttribute = "serialNumber";
	private static final String serviceAccountDN = "cn=fred001,cn=Users,cn=Partition1,dc=XYZ,dc=COM";
	private static final String serviceAccountCredential = "xxxx";
	private static final String ldapSecurityType = "simple";
	private static final String connectionUrl = "ldap://ldap_host:20001/CN=Partition1,dc=XYZ,dc=COM";
	private static final String userFilter = "(&(objectclass={0})(" + certificateMappingAttribute + "={1}))";
	private static final String userClass = "user";
	private static final String userBaseRDN = "CN=Users";

	private static final String CONNECTION_POOLING = "com.sun.jndi.ldap.connect.pool";
	private static final String CONNECTION_TIMEOUT = "com.sun.jndi.ldap.connect.timeout";
    private static final String DN_ATTR_NM = "distinguishedName";

}
