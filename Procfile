web: java -Dserver.port=$PORT -jar target/BottleCapCollector-0.0.1-SNAPSHOT.jar
release: ./mvnw -Dliquibase.changeLogFile=db/changelog/changelog-master.xml
-Dliquibase.url=$SPRING_DATASOURCE_URL
-Dliquibase.promptOnNonLocalDatabase=false
-Dliquibase.password=$SPRING_DATASOURCE_PASSWORD liquibase:update
