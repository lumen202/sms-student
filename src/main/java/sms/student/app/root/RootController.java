package sms.student.app.root;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import dev.finalproject.data.AttendanceLogDAO;
import dev.finalproject.data.AttendanceRecordDAO;
import dev.finalproject.database.DataManager;
import dev.finalproject.models.AttendanceLog;
import dev.finalproject.models.AttendanceRecord;
import dev.finalproject.models.Student;
import dev.sol.core.application.FXController;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import javafx.util.Duration;
import sms.student.util.StageUtil;
import sms.student.util.SystemTrayUtil;

public class RootController extends FXController {
    // FXML Injected Fields
    @FXML
    private Label timeLabel;
    @FXML
    private Label dateLabel;
    @FXML
    private Button loginButton;
    @FXML
    private AnchorPane root;
    @FXML
    private Label nameLabel;
    @FXML
    private Label errorLabel;

    // Data Collections
    private ObservableList<Student> studentMasterList;
    private ObservableList<AttendanceRecord> attendanceRecord;
    private ObservableList<AttendanceLog> attendanceLogs;

    // State Management
    private Timeline timeline;
    private boolean isLoggedIn = false;
    private AttendanceLog currentLog = null;
    private String studentId;

    // Window Management
    private double xOffset;
    private double yOffset;
    private Stage stage;
    private SystemTrayUtil systemTrayUtil;

    @Override
    public void load() {
        stage = (Stage) getParameter("STAGE");
        if (stage == null) {
            throw new IllegalStateException("Stage parameter is required");
        }

        // Initialize system tray first
        systemTrayUtil = SystemTrayUtil.getInstance();
        systemTrayUtil.setupTray(stage, this::cleanup);

        super.load();
    }

    @Override
    protected void load_bindings() {
        // Initialize time updates in a separate thread
        Task<Void> initTask = new Task<>() {
            @Override
            protected Void call() {
                Platform.runLater(() -> initializeTimeUpdates());
                return null;
            }
        };

        // Show loading state
        timeLabel.setText("Loading...");
        dateLabel.setText("");
        loginButton.setDisable(true);

        new Thread(initTask).start();
    }

