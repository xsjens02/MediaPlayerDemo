package com.example.mediaplayerdemo;

import com.coremedia.iso.IsoFile;
import com.coremedia.iso.boxes.MovieBox;
import javafx.scene.media.Media;
import java.io.File;
import java.io.IOException;

public class MediaFile {
    //region static implementation
    private static final MediaFile sharedObj = new MediaFile();
    private static final String file = "mp4";
    //endregion
    //region instance variables
    private Media media;
    private int mediaID;
    private String title;
    private int duration;
    private String path;
    private int playlistPosition;
    //endregion
    //region constructors
    /**
     * Construct an empty instance (default)
     */
    private MediaFile() {}

    /**
     * Construct an instance by giving instance variables manually
     * @param mediaID is the instance ID
     * @param title of instance
     * @param duration of instance
     * @param path is the file direction for instance
     */
    public MediaFile(int mediaID, String title, int duration, String path) {
        this.mediaID = mediaID;
        this.title = title;
        this.duration = duration;
        this.path = path;
    }

    /**
     * Construct an instance with an ID and position in playlist
     * @param mediaID is the instance ID
     * @param playlistPosition position in playlist
     */
    public MediaFile(int mediaID, int playlistPosition) {
        this.mediaID = mediaID;
        this.playlistPosition = playlistPosition;
        fetchTitleFromDB();
        fetchPathFromDB();
    }

    /**
     * Construct an instance from a file
     * @param file to construct from
     */
    public MediaFile(File file) {
        if (file.isFile() && file.getName().toLowerCase().endsWith(".mp4")) {
            this.path = file.getPath();
            this.title = generateTitle(file);
            this.duration = generateDuration(file);
            fetchIdFromDB();
        }
    }
    //endregion
    //region getter and setter shared object (static object)
    /**
     * Get static object (accessible from anywhere)
     * @return sharedObject (static object)
     */
    public static MediaFile getSharedObj() {
        return sharedObj;
    }

    /**
     * Set(copy) static objects instance variables from another instance
     * @param object to set(copy) instance variables from
     */
    public void setSharedObj(MediaFile object) {
        this.mediaID = object.mediaID;
        this.title = object.title;
        this.duration = object.getDuration();
        this.path = object.getPath();
        setInstanceMedia(object.getPath());
    }
    //endregion
    //region getters and setters
    public String getFile() {
        return file;
    }
    public Media getMedia() {
        return this.media;
    }
    public int getMediaID() {
        return this.mediaID;
    }
    public String getTitle() {
        return this.title;
    }
    public int getDuration() {
        return this.duration;
    }
    public String getPath() {
        return this.path;
    }
    public int getPlaylistPosition() {
        return this.playlistPosition;
    }

    /**
     * Set media of an instance
     * @param path to file used to set media
     */
    public void setInstanceMedia(String path) {
        String mediaFile = new File(path).getAbsolutePath();
        this.media = new Media(new File(mediaFile).toURI().toString());
    }
    public void setMediaID(int mediaID) {
        this.mediaID = mediaID;
    }
    public void setTitle(String title) {
        this.title = title;
    }
    public void setDuration(int duration) {
        this.duration = duration;
    }
    public void setPath(String path) {
        this.path = path;
    }
    public void setPlaylistPosition(int playlistPosition) {
        this.playlistPosition = playlistPosition;
    }
    //endregion
    //region additional assisting methods
    /**
     * Get instance insert values formatted for use as value in sql-statement
     * @return instance insert values formatted
     */
    public String getInsertValuesSQL() {
        return "('" + this.path + "', '" + this.title + "', '" +  this.getFile() + "', '" + this.duration + "')";
    }

    /**
     * Get instance title value value formatted for use as value in sql-statement
     * @return instance title value formatted
     */
    public String getTitleValueSQL() {
        return "'" + this.title + "'";
    }

    /**
     * Get instance path value formatted for use as value in sql-statement
     * @return instance path value formatted
     */
    public String getPathValueSQL() {
        return "'" + this.path + "'";
    }

    /**
     * Generate formatted title from file
     * @param file to get title from
     * @return title formatted
     */
    private String generateTitle(File file) {
        String title = file.getName();
        return title.substring(0,title.length() - 4);
    }

    /**
     * Generate duration from file
     * @param file to generate duration from
     * @return duration
     */
    private int generateDuration(File file) {
        IsoFile isoFile;
        try {
            isoFile = new IsoFile(file.getAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        MovieBox movieBox = isoFile.getMovieBox();
        double duration = movieBox.getMovieHeaderBox().getDuration();
        double timescale = movieBox.getMovieHeaderBox().getTimescale();
        return (int) (duration / timescale);
    }

    /**
     * Setting instance title from database
     */
    private void fetchTitleFromDB() {
        String sqlSpecifier = "WHERE fldMediaID=" + this.mediaID;
        this.title = dbSorting.getStringDataFromDB("tblMedia", "fldTitle", sqlSpecifier).trim();
    }

    /**
     * Setting instance path from database
     */
    private void fetchPathFromDB() {
        String sqlSpecifier = "WHERE fldMediaID=" + this.mediaID;
        this.path = dbSorting.getStringDataFromDB("tblMedia", "fldPath", sqlSpecifier).trim();
    }

    /**
     * Setting instance title from database
     */
    private void fetchIdFromDB() {
        String sqlSpecifier = "WHERE fldPath=" + this.getPathValueSQL() + " and fldTitle=" + this.getTitleValueSQL() + " and fldDuration=" + this.getDuration();
        int ID = dbSorting.getIntDataFromDB("tblMedia", "fldMediaID", sqlSpecifier);
        if (ID != 0) {
            this.mediaID = ID;
        }
    }

    /**
     * Reset media of an instance
     */
    public void resetMedia() {
        this.media = null;
    }

    /**
     * Create a media object from file path
     * @param path to file that will be converted to media object
     * @return media object
     */
    public static Media createMedia(String path) {
        String mediaFile = new File(path).getAbsolutePath();
        return new Media(new File(mediaFile).toURI().toString());
    }

    /**
     * Default method called when instance is displayed on listview
     * @return text to display representing object on listview
     */
    @Override
    public String toString() {
        return this.title;
    }
    //endregion
}
