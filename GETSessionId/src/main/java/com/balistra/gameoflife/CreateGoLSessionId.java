package com.balistra.gameoflife;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import java.util.UUID;

public class CreateGoLSessionId implements RequestHandler<Object, Object> {

    @Override
    public String handleRequest(Object input, Context context) {

        AWSHelper awsHelper = new AWSHelper(context.getLogger());

        // Create the session ID
        String sessionId = UUID.randomUUID().toString();

        // Store the session ID on DynamoDB
        awsHelper.storeNewSessionOnDynamo(sessionId);

        // Send the new session ID to a SNS TOPIC
        awsHelper.publishToSessionsTopic(sessionId);

        return sessionId;
    }
}
