package sms.student.app;

import dev.finalproject.App;
import dev.sol.core.application.loader.FXLoader;
import javafx.scene.Scene;
import javafx.application.Platform;

public class RootLoader extends FXLoader {
    
    @Override
    public void load() {
        try {
            Scene scene = (Scene) params.get("scene");
            javafx.stage.Stage stage = (javafx.stage.Stage) params.get("OWNER");
            String studentId = (String) params.get("STUDENT_ID");
            
            if (scene == null || stage == null || studentId == null) {
                throw new IllegalStateException("Required parameters are missing");
            }
            
            System.out.println("RootLoader received Student ID: " + studentId);
            
            // Configure stage
            scene.setRoot(root);
            stage.setScene(scene);
            stage.initStyle(javafx.stage.StageStyle.UNDECORATED);
            
            // Position window
            javafx.stage.Screen screen = javafx.stage.Screen.getPrimary();
            javafx.geometry.Rectangle2D bounds = screen.getVisualBounds();
            stage.setX(bounds.getMaxX() - scene.getRoot().prefWidth(-1) - 20);
            stage.setY(bounds.getMaxY() - scene.getRoot().prefHeight(-1) - 50);
            stage.setAlwaysOnTop(true);
            
            // Pass parameters to controller
            RootController controller = loader.getController();
            controller.setParameters(params);
            controller.load();
            
            stage.show();
            
        } catch (Exception e) {
            System.err.println("Error in RootLoader: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
}
