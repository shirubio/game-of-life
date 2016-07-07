package com.balistra.gameoflife;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.*;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClient;

import java.util.HashMap;
import java.util.Map;

final class AWSHelper {
    static final String SNS_SESSIONS_TOPIC = "GOL_NEW_SESSIONS";
    static final String SNS_SESSIONS_ARN = "arn:aws:sns:us-east-1:<YOUR ACCOUNT ID HERE>:GOL-NEW-SESSIONS";
    static final String SNS_IMAGES_TOPIC = "GOL_CREATE_IMAGE";
    static final String SNS_IMAGES_ARN = "arn:aws:sns:us-east-1:<YOUR ACCOUNT ID HERE>:GOL-CREATE-IMAGE";
    static final String S3_ENDPOINT = "https://s3.amazonaws.com/<YOUR PREFIX HERE>-game-of-life/";
    static final String S3_NAME = "<YOUR PREFIX HERE>-game-of-life";
    static final String DDB_SESSION_TABLE = "GOL-SESSIONS";
    static final String DDB_SESSION_TABLE_KEY = "SESSION-ID";
    static final String DDB_IMAGE_TABLE = "GOL-IMAGES";
    static final String DDB_IMAGE_TABLE_KEY = "IMAGE-ID";
    private static final Region DEFAULT_REGION = Region.getRegion(Regions.US_EAST_1);

    // handle to DynamoDB API
    private static AmazonDynamoDBClient dynamoDB = null;

    // The handle to SNS API
    private static AmazonSNS sns;

    private static AmazonS3 s3;

    static AmazonSNS getSNS() {
        if (sns == null) {
            sns = new AmazonSNSClient();
            sns.setRegion(DEFAULT_REGION);
        }
        return sns;
    }

    static AmazonDynamoDB getDynamoDB() {
        if (dynamoDB == null) {
            dynamoDB = new AmazonDynamoDBClient();
            dynamoDB.setRegion(DEFAULT_REGION);
        }
        return dynamoDB;
    }

    static AmazonS3 getS3() {
        if (s3 == null) {
            s3 = new AmazonS3Client();
            s3.setRegion(DEFAULT_REGION);
        }
        return s3;
    }

    static void storeNewImageOnDynamo(String sessionId, String imageIndex, String s3Location) {
        String imageKey = sessionId + "-" + imageIndex;
        Map<String, AttributeValue> newItem = new HashMap<>();
        newItem.put(DDB_IMAGE_TABLE_KEY, new AttributeValue().withS(imageKey));
        newItem.put("SESSION_ID", new AttributeValue().withS(sessionId));
        newItem.put("IMAGE_INDEX", new AttributeValue().withN(imageIndex));
        newItem.put("S3_LOCATION", new AttributeValue().withS(s3Location));

        PutItemRequest putItemRequest = new PutItemRequest(DDB_IMAGE_TABLE, newItem);
        PutItemResult putItemResult = getDynamoDB().putItem(putItemRequest);
    }

    static String retriveImageLocationFromDynamo(String imageKey) {
        if (imageKey == null)
            return null;

        GetItemRequest request = new GetItemRequest();
        request.setTableName(DDB_IMAGE_TABLE);
        Map<String, AttributeValue> key = new HashMap<>();
        key.put(DDB_IMAGE_TABLE_KEY, new AttributeValue().withS(imageKey));

        request.setKey(key);

        GetItemResult sessionInfo = getDynamoDB().getItem(request);
        if (sessionInfo == null || sessionInfo.getItem() == null)
            return null;

        String result = sessionInfo.getItem().get("S3_LOCATION").getS();
        return result;
    }

    static void storeNewSessionOnDynamo(String sessionId) {
        Map<String, AttributeValue> newItem = new HashMap<>();
        newItem.put(DDB_SESSION_TABLE_KEY, new AttributeValue().withS(sessionId));

        PutItemRequest putItemRequest = new PutItemRequest(DDB_SESSION_TABLE, newItem);
        PutItemResult putItemResult = getDynamoDB().putItem(putItemRequest);
    }
}
