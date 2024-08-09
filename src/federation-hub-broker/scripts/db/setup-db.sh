setup_mongo () {
  username=''
  password=''

  # try to get username from /opt/tak/CoreConfig.xml
  if [ -f "/opt/tak/federation-hub/configs/federation-hub-broker.yml" ]; then
    username=$(echo $(grep -m 1 "dbUsername:" /opt/tak/federation-hub/configs/federation-hub-broker.yml) | sed 's/.*dbUsername: *//' | sed 's/"//g')
  fi

  # cant find username - use default
  if [ -z "$username" ]; then
    username='martiuser'
    sed -i "/dbUsername:/d" /opt/tak/federation-hub/configs/federation-hub-broker.yml
    echo "" >> /opt/tak/federation-hub/configs/federation-hub-broker.yml
    echo "dbUsername: $username" >> /opt/tak/federation-hub/configs/federation-hub-broker.yml
  fi

  # try to get password from /opt/tak/CoreConfig.xml
  if [ -f "/opt/tak/federation-hub/configs/federation-hub-broker.yml" ]; then
    password=$(echo $(grep -m 1 "dbPassword:" /opt/tak/federation-hub/configs/federation-hub-broker.yml) | sed 's/.*dbPassword: *//' | sed 's/"//g')
  fi

  # cant find password - generate one
  if [ -z "$password" ]; then
    password=$(openssl rand -base64 12 | tr -dc 'a-zA-Z0-9')
    sed -i "/dbPassword:/d" /opt/tak/federation-hub/configs/federation-hub-broker.yml
    echo "" >> /opt/tak/federation-hub/configs/federation-hub-broker.yml
    echo "dbPassword: $password" >> /opt/tak/federation-hub/configs/federation-hub-broker.yml
  fi

  echo "db.createUser({user: '$username', pwd: '$password', roles: [ { role: 'root', db: 'admin' } ]})" > /opt/tak/federation-hub/scripts/db/create_user.js
  echo "Creating the following admin user within mongo:"
  cat "/opt/tak/federation-hub/scripts/db/create_user.js"
  sleep 1
  $(echo "mongosh admin --file /opt/tak/federation-hub/scripts/db/create_user.js")
  sleep 1
}

setup_mongo
