package com.example.mediaplayerdemo;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.FileChooser;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;

public class PlayerController implements Initializable {
    private Media media;
    private ArrayList<Media> mediaList = new ArrayList<>();
    private int currentMediaIndex = 0;
    private MediaPlayer mediaPlayer;
    private boolean mediaPlaying;

    @FXML
    private MediaView mediaView;
    @FXML
    private Button btnPause, btnPlay, btnStop;
    @FXML
    private Label lblTime, lblSound;
    @FXML
    private Button btnPrevious, btnNext;
    @FXML
    private Label lblListOverview, lblFullScreen;
    @FXML
    private Slider sliderTime, sliderVolume;

    @FXML
    private VBox vboxParent;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        dbSorting.initializeDB("C:\\Users\\masth\\Desktop\\MediaPlayerDemo\\src\\main\\java\\MediaFilesFolder");
        showListControls(false);
        sliderVolume.setVisible(false);

        Image imagePause = new Image("C:\\Users\\masth\\Desktop\\MediaPlayerDemo\\src\\main\\java\\Icon\\pause.png");
        ImageView imageViewPause = new ImageView(imagePause);
        imageViewPause.setFitHeight(10);
        imageViewPause.setFitWidth(10);
        btnPause.setGraphic(imageViewPause);

        Image imagePlay = new Image("C:\\Users\\masth\\Desktop\\MediaPlayerDemo\\src\\main\\java\\Icon\\play.png");
        ImageView imageViewPlay = new ImageView(imagePlay);
        imageViewPlay.setFitHeight(10);
        imageViewPlay.setFitWidth(10);
        btnPlay.setGraphic(imageViewPlay);

        Image imageStop = new Image("C:\\Users\\masth\\Desktop\\MediaPlayerDemo\\src\\main\\java\\Icon\\stop.png");
        ImageView imageViewStop = new ImageView(imageStop);
        imageViewStop.setFitHeight(10);
        imageViewStop.setFitWidth(10);
        btnStop.setGraphic(imageViewStop);

        Image imagePrevious = new Image("C:\\Users\\masth\\Desktop\\MediaPlayerDemo\\src\\main\\java\\Icon\\previous.png");
        ImageView imageViewPrevious = new ImageView(imagePrevious);
        imageViewPrevious.setFitHeight(20);
        imageViewPrevious.setFitWidth(20);
        btnPrevious.setGraphic(imageViewPrevious);

        Image imageForward = new Image("C:\\Users\\masth\\Desktop\\MediaPlayerDemo\\src\\main\\java\\Icon\\forward.png");
        ImageView imageViewForward = new ImageView(imageForward);
        imageViewForward.setFitHeight(20);
        imageViewForward.setFitWidth(20);
        btnNext.setGraphic(imageViewForward);
    }

    @FXML
    private void onDriveOption() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select File");
        File selectedFile = fileChooser.showOpenDialog(null);
        if (selectedFile != null) {
            if (selectedFile.getName().toLowerCase().endsWith(".mp4")) {
                this.media = MediaFile.createMedia(selectedFile.getPath());
                setMediaView(this.media);
            }
        }
    };
    @FXML
    private void onFolderOption() {
        if (mediaPlaying) {
            stopMediaPlayer();
        }
        try {
            new SceneSwitch("FileView.fxml", "File Menu");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        media = MediaFile.getSharedObj().getMedia();
        if (media != null) {
            setMediaView(media);
            MediaFile.getSharedObj().resetObj();
        }
    }

    @FXML
    private void onPlaylistOption() {
        if (mediaPlaying) {
            stopMediaPlayer();
        }
        if (!mediaList.isEmpty()) {
            mediaList.clear();
        }
        try {
            new SceneSwitch("PlaylistView.fxml", "Playlist Menu");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (!Playlist.getSharedObj().getPlaylistFiles().isEmpty()) {
            ArrayList<MediaFile> converter = Playlist.getSharedObj().getPlaylistFiles();
            for (MediaFile mediaFile : converter) {
                Media newMedia = MediaFile.createMedia(mediaFile.getPath());
                mediaList.add(newMedia);
            }
            playPlaylist();
        }
    }

    @FXML
    void onPauseClick() {


        if (mediaPlaying) {
            mediaPlaying = false;
            mediaPlayer.pause();
        }
    }
    @FXML
    private void onPlayClick() {
        if (!mediaPlaying) {
            mediaPlaying = true;
            mediaPlayer.play();
        }
    }
    @FXML
    private void onStopClick() {
        stopMediaPlayer();
    }

    @FXML
    private void onSoundEnter() {
        sliderVolume.setVisible(true);
    }
    @FXML
    private void onSoundExit() {
        //sliderVolume.setVisible(false);
    }
    @FXML
    private void onPreviousClick() {}
    @FXML
    private void onNextClick() {}
    @FXML
    void onFullScreenClick() {}

    private void setMediaView(Media media) {
        mediaPlayer = new MediaPlayer(media);
        mediaView.setMediaPlayer(mediaPlayer);
        mediaPlayer.setAutoPlay(false);
        mediaPlaying = false;
        //Scene scene = vboxParent.getScene();
        //mediaView.fitWidthProperty().bind(scene.widthProperty());
        //mediaView.fitHeightProperty().bind(scene.heightProperty());

        setSliderTime();
    }

    private void setSliderTime() {
        mediaPlayer.currentTimeProperty().addListener(((observableValue, oldValue, newValue) -> {
            sliderTime.setValue(newValue.toSeconds());
            lblTime.setText("Time: " + (int)sliderTime.getValue() + "/" + (int)media.getDuration().toSeconds());
        }));

        mediaPlayer.setOnReady(() ->{
            Duration totalDuration = media.getDuration();
            sliderTime.setMax(totalDuration.toSeconds());
            lblTime.setText("Time: 00 /" + (int)media.getDuration().toSeconds());
        });
    }

    private void stopMediaPlayer() {
        mediaPlaying = false;
        mediaPlayer.stop();
    }
    private void playPlaylist() {
        showListControls(true);
        if (currentMediaIndex < mediaList.size()) {
            media = mediaList.get(currentMediaIndex);
            mediaPlayer = new MediaPlayer(media);
            mediaView.setMediaPlayer(mediaPlayer);
            mediaPlayer.setAutoPlay(true);
            mediaPlaying = true;
            lblListOverview.setText("Track: " + (currentMediaIndex + 1) + "/" + mediaList.size());
            currentMediaIndex++;
            mediaPlayer.setOnEndOfMedia(this::playPlaylist);
        }
    }

    private void showListControls(boolean showControls) {
        btnNext.setVisible(showControls);
        btnPrevious.setVisible(showControls);
        lblListOverview.setVisible(showControls);
    }

}
