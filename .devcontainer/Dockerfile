FROM openjdk:11-jdk-slim-bullseye

ENV DEBIAN_FRONTEND=noninteractive

RUN apt update && apt-get install -y \
    ca-certificates \
    curl \
    gnupg \
    lsb-release \
    apt-transport-https \
    zsh \
    sudo \
    git \
    make

RUN groupadd --gid 1000 vscode && \
    useradd --uid 1000 --gid 1000 -m vscode && \
    echo vscode ALL=\(root\) NOPASSWD:ALL > /etc/sudoers.d/vscode && \
    chmod 0440 /etc/sudoers.d/vscode

RUN curl -s https://raw.githubusercontent.com/k3d-io/k3d/main/install.sh | bash

RUN curl -o /tmp/k9s.tar.gz -L https://github.com/derailed/k9s/releases/download/v0.27.2/k9s_Linux_amd64.tar.gz && \
    mkdir /tmp/k9s && tar xvzf /tmp/k9s.tar.gz -C /tmp/k9s && mv /tmp/k9s/k9s /bin/k9s && rm -r /tmp/k9s && rm /tmp/k9s.tar.gz

USER vscode

RUN sh -c "$(curl -fsSL https://raw.githubusercontent.com/ohmyzsh/ohmyzsh/master/tools/install.sh)" && \
    git clone https://github.com/zsh-users/zsh-autosuggestions ${ZSH_CUSTOM:-~/.oh-my-zsh/custom}/plugins/zsh-autosuggestions && \
    git clone https://github.com/zsh-users/zsh-syntax-highlighting.git ${ZSH_CUSTOM:-~/.oh-my-zsh/custom}/plugins/zsh-syntax-highlighting && \
    sed -i -E -e 's/^plugins=.*/plugins=(git helm kubectl zsh-autosuggestions zsh-syntax-highlighting)/' ~/.zshrc && \
    echo "source <(kubectl completion zsh)" >> ~/.zshrc
