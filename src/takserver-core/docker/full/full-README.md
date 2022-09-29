# Docker Full Image Setup

From the docker directory perform the following steps:  

1.  Build the docker container.  
	`docker build --file Dockerfile.takserver --tag takserver-full:latest ../`
2.  Modify the empty values in EDIT_ME.env.  
3.  Start the docker-compose file.  
    `docker-compose up`  
