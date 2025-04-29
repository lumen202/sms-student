package sms.student;

import dev.finalproject.database.DataManager;
import dev.sol.core.application.FXApplication;
import dev.sol.core.application.loader.FXLoaderFactory;
import dev.sol.core.scene.FXSkin;
import javafx.application.Platform;
import javafx.scene.image.Image;
import sms.student.app.auth.AuthController;
import sms.student.app.auth.AuthLoader;

public class App extends FXApplication {

    @Override
    public void initialize() throws Exception {
        configureApplication();
        initializeDataset();
        // Prevent application from exiting when all stages are hidden
        Platform.setImplicitExit(false);

        // Initialize UI
    
        initialize_application();
        

    }

    private void configureApplication() {
        setTitle("Student Management System - Student");
        setSkin(FXSkin.PRIMER_LIGHT);
        getApplicationStage().getIcons().add(
                new Image(getClass()
                        .getResource("/sms/student/assets/icons/logo.png")
                        .toExternalForm()));
    }

    public void initializeDataset() {
        DataManager.getInstance().initializeData();

    }

    private void initialize_application() {
        FXLoaderFactory.createInstance(AuthLoader.class,
                App.class.getResource("/sms/student/app/auth/AUTH.fxml"))
                .addParameter("scene", applicationScene)
                .addParameter("OWNER", applicationStage)
                .initialize()
                .load();
        applicationStage.requestFocus();
    }

}
