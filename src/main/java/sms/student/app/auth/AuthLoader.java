package sms.student.app.auth;

import dev.sol.core.application.loader.FXLoader;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class AuthLoader extends FXLoader {

    @Override
    public void load() {
        Scene scene = (Scene) params.get("scene");
        Stage stage = (Stage) params.get("OWNER");

        // Configure stage
        scene.setRoot(root);
        stage.setScene(scene);
        stage.setResizable(false);

        // Remove size settings since they're in FXML now

        // Position window at center
        stage.centerOnScreen();

        // Add window close handler
        stage.setOnCloseRequest(event -> {
            event.consume(); // Prevent default close
            Platform.runLater(() -> {
                stage.close();
                Platform.exit();
                System.exit(0);
            });
        });

        // Load controller
        AuthController controller = loader.getController();
        controller.addParameter("SCENE", scene)
                .addParameter("OWNER", stage)
                .load();

        stage.show();
    }
}
