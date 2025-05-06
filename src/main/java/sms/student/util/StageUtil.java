package sms.student.util;

import javafx.application.Platform;
import javafx.stage.Stage;

public class StageUtil {
    private static final long FOCUS_DELAY = 100;

    public static void showStage(Stage stage) {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> showStage(stage));
            return;
        }

        System.out.println("Showing stage");
        stage.show();
        stage.setIconified(false);
        stage.toFront();
        stage.setAlwaysOnTop(true);
        stage.requestFocus();

        // Reset alwaysOnTop after a brief delay
        new Thread(() -> {
            try {
                Thread.sleep(FOCUS_DELAY);
                Platform.runLater(() -> stage.setAlwaysOnTop(false));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    public static void hideStage(Stage stage) {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> hideStage(stage));
            return;
        }
        System.out.println("Hiding stage");
        stage.hide();
    }

    public static void makeDraggable(Stage stage, double x, double y) {
        if (stage != null) {
            stage.setX(x);
            stage.setY(y);
        }
    }
}
