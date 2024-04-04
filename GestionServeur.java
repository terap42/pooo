import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.net.URI;

public class GestionServeur extends Application {
    private Serveur server;
    private Label statusLabel;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        Label titleLabel = new Label("Gestionnaire de Serveur vous permet de faciliter avec son graphisme afin de n'est plus aller sur le boutton run");
        
        titleLabel.setStyle("-fx-font-size: 24px; ");

        statusLabel = new Label("Le serveur est: Stoper");
        statusLabel.setStyle("-fx-font-size: 35px; -fx-font-weight: bold;");

        Button startButton = new Button("Demarer le Serveur");
        startButton.setStyle("-fx-font-size: 34px; -fx-text-fill: black;");
        startButton.setOnAction(e -> startServer());

        Button stopButton = new Button("Stoper Serveur");
        stopButton.setStyle("-fx-font-size: 35px; -fx-text-fill: black;");
        stopButton.setOnAction(e -> stopServer());

        Button openBrowserButton = new Button("Voir Navigateur");
        openBrowserButton.setStyle("-fx-font-size: 35px;");
        openBrowserButton.setOnAction(e -> openInBrowser());

        VBox root = new VBox(30);
        root.setPadding(new Insets(30));
        root.getChildren().addAll(titleLabel, statusLabel, startButton, stopButton, openBrowserButton);
        root.setAlignment(javafx.geometry.Pos.CENTER);
        root.setBackground(new Background(new BackgroundFill(Color.LIGHTGRAY, CornerRadii.EMPTY, Insets.EMPTY)));

        Scene scene = new Scene(root, 1000, 1000);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Gestionnaire");
        primaryStage.setOnCloseRequest(e -> {
            stopServer();
            Platform.exit();
        });
        primaryStage.show();
    }

    private void startServer() {
        if (server == null) {
            server = new Serveur(8221); // Spécifiez le port ici
        }

        // Mettre à jour le statut avant de démarrer le serveur
        statusLabel.setText("Statut du serveur: En cours");

        new Thread(() -> {
            try {
                server.startServer();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void stopServer() {
        if (server != null) {
            // Mettre à jour le statut avant d'arrêter le serveur
            statusLabel.setText("Statut du serveur: Arrêté");

            new Thread(() -> {
                try {
                    server.stopServer();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    private void openInBrowser() {
        // Vérifier si le serveur est démarré
        if (server != null) {
            // URL locale par défaut
            String url = "http://localhost:8221"; // Modifier le port si nécessaire
    
            try {
                // Ouvrir l'URL dans le navigateur par défaut
                Desktop.getDesktop().browse(new URI(url));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } else {
            // Afficher un message d'avertissement si le serveur n'est pas démarré
            statusLabel.setText("Le serveur doit être démarré pour ouvrir dans le navigateur.");
        }
    }
    
}
