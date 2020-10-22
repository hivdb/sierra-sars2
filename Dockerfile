FROM hivdb/tomcat-with-nucamino:latest as builder
WORKDIR /sierra
COPY gradlew build.gradle settings.gradle /sierra/
COPY sierra-core /sierra/sierra-core
COPY asi_interpreter /sierra/asi_interpreter 
COPY gradle /sierra/gradle
RUN /sierra/gradlew dependencies
COPY src /sierra/src
RUN /sierra/gradlew assemble
RUN mv build/libs/Sierra-SARS2-*.war build/libs/Sierra-SARS2.war 2>/dev/null
ENV MINIMAP2_VERSION=2.17
RUN cd /tmp && \
    curl -sSL https://github.com/lh3/minimap2/releases/download/v2.17/minimap2-${MINIMAP2_VERSION}_x64-linux.tar.bz2 -o minimap2.tar.bz2 && \
    tar jxf minimap2.tar.bz2 && \
    mv minimap2-${MINIMAP2_VERSION}_x64-linux /usr/local/minimap2
COPY docker-payload/postalign_linux-amd64.tar.gz /tmp
RUN cd /tmp && \
    tar zxf postalign_linux-amd64.tar.gz && \
    mv postalign /usr/local/postalign


FROM hivdb/tomcat-with-nucamino:latest
COPY --from=builder /usr/local/minimap2 /usr/local/minimap2
COPY --from=builder /usr/local/postalign /usr/local/postalign
COPY --from=builder /sierra/build/libs/Sierra-SARS2.war /usr/share/tomcat/webapps
RUN cd /usr/local/bin && \
    ln -s ../minimap2/minimap2 && \
    ln -s ../minimap2/k8 && \
    ln -s ../minimap2/paftools.js && \
    echo '#! /bin/sh' > /usr/local/bin/postalign && \
    echo '/usr/local/postalign/postalign $@' >> /usr/local/bin/postalign && \
    chmod +x /usr/local/bin/postalign
