package sms.student.util;

import java.awt.AWTException;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

import javafx.application.Platform;
import javafx.stage.Stage;

public class SystemTrayUtil {
    private static SystemTrayUtil instance;
    private TrayIcon trayIcon;
    private PopupMenu popup;
    private MenuItem showItem;
    private MenuItem hideItem;
    private MenuItem exitItem;
    private final AtomicBoolean actionInProgress = new AtomicBoolean(false);
    private static final long DEBOUNCE_TIME = 500; // 500ms debounce
    private long lastActionTime = 0;

    private SystemTrayUtil() {
    }

    public static SystemTrayUtil getInstance() {
        if (instance == null) {
            instance = new SystemTrayUtil();
        }
        return instance;
    }

    public void setupTray(Stage stage, Runnable cleanupAction) {
        if (!SystemTray.isSupported()) {
            System.out.println("SystemTray is not supported");
            return;
        }

        try {
            SystemTray tray = SystemTray.getSystemTray();

            // Try multiple paths for the logo
            BufferedImage image = null;
            String[] possiblePaths = {
                    "/sms/student/app/assets/img/logo.png",
                    "/sms/student/assets/img/logo.png",
                    "/sms/student/app/icons/logo.png"
            };

            for (String path : possiblePaths) {
                try {
                    var resourceStream = getClass().getResourceAsStream(path);
                    if (resourceStream != null) {
                        image = ImageIO.read(resourceStream);
                        System.out.println("Found logo at: " + path);
                        break;
                    }
                } catch (Exception e) {
                    System.out.println("Could not load logo from: " + path);
                }
            }

            // If no logo found, create a default image
            if (image == null) {
                image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
                System.out.println("Using default image for tray icon");
            }

            popup = new PopupMenu();
            showItem = new MenuItem("Show");
            hideItem = new MenuItem("Hide");
            exitItem = new MenuItem("Exit");

            trayIcon = new TrayIcon(image, "Student Attendance", popup);
            trayIcon.setImageAutoSize(true);

            setupMenuItems(stage, cleanupAction);
            tray.add(trayIcon);

            // Add listeners for stage state changes
            stage.iconifiedProperty()
                    .addListener((obs, oldVal, newVal) -> SwingUtilities.invokeLater(() -> updatePopupMenu(stage)));
            stage.showingProperty()
                    .addListener((obs, oldVal, newVal) -> SwingUtilities.invokeLater(() -> updatePopupMenu(stage)));

            stage.setOnCloseRequest(event -> {
                event.consume();
                minimizeToTray(stage);
            });
        } catch (AWTException e) {
            System.err.println("Error setting up system tray: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean shouldDebounce() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastActionTime < DEBOUNCE_TIME) {
            System.out.println("Debouncing action");
            return true;
        }
        lastActionTime = currentTime;
        return false;
    }

    private void performStageAction(Stage stage, Runnable action) {
        if (shouldDebounce() || !actionInProgress.compareAndSet(false, true)) {
            System.out.println("Action in progress or debounced");
            return;
        }

        SwingUtilities.invokeLater(() -> {
            try {
                Platform.runLater(() -> {
                    try {
                        action.run();
                    } finally {
                        actionInProgress.set(false);
                    }
                });
            } catch (Exception e) {
                actionInProgress.set(false);
                e.printStackTrace();
            }
        });
    }

    private void setupMenuItems(Stage stage, Runnable cleanupAction) {
        showItem.addActionListener(e -> {
            System.out.println("Show menu item clicked");
            StageUtil.showStage(stage);
            updatePopupMenu(stage);
        });

        hideItem.addActionListener(e -> {
            System.out.println("Hide menu item clicked");
            StageUtil.hideStage(stage);
            updatePopupMenu(stage);
        });

        exitItem.addActionListener(e -> {
            System.out.println("Exit menu item clicked");
            cleanupApplication(cleanupAction);
        });

        trayIcon.addActionListener(e -> {
            System.out.println("Tray icon double-clicked");
            StageUtil.showStage(stage);
            updatePopupMenu(stage);
        });

        updatePopupMenu(stage);
    }

    private void cleanupApplication(Runnable cleanupAction) {
        Platform.runLater(() -> {
            System.out.println("Running exit sequence");
            cleanupAction.run();
            Platform.exit();
            SystemTray.getSystemTray().remove(trayIcon);
            System.exit(0);
        });
    }

    private void hideStage(Stage stage) {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> hideStage(stage));
            return;
        }
        System.out.println("Hiding stage");
        stage.hide();
        updatePopupMenu(stage);
    }

    public void showStage(Stage stage) {
        Platform.runLater(() -> {
            System.out.println("=== Show Stage Debug ===");
            stage.show();
            stage.setIconified(false);
            stage.toFront();
            stage.setAlwaysOnTop(true);
            stage.requestFocus();

            // Reset alwaysOnTop after showing
            Platform.runLater(() -> {
                stage.setAlwaysOnTop(false);
                updatePopupMenu(stage);
            });

            System.out.println("Stage shown and focused");
            System.out.println("=== End Show Stage Debug ===");
        });
    }

    private void updatePopupMenu(Stage stage) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> updatePopupMenu(stage));
            return;
        }

        Platform.runLater(() -> {
            boolean isShowing = stage.isShowing() && !stage.isIconified();
            SwingUtilities.invokeLater(() -> {
                popup.removeAll();
                if (isShowing) {
                    popup.add(hideItem);
                } else {
                    popup.add(showItem);
                }
                popup.addSeparator();
                popup.add(exitItem);
            });
        });
    }

    private void minimizeToTray(Stage stage) {
        StageUtil.hideStage(stage);
        updatePopupMenu(stage);
    }

    public void cleanup() {
        if (SystemTray.isSupported() && trayIcon != null) {
            SystemTray.getSystemTray().remove(trayIcon);
        }
    }
}
