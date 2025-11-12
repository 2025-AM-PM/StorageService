FROM eclipse-temurin:21-jre
WORKDIR /app
ENV TZ=Asia/Seoul
COPY app.jar /app/app.jar
EXPOSE 6736
ENTRYPOINT ["java","-jar","/app/app.jar"]