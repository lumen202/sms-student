package sms.student.util;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import dev.finalproject.database.DataManager;
import dev.finalproject.models.Student;
import java.util.List;

public class KeyDecryptor {

    private static final String SECRET_KEY = "MySuperSecretKey";

    @SuppressWarnings("unchecked")
    public static DecryptedInfo getDecryptedInfo(String encryptedKey) {
        try {
            byte[] fixedKey = SECRET_KEY.getBytes();
            SecretKey secretKey = new SecretKeySpec(fixedKey, "AES");

            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);

            byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedKey));
            String decryptedString = new String(decryptedBytes);

            String[] parts = decryptedString.split("\\|");
            int studentId = Integer.parseInt(parts[0]);
            String schoolYear = parts[1];

            Student student = findStudentById(studentId);
            String fullName = student != null ? student.getFirstName() + " " + student.getLastName() : null;

            return new DecryptedInfo(
                    String.valueOf(studentId),
                    schoolYear,
                    fullName,
                    student
            );
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String decryptStudentKey(String encryptedKey) {
        try {
            DecryptedInfo info = getDecryptedInfo(encryptedKey);
            return info != null ? info.getStudentId() : null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static Student findStudentById(int studentId) {
        Object listObj = DataManager.getInstance()
                .getCollectionsRegistry()
                .getList("STUDENT");

        if (listObj instanceof List<?>) {
            List<?> list = (List<?>) listObj;
            if (!list.isEmpty() && list.get(0) instanceof Student) {
                List<Student> students = (List<Student>) list;
                return students.stream()
                        .filter(s -> s.getStudentID() == studentId)
                        .findFirst()
                        .orElse(null);
            }
        }
        return null;
    }

    public static class DecryptedInfo {

        private final String studentId;
        private final String schoolYear;
        private final String fullName;
        private final Student student;

        public DecryptedInfo(String studentId, String schoolYear, String fullName, Student student) {
            this.studentId = studentId;
            this.schoolYear = schoolYear;
            this.fullName = fullName;
            this.student = student;
        }

        public String getStudentId() {
            return studentId;
        }

        public String getSchoolYear() {
            return schoolYear;
        }

        public String getFullName() {
            return fullName;
        }

        public Student getStudent() {
            return student;
        }

        @Override
        public String toString() {
            String name = fullName != null ? fullName : "Unknown";
            String studentName = student != null ? student.getFirstName() + " " + student.getLastName() : "Unknown";
            return String.format("Student: %s (ID: %s), School Year: %s",
                    studentName,
                    studentId,
                    schoolYear);
        }
    }
}
