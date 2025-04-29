package sms.student.util;

import dev.finalproject.models.Student;
import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class JSONStorage {
    private static final String PROJECT_ROOT = System.getProperty("user.dir");
    private static final String STORAGE_DIR = PROJECT_ROOT + File.separator + "data" + File.separator + "attendance_logs";

    public static void saveStudentData(Student student) {
        try {
            // Create directory structure if it doesn't exist
            Path storagePath = Paths.get(STORAGE_DIR);
            Files.createDirectories(storagePath);
            
            // Create year and month subdirectories
            String yearMonth = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy" + File.separator + "MM"));
            Path datePath = storagePath.resolve(yearMonth);
            Files.createDirectories(datePath);

            // Create filename with timestamp
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd_HHmmss"));
            String filename = String.format("student_%s_%s.json", 
                student.getStudentID(), timestamp);

            // Full path for the JSON file
            Path jsonPath = datePath.resolve(filename);

            // Create JSON content
            String jsonString = String.format(
                "{\"studentId\":%d,\"firstName\":\"%s\",\"lastName\":\"%s\",\"timestamp\":\"%s\"}",
                student.getStudentID(),
                student.getFirstName(),
                student.getLastName(),
                LocalDateTime.now().toString()
            );

            // Write file
            Files.writeString(jsonPath, jsonString);
            System.out.println("Student data saved to: " + jsonPath);

        } catch (IOException e) {
            System.err.println("Error saving student data: " + e.getMessage());
        }
    }
}
