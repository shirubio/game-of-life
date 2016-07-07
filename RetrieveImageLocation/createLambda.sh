#!/bin/bash      

mvn clean compile package

aws lambda delete-function \
--function-name GOLRetrieveImageLocation \
--profile gameoflife


aws lambda create-function \
--function-name GOLRetrieveImageLocation \
--runtime java8 \
--role arn:aws:iam::<YOUR ACCOUNT ID HERE>:role/lambda_basic_execution \
--handler com.balistra.gameoflife.RetrieveImageLocation \
--description "Game of Life - GET the location of a JPEG image" \
--timeout 60 \
--memory-size 512 \
--publish  \
--zip-file fileb://./target/RetrieveImageLocation-0.0.1-SNAPSHOT.jar \
--profile gameoflife

