# Script for generating a report based on a jacoco.exec created by the run-jar-with-jacoco.sh
java -jar ./jacococli.jar report ./target/jacoco.exec --classfiles ./target/classes --sourcefiles ./src/main/java/ --html target/report/

