module sms.student {
    requires transitive javafx.controls;
    requires transitive core.fx;
    requires transitive core.db;
    requires transitive dev.finalproject;
    requires transitive java.desktop;
    requires transitive javafx.swing;
    requires transitive com.google.zxing;
    requires transitive com.google.zxing.javase;
    requires transitive opencv;

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
    requires bridj;
    requires org.slf4j;
    requires com.google.gson;
    requires json.simple;
    requires jakarta.json;

    opens sms.student to javafx.fxml, core.fx;
    opens sms.student.app.auth to javafx.fxml, core.fx;
    opens sms.student.app.confirm to javafx.fxml, core.fx;
    opens sms.student.app.root to javafx.fxml, core.fx;
    opens sms.student.util to javafx.fxml, core.fx, jakarta.json;

    exports sms.student;
    exports sms.student.util;
}