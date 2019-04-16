# This is image used to launch Adobe AEM Dispatcher Apache 2.4 module. It lacks Dispatcher distribution which needs
# to be provided separately. its been proved to work seamlessly with the Dispatcher based on official Adobe
# documentation https://docs.adobe.com/content/help/en/experience-manager-dispatcher/using/dispatcher.html
#
# Note: Without additional setup during container start it is a simple httpd container.

FROM centos:7

LABEL maintainer="mierzwid"

# Install httpd and enable it
RUN yum -y install httpd mod_ssl mod_so mod_security openssl; yum clean all; systemctl enable httpd.service

# Cache directories
RUN mkdir -p /opt/aem/dispatcher/cache
RUN chown -R apache:apache /opt/aem/dispatcher/cache

RUN yum install -y tree

CMD ["-D", "FOREGROUND"]
ENTRYPOINT ["/usr/sbin/httpd"]
