FROM docker:24.0.2

ARG EKSCTL_VERSION=v0.212.0
ARG AWS_CLI_VERSION=2.7.11
ARG K8S_VERSION=v1.32.2
ARG HELM_VERSION=v3.17.1

RUN apk add --no-cache unzip python3 py3-pip curl bash wget vim aws-cli mandoc git tmux py3-boto3 py3-yaml
RUN python3 -m pip install pyyaml boto3

# Install kubectl
RUN wget https://dl.k8s.io/release/${K8S_VERSION}/bin/linux/amd64/kubectl -O /usr/local/bin/kubectl && \
	chmod +x /usr/local/bin/kubectl

# Install helm
RUN wget https://get.helm.sh/helm-${HELM_VERSION}-linux-amd64.tar.gz -O /tmp/helm.tar.gz && \
	mkdir /tmp/helm-extracted && tar -xzvf /tmp/helm.tar.gz -C /tmp/helm-extracted && \
	mv /tmp/helm-extracted/linux-amd64/helm /usr/local/bin/helm && \
	chmod +x /usr/local/bin/helm && \
	rm -r /tmp/helm.tar.gz /tmp/helm-extracted

# Install eksctl
RUN curl --silent --location "https://github.com/weaveworks/eksctl/releases/download/${EKSCTL_VERSION}/eksctl_Linux_amd64.tar.gz" | tar xz -C /tmp && mv /tmp/eksctl /usr/local/bin/eksctl && chmod +x /usr/local/bin/eksctl

RUN /usr/local/bin/helm plugin install https://github.com/databus23/helm-diff

COPY docker_entrypoint.sh /
COPY collect-cluster-logs.py /
CMD bash /docker_entrypoint.sh
