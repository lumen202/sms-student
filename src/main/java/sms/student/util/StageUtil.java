package sms.student.util;

import javafx.application.Platform;
import javafx.stage.Stage;

public class StageUtil {
    public static void showStage(Stage stage) {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> showStage(stage));
            return;
        }

        if (!stage.isShowing()) {
            stage.show();
        } else if (stage.isIconified()) {
            stage.setIconified(false);
        }

        stage.toFront();
        stage.requestFocus();

        stage.setAlwaysOnTop(true);
        new Thread(() -> {
            try {
                Thread.sleep(200);
                Platform.runLater(() -> stage.setAlwaysOnTop(false));
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }).start();
    }

    public static void makeDraggable(Stage stage, double mouseX, double mouseY) {
        if (stage != null) {
            stage.setX(mouseX);
            stage.setY(mouseY);
        }
    }
}
