// App.java
package il.cshaifasweng.OCSFMediatorExample.client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import javafx.scene.control.TextInputDialog;
import java.io.IOException;

import org.greenrobot.eventbus.EventBus;

public class App extends Application {
    private static Scene scene;
    private static SimpleClient client;

    // A simple return Client
    public static SimpleClient getClient() {
        return client;
    }

    // A simple set Client
    public static void setClient(SimpleClient c) {
        client = c;
    }

    // Loads the game itself, starts by creating an instance of a loader and stage, then proceeds to load and register the event bus
    public static void loadGameStage() throws IOException {
        Stage gameStage = new Stage();
        FXMLLoader loader = new FXMLLoader(App.class.getResource("primary.fxml"));
        Parent root = loader.load();
        Object controller = loader.getController();
        if (controller != null) {
            EventBus.getDefault().register(controller); // Helps with redundant register instances
        }
        scene = new Scene(root, 640, 480); // Creates a new Scene and shows it
        gameStage.setScene(scene);
        gameStage.setTitle("Tic Tac Toe");
        gameStage.show();
    }

    private String[] showConnectionDialog() {
        TextInputDialog dialog = new TextInputDialog("localhost:3000");
        dialog.setTitle("Connect to Server");
        dialog.setHeaderText("Enter Server IP and Port (e.g. 3000)");
        dialog.setContentText("Format: ip:port");
        // This recognizes the port as well as ip in a unique format : ip:port, that way it keeps things easier

        return dialog.showAndWait()
                .map(input -> input.split(":"))
                .filter(arr -> arr.length == 2)
                .orElse(null);
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Connection Error");
        alert.setHeaderText("An error occurred");
        alert.setContentText(message);
        alert.showAndWait();
    }

    @Override
    // Identifies connection info and helps the user connect
    public void start(Stage stage) throws IOException {
        String[] connectionInfo = showConnectionDialog();

        if (connectionInfo == null) {
            Platform.exit();
            return;
        }

        String ip = connectionInfo[0];
        int port;

        try {
            port = Integer.parseInt(connectionInfo[1]);
        } catch (NumberFormatException e) {
            showError("Port must be a number.");
            Platform.exit();
            return;
        }

        // Loads FXML & register controller BEFORE network messages come in
        FXMLLoader loader = new FXMLLoader(App.class.getResource("primary.fxml"));
        Parent root = loader.load();

        Object controller = loader.getController();
        if (controller != null) {
            System.out.println("Registering controller: " + controller.getClass().getSimpleName());
            EventBus.getDefault().register(controller);
        }

        scene = new Scene(root, 640, 480);
        stage.setScene(scene);
        stage.setTitle("Tic Tac Toe");
        stage.show();

        // Now connect client — AFTER UI is ready to listen
        try {
            client = new SimpleClient(ip, port);
            client.openConnection();
            client.sendToServer("add client");
            setClient(client);
        } catch (Exception e) {
            showError("Could not connect to server at " + ip + ":" + port);
            Platform.exit();
        }

    }
    // Load FXML which registers controllers
    private static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource(fxml + ".fxml"));
        Parent root = fxmlLoader.load();

        Object controller = fxmlLoader.getController();
        if (controller != null) {
            System.out.println("Registering controller: " + controller.getClass().getSimpleName());
            EventBus.getDefault().register(controller);
        }

        return root;
    }


    // Stops the process and sends a message to remove the client
    @Override
    public void stop() throws Exception {
        EventBus.getDefault().unregister(this);
        client.sendToServer("remove client");
        client.closeConnection();
        super.stop();
    }

    public static void main(String[] args) {
        launch();
    }
}