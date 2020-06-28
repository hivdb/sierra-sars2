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

FROM hivdb/tomcat-with-nucamino:latest
COPY --from=builder /sierra/build/libs/Sierra-SARS2.war /usr/share/tomcat/webapps
