FROM maven:3.8.4-openjdk-8
WORKDIR /minie
COPY . .
EXPOSE 8080
CMD ["mvn", "clean", "compile", "exec:java"]