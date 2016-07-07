package com.balistra.gameoflife;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

/**
 * @author Silvio de Morais (shirubio@gmail.com)
 */

public class RetrieveImageLocation implements RequestHandler<Request, String> {

    @Override
    public String handleRequest(Request request, Context context) {
        context.getLogger().log("Input: " + request);

        String location = AWSHelper.retriveImageLocationFromDynamo(request.getImageKey());
        if (location == null || "".equals(location.trim()))
            return "wait";
        else
            return location;
    }
}
