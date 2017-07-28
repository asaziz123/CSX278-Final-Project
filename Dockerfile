FROM clojure
RUN mkdir -p /usr/src/app
WORKDIR /usr/src/app
COPY target/engn-web.jar /usr/src/app/app-standalone.jar
CMD ["java", "-jar", "app-standalone.jar"]
