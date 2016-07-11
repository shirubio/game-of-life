#!/bin/bash      

mvn clean compile package

java -jar target/CreateGameEnvironment-0.0.1-SNAPSHOT.jar 

cp GOL.properties ../BoardCalculator/src/main/resources/GOL.properties
cp GOL.properties ../BoardImageGenerator/src/main/resources/GOL.properties 
cp GOL.properties ../GETSessionId/src/main/resources/GOL.properties
cp GOL.properties ../RetrieveImageLocation/src/main/resources/GOL.properties

