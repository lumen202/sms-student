package sms.student;

import dev.finalproject.data.AddressDAO;
import dev.finalproject.data.AttendanceLogDAO;
import dev.finalproject.data.AttendanceRecordDAO;
import dev.finalproject.data.ClusterDAO;
import dev.finalproject.data.GuardianDAO;
import dev.finalproject.data.SchoolYearDAO;
import dev.finalproject.data.StudentDAO;
import dev.finalproject.data.StudentGuardianDAO;
import dev.sol.core.application.FXApplication;
import dev.sol.core.application.loader.FXLoaderFactory;
import dev.sol.core.registry.FXCollectionsRegister;
import dev.sol.core.registry.FXControllerRegister;
import dev.sol.core.registry.FXNodeRegister;
import dev.sol.core.scene.FXSkin;
import dev.sol.db.DBService;
import javafx.collections.FXCollections;
import javafx.scene.image.Image;
import javafx.concurrent.Task;
import javafx.application.Platform;
import sms.student.app.RootController;
import sms.student.app.RootLoader;

/**
 * JavaFX App
 */
public class App extends FXApplication {

    public static final FXControllerRegister CONTROLLER_REGISTRY = FXControllerRegister.INSTANCE;
    public static final FXCollectionsRegister COLLECTIONS_REGISTRY = FXCollectionsRegister.INSTANCE;
    public static final FXNodeRegister NODE_REGISTER = FXNodeRegister.INSTANCE;
    public static final DBService DB_SMS = DBService.INSTANCE
            .initialize("jdbc:mysql://localhost/student_management_system_db?user=root&password=");

    @Override
    public void initialize() throws Exception {
        setTitle("Student Management System - Student");
        setSkin(FXSkin.PRIMER_LIGHT);
        getApplicationStage().getIcons().add(
                new Image(getClass()
                        .getResource("/sms/student/assets/img/logo.png")
                        .toExternalForm()));

        // Initialize dataset
        initialize_dataset();
        // Initialize UI first
        initialize_application();

        // Load data in background
        Task<Void> dataLoadTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                initialize_dataset();
                return null;
            }
        };

        dataLoadTask.setOnSucceeded(e -> {
            Platform.runLater(() -> {
                RootController controller = (RootController) CONTROLLER_REGISTRY.getController("ROOT");
                if (controller != null) {
                    controller.load();
                }
            });
        });

        new Thread(dataLoadTask).start();
    }

    public void initialize_dataset() {

        COLLECTIONS_REGISTRY.register("CLUSTER",
                FXCollections.observableArrayList(ClusterDAO.getClusterList()));

        COLLECTIONS_REGISTRY.register("SCHOOL_YEAR",
                FXCollections.observableArrayList(SchoolYearDAO.getSchoolYearList()));

        StudentDAO.initialize(
                COLLECTIONS_REGISTRY.getList("CLUSTER"),
                COLLECTIONS_REGISTRY.getList("SCHOOL_YEAR"));

        COLLECTIONS_REGISTRY.register("STUDENT",
                FXCollections.observableArrayList(StudentDAO.getStudentList()));

        COLLECTIONS_REGISTRY.register("GUARDIAN",
                FXCollections.observableArrayList(GuardianDAO.getGuardianList()));

        COLLECTIONS_REGISTRY.register("STUDENT_GUARDIAN",
                FXCollections.observableArrayList(StudentGuardianDAO.getStudentGuardianList()));

        COLLECTIONS_REGISTRY.register("ATTENDANCE_RECORD",
                FXCollections.observableArrayList(AttendanceRecordDAO.getRecordList()));

        AddressDAO.initialize(COLLECTIONS_REGISTRY.getList("STUDENT"));
        COLLECTIONS_REGISTRY.register("ADDRESS",
                FXCollections.observableArrayList(AddressDAO.getAddressesList()));

        AttendanceLogDAO.initialize(
                COLLECTIONS_REGISTRY.getList("STUDENT"),
                COLLECTIONS_REGISTRY.getList("ATTENDANCE_RECORD"));
        COLLECTIONS_REGISTRY.register("ATTENDANCE_LOG",
                FXCollections.observableArrayList(AttendanceLogDAO.getAttendanceLogList())); // Fix: call correct method
    }

    private void initialize_application() {
        RootLoader rootLoader = (RootLoader) FXLoaderFactory
                .createInstance(RootLoader.class,
                        App.class.getResource("/sms/student/app/ROOT.fxml"))
                .addParameter("scene", applicationScene)
                .addParameter("OWNER", applicationStage)
                .initialize();
        applicationStage.requestFocus();
        rootLoader.load();
    }
}