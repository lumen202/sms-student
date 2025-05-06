package sms.student.app.root;

import dev.sol.core.application.loader.FXLoader;
import javafx.scene.Scene;
import javafx.application.Platform;
import javafx.stage.Stage;

public class RootLoader extends FXLoader {

    @Override
    public void load() {
        try {
            Scene scene = new Scene(root);
            Stage stage = (Stage) params.get("STAGE");
            String studentId = (String) params.get("STUDENT_ID");

            if (studentId == null) {
                throw new IllegalStateException("Student ID is required");
            }

            // Configure stage for widget-like behavior
            stage.setScene(scene);
            stage.setAlwaysOnTop(true);

            // Set up focus behavior
            stage.focusedProperty().addListener((obs, oldVal, newVal) -> {
                if (!newVal) {
                    Platform.runLater(() -> {
                        stage.setAlwaysOnTop(true);
                        stage.toFront();
                    });
                }
            });

            // Position window
            javafx.stage.Screen screen = javafx.stage.Screen.getPrimary();
            javafx.geometry.Rectangle2D bounds = screen.getVisualBounds();
            stage.setX(bounds.getMaxX() - scene.getRoot().prefWidth(-1) - 20);
            stage.setY(bounds.getMaxY() - scene.getRoot().prefHeight(-1) - 50);

            // Pass parameters to controller
            RootController controller = loader.getController();
            controller.addParameter("STAGE", stage)
                    .addParameter("STUDENT_ID", studentId)
                    .load();

            // Show stage after everything is set up
            Platform.runLater(() -> {
                stage.show();
                stage.toFront();
            });

        } catch (Exception e) {
            System.err.println("Error in RootLoader: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
