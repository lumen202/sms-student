package sms.student.app.confirm;

import dev.sol.core.application.FXController;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import dev.finalproject.models.Student;

public class ConfirmController extends FXController {

    @FXML
    private Label nameLabel;
    @FXML
    private Label idLabel;
    @FXML
    private Label yearLabel;

    private Student student;
    private String schoolYear;
    private Runnable onConfirm;
    private Runnable onCancel;

    @Override
    protected void load_fields() {
        student = (Student) getParameter("STUDENT");
        schoolYear = (String) getParameter("SCHOOL_YEAR");
        onConfirm = (Runnable) getParameter("ON_CONFIRM");
        onCancel = (Runnable) getParameter("ON_CANCEL");

        nameLabel.setText(student.getFirstName() + " " + student.getLastName());
        idLabel.setText(String.valueOf(student.getStudentID()));
        yearLabel.setText(schoolYear);
    }

    @FXML
    private void handleConfirm() {
        Stage stage = (Stage) nameLabel.getScene().getWindow();
        stage.close();
        if (onConfirm != null) {
            onConfirm.run();
        }
    }

    @FXML
    private void handleCancel() {
        Stage stage = (Stage) nameLabel.getScene().getWindow();
        stage.close();
        if (onCancel != null) {
            onCancel.run();
        }
    }

    @Override
    protected void load_bindings() {
        // No bindings needed
    }

    @Override
    protected void load_listeners() {
        // No listeners needed
    }
}
