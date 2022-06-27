# Version History

## 4.5.0

4.5.0
* Platform
  * Add support for Red Hat Enterprise Linux 8 and CentOS 8
  * Significant performance improvements
  * Fix database container restart connectivity issue in docker. See section 4.6 Docker Install
* Admin UX
  * Add new User Management UI for local file-based users.
  * Added new takserver-esapi.log for intrusion detection messages. These messages were
previously in takserver-api.log and are now written to a dedicated log file.
  * Added persistence for CoT injectors.
  * In UserManager.jar, add options -ig and -og to UserManager to dynamically specify In
groups and Out groups. Fix username / password user deletion logic in UserManager.
Update groups dynamically when changed from UserManager.
  * In UserManager.jar when changing groups for a user, instead of always adding what's
specified (group name), require the full list of group to be specified, and add / remove
internally to match. This adds support for dynamically removing groups from a user.
  * On upgrade, preserve existing clear-old-data.sqlscript
* Security
  * Updated default CRL validity period to 2 years, matching default validity of server
certificates.
  * Update tomcat to 9.0.54 to address CVE-2021-42340 Denial of Service see https://us-
cert.cisa.gov/ncas/current-activity/2021/10/15/
  * Dependencies updates, including Apache Ignite
  * Support SELinux enforcing mode
3
* Situational Awareness
  * Added optional user facing web page for submitting location reports, configurable to
broadcast or add locations to a mission
* Authentication
  * Add controls for limiting access to a set of ldap groups
  * Add option to set group name based on DN in CAC/PIV
  * Allow for concurrent WebTAK logins with same username
  * Support revocation of client certifications through enrollment, without requiring a server
restart.
* Data Management
  * Add feature to support data sync mission archive and retrieval.
* TAK Server Plugin SDK
  * Add new MessageInterceptor plugin type. Plugins of this type intercept messages after they are received by TAK Server from clients or federates, but before the messages are broadcast out to receiving clients. Plugin code can modify or enrich each message that is intercepted.
  * SDK available here: https://git.tak.gov/sdks/takserver/tak-server-sdk
* Federation
  * Add feature to block files by file-type (such as ATAK .pref files)
  * Block duplicate connections
* WebTAK
  * Features
    * added routes tool (receive-only)
    * delete feed in Data Sync
    * re-write of Data Sync tool to improve operations
    * better recognition and notification of lost server connection
    * ability to reconnect with server without a page refresh
  * Bugfixes
    * disabled password autofill on text inputs
  * Security Patches
    * n/a



