-injars UserManager-all.jar
-outjars ../UserManager.jar

-libraryjars  <java.home>/lib/rt.jar

#-libraryjars ../takcl-core/lib/spring-core-4.1.6.RELEASE.jar

#-libraryjars ../../../martiRouter/lib/annotations-12.0.jar
#-libraryjars ../../../martiRouter/lib/dom4j-1.6.1.jar
#-libraryjars ../../../martiRouter/lib/guava-18.0.jar
#-libraryjars ../../../martiRouter/lib/jackson-annotations-2.5.2.jar
#-libraryjars ../../../martiRouter/lib/logback-core-1.1.3.jar
#-libraryjars ../../../martiRouter/lib/slf4j-api-1.7.12.jar
#-libraryjars ../../../martiRouter/lib/spring-security-core-3.2.7.RELEASE.jar

#-libraryjars ../../system-test/lib/commons-io-2.4.jar
#-libraryjars ../../system-test/lib/commons-logging-1.1.3.jar
#-libraryjars ../../system-test/lib/httpclient-4.3.3.jar
#-libraryjars ../../system-test/lib/httpcore-4.3.2.jar
#-libraryjars ../../system-test/lib/junit-4.12.jar

-printmapping UserManager.map

-keepattributes Exceptions,InnerClasses,Signature,Deprecated,SourceFile,LineNumberTable,*Annotation*,EnclosingMethod

-dontobfuscate
-dontnote com.google.common.util.concurrent.MoreExecutors
-dontnote org.apache.commons.logging.**
-dontnote org.springframework.cglib.**
-dontnote org.springframework.objenesis.**
-dontnote org.springframework.core.annotation.**
-dontnote org.springframework.core.io.support.PathMatchingResourcePatternResolver
-dontnote org.springframework.security.authentication.DefaultAuthenticationEventPublisher
-dontnote org.springframework.core.annotation.**
-dontnote ch.qos.logback.core.**
-dontnote org.apache.http.**
-dontnote org.dom4j.tree.NamespaceCache
-dontnote com.google.common.cache.**
-dontnote com.google.common.base.internal.**
-dontnote org.dom4j.io.SAXContentHandler
-dontnote junit.**
-dontnote org.junit.**

-optimizations !class/marking/final,!code/simplification/arithmetic,!field,!code/allocation/variable


-keep class com.bbn.marti.config.** { *; }
-keep class com.bbn.marti.xml.bindings.** { *; }
-keep class com.bbn.marti.UserManager { *; }
-keep class com.bbn.marti.takcl.AppModules.generic.AbstractAppModule { *; }
-keep class com.bbn.marti.takcl.AppModules.generic.AbstractServerAppModule { *; }
-keep class com.bbn.marti.takcl.AppModules.OnlineFileAuthModule { *; }
-keep class com.bbn.marti.takcl.AppModules.OfflineFileAuthModule { *; }
-keep class com.bbn.marti.takcl.AppModules.generic.FileAuthModuleInterface { *; }
-keep class com.bbn.marti.takcl.BashCompletionHelper { *; }
-keep class com.bbn.marti.takcl.BooleanObject { *; }
-keep class com.bbn.marti.takcl.TAKCLCore { *; }
-keep class com.bbn.marti.takcl.Util { *; }
-keep class com.bbn.marti.remote.groups.UserManagementInterface { *; }
-keep class com.bbn.marti.xml.bindings.Role { *; }
-keep class com.bbn.marti.xml.bindings.UserAuthenticationFile { *; }
-keep class com.bbn.marti.takcl.config.** { *; }
-keep class org.joda.** { *; }

-keep class com.bbn.marti.test.shared.data.servers.ServerProfileInterface
-keep class com.bbn.marti.test.shared.data.generated.CLINonvalidatingUsers

-keep class org.apache.commons.logging.**               { *; }

-dontwarn org.springframework.**
-dontwarn org.junit.**
-dontwarn junit.**
-dontwarn org.dom4j.**
-dontwarn org.slf4j.**
-dontwarn org.apache.**
-dontwarn com.google.**
-dontwarn ch.qos.logback.**
-dontwarn org.joda.**

-keep public class com.bbn.marti.UserManager {
    public static void main(java.lang.String[]);
}
