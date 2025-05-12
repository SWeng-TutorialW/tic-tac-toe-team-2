// SimpleServer.java
package il.cshaifasweng.OCSFMediatorExample.server;

import il.cshaifasweng.OCSFMediatorExample.server.ocsf.AbstractServer;
import il.cshaifasweng.OCSFMediatorExample.server.ocsf.ConnectionToClient;
import il.cshaifasweng.OCSFMediatorExample.server.ocsf.SubscribedClient;
import il.cshaifasweng.OCSFMediatorExample.entities.Warning;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class SimpleServer extends AbstractServer {

	private char[][] board = new char[3][3];
	private int currentTurnIndex;
	private ArrayList<SubscribedClient> players = new ArrayList<>();
	private Map<ConnectionToClient, Character> symbolMap = new HashMap<>();
	private boolean gameStarted = false;

	private static ArrayList<SubscribedClient> SubscribersList = new ArrayList<>();

	public SimpleServer(int port) {
		super(port);
	}
	// This will handle our initial connections, whose turn it is,
	@Override
	protected void handleMessageFromClient(Object msg, ConnectionToClient client) {
		String msgString = msg.toString();
		// Handles warnings
		if (msgString.startsWith("#warning")) {
			try {
				client.sendToClient(new Warning("Warning from server!"));
				System.out.format("Sent warning to client %s\n", Objects.requireNonNull(client.getInetAddress()).getHostAddress());
			} catch (IOException e) {
				e.printStackTrace();
			} // Handles adding a client
		} else if (msgString.startsWith("add client")) {
			if (players.size() < 2) {
				SubscribedClient connection = new SubscribedClient(client);
				players.add(connection);
				SubscribersList.add(connection);
				try {
					client.sendToClient("client added successfully");
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			// If the size is 2, this means we can start the game! so we change it to true and reset the board
			if (players.size() == 2 && !gameStarted) {
				gameStarted = true;
				resetBoard();
				// Gives out a random symbol for each
				int random = (int)(Math.random() * 2);
				SubscribedClient playerX = players.get(random);
				SubscribedClient playerO = players.get(1 - random);
				// HashMap is used here for the choosing process (which symbol to place on the grid for example)
				symbolMap.put(playerX.getClient(), 'X');
				symbolMap.put(playerO.getClient(), 'O');

				try {
					// In case of a localhost making both clients, we'd want them to have unique ids, hence why they both receive a different id
					playerX.getClient().sendToClient("symbol X " + System.identityHashCode(playerX.getClient()));
					playerO.getClient().sendToClient("symbol O " + System.identityHashCode(playerO.getClient()));

					// Prints out the symbols assigned for debugging
					System.out.println("Assigned symbols:");
					System.out.println("Player X: " + playerX.getClient());
					System.out.println("Player O: " + playerO.getClient());
					System.out.println("Symbol map: " + symbolMap);

				} catch (IOException e) {
					e.printStackTrace();
				}
				// Random player starts his turn
				currentTurnIndex = (int)(Math.random() * 2);
				broadcastBoard();

				try {
					players.get(currentTurnIndex).getClient().sendToClient("your turn");
					System.out.println("Initial turn: " + players.get(currentTurnIndex).getClient());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		// In case of a client removing, we'd like it to be removed from the subscriber list and the players list entirely
		} else if (msgString.startsWith("remove client")) {
			SubscribersList.removeIf(subscribedClient -> subscribedClient.getClient().equals(client));
			players.removeIf(p -> p.getClient().equals(client));
			symbolMap.remove(client);
			System.out.println("Client removed: " + client);
		}
		// If we receive a move, we'll update it
		else if (msgString.startsWith("move")) {
			System.out.println("Received move from client: " + client + " | Message: " + msgString);

			if (!gameStarted) return;

			if (!players.get(currentTurnIndex).getClient().equals(client)) {
				try {
					client.sendToClient("not your turn"); // Doesn't happen, though it is useful to have just incase
				} catch (IOException e) {
					e.printStackTrace();
				}
				return;
			}

			String[] parts = msgString.split(" ");
			int row = Integer.parseInt(parts[1]);
			int col = Integer.parseInt(parts[2]);
			char symbol = symbolMap.get(client);

			// If the tile isn't empty, you are not allowed to make the move.
			if (board[row][col] != '\0') {
				try {
					client.sendToClient("invalid move");
				} catch (IOException e) {
					e.printStackTrace();
				}
				return;
			}

			board[row][col] = symbol;
			broadcastBoard();

			// Our win condition and draw tests, if neither happen then we continue as normal
			if (checkWin(symbol)) {
				sendToAllClients("win " + symbol);
				resetGame();
			} else if (checkDraw()) {
				sendToAllClients("draw");
				resetGame();
			} else {
				currentTurnIndex = 1 - currentTurnIndex;
				try {
					players.get(currentTurnIndex).getClient().sendToClient("your turn");
					System.out.println("Switched turn to: " + players.get(currentTurnIndex).getClient());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	// Resets the game entirely, this allows clients to exit and join back to start a new instance of a game
	private void resetGame() {
		resetBoard();
		players.clear();
		symbolMap.clear();
		gameStarted = false;
	}

	// Broadcasts the board to both players by sending it to all clients
	private void broadcastBoard() {
		StringBuilder builder = new StringBuilder();
		for (char[] row : board) {
			for (char c : row) {
				builder.append(c == '\0' ? "-" : c);
			}
		}
		sendToAllClients(builder.toString());
	}

	// Resets the board
	private void resetBoard() {
		for (int i = 0; i < 3; i++)
			for (int j = 0; j < 3; j++)
				board[i][j] = '\0';
	}
	// Win condition check
	private boolean checkWin(char symbol) {
		for (int i = 0; i < 3; i++) {
			if (board[i][0] == symbol && board[i][1] == symbol && board[i][2] == symbol) return true;
			if (board[0][i] == symbol && board[1][i] == symbol && board[2][i] == symbol) return true;
		}
		return (board[0][0] == symbol && board[1][1] == symbol && board[2][2] == symbol)
				|| (board[0][2] == symbol && board[1][1] == symbol && board[2][0] == symbol);
	}
	// Draw condition check
	private boolean checkDraw() {
		for (char[] row : board)
			for (char cell : row)
				if (cell == '\0') return false;
		return true;
	}
	// Sends to all clients a message
	private void sendToAllClients(String message) {
		for (SubscribedClient sc : players) {
			try {
				sc.getClient().sendToClient(message);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}