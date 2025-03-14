package com.example.fxmp3player;

import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.ID3v24Tag;
import com.mpatric.mp3agic.Mp3File;
import com.mpatric.mp3agic.NotSupportedException;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class TrackInfoController {
    @FXML
    private TextField titleText;
    @FXML
    private TextField artistText;
    @FXML
    private TextField  albumText;
    @FXML
    private TextArea lyricsText;
    @FXML
    private TextField trackText;
    @FXML
    private TextField discText;
    @FXML
    private TextField yearText;
    @FXML
    private Button saveButton;
    @FXML
    private Button discardButton;

    private Mp3File mp3file;

    public void setMp3file(Mp3File mp3file) {
        if (mp3file != null) {
            ID3v2 id3v2 = mp3file.getId3v2Tag();
            this.mp3file = mp3file;
            this.titleText.setText(id3v2.getTitle());
            this.artistText.setText(id3v2.getArtist());
            this.trackText.setText(id3v2.getTrack());
            this.albumText.setText(id3v2.getAlbum());
            this.yearText.setText(id3v2.getYear());
            this.lyricsText.setText(id3v2.getLyrics());
        }
    }

    @FXML
    public void close(ActionEvent event) {
        Stage stage = (Stage) discardButton.getScene().getWindow();
        stage.close();
    }

    @FXML
    public void save(ActionEvent event) {
        if (mp3file != null) {
            ID3v2 id3v2 = mp3file.getId3v2Tag();
            id3v2.setTitle(this.titleText.getText());
            id3v2.setArtist(this.artistText.getText());
            id3v2.setTrack(this.trackText.getText());
            id3v2.setAlbum(this.albumText.getText());
            id3v2.setYear(this.yearText.getText());
            id3v2.setLyrics(this.lyricsText.getText());
        }
        try {
            mp3file.save(mp3file.getFilename()+1);
            Files.move(Path.of(mp3file.getFilename()+1), Path.of(mp3file.getFilename()), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException | NotSupportedException e) {
            e.printStackTrace();
        }
        close(event);
    }
}
