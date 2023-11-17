

package com.bbn.marti.groups;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.naming.CommunicationException;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.LdapContext;
import javax.naming.ldap.LdapReferralException;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.marti.config.Auth;
import com.bbn.marti.config.LdapStyle;
import com.bbn.marti.remote.exception.UnauthorizedException;
import com.bbn.marti.remote.groups.AuthCallback;
import com.bbn.marti.remote.groups.AuthResult;
import com.bbn.marti.remote.groups.AuthStatus;
import com.bbn.marti.remote.groups.AuthenticatedUser;
import com.bbn.marti.remote.groups.Direction;
import com.bbn.marti.remote.groups.Group;
import com.bbn.marti.remote.groups.GroupManager;
import com.bbn.marti.remote.groups.User;
import com.bbn.marti.remote.util.GroupNameExtractor;
import com.bbn.marti.service.DistributedConfiguration;
import com.bbn.marti.util.spring.SpringContextBeanForApi;
import com.bbn.marti.xml.bindings.UserAuthenticationFile;
import tak.server.cache.ActiveGroupCacheHelper;

public class LdapAuthenticator extends AbstractAuthenticator implements Serializable {

    private static final long serialVersionUID = 6981704620373324897L;

    private final String ldapUrl;
    private final LdapStyle style;
    private final String userString;
    private final String groupPrefix;
    private final String readOnlyGroup;
    private final String readGroupSuffix;
    private final String writeGroupSuffix;

    private Auth.Ldap conf;
    
    public Auth.Ldap getConf() {
        return conf;
    }

    // attributes to apply to the user object, which will return only attributes indicating group membership.
    private final String[] groupUserAttrs = {"memberOf", "ntUserWorkstations"};
    private final String[] distinguishedNameAttr = {"distinguishedName"};

    private final boolean debug = false;

    private static final Logger logger = LoggerFactory.getLogger(LdapAuthenticator.class);
    
    private static LdapAuthenticator instance;

    ActiveGroupCacheHelper activeGroupCacheHelper;
    
    public static synchronized LdapAuthenticator getInstance(Auth.Ldap ldapConfig, GroupManager groupManager) {
        if (instance == null) {
            try {
                instance = new LdapAuthenticator(
                        SpringContextBeanForApi.getSpringContext().getBean(GroupManager.class),
                        SpringContextBeanForApi.getSpringContext().getBean(ActiveGroupCacheHelper.class));
            } catch (Exception e) {
               throw new RuntimeException(e);
            }
        }
        
        return instance;
    }
    
    public static synchronized LdapAuthenticator getInstance() {
        if (instance == null) {
            throw new IllegalStateException("LdapAuthenticator not available");
        }
        
        return instance;
    }

    public LdapAuthenticator(GroupManager groupManager, ActiveGroupCacheHelper activeGroupCacheHelper) throws NamingException, RemoteException {
        
    	conf = DistributedConfiguration.getInstance().getAuth().getLdap();
    	
    	if (groupManager == null) {
            throw new IllegalArgumentException("null groupManager");
        }

        this.groupManager = groupManager;
        this.activeGroupCacheHelper = activeGroupCacheHelper;

        if (conf == null) {
            throw new IllegalArgumentException("null ldap configuration");
        }

        // register with groupManager so that it can be accessed over RMI. Only one LDAP authenticator will be registered, due to the singleton LdapAuthenticator property.
        groupManager.registerAuthenticator("ldap", this);

        ldapUrl = conf.getUrl();
        userString = conf.getUserstring();

        groupPrefix = conf.getGroupprefix().toLowerCase();
        readOnlyGroup = conf.getReadOnlyGroup();
        readGroupSuffix = conf.getReadGroupSuffix();
        writeGroupSuffix = conf.getWriteGroupSuffix();

        Integer updateInterval = conf.getUpdateinterval();
        if (updateInterval != null) {
            setUpdateIntervalSeconds(updateInterval);
        }

        if (logger.isDebugEnabled()) {
        	logger.debug("LDAP group prefix: \"" + groupPrefix + "\"");
        }

        if (ldapUrl == null || ldapUrl.length() == 0 || 
                userString == null || userString.length() == 0) {
            throw new IllegalArgumentException("invalid ldapUrl or userstring");
        }

        style = conf.getStyle();
    }

