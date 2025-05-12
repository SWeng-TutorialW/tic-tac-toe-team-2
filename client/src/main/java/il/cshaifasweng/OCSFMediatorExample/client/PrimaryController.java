package il.cshaifasweng.OCSFMediatorExample.client;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import org.greenrobot.eventbus.Subscribe;
// Our controller for the game itself
public class PrimaryController {

	@FXML private Label symbolLabel;
	@FXML private Label turnLabel;
	@FXML private Label statusLabel;
	@FXML private GridPane boardGrid;

	private Button[][] buttons = new Button[3][3];
	private String mySymbol = "";
	private boolean myTurn = false;
	private String myClientId = "";

	@FXML
	void initialize() {
		symbolLabel.setText("Your Sign: -");
		turnLabel.setText("Current Turn: -");
		statusLabel.setText("Waiting for players...");
		// Initializes the buttons of the grid, each one separately.
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 3; col++) {
				Button button = new Button();
				button.setPrefSize(100, 100);
				final int r = row, c = col;
				button.setOnAction(e -> handleClick(r, c));
				buttons[row][col] = button;
				boardGrid.add(button, col, row);
			}
		}
		setBoardEnabled(false); // Prevent early clicks by the players until enabled
	}
	// Handles clicking by the players, more importantly, makes sure the client doesn't just get to spam his clicks by disabling after he chose a tile
	private void handleClick(int row, int col) {
		System.out.println("Clicked: " + row + "," + col + " | MyTurn: " + myTurn);
		if (!myTurn || !buttons[row][col].getText().isEmpty()) return;

		try {
			App.getClient().sendToServer("move " + row + " " + col);
			myTurn = false;
			turnLabel.setText("Current Turn: Opponent");
			setBoardEnabled(false); // Disable again until turn returns
		} catch (Exception e) {
			e.printStackTrace();
		}
	}



	@Subscribe
	public void onWarningEvent(WarningEvent event) {
		Platform.runLater(() -> handleMessage(event.getWarning().getMessage()));
	}

	// The server is the one determining the wins, loses, draws etc, as well as the actions, but the controller here handles messages from the SimpleServer
	private void handleMessage(String message) {
		System.out.println("Received message: " + message);

		if (message.startsWith("symbol")) {
			String[] parts = message.split(" ");
			mySymbol = parts[1];
			myClientId = parts.length > 2 ? parts[2] : "";  // store hash for future turn comparison
			symbolLabel.setText("Your Sign: " + mySymbol);
			statusLabel.setText("Waiting for second player...");
		}
		else if (message.equals("your turn")) {
			myTurn = true;
			turnLabel.setText("Current Turn: You");
			statusLabel.setText("Game started!");
			setBoardEnabled(true); // Enable the board for clicking
		} else if (message.equals("not your turn")) { // Practically never happens because the board doesn't let the player click, however I kept it anyways
			showPopup("Not Your Turn", "Please wait for your turn.");
		} else if (message.equals("invalid move")) {
			showPopup("Invalid Move", "This cell is already taken.");
		} else if (message.equals("draw")) {
			showPopup("Game Over", "The game ended in a draw.");
			myTurn = false;
		} else if (message.startsWith("win")) {
			char winner = message.split(" ")[1].charAt(0);
			showPopup("Game Over", "Player " + winner + " wins!");
			myTurn = false;
		} else if (message.matches("[-XO]{9}")) {
			updateBoard(message);
			if (!myTurn) {
				turnLabel.setText("Current Turn: Opponent");
			}
		}
	}


	// Updates the board with the newly inserted symbol
	private void updateBoard(String boardState) {
		for (int i = 0; i < 9; i++) {
			int row = i / 3, col = i % 3;
			char c = boardState.charAt(i);
			buttons[row][col].setText(c == '-' ? "" : String.valueOf(c));
		}
	}
	// Presents popup as an alert
	private void showPopup(String title, String message) {
		javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
		alert.setTitle(title);
		alert.setHeaderText(null);
		alert.setContentText(message);
		alert.showAndWait();
	}
	// Allows the board to be enabled and disabled
	private void setBoardEnabled(boolean enabled) {
		for (Button[] row : buttons) {
			for (Button button : row) {
				button.setDisable(!enabled);
			}
		}
	}

}
