package com.example.mediaplayerdemo;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;

public class PlayerController implements Initializable {
    //region FXML annotations
    @FXML
    private VBox vboxParent;
    @FXML
    public BorderPane borderPane;
    @FXML
    private Slider sliderTime, sliderVolume;
    @FXML
    private Label lblTime;
    @FXML
    private Label lblListOverview;
    @FXML
    private Button btnPause, btnPlay, btnStop;
    @FXML
    private Button btnPrevious, btnNext;
    @FXML
    public Button btnFullscreen;
    @FXML
    private MediaView mediaView;
    //endregion
    //region instance variables
    private MediaPlayer mediaPlayer;
    private Media media;
    private ArrayList<Media> mediaList = new ArrayList<>();
    private int mediaListIndex = 0;
    private boolean mediaPlaying = false;

    //endregion
    //region initialize

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        dbSorting.initializeDB("src/main/java/MediaFilesFolder"); // assign the path to the media folder
        initializeVariables(); // initialise further variables
        setIconImages(); // load all the images in place of buttons

        Platform.runLater(() -> { // use runLater to introduce wait, so it does not try to grab something that is not loaded yet
            Stage stage = (Stage) vboxParent.getScene().getWindow(); // assign the stage
            borderPane.setOnKeyPressed(this::handleKeyPressPlayPause); // integrate the keypress for play/pause
            btnFullscreen.setOnAction(event -> onFullScreenClick()); // integrate the fullscreen button event
            setupKeyEventHandler(stage); // integrate the fullscreen enter/exit key bind
        });
    }
    //endregion
    //region control handlers

    /**
     * When you select the choose from drive option in the scene, a new scene opens allowing you to select a file.
     * If the selected file is not .mp4 it is rejected.
     */
    @FXML
    private void onDriveOption() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select File");
        File selectedFile = fileChooser.showOpenDialog(null);
        if (selectedFile != null) {
            if (selectedFile.getName().toLowerCase().endsWith(".mp4")) {
                this.media = MediaFile.createMedia(selectedFile.getPath());
                playMedia(false);
            }
        }
    }


    /**
     * Alternatively, when you click choose from folder, it loads a new scene for further handling.
     */
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
        if (MediaFile.getSharedObj().getMedia() != null) {
            this.media = MediaFile.getSharedObj().getMedia();
            playMedia(false);
            showListControls(false);
        }
    }

    /**
     * When you choose the playlist option, it loads a new scene for handling playlists.
     * If you have chosen a playlist, and the scene is exited, it will play, otherwise not.
     */
    @FXML
    private void onPlaylistOption() {
        if (mediaPlaying) {
            stopMediaPlayer();
        }
        try {
            new SceneSwitch("PlaylistView.fxml", "Playlist Menu");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (!Playlist.getSharedObj().getPlaylistFiles().isEmpty()) {
            mediaList = Playlist.getSharedObj().getPlaylistFiles();
            mediaListIndex = 0;
            playPlaylist();
        }
    }

    /**
     * Allows the user to interact and skip around in the media using the slider controller.
     */
    @FXML
    private void onSliderTimePressed() {
        if (mediaPlayer != null) {
            double sliderValue = sliderTime.getValue();
            Duration seekTime = Duration.seconds(sliderValue);
            mediaPlayer.seek(seekTime);
        }
    }

    /**
     * Handles pausing the media when the pause button is clicked.
     */
    @FXML
    void onPauseClick() {
        if (mediaPlaying) {
            mediaPlaying = false;
            mediaPlayer.pause();
        }
    }

    /**
     * Handles playing the media when the play button is clicked.
     */
    @FXML
    private void onPlayClick() {
        if (!mediaPlaying) {
            mediaPlaying = true;
            mediaPlayer.play();
        }
    }

    /**
     * Handles stopping the media when the stop button is clicked.
     */
    @FXML
    private void onStopClick() {
        stopMediaPlayer();
    }

    /**
     * Handles making the sound slider visible when hovering near the sound icon.
     */
    @FXML
    private void onSoundEnter() {
        sliderVolume.setVisible(true);
    }

    /**
     * Handles hiding the sound slider when no longer hovering near the sound icon.
     */
    @FXML
    private void onSoundExit() {
        sliderVolume.setVisible(false);
    }

    /**
     * Handles displaying the sound slider controller after making it visible by hovering over the icon.
     */
    @FXML
    private void onSliderVolumeEnter() {
        sliderVolume.setVisible(true);
    }

    /**
     * Handles hiding the sound slider controller after making it visible by hovering over the icon when leaving it.
     */
    @FXML
    private void onSliderVolumeExit() {
        sliderVolume.setVisible(false);
    }

    /**
     * Handles going to the previous track in the playlist using the button controller.
     */
    @FXML
    private void onPreviousClick() {
        if (mediaListIndex > 0) {
            stopMediaPlayer();
            mediaListIndex --;
            playPlaylist();
        } else if (mediaListIndex == 0) {
            stopMediaPlayer();
            mediaListIndex = mediaList.size() - 1;
            playPlaylist();
        }
    }

    /**
     * Handles going to the next track in the playlist when the button controller is pressed.
     */
    @FXML
    private void onNextClick() {
        if (mediaListIndex < mediaList.size() - 1) {
            stopMediaPlayer();
            mediaListIndex++;
            playPlaylist();
        } else if (mediaListIndex == mediaList.size() - 1) {
            stopMediaPlayer();
            mediaListIndex = 0;
            playPlaylist();
        }
    }

    /**
     * Handles entering fullscreen when the controller button for it is clicked.
     */
    @FXML
    private void onFullScreenClick() {
        Stage stage = (Stage) vboxParent.getScene().getWindow();
        toggleFullScreen(stage);
    }

    //endregion
    //region additional assisting methods

    /**
     * Creates the media you have selected and enables it.
     * It enables a single media file for playing.
     * @param autoPlay a flag to allow the media to start automatically once a media is selected.
     */
    private void playMedia(boolean autoPlay) {
        mediaPlayer = new MediaPlayer(media);
        mediaView.setMediaPlayer(mediaPlayer);
        setSliderTime();
        setSliderVolume();
        mediaPlayer.setAutoPlay(autoPlay);
        mediaPlaying = autoPlay;
        Scene mediaScene = borderPane.getCenter().getScene();
        double width = mediaScene.getWidth();
        double height = mediaScene.getHeight() - 100;
        mediaView.setFitWidth(width);
        mediaView.setFitHeight(height);
    }


    /**
     * Start the selected playlist. Playing the media in the order it is assigned in the playlist.
     * It prepares the media array for playing.
     */
    private void playPlaylist() {
        showListControls(true);
        if (mediaListIndex == 0) {
            media = mediaList.get(mediaListIndex);
            playMedia(false);
            lblListOverview.setText("Track: " + (mediaListIndex + 1) + "/" + mediaList.size());
            mediaPlayer.setOnEndOfMedia(() -> {
                mediaListIndex++;
                playPlaylist();
            });
        } else if (mediaListIndex < mediaList.size()) {
            media = mediaList.get(mediaListIndex);
            playMedia(true);
            lblListOverview.setText("Track: " + (mediaListIndex + 1) + "/" + mediaList.size());
            mediaPlayer.setOnEndOfMedia(() -> {
                mediaListIndex++;
                playPlaylist();
            });
        } else if (mediaListIndex == mediaList.size()) {
            mediaListIndex = 0;
            media = mediaList.get(mediaListIndex);
            playMedia(false);
            lblListOverview.setText("Track: " + (mediaListIndex + 1) + "/" + mediaList.size());
            mediaPlayer.setOnEndOfMedia(() -> {
                mediaListIndex++;
                playPlaylist();
            });
        }
    }

    /**
     * Handles stopping the current media from playing.
     */
    private void stopMediaPlayer() {
        mediaPlaying = false;
        mediaPlayer.stop();
    }

    /**
     * The pause/play key bind function.
     * The code that is triggered on space/P is to stop, or if already stopped; resume the media.
     * @param event the keypress event. Listens for when a key with a certain keycode is pressed to trigger the code.
     */
    private void handleKeyPressPlayPause(KeyEvent event) {
        if (event.getCode() == KeyCode.P || event.getCode() == KeyCode.SPACE) {
            if (mediaPlaying) {
                mediaPlaying = false;
                mediaPlayer.pause();
            } else {
                mediaPlaying = true;
                mediaPlayer.play();
            }
        }
    }

    /**
     * Handles entering and exiting fullscreen using the "F" or "escape" key binds.
     * @param stage this is simply a parameter for the stage itself, the program.
     */
    private void setupKeyEventHandler(Stage stage) {
        stage.getScene().setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.F && !stage.isFullScreen()) {
                stage.setFullScreen(true);
                hideControls();
            } else if (event.getCode() == KeyCode.ESCAPE && stage.isFullScreen() || event.getCode() == KeyCode.F && stage.isFullScreen()) {
                stage.setFullScreen(false);
                showControls();
            }
        });
    }


    /**
     * Initialises the duration slider of the media. This is the slider controller that tracks the media's duration.
     */
    private void setSliderTime() {
        mediaPlayer.currentTimeProperty().addListener(((observableValue, oldValue, newValue) -> {
            sliderTime.setValue(newValue.toSeconds());
            lblTime.setText("Duration: " + (int)sliderTime.getValue() + ":" + (int)media.getDuration().toSeconds());
        }));

        mediaPlayer.setOnReady(() ->{
            Duration totalDuration = media.getDuration();
            sliderTime.setMax(totalDuration.toSeconds());
            lblTime.setText("Duration: 00:" + (int)media.getDuration().toSeconds());
        });
    }


    /**
     * Initialises the volume slider controller.
     */
    private void setSliderVolume() {
        sliderVolume.valueProperty().addListener((Observable, oldValue, newValue) -> {
            mediaPlayer.setVolume((Double) newValue);
        });
    }

    /**
     * Shows the controls in the scene
     */
    private void showControls() {
        btnPlay.setVisible(true);
        btnPause.setVisible(true);
        btnStop.setVisible(true);
        btnFullscreen.setVisible(true);
    }

    /**
     * Hides the controls in the scene
     */
    private void hideControls() {
        btnPlay.setVisible(false);
        btnPause.setVisible(false);
        btnStop.setVisible(false);
        btnFullscreen.setVisible(true);
    }

    /**
     * Handles hiding and displaying a select list of controllers.
     * @param showControls handles showing or hiding the previous and next buttons, and the listview.
     */
    private void showListControls(boolean showControls) {
        btnNext.setVisible(showControls);
        btnPrevious.setVisible(showControls);
        lblListOverview.setVisible(showControls);
    }

    /**
     * Handles changing between fullscreen and not when clicking the button controller.
     * @param stage this is simply a parameter for the stage itself, the program.
     */
    private void toggleFullScreen(Stage stage) {
        // Get the current fullscreen state
        boolean currentFullscreenState = stage.isFullScreen();

        // Toggle fullscreen
        stage.setFullScreen(!currentFullscreenState);

        // Adjust controls based on the new fullscreen state
        if (stage.isFullScreen()) {
            hideControls();
        } else {
            showControls();
        }
    }


    /**
     * Handles displaying images in place of the buttons.
     * This happens for all the displayed images, pause, play, etc.
     */
    private void setIconImages() {
        Image imagePause = new Image(getClass().getResource("/Icon/pause.png").toExternalForm());
        ImageView imageViewPause = new ImageView(imagePause);
        imageViewPause.setFitHeight(10);
        imageViewPause.setFitWidth(10);
        btnPause.setGraphic(imageViewPause);

        Image imagePlay = new Image(getClass().getResource("/Icon/play.png").toExternalForm());
        ImageView imageViewPlay = new ImageView(imagePlay);
        imageViewPlay.setFitHeight(10);
        imageViewPlay.setFitWidth(10);
        btnPlay.setGraphic(imageViewPlay);

        Image imageStop = new Image(getClass().getResource("/Icon/stop.png").toExternalForm());
        ImageView imageViewStop = new ImageView(imageStop);
        imageViewStop.setFitHeight(10);
        imageViewStop.setFitWidth(10);
        btnStop.setGraphic(imageViewStop);

        Image imagePrevious = new Image(getClass().getResource("/Icon/previous.png").toExternalForm());
        ImageView imageViewPrevious = new ImageView(imagePrevious);
        imageViewPrevious.setFitHeight(20);
        imageViewPrevious.setFitWidth(20);
        btnPrevious.setGraphic(imageViewPrevious);

        Image imageForward = new Image(getClass().getResource("/Icon/forward.png").toExternalForm());
        ImageView imageViewForward = new ImageView(imageForward);
        imageViewForward.setFitHeight(20);
        imageViewForward.setFitWidth(20);
        btnNext.setGraphic(imageViewForward);

        Image imageFullscreen = new Image(getClass().getResource("/Icon/fullscreen.png").toExternalForm());
        ImageView imageViewFullscreen = new ImageView(imageFullscreen);
        imageViewFullscreen.setFitHeight(20);
        imageViewFullscreen.setFitWidth(20);
        btnFullscreen.setGraphic(imageViewFullscreen);
    }


    /**
     * Initialises certain variables, and triggers the hiding of a few controls for later use.
     */
    private void initializeVariables() {
        media = null;
        mediaPlayer = null;
        mediaListIndex = 0;
        mediaPlaying = false;
        showListControls(false);
        sliderVolume.setVisible(false);
    }
    //endregion
}