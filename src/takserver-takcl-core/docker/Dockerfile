FROM centos:7

# Setup basic environment
RUN yum install -y epel-release
RUN yum install -y https://download.postgresql.org/pub/repos/yum/reporpms/EL-7-x86_64/pgdg-redhat-repo-latest.noarch.rpm
RUN yum update -y
RUN yum install -y sudo \
    # Install OpenSSH Server
    openssh-server openssh-clients \
    # Install takserver dependencies
	perl-PlRPC perl-version \
    java-11-openjdk-devel java-11-openjdk postgresql10 postgresql10-contrib postgresql10-server postgis30_10 postgis30_10-utils \
	unzip openssl \
	perl-Compress-Raw-Bzip2 perl-Compress-Raw-Zlib perl-DBD-Pg perl-DBI perl-Data-Dumper perl-IO-Compress perl-Net-Daemon \
    # Install Nicities
    git patch net-utils htop tmux vim

# Add the default user
RUN useradd -ms /bin/bash -d /home/jenkins -u 1004 jenkins

# Add the default user to sudoers
RUN echo "jenkins ALL=(ALL) NOPASSWD: ALL" >> /etc/sudoers


RUN mkdir /init_files
RUN sudo chown -R jenkins:jenkins /init_files
RUN sudo chmod -R 777 /init_files


# Set the docker execution context from here on
USER jenkins
ENV SHELL /bin/bash
ENV USER jenkins

# Set up for SSH access
RUN mkdir /home/jenkins/.ssh
RUN sudo mkdir /var/run/sshd
RUN sudo ssh-keygen -A
EXPOSE 22

# Copy the takserver files
#RUN mkdir /home/jenkins/rpm

COPY init-db.sh takserver-base.rpm* /init_files/
#WORKDIR /opt/tak/rpm
#RUN rpm2cpio takserver-base.rpm | cpio -idm
#RUN cp /opt/tak/rpm/opt/tak/CoreConfig.example.xml /opt/tak/rpm/opt/tak/CoreConfig.xml

# Set SSH key
ARG PUBLIC_KEY
RUN echo ${PUBLIC_KEY} > /home/jenkins/.ssh/authorized_keys

#RUN sudo chown -R jenkins:jenkins /home/jenkins/rpm
#RUN sudo chmod -R 700 /home/jenkins/rpm
#RUN sudo usermod -G staff /home/jenkins/rpm

# Copy the docker init file
COPY docker-entrypoint.sh /docker-entrypoint.sh
#COPY init-db.sh /init-db.sh

CMD ["bash", "/docker-entrypoint.sh"]
