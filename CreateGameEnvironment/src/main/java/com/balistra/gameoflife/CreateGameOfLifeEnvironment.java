package com.balistra.gameoflife;


import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.*;
import com.amazonaws.services.dynamodbv2.util.TableUtils;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClient;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;
import java.util.UUID;

final class CreateGameOfLifeEnvironment {

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


    public static void main(String[] args) {
        OutputStream properties = null;
        try {
            UUID uuid = java.util.UUID.randomUUID();
            Properties props = new Properties();

            // Create S3 Bucket
            String bucketName = "game-of-life" + uuid;
            getS3().createBucket(bucketName);
            props.setProperty(s3NameProp, bucketName);

            props.setProperty(s3EndpointProp, "https://s3.amazonaws.com/" + bucketName);


            // Create SNS Topics
            String topicARN = getSNS().createTopic("GOL-NEW-SESSIONS" + uuid).getTopicArn();
            props.setProperty(snsTopicSessionsARNProp, topicARN);

            topicARN = getSNS().createTopic("GOL-CREATE-IMAGES" + uuid).getTopicArn();
            props.setProperty(snsTopicImagesARNProp, topicARN);

            // Create DynamoDB tables
            String tableName = "GOL-SESSIONS";
            String tableKey = "SESSION-ID";

            CreateTableRequest createTableRequest = new CreateTableRequest().withTableName("GOL-SESSIONS" + uuid)
                    .withKeySchema(new KeySchemaElement().withAttributeName("SESSION-ID").withKeyType(KeyType.HASH))
                    .withAttributeDefinitions(new AttributeDefinition().withAttributeName("SESSION-ID")
                            .withAttributeType(ScalarAttributeType.S))
                    .withProvisionedThroughput(
                            new ProvisionedThroughput().withReadCapacityUnits(1L).withWriteCapacityUnits(1L));
            TableUtils.createTableIfNotExists(getDynamoDB(), createTableRequest);

            props.setProperty(ddbSessionTableNameProp, "GOL-SESSIONS" + uuid);
            props.setProperty(ddbSessionTableKeyNameProp, "SESSIONS-ID");

            createTableRequest = new CreateTableRequest().withTableName("GOL-IMAGES" + uuid)
                    .withKeySchema(new KeySchemaElement().withAttributeName("IMAGE-ID").withKeyType(KeyType.HASH))
                    .withAttributeDefinitions(new AttributeDefinition().withAttributeName("IMAGE-ID")
                            .withAttributeType(ScalarAttributeType.S))
                    .withProvisionedThroughput(
                            new ProvisionedThroughput().withReadCapacityUnits(1L).withWriteCapacityUnits(1L));
            TableUtils.createTableIfNotExists(getDynamoDB(), createTableRequest);

            props.setProperty(ddbImageTableNameProp, "GOL-IMAGES" + uuid);
            props.setProperty(ddbImageTableKeyNameProp, "IMAGE-ID");

            properties = new FileOutputStream("GOL.properties");
            props.store(properties, null);

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

    private static AmazonS3 getS3() {
        if (s3 == null) {
            s3 = new AmazonS3Client();
            s3.setRegion(DEFAULT_REGION);
        }
        return s3;
    }

    private static AmazonSNS getSNS() {
        if (sns == null) {
            sns = new AmazonSNSClient();
            sns.setRegion(DEFAULT_REGION);
        }
        return sns;
    }

    private static AmazonDynamoDB getDynamoDB() {
        if (dynamoDB == null) {
            dynamoDB = new AmazonDynamoDBClient();
            dynamoDB.setRegion(DEFAULT_REGION);
        }
        return dynamoDB;
    }
}

