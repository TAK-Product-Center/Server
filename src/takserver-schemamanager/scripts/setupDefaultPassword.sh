curPassword=""
if [ -f "/opt/tak/CoreConfig.xml" ]; then
  curPassword=$(echo $(grep -m 1 "<connection" /opt/tak/CoreConfig.xml)  | sed 's/.*password="//; s/".*//')
else 
  curPassword=$(echo $(grep -m 1 "<connection" /opt/tak/CoreConfig.example.xml)  | sed 's/.*password="//; s/".*//')
  if [ -z "$curPassword" ]; then
  	curPassword=$(openssl rand -base64 12 | tr -dc 'a-zA-Z0-9')
  fi
fi

sed -i "s/password=\"\"/password=\"$curPassword\"/g" /opt/tak/CoreConfig.example.xml