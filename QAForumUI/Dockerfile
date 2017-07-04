FROM maven

COPY ./target/QAForum-fat.jar ./QAForum-fat.jar 

CMD ["java","-jar","./QAForum-fat.jar","-DMONGOHOST=mongodb-cmad-server-1"]
