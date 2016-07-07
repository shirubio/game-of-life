package com.balistra.gameoflife;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.amazonaws.services.lambda.runtime.events.SNSEvent.SNS;
import com.amazonaws.services.lambda.runtime.events.SNSEvent.SNSRecord;

import java.util.List;

/**
 * @author Silvio de Morais (shirubio@gmail.com)
 */
public class BoardCalculator implements RequestHandler<SNSEvent, Object> {

    private static final int X_LENGHT = 64;
    private static final int Y_HEIGHT = 64;
    private static final int WHITE_COVERAGE = 60;
    private static final int NUMBER_OF_BOARDS = 20;

    /**
     * Calculate a next board configuration based on the current one
     *
     * @param board current board configuration
     * @return The next Game of Life board
     */
    private static int[][] calculateNextBoard(int[][] board) {
        int[][] result = new int[X_LENGHT][Y_HEIGHT];
        for (int i = 0; i < X_LENGHT; i++)
            for (int j = 0; j < Y_HEIGHT; j++) {
                result[i][j] = deadOrAlive(board[i][j], numOfNeighbors(board, i, j));
            }
        return result;
    }

    /**
     * @param currentStatus  0 - dead, 1 - alive
     * @param numOfNeighbors
     * @return the next status of this cell
     */
    private static int deadOrAlive(int currentStatus, int numOfNeighbors) {
        if (numOfNeighbors == 3)
            return 1; // alive
        if (numOfNeighbors == 4)
            return currentStatus;
        return 0; // dead
    }

    /**
     * Calculate the number of neighbors of a cell
     *
     * @param board the current board configuration
     * @param i     coordinate of the cell
     * @param j     coordinate of the cell
     * @return the number of live cells nearby
     */
    private static int numOfNeighbors(int[][] board, int i, int j) {
        int ii = i - 1 < 0 ? 0 : i - 1;
        int iii = i + 1 >= X_LENGHT ? X_LENGHT - 1 : i + 1;
        int jj = j - 1 < 0 ? 0 : j - 1;
        int jjj = j + 1 >= Y_HEIGHT ? Y_HEIGHT - 1 : j + 1;
        int result = 0;
        for (int x = ii; x <= iii; x++)
            for (int y = jj; y <= jjj; y++) {
                result += board[x][y];
            }
        return result;
    }

    /**
     * Generate the first board by randomly generating the first state of each
     * cell
     *
     * @param whiteCoverage (1-100) percentage of the board to be left empty (dead cells)
     * @return a matrix with all cells on a board marked (0 - dead) or (1 -
     * alive)
     */
    private static int[][] generateFirstBoard(int whiteCoverage) {
        int[][] result = new int[X_LENGHT][Y_HEIGHT];
        for (int x = 0; x < X_LENGHT; x++)
            for (int y = 0; y < Y_HEIGHT; y++) {
                int k = (int) (Math.random() * 100);
                result[x][y] = k < whiteCoverage ? 0 : 1;
            }
        return result;
    }

    @Override
    public Object handleRequest(SNSEvent snsEvent, Context context) {

        // You gotta love Java's deep objects...
        List<SNSRecord> records = snsEvent.getRecords();
        SNSRecord record = records.get(0);
        SNS sns = record.getSNS();
        String sessionId = sns.getMessage();


        context.getLogger().log("Game of Life Session ID: " + sessionId);

        calculateBoards(sessionId, NUMBER_OF_BOARDS);
        return sessionId;
    }

    private void calculateBoards(String sessionId, int numOfBoardsToCalculate) {
//		StringBuilder result = new StringBuilder();

        // the Session ID
//		result.append(sessionId);

        // generate the first board
        int[][] board = generateFirstBoard(WHITE_COVERAGE);
        String boardAsString = writeResults(sessionId, 0, board);
//		result.append(boardAsString);
        callGenerateImage(boardAsString);

        // now generate all other boards
        for (int i = 1; i <= numOfBoardsToCalculate; i++) {
            board = calculateNextBoard(board);
            boardAsString = writeResults(sessionId, i, board);
//			result.append(boardAsString);
            callGenerateImage(boardAsString);
        }
    }

    private void callGenerateImage(String boardAsString) {
        AWSHelper.getSNS().publish(AWSHelper.SNS_IMAGES_ARN, boardAsString);
    }

    private String writeResults(String sessionId, int index, int[][] board) {
        StringBuilder sb = new StringBuilder();
        sb.append(sessionId);
        sb.append(":");
        sb.append(index);
        sb.append("[");
        for (int i = 0; i < X_LENGHT; i++) {
            for (int j = 0; j < Y_HEIGHT; j++) {
                sb.append(board[i][j]);
                sb.append(",");
            }
        }
        // Delete the last "," before closing
        sb.deleteCharAt(sb.length() - 1);
        sb.append("]");
        return sb.toString();
    }

}
