package com.balistra.gameoflife;

import java.util.UUID;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

public class CreateGoLSessionId implements RequestHandler<Object, Object> {

	@Override
	public String handleRequest(Object input, Context context) {

		// Create the session ID
		String sessionId = UUID.randomUUID().toString();

		// Store the session ID on DynamoDB
		AWSHelper.storeNewSessionOnDynamo(sessionId);
		
		// Send the new session ID to a SNS TOPIC
		AWSHelper.getSNS().publish(AWSHelper.SNS_SESSIONS_ARN, sessionId);

		return sessionId;
	}
}
