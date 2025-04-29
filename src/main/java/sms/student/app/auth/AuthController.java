package sms.student.app.auth;

import dev.sol.core.application.FXController;
import dev.sol.core.application.loader.FXLoaderFactory;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.scene.Scene;
import javafx.scene.Group;
import sms.student.util.KeyDecryptor;
import sms.student.util.QRScanner;
import dev.finalproject.models.Student;
import javafx.application.Platform;
import javafx.scene.control.ProgressIndicator;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.scene.image.ImageView;
import javafx.scene.control.Button;
import javafx.stage.FileChooser;
import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import javax.imageio.ImageIO;
import java.io.File;
import java.awt.image.BufferedImage;
import javafx.scene.input.KeyCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sms.student.App;
import sms.student.app.confirm.ConfirmLoader;
import sms.student.app.root.RootLoader;
import sms.student.util.JSONStorage;

public class AuthController extends FXController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @FXML
    private VBox root;
    @FXML
    private Label errorLabel;
    @FXML
    private Button scanButton;
    @FXML
    private Button uploadButton;
    private QRScanner qrScanner;
    private Stage webcamStage;

    @Override
    protected void load_fields() {
        errorLabel.setText("");
        setupButtons();
    }

    private void setupButtons() {
        // Remove default focus
        scanButton.setFocusTraversable(false);
        uploadButton.setFocusTraversable(false);
        
        scanButton.setOnAction(e -> initializeWebcamScanning());
        uploadButton.setOnAction(e -> handleFileUpload());
    }

    private void proceedToMainWindow(Student student) {
        Stage rootStage = new Stage();
        rootStage.initStyle(StageStyle.UNDECORATED);

        String studentId = String.valueOf(student.getStudentID());
        logger.info("Creating main window for student ID: {}", studentId);

        RootLoader rootLoader = (RootLoader) FXLoaderFactory
                .createInstance(RootLoader.class,
                        App.class.getResource("/sms/student/app/root/ROOT.fxml"))
                .addParameter("scene", new Scene(new Group()))
                .addParameter("OWNER", rootStage)
                .addParameter("STUDENT_ID", studentId)
                .initialize();

        rootStage.requestFocus();
        rootLoader.load();
        Platform.setImplicitExit(false);

        // Close current stage
        Stage currentStage = (Stage) root.getScene().getWindow();
        Platform.runLater(() -> currentStage.close());
    }

    private void initializeWebcamScanning() {
        qrScanner = new QRScanner();
        if (!qrScanner.isWebcamAvailable()) {
            errorLabel.setText("No webcam found");
            return;
        }

        webcamStage = new Stage();
        webcamStage.initStyle(StageStyle.UNDECORATED);

        ImageView webcamView = new ImageView();
        webcamView.setFitWidth(640);
        webcamView.setFitHeight(480);
        webcamView.imageProperty().bind(qrScanner.imageProperty());

        VBox webcamContainer = new VBox(10);
        webcamContainer.setAlignment(Pos.CENTER);
        webcamContainer.getChildren().addAll(
                new Label("Scanning QR Code..."),
                webcamView,
                new Label("Press ESC to cancel"));
        webcamContainer.setStyle("-fx-background-color: black; -fx-padding: 20;");

        Scene webcamScene = new Scene(webcamContainer);
        webcamScene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                stopWebcamScanning();
            }
        });

        webcamStage.setScene(webcamScene);
        webcamStage.show();

        qrScanner.startScanning(this::processQRCode);
    }

    private void stopWebcamScanning() {
        if (qrScanner != null) {
            qrScanner.stopScanning();
        }
        if (webcamStage != null) {
            webcamStage.close();
        }
    }

    private void processQRCode(String qrText) {
        logger.info("Processing QR code");
        Platform.runLater(() -> {
            stopWebcamScanning();

            KeyDecryptor.DecryptedInfo info = KeyDecryptor.getDecryptedInfo(qrText);
            if (info != null && info.getStudent() != null) {
                // Save student data to JSON before showing confirmation
                JSONStorage.saveStudentData(info.getStudent());
                showConfirmationDialog(info.getStudent(), info.getSchoolYear());
            } else {
                errorLabel.setText("Invalid QR code");
            }
        });
    }

    private void showConfirmationDialog(Student student, String schoolYear) {
        try {
            Stage confirmStage = new Stage();
            confirmStage.initOwner(root.getScene().getWindow());
            confirmStage.initStyle(StageStyle.UNDECORATED);

            ConfirmLoader loader = (ConfirmLoader) FXLoaderFactory
                    .createInstance(ConfirmLoader.class,
                            getClass().getResource("/sms/student/app/confirm/CONFIRM.fxml"))
                    .addParameter("STUDENT", student)
                    .addParameter("SCHOOL_YEAR", schoolYear)
                    .addParameter("ON_CONFIRM", (Runnable) () -> proceedToMainWindow(student))
                    .addParameter("ON_CANCEL", (Runnable) () -> errorLabel.setText(""))
                    .addParameter("OWNER", confirmStage)
                    .initialize();

            loader.load();

        } catch (Exception e) {
            logger.error("Error showing confirmation dialog", e);
            errorLabel.setText("Error showing confirmation dialog");
        }
    }

    private void handleFileUpload() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select QR Code Image");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));

        File file = fileChooser.showOpenDialog(root.getScene().getWindow());
        if (file != null) {
            try {
                BufferedImage bufferedImage = ImageIO.read(file);
                LuminanceSource source = new BufferedImageLuminanceSource(bufferedImage);
                BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

                Result result = new MultiFormatReader().decode(bitmap);
                if (result != null) {
                    processQRCode(result.getText());
                }
            } catch (Exception e) {
                logger.error("Error reading QR code from file", e);
                errorLabel.setText("Could not read QR code from image");
            }
        }
    }

    public void cleanup() {
        stopWebcamScanning();
    }

    @Override
    protected void load_bindings() {
        // No bindings to load
    }

    @Override
    protected void load_listeners() {
        // No listeners to load
    }
}
