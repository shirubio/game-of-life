package com.balistra.gameoflife;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.amazonaws.services.lambda.runtime.events.SNSEvent.SNS;
import com.amazonaws.services.lambda.runtime.events.SNSEvent.SNSRecord;
import com.amazonaws.services.route53.model.InvalidArgumentException;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.PutObjectRequest;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

public class BoardImageGenerator implements RequestHandler<SNSEvent, Object> {
    private static final int BLOCK_SIZE = 10;
    private static final int X_LENGHT = 64;
    private static final int Y_HEIGHT = 64;

    private static String sessionId = null;
    private static String index = null;

    private static String JPEG = "jpg";
    private static String JPEG_EXTENSION = ".jpg";
    private LambdaLogger logger;
    private         AWSHelper awsHelper;


    @Override
    public Object handleRequest(SNSEvent snsEvent, Context context) {
        logger = context.getLogger();
        awsHelper = new AWSHelper(logger);

        logger.log("Input: " + snsEvent);

        List<SNSRecord> records = snsEvent.getRecords();
        logger.log("Passed 001");
        SNSRecord record = records.get(0);
        logger.log("Passed 002");
        SNS sns = record.getSNS();
        logger.log("Passed 003");
        String boardToProcess = sns.getMessage();
        logger.log("Passed 004");


        processBoard(boardToProcess);
        logger.log("Passed 005");

        return null;
    }

    private void processBoard(String boardToProcess) {
        // Isolate the Session ID
        int pos1 = boardToProcess.indexOf(':');
        sessionId = boardToProcess.substring(0, pos1);
        // Isolate the Image index
        int pos2 = boardToProcess.indexOf('[');
        index = boardToProcess.substring(pos1 + 1, pos2);
        String fileName = "IMG" + sessionId + "-" + index;
        int nextDigitLocation = pos2 + 1;

        // Now assemble the binary array
        int[][] board = new int[X_LENGHT][Y_HEIGHT];
        for (int x = 0; x < X_LENGHT; x++)
            for (int y = 0; y < Y_HEIGHT; y++) {
                char ch = boardToProcess.charAt(nextDigitLocation);
                if (ch != '0' && ch != '1')
                    throw new InvalidArgumentException("Can only process characters 0 or 1");
                int pixel = boardToProcess.charAt(nextDigitLocation) - 48;
                if (pixel != 0 && pixel != 1)
                    throw new InvalidArgumentException("Can only process pixels 0 or 1");
                board[x][y] = pixel;
                nextDigitLocation += 2;
            }

        try {
            generateBoardImage(fileName, board, X_LENGHT, Y_HEIGHT);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     *
     * @param fileName
     * @param board
     * @param X
     * @param Y
     * @throws Exception
     */
    private void generateBoardImage(String fileName, int[][] board, int X, int Y) throws Exception {
        BufferedImage bi = new BufferedImage(X * BLOCK_SIZE, Y * BLOCK_SIZE, BufferedImage.TYPE_BYTE_BINARY);
        Graphics2D g = (Graphics2D) bi.getGraphics();

        for (int x = 0; x < X; x++)
            for (int y = 0; y < Y; y++) {
                g.setColor(board[x][y] == 0 ? Color.WHITE : Color.BLACK);
                g.fillRect(x * 10, y * 10, (BLOCK_SIZE), BLOCK_SIZE);
            }
        g.dispose();
        store(bi, fileName);
    }

    /**
     * Save a JPEG image to file
     *
     * @param img the image to be saved
     * @param fileName file name to use
     * @throws IOException
     */
    private void store(BufferedImage img, String fileName) throws IOException {
        File file = File.createTempFile(fileName, JPEG_EXTENSION);
        fileName += JPEG_EXTENSION;
        file.deleteOnExit();

        ImageWriter writer = null;
        Iterator<ImageWriter> iter = ImageIO.getImageWritersByFormatName(JPEG);
        if (iter.hasNext()) {
            writer = iter.next();
        }

        ImageOutputStream ios = ImageIO.createImageOutputStream(file);
        writer.setOutput(ios);
        ImageWriteParam param = new JPEGImageWriteParam(java.util.Locale.getDefault());
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(0.98f);
        writer.write(null, new IIOImage(img, null, null), param);

        // Save the image to S3 and DynamoDB
        awsHelper.saveImageToAWS(fileName, file, sessionId, index);
    }

}