package com.example.timerplease;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

public class TimerApp extends Application {

    private Timeline timeline;
    private Label timerLabel;
    private int secondsElapsed;

    @Override
    public void start(Stage primaryStage) {
        Button startButton = new Button("Start");
        startButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                if (timeline != null) {
                    timeline.stop();
                }
                startTimer();
            }
        });

        timerLabel = new Label("00:00");

        VBox root = new VBox(10, startButton, timerLabel);
        root.setPrefSize(200, 100);

        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Timer App");
        primaryStage.show();
    }

    private void startTimer() {
        secondsElapsed = 0;
        timeline = new Timeline(new KeyFrame(Duration.seconds(1), new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                secondsElapsed++;
                updateTimerLabel();
            }
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    private void updateTimerLabel() {
        int minutes = secondsElapsed / 60;
        int seconds = secondsElapsed % 60;
        timerLabel.setText(String.format("%02d:%02d", minutes, seconds));
    }

    public static void main(String[] args) {
        launch(args);
    }
}

