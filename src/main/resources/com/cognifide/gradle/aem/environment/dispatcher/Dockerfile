# This image is based on offical Adobe documentation:
# https://docs.adobe.com/content/help/en/experience-manager-dispatcher/using/dispatcher.html

FROM centos:7

LABEL maintainer="mierzwid"

# Install HTTPD and enable it
RUN yum -y install httpd mod_ssl mod_so mod_security openssl; yum clean all; systemctl enable httpd.service

# Download dispatcher module
RUN curl -s https://www.adobeaemcloud.com/content/companies/public/adobe/dispatcher/dispatcher/_jcr_content/top/download_9/file.res/dispatcher-apache2.4-linux-x86-64-ssl-4.2.3.tar.gz -O
RUN mkdir dispatcher
RUN tar -C dispatcher -zxvf dispatcher-apache2.*.tar.gz

# Copy and rename dispatcher module
RUN cp ./dispatcher/dispatcher-apache2.*.so /etc/httpd/modules/
RUN ln -s /etc/httpd/modules/dispatcher-apache2.*.so /etc/httpd/modules/mod_dispatcher.so

# Logs & cache directories
RUN mkdir -p /opt/aem/dispatcher/logs
RUN chown apache:apache /opt/aem/dispatcher/logs

# Cache directories
RUN mkdir -p /opt/aem/dispatcher/cache/content/example/demo
RUN mkdir -p /opt/aem/dispatcher/cache/content/example/live
RUN chown -R apache:apache /opt/aem/dispatcher/cache

RUN yum install -y tree

# Copy all config files to the image
COPY conf /etc/httpd/conf

# Setup invalidation script
RUN chown apache:apache /etc/httpd/conf/dispatcher/invalidation/invalidateHandler.sh
RUN chmod u+x /etc/httpd/conf/dispatcher/invalidation/invalidateHandler.sh

CMD ["-D", "FOREGROUND"]
ENTRYPOINT ["/usr/sbin/httpd"]