    private void initializeTimeUpdates() {
        timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> updateDateTime()));
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
        // Initial update and enable login button
        updateDateTime();
        loginButton.setDisable(false);
    }

    private void updateDateTime() {
        LocalTime currentTime = LocalTime.now();
        LocalDate currentDate = LocalDate.now();
        Platform.runLater(() -> {
            dateLabel.setText(currentDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")));
            timeLabel.setText(currentTime.format(DateTimeFormatter.ofPattern("hh:mm:ss a")));
        });
    }

    @Override
    protected void load_fields() {
        studentId = (String) getParameter("STUDENT_ID");
        System.out.println("Received Student ID in RootController: " + studentId);

        try {
            // Initialize collections
            studentMasterList = DataManager.getInstance().getCollectionsRegistry().getList("STUDENT");
            attendanceRecord = DataManager.getInstance().getCollectionsRegistry().getList("ATTENDANCE_RECORD");
            attendanceLogs = DataManager.getInstance().getCollectionsRegistry().getList("ATTENDANCE_LOG");

            // Setup UI elements
            setupUIElements();

            // Clear any error messages
            errorLabel.setText("");

            // Get and display student name
            Student student = getStudentById(studentId);
            if (student != null) {
                nameLabel.setText(student.getFirstName() + " " + student.getLastName());
                System.out.println("Found student: " + student.getFirstName() + " " + student.getLastName());
            } else {
                System.err.println("Student not found for ID: " + studentId);
            }

        } catch (Exception e) {
            System.err.println("Error loading collections: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void setupUIElements() {
        loginButton.setDisable(false);
        dateLabel.getStyleClass().add("date-header");
        loginButton.getStyleClass().add("login-button");
        updateButtonStyle();
    }

    @Override
    protected void load_listeners() {
        root.setOnMousePressed(this::handleMousePressed);
        root.setOnMouseDragged(this::handleMouseDragged);
    }

    private AttendanceRecord getOrCreateDayRecord() {
        LocalDate today = LocalDate.now();
        // Look for existing record
        for (AttendanceRecord record : attendanceRecord) {
            if (record.getMonth() == today.getMonthValue()
                    && record.getDay() == today.getDayOfMonth()
                    && record.getYear() == today.getYear()) {
                return record;
            }
        }

        // Create new record with next available ID
        int maxId = attendanceRecord.stream()
                .mapToInt(AttendanceRecord::getRecordID)
                .max()
                .orElse(0);

        AttendanceRecord newRecord = new AttendanceRecord(
                maxId + 1,
                today.getMonthValue(),
                today.getDayOfMonth(),
                today.getYear());
        AttendanceRecordDAO.insert(newRecord);
        attendanceRecord.add(newRecord);
        return newRecord;
    }

    private void logAttendanceAction(Student student, boolean isTimeIn, LocalDateTime timestamp, boolean isPM) {
        String action = isTimeIn ? "TIME IN" : "TIME OUT";
        String periodText = isPM ? "Afternoon" : "Morning";
        String format = """
                ====================================
                ATTENDANCE %s (%s)
                Student: %s %s
                Student ID: %d
                Date: %s
                Time: %s
                %s
                ====================================
                """;

        String duration = !isTimeIn && currentLog != null ? String.format("Session Duration: %s", calculateDuration(
                isPM ? currentLog.getTimeInPM() : currentLog.getTimeInAM(),
                timestamp.getHour() * 100 + timestamp.getMinute())) : "";

        System.out.printf(format,
                action, periodText,
                student.getFirstName(), student.getLastName(),
                student.getStudentID(),
                timestamp.format(DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy")),
                timestamp.format(DateTimeFormatter.ofPattern("hh:mm:ss a")),
                duration);
    }

    private String calculateDuration(int timeIn, int timeOut) {
        int inHours = timeIn / 100;
        int inMinutes = timeIn % 100;
        int outHours = timeOut / 100;
        int outMinutes = timeOut % 100;

        int totalMinutes = (outHours * 60 + outMinutes) - (inHours * 60 + inMinutes);
        int hours = totalMinutes / 60;
        int minutes = totalMinutes % 60;

        if (hours > 0) {
            return String.format("%d hour%s %d minute%s",
                    hours, hours != 1 ? "s" : "",
                    minutes, minutes != 1 ? "s" : "");
        } else {
            return String.format("%d minute%s", minutes, minutes != 1 ? "s" : "");
        }
    }

    @FXML
    private void handleLoginAction() {
        Student student = getStudentById(studentId);
        if (student == null) {
            System.err.println("Student not found with ID: " + studentId);
            return;
        }

        try {
            LocalDateTime now = LocalDateTime.now();
            boolean isPM = now.getHour() >= 12;
            int currentTime = now.getHour() * 100 + now.getMinute();
            AttendanceRecord todayRecord = getOrCreateDayRecord();

            if (!isLoggedIn) {
                handleTimeIn(student, now, isPM, currentTime, todayRecord);
            } else {
                handleTimeOut(student, now, isPM, currentTime);
                return; // Exit after logout
            }

            // Update UI state (only for login)
            isLoggedIn = !isLoggedIn;
            loginButton.setText(isLoggedIn ? "Logout" : "Login");
            updateButtonStyle();

        } catch (Exception e) {
            System.err.println("Error handling login/logout: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleTimeIn(Student student, LocalDateTime now, boolean isPM, int currentTime,
            AttendanceRecord todayRecord) {
        if (isPM && isAlreadyTimeInPM() || !isPM && isAlreadyTimeInAM()) {
            errorLabel.setText("Already timed in for this session");
            return;
        }
        logAttendanceAction(student, true, now, isPM);
        processAttendanceLog(student, isPM, currentTime, todayRecord);
    }

    private void handleTimeOut(Student student, LocalDateTime now, boolean isPM, int currentTime) {
        if (isPM && !isValidPMTimeOut() || !isPM && !isValidAMTimeOut()) {
            errorLabel.setText("Invalid time out: No time in record found");
            return;
        }
        logAttendanceAction(student, false, now, isPM);
        updateLogoutTime(isPM, currentTime);
        terminateApplication();
    }

    private void processAttendanceLog(Student student, boolean isPM, int currentTime, AttendanceRecord todayRecord) {
        currentLog = findOrCreateAttendanceLog(student, todayRecord, isPM, currentTime);
        updateLoginTime(isPM, currentTime);
    }

    private void terminateApplication() {
        cleanup();
        Platform.runLater(() -> {
            Platform.exit();
            System.exit(0);
        });
    }

    private AttendanceLog findOrCreateAttendanceLog(Student student, AttendanceRecord todayRecord, boolean isPM,
            int currentTime) {
        return attendanceLogs.stream()
                .filter(log -> log.getRecordID().getDay() == todayRecord.getDay()
                        && log.getRecordID().getMonth() == todayRecord.getMonth()
                        && log.getRecordID().getYear() == todayRecord.getYear()
                        && log.getStudentID().getStudentID() == student.getStudentID())
                .findFirst()
                .orElseGet(() -> {
                    // Create new log if none exists
                    int nextLogId = attendanceLogs.isEmpty() ? 1
                            : attendanceLogs.stream()
                                    .mapToInt(AttendanceLog::getLogID)
                                    .max()
                                    .getAsInt() + 1;

                    AttendanceLog newLog = new AttendanceLog(
                            nextLogId, todayRecord, student,
                            isPM ? 0 : currentTime, // timeInAM
                            0, // timeOutAM
                            isPM ? currentTime : 0, // timeInPM
                            0 // timeOutPM
                    );
                    AttendanceLogDAO.insert(newLog);
                    attendanceLogs.add(newLog);
                    return newLog;
                });
    }

    private void updateLoginTime(boolean isPM, int currentTime) {
        if (isPM && currentLog.getTimeInPM() == 0) {
            currentLog.setTimeInPM(currentTime);
        } else if (!isPM && currentLog.getTimeInAM() == 0) {
            currentLog.setTimeInAM(currentTime);
        }
        AttendanceLogDAO.update(currentLog);
    }

    private void updateLogoutTime(boolean isPM, int currentTime) {
        if (currentLog != null) {
            if (isPM) {
                currentLog.setTimeOutPM(currentTime);
            } else {
                currentLog.setTimeOutAM(currentTime);
            }
            AttendanceLogDAO.update(currentLog);
            currentLog = null;
        }
    }

    private Student getStudentById(String id) {
        return studentMasterList.stream()
                .filter(student -> String.valueOf(student.getStudentID()).equals(id))
                .findFirst()
                .orElse(null);
    }

    private void updateButtonStyle() {
        if (isLoggedIn) {
            loginButton.setStyle("-fx-background-color: #800000;"); // Maroon for Time Out
            loginButton.setText("Time Out");
        } else {
            loginButton.setStyle("-fx-background-color: #003366;"); // Blue for Time In
            loginButton.setText("Time In");
        }
    }

    private boolean isAlreadyTimeInAM() {
        return currentLog != null && currentLog.getTimeInAM() > 0;
    }

    private boolean isAlreadyTimeInPM() {
        return currentLog != null && currentLog.getTimeInPM() > 0;
    }

    private boolean isValidAMTimeOut() {
        return currentLog != null && currentLog.getTimeInAM() > 0;
    }

    private boolean isValidPMTimeOut() {
        return currentLog != null && currentLog.getTimeInPM() > 0;
    }

    private void handleMousePressed(MouseEvent event) {
        xOffset = event.getSceneX();
        yOffset = event.getSceneY();
    }

    private void handleMouseDragged(MouseEvent event) {
        StageUtil.makeDraggable(stage,
                event.getScreenX() - xOffset,
                event.getScreenY() - yOffset);
    }

    private void cleanup() {
        if (timeline != null) {
            timeline.stop();
        }
        if (systemTrayUtil != null) {
            systemTrayUtil.cleanup();
        }
    }

}
