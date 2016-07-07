#!/bin/bash      

mvn clean compile package

aws lambda update-function-code \
--function-name GOLBoardImageGenerator \
--zip-file fileb://./target/BoardImageGenerator-0.0.1-SNAPSHOT.jar \
--publish \
--profile gameoflife

