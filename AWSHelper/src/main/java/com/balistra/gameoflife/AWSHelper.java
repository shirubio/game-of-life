package com.balistra.gameoflife;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.*;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClient;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

final class AWSHelper {
    private static final String snsTopicSessionsARNProp = "snsTopicSessionsARN";
    private static final String snsTopicImagesARNProp = "snsTopicImagesARN";
    private static final String s3NameProp = "s3Name";
    private static final String s3EndpointProp = "s3Endpoint";
    private static final String ddbSessionTableNameProp = "ddbSessionTableName";
    private static final String ddbSessionTableKeyNameProp = "ddbSessionTableKeyName";
    private static final String ddbImageTableNameProp = "ddbImageTableName";
    private static final String ddbImageTableKeyNameProp = "ddbImageTableKeyName";

    private static final String S3_LOCATION = "S3_LOCATION";
    private static final String IMAGE_INDEX = "IMAGE_INDEX";
    private static final Region DEFAULT_REGION = Region.getRegion(Regions.US_EAST_1);
    // handle to DynamoDB API
    private static AmazonDynamoDBClient dynamoDB = null;
    // The handle to SNS API
    private static AmazonSNS sns;
    // The handle to S3 API
    private static AmazonS3 s3;
    private String snsSessionsArn;
    private String snsImagesArn;
    private String s3Name;
    private String s3Endpoint;
    private String ddbSessionTable;
    private String ddbSessionTableKey;
    private String ddbImageTable;
    private String ddbImageTableKey;

    private LambdaLogger logger;

    AWSHelper(LambdaLogger l) {
        logger = l;

        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        InputStream properties = classloader.getResourceAsStream("GOL.properties");
        Properties prop = new Properties();
        try {

            prop.load(properties);

            snsSessionsArn = prop.getProperty(snsTopicSessionsARNProp);
            snsImagesArn = prop.getProperty(snsTopicImagesARNProp);
            s3Name = prop.getProperty(s3NameProp);
            s3Endpoint = prop.getProperty(s3EndpointProp);
            ddbSessionTable = prop.getProperty(ddbSessionTableNameProp);
            ddbSessionTableKey = prop.getProperty(ddbSessionTableKeyNameProp);
            ddbImageTable = prop.getProperty(ddbImageTableNameProp);
            ddbImageTableKey = prop.getProperty(ddbImageTableKeyNameProp);

            logger.log(snsSessionsArn);
            logger.log(snsImagesArn);
            logger.log(s3Name);
            logger.log(s3Endpoint);
            logger.log(ddbSessionTable);
            logger.log(ddbSessionTableKey);
            logger.log(ddbImageTable);
            logger.log(ddbImageTableKey);

        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            if (properties != null) {
                try {
                    properties.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    private AmazonSNS getSNS() {
        if (sns == null) {
            sns = new AmazonSNSClient();
            sns.setRegion(DEFAULT_REGION);
        }
        return sns;
    }

    private AmazonDynamoDB getDynamoDB() {
        if (dynamoDB == null) {
            dynamoDB = new AmazonDynamoDBClient();
            dynamoDB.setRegion(DEFAULT_REGION);
        }
        return dynamoDB;
    }

    private AmazonS3 getS3() {
        if (s3 == null) {
            s3 = new AmazonS3Client();
            s3.setRegion(DEFAULT_REGION);
        }
        return s3;
    }

    // Game of Life specific methods

    void publishToSessionsTopic(String msg) {
        getSNS().publish(snsSessionsArn, msg);
    }

    void publishToImagesTopic(String msg) {
        getSNS().publish(snsImagesArn, msg);
    }

    void storeNewSessionOnDynamo(String sessionId) {
        Map<String, AttributeValue> newItem = new HashMap<>();
        newItem.put(ddbSessionTableKey, new AttributeValue().withS(sessionId));

        PutItemRequest putItemRequest = new PutItemRequest(ddbSessionTable, newItem);
        PutItemResult putItemResult = getDynamoDB().putItem(putItemRequest);
    }

    void storeNewImageOnDynamo(String sessionId, String imageIndex, String s3Location) {
        String imageKey = sessionId + "-" + imageIndex;
        Map<String, AttributeValue> newItem = new HashMap<>();
        newItem.put(ddbImageTableKey, new AttributeValue().withS(imageKey));
        newItem.put(ddbSessionTableKey, new AttributeValue().withS(sessionId));
        newItem.put(IMAGE_INDEX, new AttributeValue().withN(imageIndex));
        newItem.put(S3_LOCATION, new AttributeValue().withS(s3Location));

        PutItemRequest putItemRequest = new PutItemRequest(ddbImageTable, newItem);
        PutItemResult putItemResult = getDynamoDB().putItem(putItemRequest);
    }

    String retriveImageLocationFromDynamo(String imageKey) {
        if (imageKey == null)
            return null;

        GetItemRequest request = new GetItemRequest();
        request.setTableName(ddbImageTable);
        Map<String, AttributeValue> key = new HashMap<>();
        key.put(ddbImageTableKey, new AttributeValue().withS(imageKey));

        request.setKey(key);

        GetItemResult sessionInfo = getDynamoDB().getItem(request);
        if (sessionInfo == null || sessionInfo.getItem() == null)
            return null;

        String result = sessionInfo.getItem().get(S3_LOCATION).getS();
        return result;
    }

    void saveImageToAWS(String fileName, File file, String sessionId, String index) {
        PutObjectRequest request = new PutObjectRequest(s3Name, fileName, file);
        request.setCannedAcl(CannedAccessControlList.PublicRead);
        getS3().putObject(request);

        // Save the information to DB
        storeNewImageOnDynamo(sessionId, index, s3Endpoint + fileName);
    }
}
