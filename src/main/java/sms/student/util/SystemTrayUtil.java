package sms.student.util;

import java.awt.AWTException;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;

import javafx.application.Platform;
import javafx.stage.Stage;

public class SystemTrayUtil {
    private static SystemTrayUtil instance;
    private TrayIcon trayIcon;
    private PopupMenu popup;
    private MenuItem showItem;
    private MenuItem hideItem;
    private MenuItem exitItem;

    private SystemTrayUtil() {}

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
            BufferedImage image = ImageIO.read(getClass().getResourceAsStream("/sms/student/app/icons/logo.png"));

            popup = new PopupMenu();
            showItem = new MenuItem("Show");
            hideItem = new MenuItem("Hide");
            exitItem = new MenuItem("Exit");

            trayIcon = new TrayIcon(image, "Student Attendance", popup);
            trayIcon.setImageAutoSize(true);

            setupMenuItems(stage, cleanupAction);
            tray.add(trayIcon);

            stage.setOnCloseRequest(event -> {
                event.consume();
                minimizeToTray(stage);
            });
        } catch (AWTException | IOException e) {
            System.err.println("Error setting up system tray: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void setupMenuItems(Stage stage, Runnable cleanupAction) {
        showItem.addActionListener(e -> Platform.runLater(() -> {
            StageUtil.showStage(stage);
            updatePopupMenu(stage);
        }));
        
        hideItem.addActionListener(e -> Platform.runLater(() -> {
            minimizeToTray(stage);
            updatePopupMenu(stage);
        }));
        
        exitItem.addActionListener(e -> Platform.runLater(() -> {
            cleanupAction.run();
            Platform.exit();
            SystemTray.getSystemTray().remove(trayIcon);
            System.exit(0);
        }));

        trayIcon.addActionListener(e -> Platform.runLater(() -> {
            StageUtil.showStage(stage);
            updatePopupMenu(stage);
        }));
        
        trayIcon.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
                if (e.isPopupTrigger()) {
                    updatePopupMenu(stage);
                }
            }
        });

        updatePopupMenu(stage);
    }

    private void updatePopupMenu(Stage stage) {
        popup.removeAll();
        boolean isShowing = stage.isShowing() && !stage.isIconified();
        if (isShowing) {
            popup.add(hideItem);
        } else {
            popup.add(showItem);
        }
        popup.addSeparator();
        popup.add(exitItem);
    }

    private void minimizeToTray(Stage stage) {
        if (stage != null) {
            stage.hide();
            updatePopupMenu(stage);
        }
    }

    public void cleanup() {
        if (SystemTray.isSupported() && trayIcon != null) {
            SystemTray.getSystemTray().remove(trayIcon);
        }
    }
}
