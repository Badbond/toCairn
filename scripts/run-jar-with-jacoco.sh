# Script for running the created jar of a test-project using a jacoco agent.
mvn clean package
java -javaagent:./jacocoagent.jar=includes=me.soels.*,destfile=target/jacoco.exec -jar target/test-project-1.0-SNAPSHOT.jar 