    @Override
    public void authenticate(@NotNull User user, @NotNull AuthCallback cb) {
        
        if (user == null) {
            throw new IllegalArgumentException("null user");
        }
        
        if (cb == null) {
            throw new IllegalArgumentException("null callback");
        }
 
        long rt = System.currentTimeMillis();

        // do ldap authentication, update the AuthResult, and call the callback
        try {
            
            authAndAssignGroups((AuthenticatedUser) user);
            assignAuthority(user);
       
        } catch (CommunicationException e) {
            logger.error("LDAP Authentication Failed: Unable to reach LDAP server: " + ldapUrl);
            cb.authenticationReturned(user, AuthStatus.FAILURE);
            logRunningTime(rt);
            return;
        } catch (NamingException e) {
            logger.info("LDAP Authentication Failed: LDAP authentication unsuccessful: " + e.getMessage() + " for user: " + user.getId());
            cb.authenticationReturned(user, AuthStatus.FAILURE);
            logRunningTime(rt);
            return;
        } catch (Exception e) {
            logger.error("LDAP Authentication Failed: LDAP authentication unsuccessful: " + e.getMessage());
            cb.authenticationReturned(user, AuthStatus.FAILURE);
            logRunningTime(rt);
            return;
        }
       
        if (logger.isDebugEnabled()) {          	
        	logger.debug("LDAP Authentication successful for: " + user.getId());
        }
        
        cb.authenticationReturned(user, AuthStatus.SUCCESS);
        logRunningTime(rt);
    }

    private void logRunningTime(long rt) {
        rt = System.currentTimeMillis() - rt;
        if (logger.isDebugEnabled()) {          	
        	logger.debug("LDAP auth running time: " + rt + " ms");
        }
    }

    public static Set<String> applyGroupPrefixFilter(Set<String> groupNames, String prefix) {

        Set<String> filtered = new ConcurrentSkipListSet<>();

        //
        // apply the groupPrefix filter
        //
        for (String groupName : groupNames) {
            // move the group name on for further processing if it matches. an empty string prefix matches everything.
            if (groupName.toLowerCase(Locale.ENGLISH).startsWith(prefix)) {
                filtered.add(groupName);
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug("skipping non prefix-matching group " + groupName);
                }
            }
        }

