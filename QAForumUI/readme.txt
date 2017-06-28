build: mvn clean install
database: mongo on default port with cmad as database and books as collection with isbn and title as keys
run: java -jar target/cmad-advanced-staging-demo-fat.jar -cluster
homepage: http://localhost:8080/static/index.html