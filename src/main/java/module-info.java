module sms.student {

    requires transitive javafx.controls;
    requires transitive core.fx;
    requires transitive core.db;
    requires transitive dev.finalproject;
    requires transitive java.desktop;

    requires javafx.fxml;
    requires atlantafx.base;
    requires javafx.graphics;
    requires org.kordamp.ikonli.core;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.materialdesign2;
    requires org.kordamp.ikonli.materialdesign;

    requires core.util;
    requires java.sql.rowset;
    requires javafx.base;

    opens sms.student to javafx.fxml, core.fx;
    opens sms.student.app to javafx.fxml, core.fx;

    exports sms.student;
    exports sms.student.app;
}
