#!/bin/bash      

mvn clean compile package

aws lambda update-function-code \
--function-name GOLBoardCalculator \
--zip-file fileb://./target/BoardCalculator-0.0.1-SNAPSHOT.jar \
--publish \
--profile gameoflife

