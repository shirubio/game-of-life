package com.balistra.gameoflife;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

public class GameOfLife {
	private static final long WAIT_TIME = 3000;

	private static final String APIG_GET_GAME_OF_LIFE_API_URL = "https://o8zaufedrd.execute-api.us-east-1.amazonaws.com/dev";
	private static final String APIG_GET_SESSION_ID_URL = APIG_GET_GAME_OF_LIFE_API_URL + "/SessionId";
	private static final String APIG_GET_IMAGE_LOCATION_URL = APIG_GET_GAME_OF_LIFE_API_URL + "/ImageLocation";
	private static final String WAIT = "wait";

	private JFrame frame;
	private JLabel boardImageLabel;

	private String sessionId = null;
	private int currentBoardIndex = -1;

	private JLabel imageIndexLbl;
	private JLabel lblSessionId;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(() -> {
            try {
                GameOfLife window = new GameOfLife();
                window.frame.setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
	}

	/**
	 * Create the application.
	 */
	private GameOfLife() {
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frame = new JFrame();
		frame.setBounds(100, 100, 650, 750);
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(null);

		JButton btnStartSession = new JButton("Start Session");
		btnStartSession.addActionListener(e -> startNewSession());
		btnStartSession.setBounds(6, 6, 117, 29);
		frame.getContentPane().add(btnStartSession);

		JButton btnNextBoard = new JButton("<html>&gt;");
		btnNextBoard.addActionListener(e -> showNextBoard());
		btnNextBoard.setBounds(500, 6, 40, 29);
		frame.getContentPane().add(btnNextBoard);

		JButton btnPreviousBoard = new JButton("<html>&lt;");
		btnPreviousBoard.addActionListener(e -> showPreviousImage());
		btnPreviousBoard.setBounds(450, 6, 40, 29);
		frame.getContentPane().add(btnPreviousBoard);

		JPanel boardImagePanel = new JPanel();
		boardImagePanel.setBounds(6, 34, 780, 660);
		frame.getContentPane().add(boardImagePanel);
		boardImagePanel.setLayout(new BorderLayout(0, 0));

		boardImageLabel = new JLabel("");
		boardImagePanel.add(boardImageLabel, BorderLayout.CENTER);

		imageIndexLbl = new JLabel("");
		imageIndexLbl.setBounds(600, 11, 61, 16);
		frame.getContentPane().add(imageIndexLbl);

		lblSessionId = new JLabel("");
		lblSessionId.setBounds(127, 11, 357, 16);
		frame.getContentPane().add(lblSessionId);
	}

	private void startNewSession() {
		boardImageLabel.setIcon(null);
		boardImageLabel.setText("");

		try {
			sessionId = getSessionId();
		} catch (IOException e) {
			e.printStackTrace();
			sessionId = null;
		}

		lblSessionId.setText(sessionId);
		currentBoardIndex = 0;

		showBoard(currentBoardIndex);
	}

	private void showNextBoard() {
		currentBoardIndex++;
		if (currentBoardIndex > 20)
			currentBoardIndex = 20;
		showBoard(currentBoardIndex);
	}

	private void showPreviousImage() {
		currentBoardIndex--;
		if (currentBoardIndex < 0)
			currentBoardIndex = 0;
		showBoard(currentBoardIndex);
	}

	private void showBoard(int index) {
		try {
			String imageLocation;
			while (true) {
				imageLocation = retrieveImageURL(sessionId, index);
				if (imageLocation == null || WAIT.equals(imageLocation)) {
					Thread.sleep(WAIT_TIME);
					continue;
				}
				break;
			}

			ImageIcon image = new ImageIcon(new URL(imageLocation));
			boardImageLabel.setIcon(image);
			boardImageLabel.setText("");
			boardImageLabel.repaint();
			imageIndexLbl.setText("" + currentBoardIndex);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private String retrieveImageURL(String sessionId, int index) throws IOException {
		String imageKey = sessionId + "-" + index;
		String strURL = APIG_GET_IMAGE_LOCATION_URL + "/" + imageKey;
		return callGET(strURL);
	}

	private String getSessionId() throws IOException {
		return callGET(APIG_GET_SESSION_ID_URL);
	}

	private String callGET(String strURL) {
		long t0 = System.currentTimeMillis();
		System.out.println("Calling:" + strURL);

		String result;
		try {
			URL url = new URL(strURL);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setDoOutput(true);
			connection.setInstanceFollowRedirects(false);
			connection.setRequestProperty("Content-Type", "application/text");
			connection.setRequestMethod("GET");

			connection = (HttpURLConnection) url.openConnection();
			BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));

			String line = reader.readLine();
			System.out.println(connection.getResponseCode());
			connection.disconnect();
			result = line;
		} catch (Exception e) {
			e.printStackTrace();
			result = null;
		}
		System.out.println((System.currentTimeMillis() - t0) + "ms");

		if (result == null)
			result = "";
		else
			result = result.replace("\"", "");

		return result;
	}
}
