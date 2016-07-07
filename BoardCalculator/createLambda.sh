#!/bin/bash      

mvn clean compile package

aws lambda delete-function \
--function-name GOLBoardCalculator \
--profile gameoflife

aws lambda create-function \
--function-name GOLBoardCalculator \
--runtime java8 \
--role arn:aws:iam::<YOUR ACCOUNT ID HERE>:role/lambda_basic_execution \
--handler com.balistra.gameoflife.BoardCalculator \
--description "Game of Life - Calculate Board Content" \
--timeout 60 \
--memory-size 512 \
--publish  \
--zip-file fileb://./target/BoardCalculator-0.0.1-SNAPSHOT.jar \
--profile gameoflife

