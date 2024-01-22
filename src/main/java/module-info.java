module com.example.mediaplayerdemo {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;
    requires isoparser;

    requires org.controlsfx.controls;
    requires org.kordamp.bootstrapfx.core;
    requires java.sql;

    opens com.example.mediaplayerdemo to javafx.fxml;
    exports com.example.mediaplayerdemo;
}