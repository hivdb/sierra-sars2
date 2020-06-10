FROM hivdb/tomcat-with-nucamino:latest
ADD build/libs/Sierra-SARS2.war /usr/share/tomcat/webapps
