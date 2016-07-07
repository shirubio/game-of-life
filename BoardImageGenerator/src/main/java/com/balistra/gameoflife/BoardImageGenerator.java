package com.balistra.gameoflife;

import com.amazonaws.services.lambda.runtime.Context;
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

    @Override
    public Object handleRequest(SNSEvent snsEvent, Context context) {
        context.getLogger().log("Input: " + snsEvent);
        List<SNSRecord> records = snsEvent.getRecords();
        SNSRecord record = records.get(0);
        SNS sns = record.getSNS();
        String boardToProcess = sns.getMessage();


        processBoard(boardToProcess);

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
        File file = File.createTempFile(fileName, ".jpg");
        fileName += ".jpg";
        file.deleteOnExit();

        ImageWriter writer = null;
        Iterator<ImageWriter> iter = ImageIO.getImageWritersByFormatName("jpg");
        if (iter.hasNext()) {
            writer = iter.next();
        }

        ImageOutputStream ios = ImageIO.createImageOutputStream(file);
        writer.setOutput(ios);
        ImageWriteParam param = new JPEGImageWriteParam(java.util.Locale.getDefault());
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(0.98f);
        writer.write(null, new IIOImage(img, null, null), param);

        // Save the file to S3
        PutObjectRequest request = new PutObjectRequest(AWSHelper.S3_NAME, fileName, file);
        request.setCannedAcl(CannedAccessControlList.PublicRead);
        AWSHelper.getS3().putObject(request);

        // Save the information to DB
        AWSHelper.storeNewImageOnDynamo(sessionId, index, AWSHelper.S3_ENDPOINT + fileName);

    }
}