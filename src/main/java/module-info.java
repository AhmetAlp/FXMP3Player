module com.example.fxmp3player {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;
    requires mp3agic;
    requires org.json;


    opens com.example.fxmp3player to javafx.fxml;
    exports com.example.fxmp3player;
}