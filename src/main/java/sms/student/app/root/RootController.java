package sms.student.app.root;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sms.student.util.StageUtil;
import sms.student.util.SystemTrayUtil;

public class RootController extends FXController {
    private static final Logger logger = LoggerFactory.getLogger(RootController.class);

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
    private Student currentStudent;
    private boolean isProcessing = false;
    private final ExecutorService backgroundExecutor = Executors.newCachedThreadPool();

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

        // Configure stage behavior for widget-like persistence
        stage.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                Platform.runLater(() -> {
                    stage.setAlwaysOnTop(true);
                    stage.toFront();
                });
            }
        });

        // Initialize system tray
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
        try {
            studentId = (String) getParameter("STUDENT_ID");
            logger.info("Initializing RootController for student ID: {}", studentId);

            CompletableFuture.runAsync(this::initializeCollections, backgroundExecutor)
                    .thenRun(() -> Platform.runLater(() -> {
                        setupUIElements();
                        clearError();
                        displayStudentName();
                        initializeTimeUpdates();
                        checkExistingAttendanceLog();
                    }))
                    .exceptionally(e -> {
                        logger.error("Error in initialization", e);
                        Platform.runLater(() -> showError("Error initializing application"));
                        return null;
                    });
        } catch (Exception e) {
            logger.error("Error loading fields", e);
            throw e;
        }
    }

    private void initializeCollections() {
        studentMasterList = DataManager.getInstance().getCollectionsRegistry().getList("STUDENT");
        attendanceRecord = DataManager.getInstance().getCollectionsRegistry().getList("ATTENDANCE_RECORD");
        attendanceLogs = DataManager.getInstance().getCollectionsRegistry().getList("ATTENDANCE_LOG");
        currentStudent = getStudentById(studentId);
    }

    private void checkExistingAttendanceLog() {
        LocalDateTime now = LocalDateTime.now();
        boolean isPM = now.getHour() >= 12;

        AttendanceRecord todayRecord = findOrCreateTodayRecord();
        currentLog = findAttendanceLog(todayRecord);

        if (currentLog != null) {
            boolean hasTimeIn = isPM ? (currentLog.getTimeInPM() > 0 && currentLog.getTimeOutPM() == 0)
                    : (currentLog.getTimeInAM() > 0 && currentLog.getTimeOutAM() == 0);

            if (hasTimeIn) {
                isLoggedIn = true;
                Platform.runLater(() -> {
                    loginButton.setText("Time Out");
                    loginButton.setStyle("-fx-background-color: #800000;");
                });
            } else {
                isLoggedIn = false;
                Platform.runLater(() -> {
                    loginButton.setText("Time In");
                    loginButton.setStyle("-fx-background-color: #003366;");
                });
            }
        }
    }

    private AttendanceLog findAttendanceLog(AttendanceRecord record) {
        return attendanceLogs.stream()
                .filter(log -> log.getRecordID().getDay() == record.getDay()
                        && log.getRecordID().getMonth() == record.getMonth()
                        && log.getRecordID().getYear() == record.getYear()
                        && log.getStudentID().getStudentID() == currentStudent.getStudentID())
                .findFirst()
                .orElse(null);
    }

    private AttendanceRecord findOrCreateTodayRecord() {
        LocalDate today = LocalDate.now();
        return attendanceRecord.stream()
                .filter(record -> record.getMonth() == today.getMonthValue()
                        && record.getDay() == today.getDayOfMonth()
                        && record.getYear() == today.getYear())
                .findFirst()
                .orElseGet(() -> {
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
                });
    }

    private void setupUIElements() {
        loginButton.setDisable(false);
        dateLabel.getStyleClass().add("date-header");
        loginButton.getStyleClass().add("login-button");
        updateButtonStyle();
    }

    @Override
    protected void load_listeners() {
        root.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
            event.consume();
        });
        root.setOnMouseDragged(event -> {
            StageUtil.makeDraggable(stage, event.getScreenX() - xOffset, event.getScreenY() - yOffset);
            event.consume();
        });
        // Add click handler to prevent focus loss behavior
        root.setOnMouseClicked(event -> {
            stage.setAlwaysOnTop(true);
            event.consume();
        });
    }

    @FXML
    private void handleLoginAction() {
        if (isProcessing || currentStudent == null) {
            return;
        }
        try {
            isProcessing = true;
            LocalDateTime now = LocalDateTime.now();
            boolean isPM = now.getHour() >= 12;
            int currentTime = now.getHour() * 100 + now.getMinute();
            CompletableFuture.runAsync(() -> {
                try {
                    if (!isLoggedIn) {
                        handleTimeIn(now, isPM, currentTime);
                    } else {
                        handleTimeOut(now, isPM, currentTime);
                    }
                } finally {
                    isProcessing = false;
                }
            }, backgroundExecutor);
        } catch (Exception e) {
            logger.error("Error handling login/logout", e);
            showError("Error processing attendance");
            isProcessing = false;
        }
    }

    private void handleTimeIn(LocalDateTime now, boolean isPM, int currentTime) {
        if (isPM && isAlreadyTimeInPM() || !isPM && isAlreadyTimeInAM()) {
            Platform.runLater(() -> showError("Already timed in for this session"));
            return;
        }
        AttendanceRecord record = getOrCreateDayRecord();
        logAttendanceAction(true, now, isPM);
        processAttendanceLog(isPM, currentTime, record);
        Platform.runLater(() -> {
            isLoggedIn = true;
            loginButton.setText("Time Out");
            loginButton.setStyle("-fx-background-color: #800000;"); // Maroon for Time Out
            clearError();
        });
    }

    private void handleTimeOut(LocalDateTime now, boolean isPM, int currentTime) {
        if (isPM && !isValidPMTimeOut() || !isPM && !isValidAMTimeOut()) {
            Platform.runLater(() -> showError("Invalid time out: No time in record found"));
            return;
        }
        logAttendanceAction(false, now, isPM);
        updateLogoutTime(isPM, currentTime);

        Platform.runLater(() -> {
            showSuccess("Time out successful!");
            loginButton.setStyle("-fx-background-color: #003366;"); // Blue for Time In
            loginButton.setText("Time In");

            // Close application after a brief delay
            Timeline exitTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
                cleanup();
                Platform.exit();
                System.exit(0);
            }));
            exitTimeline.play();
        });
    }

    private void processAttendanceLog(boolean isPM, int currentTime, AttendanceRecord todayRecord) {
        currentLog = findOrCreateAttendanceLog(todayRecord);
        updateLoginTime(isPM, currentTime);
    }

    private AttendanceLog findOrCreateAttendanceLog(AttendanceRecord todayRecord) {
        return attendanceLogs.stream()
                .filter(log -> log.getRecordID().getDay() == todayRecord.getDay()
                        && log.getRecordID().getMonth() == todayRecord.getMonth()
                        && log.getRecordID().getYear() == todayRecord.getYear()
                        && log.getStudentID().getStudentID() == currentStudent.getStudentID())
                .findFirst()
                .orElseGet(() -> createNewAttendanceLog(todayRecord));
    }

    private AttendanceLog createNewAttendanceLog(AttendanceRecord todayRecord) {
        int nextLogId = attendanceLogs.isEmpty() ? 1
                : attendanceLogs.stream()
                        .mapToInt(AttendanceLog::getLogID)
                        .max()
                        .getAsInt() + 1;

        AttendanceLog newLog = new AttendanceLog(
                nextLogId,
                todayRecord,
                currentStudent,
                0, // timeInAM
                0, // timeOutAM
                0, // timeInPM
                0 // timeOutPM
        );

        AttendanceLogDAO.insert(newLog);
        attendanceLogs.add(newLog);
        return newLog;
    }

    private void updateLoginTime(boolean isPM, int currentTime) {
        if (currentLog == null)
            return;

        if (isPM && currentLog.getTimeInPM() == 0) {
            currentLog.setTimeInPM(currentTime);
        } else if (!isPM && currentLog.getTimeInAM() == 0) {
            currentLog.setTimeInAM(currentTime);
        }
        AttendanceLogDAO.update(currentLog);
    }

    private void updateLogoutTime(boolean isPM, int currentTime) {
        if (currentLog == null)
            return;

        if (isPM) {
            currentLog.setTimeOutPM(currentTime);
        } else {
            currentLog.setTimeOutAM(currentTime);
        }
        AttendanceLogDAO.update(currentLog);
    }

    private void showSuccess(String message) {
        Platform.runLater(() -> {
            errorLabel.setStyle("-fx-text-fill: #28a745;"); // Green color for success
            errorLabel.setText(message);
        });
    }

    private void logAttendanceAction(boolean isTimeIn, LocalDateTime timestamp, boolean isPM) {
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
                currentStudent.getFirstName(), currentStudent.getLastName(),
                currentStudent.getStudentID(),
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

    private void cleanup() {
        if (timeline != null) {
            timeline.stop();
        }
        if (systemTrayUtil != null) {
            systemTrayUtil.cleanup();
        }
        backgroundExecutor.shutdown();
    }

    private void showError(String message) {
        Platform.runLater(() -> {
            errorLabel.setStyle("-fx-text-fill: #dc3545;"); // Red color for error
            errorLabel.setText(message);
        });
    }

    private void clearError() {
        Platform.runLater(() -> {
            errorLabel.setText("");
        });
    }

    private void displayStudentName() {
        if (currentStudent != null) {
            nameLabel.setText(currentStudent.getFirstName() + " " + currentStudent.getLastName());
            logger.info("Displaying name for student: {} {}", currentStudent.getFirstName(),
                    currentStudent.getLastName());
        }
    }

    private AttendanceRecord getOrCreateDayRecord() {
        LocalDate today = LocalDate.now();
        return attendanceRecord.stream()
                .filter(record -> record.getMonth() == today.getMonthValue()
                        && record.getDay() == today.getDayOfMonth()
                        && record.getYear() == today.getYear())
                .findFirst()
                .orElseGet(() -> {
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
                });
    }
}
