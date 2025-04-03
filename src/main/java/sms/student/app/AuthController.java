package sms.student.app;

import dev.sol.core.application.FXController;
import dev.sol.core.application.loader.FXLoaderFactory;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.scene.Scene;
import javafx.scene.Group;
import sms.student.util.KeyDecryptor;
import dev.finalproject.database.DataManager;
import dev.finalproject.models.Student;
import javafx.collections.ObservableList;
import javafx.application.Platform;
import javafx.scene.control.ProgressIndicator;
import javafx.geometry.Pos;
import javafx.concurrent.Task;
import javafx.geometry.Insets;

public class AuthController extends FXController {

    @FXML
    private VBox root;
    @FXML
    private TextField keyField;
    @FXML
    private Label errorLabel;
    private ProgressIndicator progressIndicator;

    @Override
    protected void load_fields() {
        errorLabel.setText("");
        keyField.setOnAction(e -> handleSubmit());
        Platform.runLater(() -> keyField.requestFocus());
        progressIndicator = new ProgressIndicator();
        progressIndicator.setVisible(false);
        progressIndicator.setMaxSize(30, 30);
        root.getChildren().add(progressIndicator);
        VBox.setMargin(progressIndicator, new Insets(10, 0, 0, 0));
    }

    @Override
    protected void load_bindings() {
        keyField.textProperty().addListener((obs, old, newValue) -> {
            if (newValue != null) {
                errorLabel.setText("");
            }
        });
    }

    @Override
    protected void load_listeners() {

    }

    private void proceedToMainWindow(Student student) {
        Stage rootStage = new Stage();
        rootStage.initStyle(StageStyle.UNDECORATED);

        String studentId = String.valueOf(student.getStudentID());
        System.out.println("Creating RootLoader with Student ID: " + studentId);

        // Create and initialize loader properly
        RootLoader rootLoader = (RootLoader) FXLoaderFactory
                .createInstance(RootLoader.class, 
                        getClass().getResource("/sms/student/app/ROOT.fxml"))
                .addParameter("scene", new Scene(new Group()))
                .addParameter("OWNER", rootStage)
                .addParameter("STUDENT_ID", studentId)
                .initialize();

        if (rootLoader != null) {
            rootLoader.load();
            
            // Close auth window properly
            Stage currentStage = (Stage) keyField.getScene().getWindow();
            Platform.runLater(() -> currentStage.close());
        } else {
            System.err.println("Failed to create RootLoader");
        }
    }

    @FXML
    protected void handleSubmit() {
        String key = keyField.getText();
        if (key == null || key.trim().isEmpty()) {
            errorLabel.setText("Please enter a key");
            return;
        }

        progressIndicator.setVisible(true);
        KeyDecryptor.DecryptedInfo info = KeyDecryptor.getDecryptedInfo(key);
        if (info != null) {
            System.out.println("====================================");
            System.out.println("Decrypted Info: " + info.toString());
            System.out.println("Student ID from decryption: " + info.getStudentId());
            System.out.println("====================================");
            
            errorLabel.setText(info.toString());
            if (info.getStudent() != null) {
                proceedToMainWindow(info.getStudent());
            } else {
                progressIndicator.setVisible(false);
                errorLabel.setText("Student ID " + info.getStudentId() + " not found in database");
            }
        } else {
            progressIndicator.setVisible(false);
            errorLabel.setText("Invalid key. Please try again.");
            keyField.selectAll();
            keyField.requestFocus();
        }
    }
}
