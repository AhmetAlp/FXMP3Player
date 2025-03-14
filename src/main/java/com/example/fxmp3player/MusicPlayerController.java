package com.example.fxmp3player;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.TemporalAccessor;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import com.mpatric.mp3agic.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.json.JSONObject;

import static java.time.temporal.ChronoField.*;

public class MusicPlayerController {
    @FXML
    private Label chooseMusic;

    @FXML
    private Slider musicSlider;

    @FXML
    private ScrollPane sp;


    private TextFlow lyricsTextFlow;

    private MediaPlayer mPlayer;
    private Mp3File mp3file;

    private final TreeMap<LocalTime, String> lyricsMap = new TreeMap<>();

    private File selectedFile;

    ScheduledExecutorService progressBarScheduler;
    ScheduledExecutorService lyricsScheduler;

    @FXML
    protected void onChooseMusic(MouseEvent event) throws IOException {
        if (event.getButton() == MouseButton.PRIMARY) {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Select your music");
            selectedFile = chooser.showOpenDialog(null);
            if (selectedFile != null) {
                chooseMusic.setText(selectedFile.getName());
                try {
                    mp3file = new Mp3File(selectedFile);
                } catch (InvalidDataException e) {
                    throw new RuntimeException(e);
                } catch (UnsupportedTagException e) {
                    throw new RuntimeException(e);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            lyricsTextFlow = new TextFlow();
            lyricsTextFlow.setMinHeight(100);
            lyricsTextFlow.setMinWidth(300);
            lyricsTextFlow.setStyle("-fx-background-color: dimgray;");
            sp.setContent(lyricsTextFlow);
        } else if (event.getButton() == MouseButton.SECONDARY) {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("track-info.fxml"));
            Parent root = fxmlLoader.load();
            TrackInfoController tic = fxmlLoader.getController();
            tic.setMp3file(mp3file);
            Scene scene = new Scene(root);
            Stage stage = new Stage();
            stage.setScene(scene);
            stage.setTitle("Edit Track Information");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.show();
        }
    }

    @FXML
    protected void onPlayClick() {;
        if (mp3file != null && mp3file.hasId3v2Tag()) {
            if (mp3file.getId3v2Tag().getLyrics().isEmpty()) {
                try {
                    URL url = new URL("https://lrclib.net/api/get?artist_name=" +
                                        URLEncoder.encode(mp3file.getId3v2Tag().getArtist()) +
                                        "&track_name=" +
                                        URLEncoder.encode(mp3file.getId3v2Tag().getTitle()));
                    JSONObject jsonObject = LyricRestController.get(url);
                    int lyricId = jsonObject.getInt("id");
                    url = new URL("https://lrclib.net/api/get/" + lyricId);
                    jsonObject = LyricRestController.get(url);
                    mp3file.getId3v2Tag().setLyrics(jsonObject.getString("syncedLyrics"));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            parseLyrics(mp3file.getId3v2Tag().getLyrics());
        }
        if (mPlayer == null && selectedFile != null) {
            Media media = new Media(selectedFile.toURI().toString());
            mPlayer = new MediaPlayer(media);
            mPlayer.setOnReady(() -> chooseMusic.setText(selectedFile.getName()));
        }
        if (mPlayer != null) {
            mPlayer.play();
            if (progressBarScheduler != null && !progressBarScheduler.isShutdown()) {
                progressBarScheduler.shutdown();
            }
            if (lyricsScheduler != null && !lyricsScheduler.isShutdown()) {
                lyricsScheduler.shutdown();
            }
            progressBarScheduler = Executors.newSingleThreadScheduledExecutor();
            lyricsScheduler = Executors.newSingleThreadScheduledExecutor();
            progressBarScheduler.scheduleAtFixedRate(
                    () -> musicSlider.setValue(mPlayer.getCurrentTime().toMillis() / mPlayer.getTotalDuration().toMillis() * 100),
                    2, 1, TimeUnit.SECONDS);
            lyricsScheduler.scheduleAtFixedRate(
                    () ->
                            Platform.runLater(() -> {
                                boolean lineFound = false;
                                LocalTime mPlayerTime = Instant.ofEpochMilli((long) mPlayer.getCurrentTime().toMillis()).atZone(ZoneId.of("UTC")).toLocalTime();
                                lineFound = showCurrentLineLyric(mPlayerTime, 25, 2, 16);
                                if (!lineFound) {
                                    lineFound = showCurrentLineLyric(mPlayerTime, 25, 3, 16);
                                }
                            }), 2, 1, TimeUnit.SECONDS);
        }
    }

    private boolean showCurrentLineLyric(LocalTime mPlayerTime, int totalLinesSecs, int intervalSeconds, int fontSize) {
        boolean lineFound = false;
        lyricsTextFlow.getChildren().clear();
        for (Map.Entry<LocalTime, String> e : lyricsMap.entrySet()) {
            long secondInterval = Duration.between(e.getKey(), mPlayerTime).getSeconds();
            if (Math.abs(secondInterval) < totalLinesSecs) {
                Text text1 = new Text(e.getValue() + "\n");
                text1.setFont(new Font(fontSize));
                if (Math.abs(secondInterval) < intervalSeconds) {
                    lineFound = true;
                    text1.setFill(Color.WHITE);
                } else {
                    text1.setFill(Color.BLACK);
                }
                lyricsTextFlow.getChildren().add(text1);
            }
        }
        return lineFound;
    }

    @FXML
    protected void onPauseClick() {;
        mPlayer.pause();
        progressBarScheduler.shutdown();
        lyricsScheduler.shutdown();
    }

    @FXML
    protected void onStopClick() {;
        mPlayer.stop();
        mPlayer.dispose();
        mPlayer = null;
        System.gc();
        progressBarScheduler.shutdown();
        lyricsScheduler.shutdown();
    }

    private void parseLyrics(String lyrics) {
        DateTimeFormatter format = new DateTimeFormatterBuilder().parseCaseInsensitive().parseLenient().optionalStart()
                .appendValue(MINUTE_OF_DAY, 2).appendLiteral(':').optionalEnd().appendValue(SECOND_OF_MINUTE, 2)
                .optionalStart().appendFraction(NANO_OF_SECOND, 0, 9, true).optionalEnd().toFormatter()
                .withZone(ZoneOffset.UTC);

        lyrics.lines().map(line -> line.trim())
                // Only read lines that are time stamp lines
                .filter(line -> !line.endsWith("]"))
                .forEachOrdered(line -> {
                    if (line.startsWith("[")) {
                        String[] parts = line.split("]");
                        if (parts.length > 1) {
                            // Trim the starting [
                            String right = parts[0].substring(1);
                            String lyric = parts[parts.length - 1].trim();

                            TemporalAccessor temp = format.parse(right);
                            LocalTime time = LocalTime.from(temp);
                            lyricsMap.put(time, lyric);
                        }
                    }
                    // All LRC lines must start with [, so anything else is an error
                });
    }
}