        return filtered;
    }


    public Set<String> getGroupNamesFromSearchResults(Map<String, String> groupInfo) {
        Set<String> groupNames = new ConcurrentSkipListSet<>();

        for (Map.Entry<String, String> info : groupInfo.entrySet()) {

            try {
                // TODO: special case for ntUserWorkstations. Parse this and use it as group names
                if (!Strings.isNullOrEmpty(info.getKey()) && info.getKey().startsWith("ntUserWorkstations")) {

                    for (String groupName : Splitter.on(',').split(info.getValue())) {

                        groupName = groupName.trim();

                        if (logger.isDebugEnabled()) {

                            logger.debug("processing ntUserWorkstation group " + groupName);
                        }

                        groupNames.add(groupName);
                    }

                    continue;
                }
            } catch (Exception e) {
                if (logger.isDebugEnabled()) {
                    logger.debug("exception processing ntuserWorkstations groups");
                }
            }

            if (Strings.isNullOrEmpty(info.getValue())) {
                if (logger.isDebugEnabled()) {
                    logger.debug("empty group name - skipping");
                }
                continue;
            }

            String groupName = info.getValue();
            groupNames.add(groupName);
        }

        Set<String> filtered = applyGroupPrefixFilter(groupNames, groupPrefix);

        //
        // apply the groupNameExtractorRegex to the results of the groupPrefix filter
        //
        final String regex = getConf().getGroupNameExtractorRegex();
        if (regex != null && regex.length() > 0) {
            Set<String> tmp = new ConcurrentSkipListSet<>();

            // iterate across the filtered groupNames
            for (String groupName : filtered) {
                try {
                    // extract the CN from the full group name. if there is no match, groupName will be unchanged.
                    groupName = applyGroupNameExtractorRegex(groupName);
                } catch (Exception e) {
                    logger.error("exception extracting group name from DN!");
                }
                // build up a list of regex results
                tmp.add(groupName);
            }
            // swap out groupNames for the regex filtered list
            filtered = tmp;
        }

        return filtered;
    }

    public boolean assignGroups(Map<String, String> groupInfo, User user) {
        if (groupInfo == null) {
            throw new IllegalArgumentException("null group list");
        }

        // keep track of this user even if there are no groups
        groupManager.addUser(user);
        
        if (groupInfo.isEmpty()) {
            logger.warn(user + " is not a member of any group");
        }

        // extract the set of group names from the ldap search results
        Set<String> groupNames = getGroupNamesFromSearchResults(groupInfo);

        if (logger.isTraceEnabled()) {
        	logger.trace("processing group updates for " + user + " for these groups " + groupNames);
        }

        return processGroupUpdates(user, groupNames);
    }

    // Connect and bind to the LDAP server as the specified user, returning a DirContext for this LDAP connection
    private DirContext connect(String bindDn, String bindPassword) throws NamingException {

        if (Strings.isNullOrEmpty(bindDn) || Strings.isNullOrEmpty(bindPassword)) {
            throw new IllegalArgumentException("emtpy bindDn or bindPassword");
        }

        Hashtable<String, Object> env = getEnv();

        env.put(Context.SECURITY_PRINCIPAL, bindDn);
        env.put(Context.SECURITY_CREDENTIALS, bindPassword);

        return new InitialDirContext(env);
    }
    
    // Connect and bind to the LDAP server as the service user, returning a DirContext for this LDAP connection 
    private DirContext connectServiceAccount() throws NamingException {

        if (Strings.isNullOrEmpty(conf.getServiceAccountDN()) || Strings.isNullOrEmpty(conf.getServiceAccountCredential())) {
            throw new IllegalArgumentException("emtpy bindDn or bindPassword");
        }
        
        Hashtable<String, Object> env = getEnv();

        env.put(Context.SECURITY_PRINCIPAL, conf.getServiceAccountDN());
        env.put(Context.SECURITY_CREDENTIALS, conf.getServiceAccountCredential());

        return new InitialDirContext(env);
    }

    // Get a new environment Hashtable. This triggers the Java JNDI LDAP api to make a new connection to the server.
    private Hashtable<String, Object> getEnv() {

        Hashtable<String, Object> env = new Hashtable<>();

        // setup the LDAP connection environment
        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.REFERRAL, "throw"); // for AD compatibility
        env.put(Context.PROVIDER_URL, ldapUrl);

        if (conf.isEnableConnectionPool()) {
            env.put("com.sun.jndi.ldap.connect.pool", "true");
            env.put("com.sun.jndi.ldap.connect.timeout", conf.getConnectionPoolTimeout());
        }

        if (logger.isDebugEnabled()) {
        	logger.debug("ldapUrl: " + ldapUrl);
        }

        if (ldapUrl.toLowerCase().contains("ldaps")) {
            env.put(Context.SECURITY_PROTOCOL,"ssl");
            env.put("java.naming.ldap.factory.socket", "com.bbn.marti.groups.LdapSSLSocketFactory");
        }

        // debug ldap info to stderr
        if (debug) {
            env.put("com.sun.jndi.ldap.trace.ber", System.err);
        }

        return env;
    }

    // traverses up the group hierarchy and returns the list of nested group names
    public List<String> findNestedGroups(
            List<String> nestedGroups, DirContext context, String filter, String groupName,
            SearchControls constraints, List<String> parents, Set<String> processed) {

        try {
            processed.add(groupName);

            NamingEnumeration results = context.search(conf.getGroupBaseRDN(), filter, constraints);
            if (results == null || !results.hasMore()) {
                if (parents.isEmpty()) {
                    return nestedGroups;
                } else {
                    parents.remove(groupName);
                }
            }

            while (results.hasMore()) {
                SearchResult result = (SearchResult) results.next();
                parents.add(result.getNameInNamespace());
                nestedGroups.add(result.getNameInNamespace());
            }

            Iterator<String> it = parents.iterator();
            while (it.hasNext()) {
                String groupDn = it.next();
                String nextfilter = "(&(member=" + groupDn + ")(objectClass=groupOfNames))";
                parents.remove(groupDn);
                if (!processed.contains(groupDn)) {
                    findNestedGroups(nestedGroups, context, nextfilter, groupDn, constraints, parents, processed);
                }
            }

        } catch (Exception e) {
            logger.error("exception in findNestedGroups", e);
        }

        return nestedGroups;
    }

    private Map<String, String> getNestedGroupInfoByDN(DirContext ctx, String userBindDn) {
        Map<String, String> groupAttrs = new ConcurrentHashMap<>();

        Set<String> processed = new ConcurrentSkipListSet<>();
        List<String> parents = new CopyOnWriteArrayList<String>();
        List<String> nestedGroups = new CopyOnWriteArrayList<String>();

        SearchControls constraints = new SearchControls();
        constraints.setSearchScope(SearchControls.SUBTREE_SCOPE);

        String filter = "(&(member=" + userBindDn + ")(objectClass=groupOfNames))";
        nestedGroups = findNestedGroups(nestedGroups, ctx, filter, userBindDn, constraints, parents, processed);

        for (String groupName : nestedGroups) {
            groupAttrs.put("memberOf" + groupAttrs.size(), groupName);
        }

        return groupAttrs;
    }

    private Map<String, String> getGroupInfoByDN(DirContext ctx, String userBindDn) {

        if (getConf().isNestedGroupLookup()) {
            return getNestedGroupInfoByDN(ctx, userBindDn);
        }

        Map<String, String> groupAttrs = new ConcurrentHashMap<>();
        NamingEnumeration<?> results = null;
        LdapContext c = null;

        try {
            c = (LdapContext) ctx.lookup(userBindDn);
            SearchControls controls = getGroupSearchControls(false);
            controls.setSearchScope(SearchControls.OBJECT_SCOPE);
            results = c.search("", "(objectClass=*)", controls);

            // process ldap search results
            getGroupAttrs(results, groupAttrs);
        } catch (LdapReferralException e) { 
        	if (logger.isDebugEnabled()) {
        		logger.debug("ignoring LDAP referral");
        	}
        } catch (NamingException e) {
        	if (logger.isDebugEnabled()) {
        		logger.debug("exception getting user info", e);
        	}
        } finally {
            if(results != null) { try { results.close(); } catch(NamingException ne) {} }
            if(c != null) { try { c.close(); } catch(NamingException ne) {} }
        }

        return groupAttrs;
    }

    public Map<String, String> getGroupInfoBySearch(DirContext ctx, String searchFilter, boolean chain) {
    	
    	if (logger.isDebugEnabled()) {    		
    		logger.debug("ldap searchFilter: " + searchFilter);
    	}

        Map<String, String> groupAttrs = new ConcurrentHashMap<>();
        NamingEnumeration<?> results = null;

        try {
            SearchControls controls = getGroupSearchControls(chain);
            controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
            results = ctx.search("", searchFilter, controls);

            if (chain) {
                // process ldap search results
                getGroupChainAttrs(results, groupAttrs);
            } else {
                // process ldap search results
                getGroupAttrs(results, groupAttrs);
            }

        } catch (LdapReferralException e) { 
        	if (logger.isDebugEnabled()) {
        		logger.debug("ignoring LDAP referral");
        	}
        } catch (NamingException e) {
        	if (logger.isDebugEnabled()) {
        		logger.debug("exception getting user info", e);
        	}
        } finally {
            if(results != null) { try { results.close(); } catch(NamingException ne) {} }
        }

        return groupAttrs;
    }

    public Map<String, String> getGroupInfoBySearch(DirContext ctx, String userId) throws NamingException {

        String searchFilter;
        if (!conf.isMatchGroupInChain()) {
        	if (logger.isDebugEnabled())  {
        		logger.debug("getting user info using AD approach");
        	}
            // For AD, use the sAMAccountName
            searchFilter = "(sAMAccountName=" + userId + ")";
        } else {
        	if (logger.isDebugEnabled()) {
        		logger.debug("getting user info using AD chain approach");
        	}
            String distinguishedName = getDistinguishedName(ctx, userId);
            searchFilter = "(member:1.2.840.113556.1.4.1941:=" + distinguishedName + ")";
        }
        
        if (logger.isDebugEnabled()) {
        	logger.debug("connected to ldap: " + ctx);        	
        }

        return getGroupInfoBySearch(ctx, searchFilter, conf.isMatchGroupInChain());
    }

    public Date getPasswordExpiration(DirContext ctx, String userId) {

        NamingEnumeration<?> results = null;
        Map<String, String> attributeMap = new ConcurrentHashMap<>();

        try {
            // get the date the password was last set, and check to see if the password expires
            String[] userAttrList = { "pwdLastSet", "userAccountControl" };
            SearchControls userControls = new SearchControls();
            userControls.setReturningAttributes(userAttrList);
            userControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
            results = ctx.search("", "(sAMAccountName=" + userId + ")", userControls);
            getGroupAttrs(results, attributeMap);
            results.close();

            // check the password expiration bit
            int userAccountControl = Integer.parseInt(attributeMap.get("userAccountControl0"));
            int UF_DONT_EXPIRE_PASSWD = 0x10000;
            boolean expires = (userAccountControl & UF_DONT_EXPIRE_PASSWD) == 0;
            if (!expires) {
                return null;
            }

            // convert Windows FILETIME to unix timestamp
            long pwdLastSet = Long.parseLong(attributeMap.get("pwdLastSet0"));
            Date datePwdLastSet = new Date((pwdLastSet/10000) - 11644473600000L);

            // get the mas password age, so we can compute the password expiration date
            String[] domainAttrList = { "maxPwdAge" };
            SearchControls domainControls = new SearchControls();
            domainControls.setReturningAttributes(domainAttrList);
            domainControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
            results = ctx.search("", "(objectClass=domain)", domainControls);
            getGroupAttrs(results, attributeMap);
            results.close();

            long maxPwdAge = Long.parseLong(attributeMap.get("maxPwdAge0"));
            Date datePwdExpries =new Date(datePwdLastSet.getTime() + (maxPwdAge / -10000));
            return datePwdExpries;

        } catch (Exception e) {
            logger.error("exception in getPasswordExpiration!", e);
        } finally {
            if(results != null) { try { results.close(); } catch(NamingException ne) {} }
        }

        return null;
    }

    public Map<String, String> getGroupInfoBySearch(String userId) throws NamingException {

    	Map<String, String> result = null;

    	DirContext ctx = null;

        if (getConf().isLoginWithEmail()) {
            String email = userId;
            userId = getUsernameByEmail(email);
            if (userId == null) {
                throw new IllegalArgumentException("getUsernameByEmail lookup failed for : " + email);
            }
        }

    	try {
    		ctx = connectServiceAccount();

            switch(getConf().getStyle()) {
                case DS: {
                    String bindDn = userString.replace("{username}", userId);
                    result = getGroupInfoByDN(ctx, bindDn);
                    break;
                }
                case AD: {
    		        result = getGroupInfoBySearch(ctx, userId);
                    break;
                }
            }

    	} catch (Exception e) {
    		if (logger.isDebugEnabled()) {
    			logger.debug("exception getting group info using service account", e);
    		}
    	} finally {
    		if (ctx != null) {
    			try {
    				ctx.close();
    			} finally { }
    		}
    	}

    	return result != null ? result : new ConcurrentHashMap<>();
    }

    private String getDistinguishedName(DirContext ctx, String userId) throws NamingException {

        NamingEnumeration<?> results = null;
        Map<String, String> groupAttrs = new ConcurrentHashMap<>();
        String distinguishedName = null;

        try {

            SearchControls controls = new SearchControls();
            controls.setReturningAttributes(distinguishedNameAttr);

            controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
            results = ctx.search("", "(sAMAccountName=" + userId + ")", controls);

            getGroupAttrs(results, groupAttrs);
            distinguishedName = groupAttrs.get("distinguishedName0");

        } finally {
            if(results != null) { try { results.close(); } catch(NamingException ne) {} }
        }

        return distinguishedName;
    }

    private String getUsernameByEmail(String email) throws NamingException {

        DirContext ctx = null;
        NamingEnumeration<?> results = null;

        try {
            // connect to ldap using configured service credentials
            ctx = groupManager.connectLdap();

            SearchControls controls = new SearchControls();
            String[] logonNameAttr = { "sAMAccountName" };
            controls.setReturningAttributes(logonNameAttr);

            controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
            String filter = "(mail=" + email + ")";
            results = ctx.search("", filter, controls);

            Map<String, String> groupAttrs = new ConcurrentHashMap<>();
            getGroupAttrs(results, groupAttrs);
            return groupAttrs.get("sAMAccountName0");

        } finally {
            if(results != null) {
                try {
                    results.close();
                } catch(NamingException e) {
                    logger.error("Unexpected error releasing search results.", e);
                }
            }
            if (ctx != null) { try {
                    ctx.close();
                } catch (NamingException e) {
                    logger.error("Unexpected error releasing directory context.", e);
                }
            }
        }
    }

    private SearchControls getGroupSearchControls(boolean chain) {

        SearchControls controls = new SearchControls();
        controls.setReturningAttributes(chain ? distinguishedNameAttr : groupUserAttrs);
        return controls;
    }

    public String applyGroupNameExtractorRegex(String groupName) {
        final String regex = getConf().getGroupNameExtractorRegex();
        if (regex != null && regex.length() > 0) {
            GroupNameExtractor groupNameExtractor = new GroupNameExtractor(regex);
            groupName = groupNameExtractor.extractGroupName(groupName);
        }
        return groupName;
    }

    public boolean groupNamesToGroups(Set<String> groupNames, Set<Group> groups) {
        return groupNamesToGroups(groupManager, groupNames, groups, readOnlyGroup, readGroupSuffix, writeGroupSuffix);
    }

    public static boolean groupNamesToGroups(GroupManager groupManager, Set<String> groupNames, Set<Group> groups,
                                      String readOnlyGroupName, String readSuffix, String writeSuffix) {

        //
        // check to see if the user belongs to the readOnlyGroup
        //
        boolean readOnly = false;
        if (readOnlyGroupName != null && readOnlyGroupName.length() > 0) {
            for (String groupName : groupNames) {
                if (groupName.compareTo(readOnlyGroupName) == 0) {
                    groupNames.remove(groupName);
                    readOnly = true;
                    break;
                }
            }
        }

        for (String groupName : groupNames) {

            //
            // check to see if the user is a member of an explicit read/write group, set the
            // grant flags accordingly and trim off the read/write suffix before updating the user's groups
            //
            boolean grantReadAccess = true;
            boolean grantWriteAccess = true;
            if (groupName.endsWith(readSuffix)) {
                grantWriteAccess = false;
                groupName = groupName.substring(0, groupName.indexOf(readSuffix));
            } else if (groupName.endsWith(writeSuffix)) {
                grantReadAccess = false;
                groupName = groupName.substring(0, groupName.indexOf(writeSuffix));
            }

            if (grantWriteAccess && !readOnly) {
                groups.add(groupManager.hydrateGroup(new Group(groupName, Direction.IN)));
            }

            if (grantReadAccess) {
                groups.add(groupManager.hydrateGroup(new Group(groupName, Direction.OUT)));
            }
        }

        return readOnly;
    }

    public boolean processGroupUpdates(User user, Set<String> groupNames) {

        if (user == null || groupNames == null) {
            throw new IllegalArgumentException("null user or groupNames list");
        }

        if (getConf().getFiltergroup() != null && getConf().getFiltergroup().size() > 0) {
            Set<String> filterGroups = new ConcurrentSkipListSet<>(getConf().getFiltergroup());
            Set<String> intersection = Sets.intersection(groupNames, filterGroups);
            if (intersection.size() == 0) {
                throw new UnauthorizedException();
            }
        }

        Set<Group> groups = new ConcurrentSkipListSet<>();

        boolean readOnly = groupNamesToGroups(groupNames, groups);

        // do the group updates based on this set of groups
        groupManager.updateGroups(user, groups);

        return readOnly;
    }

    private void getGroupAttrs(NamingEnumeration<?> results, Map<String, String> groupAttrs) {

        if (results == null || groupAttrs == null) {
            throw new IllegalArgumentException("null results or groupAttrs argument");
        }

        String errmsg = "exception extracting group attributes";

        try {
            while (results.hasMore()) {
                try {
                    SearchResult searchResult = (SearchResult) results.next();

                    Attributes attributes = searchResult.getAttributes();

                    NamingEnumeration<?> attrs = attributes.getAll();

                    while (attrs.hasMore()) {
                        Attribute attr = (Attribute) attrs.next();

                        int attrCount = 0;
                        NamingEnumeration<?> ne = attr.getAll(); 
			            for(;ne.hasMore();) {

                            // get the attribute name and append a couting int
                            String key = ((String) attr.getID()) + attrCount++;
                            // actually get the value of the attribute (wow!)
                            String val = (String) ne.next();

                            if (logger.isTraceEnabled()) {                            	
                            	logger.trace("group attribute value: " + val);
                            }

                            if (!key.isEmpty() && !val.isEmpty()) {
                                groupAttrs.put(key, val);
                            }
                        }
			            ne.close();
                    }

		            attrs.close();

                } catch (Exception e) {
                	if (logger.isDebugEnabled()) {
                		logger.debug(errmsg, e);
                	}
                }
            }
        } catch (LdapReferralException e) {
            // ignore continuation exceptions
        } catch (Exception e) {
        	if (logger.isDebugEnabled()) {
        		logger.debug(errmsg, e);
        	}
        }
    }

    private void getGroupChainAttrs(NamingEnumeration<?> results, Map<String, String> groupAttrs) {

        if (results == null || groupAttrs == null) {
            throw new IllegalArgumentException("null results or groupAttrs argument");
        }

        String errmsg = "exception extracting group attributes";

        int attrCount = 0;

        try {
            while (results.hasMore()) {
                try {
                    SearchResult searchResult = (SearchResult) results.next();

                    Attributes attributes = searchResult.getAttributes();

                    NamingEnumeration<?> attrs = attributes.getAll();

                    while (attrs.hasMore()) {
                        Attribute attr = (Attribute) attrs.next();

                        NamingEnumeration<?> ne = attr.getAll();
                        for(;ne.hasMore();) {
                            // get the attribute name and append a couting int
                            String key = ((String) attr.getID()) + attrCount++;
                            // actually get the value of the attribute (wow!)
                            String val = (String) ne.next();

                            if (logger.isTraceEnabled()) {                            	
                            	logger.trace("group attribute value: " + val);
                            }

                            if (!key.isEmpty() && !val.isEmpty()) {
                                groupAttrs.put(key, val);
                            }
                        }
                        ne.close();
                    }

                    attrs.close();

                } catch (Exception e) {
                	if (logger.isDebugEnabled()) {
                		logger.debug(errmsg, e);
                	}
                }
            }
        } catch (LdapReferralException e) {
            // ignore continuation exceptions
        } catch (Exception e) {
        	if (logger.isDebugEnabled()) {
        		logger.debug(errmsg, e);
        	}
        }
    }

    @Override
    public String toString() {
        return "LdapAuthenticator [ldapUrl=" + ldapUrl + ", style=" + style
                + ", userString=" + userString + ", groupPrefix=" + groupPrefix
                + ", groupManager=" + groupManager + ", groupUserAttrs="
                + Arrays.toString(groupUserAttrs) + ", debug=" + debug + "]";
    }

    @Override
    public AuthResult authenticate(User user) {
        
        if (user == null) {
            throw new IllegalArgumentException("null user in synchronous authentication");
        }

        long rt = System.currentTimeMillis();

    
        // do ldap authentication, and assign groups
        try {
            
            authAndAssignGroups((AuthenticatedUser) user);
            assignAuthority(user);
            
        } catch (CommunicationException e) {
            logger.error("Unable to reach LDAP server: " + ldapUrl);
            logRunningTime(rt);
            return new AuthResult(AuthStatus.FAILURE, user);
        } catch (NamingException e) {
        	if (logger.isDebugEnabled()) {
        		logger.debug("LDAP authentication unsuccessful: " + e.getMessage() + " search: " + user.getId());
        	}
            logRunningTime(rt);
            return new AuthResult(AuthStatus.FAILURE, user);
        } catch (Exception e) {
            logger.error("LDAP authentication unsuccessful: " + e.getMessage());
            logRunningTime(rt);
            return new AuthResult(AuthStatus.FAILURE, user);
        }
        
        logRunningTime(rt);
        return new AuthResult(AuthStatus.SUCCESS, user);
    }

    private void authAndAssignGroups(AuthenticatedUser user) throws NamingException {

        DirContext ctx = null;

        try {

            Map<String, String> groupInfo = new ConcurrentHashMap<>();

            String username = user.getId();

            if (getConf().isLoginWithEmail()) {
                username = getUsernameByEmail(user.getId());
                if (username == null) {
                    throw new IllegalArgumentException("getUsernameByEmail lookup failed for : " + user.getId());
                }
            }

            // get the userString, substituting in the username for the placeholder variable {username} from the config.
            String bindDn = userString.replace("{username}", username);

            if (logger.isDebugEnabled()) {
                logger.debug("bindDn: " + bindDn);
            }

            ctx = connect(bindDn, user.getPassword()); // connect to ldap

            // Get relevant attributes from the user object. For DS, retrieve the object located at the bindDn location in the LDAP tree. For AD, search by sAMAaccountName starting from the search base in the ldapUrl.
            try {
                switch(style) {
                    case DS: {
                    	if (logger.isDebugEnabled()) {
                    		logger.debug("getting user info using DS approach");
                    	}
                        groupInfo = getGroupInfoByDN(ctx, bindDn);
                        break;
                    }
                    case AD: {
                        groupInfo = getGroupInfoBySearch(ctx, username);
                        break;
                    }
                }

                if (logger.isDebugEnabled()) {    
                	logger.debug("bind user info: " + groupInfo);
                }
            } catch (Exception e) {
                logger.error("exception getting user info", e);
            }
            assignGroups(groupInfo, user);
        } catch (Exception e) {
            logger.warn("exception during group assignment", e);
	        throw e;
        } finally {
            try {
                if (ctx != null) {
                    ctx.close();
                }
            } catch (Exception e) {
                logger.warn("Caught exception trying to close LDAP connection: " + e.getLocalizedMessage());
            }
        }
    }
    
    private void assignAuthority(User user) {
        // If this LDAP user is in the auth file (matched by username), assign an authority based on the file entry
        for (UserAuthenticationFile.User fileUser : FileAuthenticator.getInstance().getAllUsers()) {
            if (fileUser.getIdentifier() != null && fileUser.getIdentifier().equals(user.getId())) {
                
                // if the file user has a role, set it on the user
                if (fileUser.getRole() != null) {
                	if (logger.isDebugEnabled()) {                		
                		logger.debug("assigned authority " + fileUser.getRole() + " based on file auth match by username " + user.getId());
                	}
                    user.getAuthorities().add(fileUser.getRole().toString());
                }
            }
        }
        
        if (user.getAuthorities().isEmpty()) {
            user.getAuthorities().add("ROLE_ANONYMOUS"); // default to anonymous role
        }
    }
}


