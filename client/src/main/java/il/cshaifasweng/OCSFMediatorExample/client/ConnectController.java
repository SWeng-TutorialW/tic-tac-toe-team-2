package il.cshaifasweng.OCSFMediatorExample.client;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

// Our Controller for Connect
public class ConnectController {

    @FXML private TextField ipField;
    @FXML private TextField portField;

    @FXML
    private void handleConnect() {
        String ip = ipField.getText().trim();
        String portText = portField.getText().trim();

        if (ip.isEmpty() || portText.isEmpty()) {
            showError("Both IP and port are required.");
            return;
        }

        int port;
        try {
            port = Integer.parseInt(portText);
        } catch (NumberFormatException e) { // If the port is invalid, return error
            showError("Port must be a valid number.");
            return;
        }

        try {
            SimpleClient client = new SimpleClient(ip, port);
            client.openConnection();
            App.setClient(client);
            client.sendToServer("add client"); // Adds the client after confirming everything

            // Closes this window
            Stage stage = (Stage) ipField.getScene().getWindow();
            stage.close();

            // Loads the main game
            App.loadGameStage();

        } catch (Exception e) {
            showError("Failed to connect to server at " + ip + ":" + port);
        }
    }

    // Our error alert
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Connection Error");
        alert.setHeaderText("Connection Failed");
        alert.setContentText(message);
        alert.showAndWait();
    }
}
