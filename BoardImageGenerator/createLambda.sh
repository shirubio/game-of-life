#!/bin/bash      

mvn clean compile package

aws lambda delete-function \
--function-name GOLBoardImageGenerator \
--profile gameoflife


aws lambda create-function \
--function-name GOLBoardImageGenerator \
--runtime java8 \
--role arn:aws:iam::483594534433:role/lambda_basic_execution \
--handler com.balistra.gameoflife.BoardImageGenerator \
--description "Game of Life - Create JPEG from Board Content" \
--timeout 60 \
--memory-size 512 \
--publish  \
--zip-file fileb://./target/BoardImageGenerator-0.0.1-SNAPSHOT.jar \
--profile gameoflife

