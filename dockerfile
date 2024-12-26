FROM eclipse-temurin:21.0.5_11-jre-noble
WORKDIR /opt/demo
COPY ./build/libs .

EXPOSE 8080
CMD ["sh", "-c", "java -jar *.jar --spring.datasource.url=$DATA"]