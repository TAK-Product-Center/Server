# TACKL Docker Container

The purpose of this container is to be a single-container TAKSERVER installation intended for testing.

It creates a tak user and home directory in /opt/tak and extracts the contents of the build RPM to /opt/tak/rpm, 
where they will be used to set up the database.

When used locally, I use the following command to regenerate the container from time to time, where **PUBLIC_KEY** 
references the key I would like to be able to SSH into it with:

```bash
docker build --build-arg PUBLIC_KEY="`cat ~/.ssh/keys/id_rsa.pub`" --tag takserver:latest .
```

## docker_init.sh
 
 The purpose of this file is to initialize services that are not available when constructing the docker container 
 (UNIX and network sockets specifically don't work).
 
 If the directory `/usr/pgsql-15/takdata` does not exist, it will utilize the scripts from the extracted RPM to set up 
 the database for use.
 
 It will then start the database using that directory and start up an SSH server.
