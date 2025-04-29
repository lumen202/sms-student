package sms.student.util;

import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.image.Image;
import nu.pattern.OpenCV;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.videoio.VideoCapture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.concurrent.atomic.AtomicBoolean;

public class QRScanner {
    private static final Logger logger = LoggerFactory.getLogger(QRScanner.class);
    private VideoCapture capture;
    private final ObjectProperty<Image> imageProperty = new SimpleObjectProperty<>();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread captureThread;

    static {
        OpenCV.loadLocally();
    }

    public QRScanner() {
        initializeWebcam();
    }

    private void initializeWebcam() {
        try {
            capture = new VideoCapture(0);
            if (!capture.isOpened()) {
                logger.warn("No webcam found");
                capture.release();
                capture = null;
            } else {
                logger.info("Webcam initialized successfully");
            }
        } catch (Exception e) {
            logger.error("Error initializing webcam", e);
            if (capture != null) {
                capture.release();
                capture = null;
            }
        }
    }

    public void startScanning(QRCodeCallback callback) {
        if (capture == null || !capture.isOpened())
            return;

        running.set(true);
        captureThread = new Thread(() -> {
            Mat frame = new Mat();
            MatOfByte buffer = new MatOfByte();

            while (running.get()) {
                try {
                    if (capture.read(frame)) {
                        Imgcodecs.imencode(".png", frame, buffer);
                        Image fxImage = new Image(new ByteArrayInputStream(buffer.toArray()));
                        Platform.runLater(() -> imageProperty.set(fxImage));

                        // Try to decode QR code
                        BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(buffer.toArray()));
                        LuminanceSource source = new BufferedImageLuminanceSource(bufferedImage);
                        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

                        try {
                            Result result = new MultiFormatReader().decode(bitmap);
                            if (result != null) {
                                Platform.runLater(() -> callback.onQRCodeDetected(result.getText()));
                                stopScanning();
                            }
                        } catch (NotFoundException ignored) {
                            // No QR code found in this frame
                        }
                    }
                    Thread.sleep(33); // ~30 FPS
                } catch (Exception e) {
                    logger.error("Error during scanning", e);
                }
            }
            frame.release();
            buffer.release();
        });
        captureThread.setDaemon(true);
        captureThread.start();
    }

    public void stopScanning() {
        running.set(false);
        if (captureThread != null) {
            captureThread.interrupt();
        }
        if (capture != null && capture.isOpened()) {
            capture.release();
        }
    }

    public ObjectProperty<Image> imageProperty() {
        return imageProperty;
    }

    public interface QRCodeCallback {
        void onQRCodeDetected(String qrText);
    }

    public boolean isWebcamAvailable() {
        return capture != null && capture.isOpened();
    }
}