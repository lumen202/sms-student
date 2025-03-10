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
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.scene.image.Image;
import sms.student.app.RootController;
import sms.student.app.RootLoader;

public class App extends FXApplication {

    public static final FXControllerRegister CONTROLLER_REGISTRY = FXControllerRegister.INSTANCE;
    public static final FXCollectionsRegister COLLECTIONS_REGISTRY = FXCollectionsRegister.INSTANCE;
    public static final FXNodeRegister NODE_REGISTER = FXNodeRegister.INSTANCE;

    @Override
    public void initialize() throws Exception {
        setTitle("Student Management System - Student");
        setSkin(FXSkin.PRIMER_LIGHT);
        getApplicationStage().getIcons().add(
                new Image(getClass()
                        .getResource("/sms/student/assets/img/logo.png")
                        .toExternalForm()));

        // Prevent application from exiting when all stages are hidden
        Platform.setImplicitExit(false);

        // Initialize UI
        initialize_application();
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
                FXCollections.observableArrayList(AttendanceLogDAO.getAttendanceLogList()));
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

    @Override
    public void stop() throws Exception {
        // Cleanup resources if needed
        RootController controller = (RootController) CONTROLLER_REGISTRY.getController("ROOT");
        if (controller != null) {
            controller.cleanup();
        }
        super.stop();
    }
}