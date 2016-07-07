#!/bin/bash      

mvn clean compile package

aws lambda update-function-code \
--function-name GOLRetrieveImageLocation \
--zip-file fileb://./target/RetrieveImageLocation-0.0.1-SNAPSHOT.jar \
--publish \
--profile gameoflife

