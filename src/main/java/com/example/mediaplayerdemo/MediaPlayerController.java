package com.example.mediaplayerdemo;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class MediaPlayerController implements Initializable {
    private Media media;
    private MediaPlayer mediaPlayer;
    private boolean isMediaPlaying;

    @FXML
    private Button btnPlay;
    @FXML
    private Slider sldTime;
    @FXML
    private Label lblTime;

    @FXML
    private MediaView mediaView;
    @FXML
    private VBox vbox1;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        dbSorting.initializeDB("C:\\Users\\au1wp\\Desktop\\MediaFiles");
    }

    @FXML
    private void onLibraryOption() throws IOException {
        try {
            new SceneSwitch("FileView.fxml", "Select File");
        } catch (Exception e) {
            e.printStackTrace();
        }
        media = MediaFile.getSharedObj().getMedia();
        if (media != null) {
            setMediaView(media);
            MediaFile.getSharedObj().resetObj();
        }
    }

    @FXML
    private void onPlaylistOption() {
        try {
            new SceneSwitch("PlaylistView.fxml", "Playlist");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        MediaFile.getSharedObj().resetObj();
    }

    @FXML
    private void onDriveOption() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Media");
        File selectedFile = fileChooser.showOpenDialog(null);
        if (selectedFile != null) {
            setMediaView(media);
            setSlider();

            Scene scene = vbox1.getScene();
            mediaView.fitWidthProperty().bind(scene.heightProperty());
            mediaView.fitHeightProperty().bind(scene.heightProperty());
        }
    }

    @FXML
    void btnPlayClick() {
        if (isMediaPlaying) {
            btnPlay.setText("Play");
            mediaPlayer.pause();
            isMediaPlaying = false;
        } else {
            btnPlay.setText("Pause");
            mediaPlayer.play();
            isMediaPlaying = true;
        }
    }

    @FXML
    void btnStopClick() {
        btnPlay.setText("Play");
        mediaPlayer.stop();
        isMediaPlaying = false;
    }

    @FXML
    void sldTimePress() {
        mediaPlayer.seek(Duration.seconds(sldTime.getValue()));
    }

    private void setMediaView(Media media) {
        mediaPlayer = new MediaPlayer(media);
        mediaView.setMediaPlayer(mediaPlayer);
        mediaPlayer.setAutoPlay(false);
        isMediaPlaying = false;
    }

    private void setSlider() {
        mediaPlayer.currentTimeProperty().addListener(((observableValue, oldValue, newValue) -> {
            sldTime.setValue(newValue.toSeconds());
            lblTime.setText("Time: " + (int)sldTime.getValue() + "/" + (int)media.getDuration().toSeconds());
        }));

        mediaPlayer.setOnReady(() ->{
            Duration totalDuration = media.getDuration();
            sldTime.setMax(totalDuration.toSeconds());
            lblTime.setText("Time: 00 /" + (int)media.getDuration().toSeconds());
        });
    }
}