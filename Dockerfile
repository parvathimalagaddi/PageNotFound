FROM maven

COPY ./QAForum ./

RUN mvn clean package 

CMD ["java","-jar","./target/QAForum-fat.jar"]
