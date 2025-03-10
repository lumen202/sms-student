package sms.student.app;

import dev.finalproject.App;
import dev.sol.core.application.loader.FXLoader;
import javafx.scene.Scene;
import javafx.application.Platform;

public class RootLoader extends FXLoader {

    @Override
    public void load() {
        Scene scene = (Scene) params.get("scene");
        javafx.stage.Stage stage = (javafx.stage.Stage) params.get("OWNER");
        
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
        
        // Load controller
        RootController controller = loader.getController();
        System.out.println("Initializing controller with stage: " + stage);
        App.CONTROLLER_REGISTRY.register("ROOT", controller);
        controller.addParameter("SCENE", scene)
                .addParameter("OWNER", stage)
                .load();
        
        // Show stage initially (remove the hide call)
        stage.show();
    }
    
}
