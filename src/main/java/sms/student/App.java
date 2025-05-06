package sms.student;

import dev.finalproject.database.DataManager;
import dev.sol.core.application.FXApplication;
import dev.sol.core.application.loader.FXLoaderFactory;
import dev.sol.core.scene.FXSkin;
import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import sms.student.app.auth.AuthLoader;
import sms.student.app.root.RootLoader;
import sms.student.util.JSONStorage;

public class App extends FXApplication {
    String lastStudentId = JSONStorage.getLastLoggedInStudentId();

    @Override
    public void initialize() throws Exception {
        // Initialize dataset once at the start
        initializeDataset();
        // Set implicit exit to false to prevent application from closing unexpectedly
        Platform.setImplicitExit(false);
        configureApplication();
        initalizeApp(lastStudentId);
    }

    private void configureApplication() {
        setTitle("Student Management System - Student");
        setSkin(FXSkin.PRIMER_LIGHT);
        try {
            getApplicationStage().getIcons().add(
                    new Image(getClass()
                            .getResource("/sms/student/assets/icons/logo.png")
                            .toExternalForm()));
        } catch (Exception e) {
            System.err.println("Could not load application icon: " + e.getMessage());
        }
    }

    private void initalizeApp(String studentID) {
        System.out.println("Last logged in student ID: " + lastStudentId);

        if (lastStudentId != null && !lastStudentId.trim().isEmpty()) {
            System.out.println("Opening root window for student: " + lastStudentId);
            openRootWindowForStudent(lastStudentId);
        } else {
            System.out.println("No previous login found, showing auth window");
            initializeAuthWindow();
        }

    }

    private void initializeDataset() {
        DataManager.getInstance().initializeData();
    }

    private void initializeAuthWindow() {
        FXLoaderFactory.createInstance(AuthLoader.class,
                App.class.getResource("/sms/student/app/auth/AUTH.fxml"))
                .addParameter("scene", applicationScene)
                .addParameter("OWNER", applicationStage)
                .initialize()
                .load();
        applicationStage.requestFocus();
    }

    private void openRootWindow(String studentId, Stage rootStage) {
        FXLoaderFactory.createInstance(RootLoader.class,
                App.class.getResource("/sms/student/app/root/ROOT.fxml"))
                .addParameter("STUDENT_ID", studentId)
                .addParameter("STAGE", rootStage)
                .initialize()
                .load();
    }

    private void openRootWindowForStudent(String studentId) {
        Stage rootStage = new Stage(StageStyle.UNDECORATED);
        openRootWindow(studentId, rootStage);
        if (applicationStage != null) {
            Platform.runLater(() -> {
                applicationStage.setScene(null);
                applicationStage.hide();
                applicationStage = null;
            });
        }
    }
}