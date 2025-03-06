package sms.student.app;

import dev.finalproject.App;
import dev.finalproject.models.AttendanceLog;
import dev.finalproject.models.AttendanceRecord;
import dev.finalproject.models.Student;
import dev.finalproject.data.AttendanceLogDAO;
import dev.finalproject.data.AttendanceRecordDAO;
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
import javafx.util.Duration;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class RootController extends FXController {

    @FXML
    private Label timeLabel;

    @FXML
    private Label dateLabel;

    @FXML
    private Button loginButton;

    private ObservableList<Student> studentMasterList;
    private ObservableList<AttendanceRecord> attendanceRecord;
    private ObservableList<AttendanceLog> attendanceLogs;

    private Timeline timeline;
    private boolean isLoggedIn = false;
    private AttendanceLog currentLog = null; // Holds the currently active log

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
            dateLabel.setText(currentDate.format(DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy")));
            timeLabel.setText(currentTime.format(DateTimeFormatter.ofPattern("hh:mm:ss a")));
        });
    }

    @Override
    protected void load_fields() {
        try {
            studentMasterList = App.COLLECTIONS_REGISTRY.getList("STUDENT");
            attendanceRecord = App.COLLECTIONS_REGISTRY.getList("ATTENDANCE_RECORD");
            attendanceLogs = App.COLLECTIONS_REGISTRY.getList("ATTENDANCE_LOG");

            loginButton.setDisable(true);
            dateLabel.getStyleClass().add("date-header");
            loginButton.getStyleClass().add("login-button");
            updateButtonStyle();
        } catch (Exception e) {
            System.err.println("Error loading collections: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    protected void load_listeners() {
        // No additional listeners needed
    }

    private AttendanceRecord getOrCreateDayRecord() {
        LocalDate today = LocalDate.now();
        // Look for an existing attendance record for today
        for (AttendanceRecord record : attendanceRecord) {
            if (record.getMonth() == today.getMonthValue() &&
                record.getDay() == today.getDayOfMonth() &&
                record.getYear() == today.getYear()) {
                return record;
            }
        }

        // Create a new attendance record for today with the next available ID
        int nextRecordId = 1;
        for (AttendanceRecord record : attendanceRecord) {
            if (record.getRecordID() >= nextRecordId) {
                nextRecordId = record.getRecordID() + 1;
            }
        }

        AttendanceRecord newRecord = new AttendanceRecord(
                nextRecordId,
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

    @FXML
    private void handleLoginAction() {
        LocalDateTime now = LocalDateTime.now();
        int currentTime = now.getHour() * 100 + now.getMinute();
        boolean isPM = now.getHour() >= 12;

        try {
            Student student = getStudentWithID1();
            if (student == null) {
                System.err.println("Student with ID 1 not found!");
                return;
            }

            AttendanceRecord todayRecord = getOrCreateDayRecord();

            AttendanceLog existingLog = attendanceLogs.stream()
                .filter(log -> log.getRecordID().getDay() == todayRecord.getDay() &&
                               log.getRecordID().getMonth() == todayRecord.getMonth() &&
                               log.getRecordID().getYear() == todayRecord.getYear() &&
                               log.getStudentID().getStudentID() == student.getStudentID())
                .findFirst()
                .orElse(null);

            if (!isLoggedIn) { // Logging in
                if (existingLog != null) {
                    currentLog = existingLog;
                    if (isPM && currentLog.getTimeInPM() == 0) {
                        currentLog.setTimeInPM(currentTime);
                        AttendanceLogDAO.update(currentLog);
                        System.out.println("Existing log updated with PM login time.");
                    } else if (!isPM && currentLog.getTimeInAM() == 0) {
                        currentLog.setTimeInAM(currentTime);
                        AttendanceLogDAO.update(currentLog);
                        System.out.println("Existing log updated with AM login time.");
                    } else {
                        System.out.println("Attendance login already recorded for this time period.");
                    }
                } else {
                    int nextLogId = 1;
                    if (!attendanceLogs.isEmpty()) {
                        nextLogId = attendanceLogs.stream()
                                .mapToInt(AttendanceLog::getLogID)
                                .max()
                                .getAsInt() + 1;
                    }
                    currentLog = new AttendanceLog(
                            nextLogId,
                            todayRecord,
                            student,
                            isPM ? 0 : currentTime,    // timeInAM
                            0,                         // timeOutAM
                            isPM ? currentTime : 0,    // timeInPM
                            0                          // timeOutPM
                    );
                    AttendanceLogDAO.insert(currentLog);
                    attendanceLogs.add(currentLog);
                    System.out.println("New attendance log created for " + (isPM ? "PM" : "AM") + " login.");
                }
            } else { // Logging out
                if (currentLog != null) {
                    if (isPM) {
                        currentLog.setTimeOutPM(currentTime);
                    } else {
                        currentLog.setTimeOutAM(currentTime);
                    }
                    AttendanceLogDAO.update(currentLog);
                    currentLog = null;
                    System.out.println("Attendance log updated with " + (isPM ? "PM" : "AM") + " logout time.");
                } else {
                    System.err.println("No active log to update for logout.");
                }
            }

            isLoggedIn = !isLoggedIn;
            loginButton.setText(isLoggedIn ? "Logout" : "Login");
            updateButtonStyle();

            String action = isLoggedIn ? "Login" : "Logout";
            System.out.println(action + " Time for Student ID 1: " +
                    now.format(DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy")) + " at " +
                    now.format(DateTimeFormatter.ofPattern("hh:mm:ss a")));
        } catch (Exception e) {
            System.err.println("Error handling login/logout: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateButtonStyle() {
        if (isLoggedIn) {
            loginButton.setStyle("-fx-background-color: #800000; -fx-text-fill: white;"); // Maroon
        } else {
            loginButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white;"); // Material Blue
        }
    }

    public void cleanup() {
        if (timeline != null) {
            timeline.stop();
        }
    }
}
