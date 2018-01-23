package com.anastasia.app.image3d;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.awt.*;
import java.io.IOException;

public class Image3dApplication extends Application {

    final static int SIZE_COEFF = 3;

    @Override
    public void start(Stage primaryStage) throws Exception{
        initScene(primaryStage);
        primaryStage.show();

    }

    private void initScene(Stage primaryStage) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("image3d_viewer.fxml"));
        primaryStage.setTitle("Киндер-Сюрприз Петасовой");

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        primaryStage.setScene(
                new Scene(root,
                        screenSize.width * SIZE_COEFF / (SIZE_COEFF + 1),
                        screenSize.height * SIZE_COEFF / (SIZE_COEFF + 1)
                )
        );

        primaryStage.setOnCloseRequest(event -> {
            Platform.exit();
            System.exit(0);
        });
    }


    public static void main(String[] args) {
        launch(args);
    }
}
