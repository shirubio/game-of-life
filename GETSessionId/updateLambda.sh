#!/bin/bash      

mvn clean compile package

aws lambda update-function-code \
--function-name GOLGetSessionId \
--zip-file fileb://./target/GETSessionId-0.0.1-SNAPSHOT.jar \
--publish \
--profile gameoflife

