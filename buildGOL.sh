#!/bin/bash      

cd AWSHelper
mvn clean install

cd ../BoardCalculator
./uploadLambda.sh




