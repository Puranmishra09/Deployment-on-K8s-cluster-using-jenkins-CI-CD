FROM rockylinux:8
LABEL maintainer="puranmishra2024@gmail.com"
# Update repositories and install Apache
RUN dnf -y update && \
    dnf -y install httpd zip unzip curl

# Download and extract static site
ADD https://github.com/Krishnamohan-Yerrabilli/static-site/archive/refs/heads/main.zip /var/www/html/
WORKDIR /var/www/html/

RUN unzip main.zip && \
    cp -rvf static-site-main/* . && \
    rm -rf static-site-main main.zip

# Expose ports and start Apache
EXPOSE 80 22
CMD ["/usr/sbin/httpd", "-D", "FOREGROUND"]
