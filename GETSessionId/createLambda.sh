#!/bin/bash      

mvn clean compile package

aws lambda delete-function \
--function-name GOLGetSessionId \
--profile gameoflife


aws lambda create-function \
--function-name GOLGetSessionId \
--runtime java8 \
--role arn:aws:iam::483594534433:role/lambda_basic_execution \
--handler com.balistra.gameoflife.CreateGoLSessionId \
--description "Game of Life - GET Session ID" \
--timeout 60 \
--memory-size 512 \
--publish  \
--zip-file fileb://./target/GETSessionId-0.0.1-SNAPSHOT.jar \
--profile gameoflife

