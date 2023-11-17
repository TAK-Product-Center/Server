#!/bin/sh

if [ ! -f "/opt/tak/takserver-api.sh" ] && \
   [ ! -f "/opt/tak/takserver-messaging.sh" ]; then
   echo "Skipping war file extraction"
   exit 0
fi

cd /opt/tak
if [ -d /opt/tak/extract ]; then
  echo "Deleting current /opt/tak/extract"
  rm -fR /opt/tak/extract
fi
mkdir /opt/tak/extract

echo "Extracting /opt/tak/takserver.war into /opt/tak/extract directory"
(cd /opt/tak/extract && exec jar -xf /opt/tak/takserver.war)
chown -R tak:tak /opt/tak/extract

TAKSERVER_COMMON_JAR=$(ls extract/WEB-INF/lib/takserver-common-*.jar)
VERSION=$(echo $(basename $TAKSERVER_COMMON_JAR) | sed -En "s/takserver-common-(.+)\.jar/\1/p")

if [ -f "/opt/tak/takserver-api.sh" ]; then
    if [ ! -f "/opt/tak/takserver-api.sh.bak" ]; then
        mv /opt/tak/takserver-api.sh /opt/tak/takserver-api.sh.bak
    fi

    cat << EOF > /opt/tak/takserver-api.sh
#!/bin/sh
. ./setenv.sh

java -verbose:class -Xmx\${API_MAX_HEAP}m \\
  -XX:+CompactStrings \\
  -Dspring.profiles.active=api \\
  -Dkeystore.pkcs12.legacy \\
  -Dorg.xml.sax.parser="com.sun.org.apache.xerces.internal.parsers.SAXParser" \\
  -Djavax.xml.parsers.DocumentBuilderFactory="com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl" \\
  -Djavax.xml.parsers.SAXParserFactory="com.sun.org.apache.xerces.internal.jaxp.SAXParserFactoryImpl" \\
  -classpath "extract/WEB-INF/lib/takserver-common-${VERSION}.jar:extract/WEB-INF/lib/takserver-war-${VERSION}.jar:extract/WEB-INF/lib/takserver-fig-core-${VERSION}.jar:extract/WEB-INF/lib/takserver-plugins-${VERSION}.jar:extract/WEB-INF/lib/*:extract/WEB-INF/classes/*:extract/WEB-INF/classes/."  \\
  tak.server.ServerConfiguration $@

# To run with plugin support enable in CoreConfig and place plugin jars in /opt/tak/lib.
EOF
    echo "New takserver-api.sh written"
    chown tak:tak /opt/tak/takserver-api.sh
    chmod u+x /opt/tak/takserver-api.sh
fi

if [ -f "/opt/tak/takserver-messaging.sh" ]; then
    if [ ! -f "/opt/tak/takserver-messaging.sh.bak" ]; then
        mv /opt/tak/takserver-messaging.sh /opt/tak/takserver-messaging.sh.bak
    fi

    cat << EOF > /opt/tak/takserver-messaging.sh
#!/bin/sh
rm -rf ./tmp/
. ./setenv.sh

java -Xmx\${MESSAGING_MAX_HEAP}m \\
  -XX:+CompactStrings \\
  -Dspring.profiles.active=messaging \\
  -Dorg.xml.sax.parser="com.sun.org.apache.xerces.internal.parsers.SAXParser" \\
  -Djavax.xml.parsers.DocumentBuilderFactory="com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl" \\
  -Djavax.xml.parsers.SAXParserFactory="com.sun.org.apache.xerces.internal.jaxp.SAXParserFactoryImpl" \\
  -classpath "extract/WEB-INF/lib/takserver-common-${VERSION}.jar:extract/WEB-INF/lib/takserver-war-${VERSION}.jar:extract/WEB-INF/lib/takserver-fig-core-${VERSION}.jar:extract/WEB-INF/lib/takserver-plugins-${VERSION}.jar:extract/WEB-INF/lib/*:extract/WEB-INF/classes/*:extract/WEB-INF/classes/."  \\
  tak.server.ServerConfiguration $@

# To run with plugin support enable in CoreConfig and place plugin jars in /opt/tak/lib.
EOF
    echo "New takserver-messaging.sh written"
    chown tak:tak /opt/tak/takserver-messaging.sh
    chmod u+x /opt/tak/takserver-messaging.sh
fi
echo
echo