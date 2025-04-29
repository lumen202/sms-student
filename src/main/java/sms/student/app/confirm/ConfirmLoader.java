package sms.student.app.confirm;

import dev.sol.core.application.loader.FXLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.StageStyle;

public class ConfirmLoader extends FXLoader {

    @Override
    public void load() {
        Stage stage = (Stage) params.get("OWNER");

        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.initStyle(StageStyle.UNDECORATED);

        // Pass parameters to controller
        ConfirmController controller = loader.getController();
        controller.setParameters(params);
        controller.load();

        // Center on owner window
        Window owner = stage.getOwner();
        if (owner != null) {
            double centerX = owner.getX() + (owner.getWidth() - root.prefWidth(-1)) / 2;
            double centerY = owner.getY() + (owner.getHeight() - root.prefHeight(-1)) / 2;
            stage.setX(centerX);
            stage.setY(centerY);
        } else {
            stage.centerOnScreen();
        }

        stage.show();
    }
}
