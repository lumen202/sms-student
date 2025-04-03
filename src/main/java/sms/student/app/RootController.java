package sms.student.app;

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

    @FXML
    private Label timeLabel;

    @FXML
    private Label dateLabel;

    @FXML
    private Button loginButton;

    @FXML
    private AnchorPane root;

    private ObservableList<Student> studentMasterList;
    private ObservableList<AttendanceRecord> attendanceRecord;
    private ObservableList<AttendanceLog> attendanceLogs;

    private Timeline timeline;
    private boolean isLoggedIn = false;
    private AttendanceLog currentLog = null; // Holds the currently active log

    private double xOffset;
    private double yOffset;

    private Stage stage;

    private SystemTrayUtil systemTrayUtil;

    private String studentId;

    @Override
    public void load() {
        stage = (Stage) getParameter("OWNER");
        System.out.println("Stage in load(): " + (stage != null ? "found" : "null"));
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
            stage = (Stage) root.getScene().getWindow();
            studentMasterList = DataManager.getInstance().getCollectionsRegistry().getList("STUDENT");
            attendanceRecord = DataManager.getInstance().getCollectionsRegistry().getList("ATTENDANCE_RECORD");
            attendanceLogs = DataManager.getInstance().getCollectionsRegistry().getList("ATTENDANCE_LOG");

            // Enable login button after loading data
            loginButton.setDisable(false);
            dateLabel.getStyleClass().add("date-header");
            loginButton.getStyleClass().add("login-button");
            updateButtonStyle();

            // Verify student exists
            Student student = getStudentById(studentId);
            if (student != null) {
                System.out.println("Found student: " + student.getFirstName() + " " + student.getLastName());
            } else {
                System.err.println("Student not found for ID: " + studentId);
            }

            systemTrayUtil = SystemTrayUtil.getInstance();
            systemTrayUtil.setupTray(stage, this::cleanup);
        } catch (Exception e) {
            System.err.println("Error loading collections: " + e.getMessage());
            e.printStackTrace();
        }
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
                today.getYear()
        );
        AttendanceRecordDAO.insert(newRecord);
        attendanceRecord.add(newRecord);
        return newRecord;
    }

    private Student getStudentWithID1() {
        return studentMasterList.stream()
                .filter(student -> student.getStudentID() == 1)
                .findFirst()
                .orElse(null);
    }

    private void logAttendanceAction(Student student, boolean isLogin, LocalDateTime timestamp, boolean isPM) {
        System.out.println("====================================");
        System.out.println("ATTENDANCE " + (isLogin ? "LOGIN" : "LOGOUT"));
        System.out.println("Student: " + student.getFirstName() + " " + student.getLastName());
        System.out.println("Student ID: " + student.getStudentID());
        System.out.println("Date: " + timestamp.format(DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy")));
        System.out.println("Time: " + timestamp.format(DateTimeFormatter.ofPattern("hh:mm:ss a")));
        System.out.println("Period: " + (isPM ? "PM" : "AM"));
        if (!isLogin && currentLog != null) {
            System.out.println("Session Duration: " + calculateDuration(isPM
                    ? currentLog.getTimeInPM() : currentLog.getTimeInAM(),
                    timestamp.getHour() * 100 + timestamp.getMinute()));
        }
        System.out.println("====================================");
    }

    private String calculateDuration(int timeIn, int timeOut) {
        int inHours = timeIn / 100;
        int inMinutes = timeIn % 100;
        int outHours = timeOut / 100;
        int outMinutes = timeOut % 100;

        int totalMinutes = (outHours * 60 + outMinutes) - (inHours * 60 + inMinutes);
        return String.format("%d hours, %d minutes", totalMinutes / 60, totalMinutes % 60);
    }

    @FXML
    private void handleLoginAction() {
        Student student = getStudentById(studentId);
        if (student == null) {
            System.err.println("Student not found with ID: " + studentId);
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        boolean isPM = now.getHour() >= 12;
        int currentTime = now.getHour() * 100 + now.getMinute();
        
        try {
            AttendanceRecord todayRecord = getOrCreateDayRecord();
            
            if (!isLoggedIn) { // Login Process
                logAttendanceAction(student, true, now, isPM);
                
                currentLog = attendanceLogs.stream()
                    .filter(log -> log.getRecordID().getDay() == todayRecord.getDay()
                        && log.getRecordID().getMonth() == todayRecord.getMonth()
                        && log.getRecordID().getYear() == todayRecord.getYear()
                        && log.getStudentID().getStudentID() == student.getStudentID())
                    .findFirst()
                    .orElseGet(() -> {
                        // Create new log if none exists
                        int nextLogId = attendanceLogs.isEmpty() ? 1 :
                            attendanceLogs.stream()
                                .mapToInt(AttendanceLog::getLogID)
                                .max()
                                .getAsInt() + 1;
                                
                        AttendanceLog newLog = new AttendanceLog(
                            nextLogId, todayRecord, student,
                            isPM ? 0 : currentTime,  // timeInAM
                            0,                       // timeOutAM
                            isPM ? currentTime : 0,  // timeInPM
                            0                        // timeOutPM
                        );
                        AttendanceLogDAO.insert(newLog);
                        attendanceLogs.add(newLog);
                        return newLog;
                    });
                
                // Update existing log's time
                if (isPM && currentLog.getTimeInPM() == 0) {
                    currentLog.setTimeInPM(currentTime);
                } else if (!isPM && currentLog.getTimeInAM() == 0) {
                    currentLog.setTimeInAM(currentTime);
                }
                AttendanceLogDAO.update(currentLog);
                
            } else { // Logout Process
                logAttendanceAction(student, false, now, isPM);
                
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
            
            // Toggle login state
            isLoggedIn = !isLoggedIn;
            loginButton.setText(isLoggedIn ? "Logout" : "Login");
            updateButtonStyle();
            
        } catch (Exception e) {
            System.err.println("Error handling login/logout: " + e.getMessage());
            e.printStackTrace();
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
            loginButton.setStyle("-fx-background-color: #DC3545;"); // Red for logout
        } else {
            loginButton.setStyle("-fx-background-color: #2196F3;"); // Blue for login
        }
    }

    private void handleMousePressed(MouseEvent event) {
        xOffset = event.getSceneX();
        yOffset = event.getSceneY();
    }

    private void handleMouseDragged(MouseEvent event) {
        StageUtil.makeDraggable(stage,
                event.getScreenX() - xOffset,
                event.getScreenY() - yOffset
        );
    }

    public void cleanup() {
        if (timeline != null) {
            timeline.stop();
        }
        if (systemTrayUtil != null) {
            systemTrayUtil.cleanup();
        }
    }
}
