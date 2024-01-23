package com.example.mediaplayerdemo;

import javafx.scene.media.Media;

import java.util.ArrayList;

public class Playlist {
    //region static implementation
    private static final Playlist sharedObj = new Playlist();
    //endregion
    //region instance variables
    private int playlistID;
    private String playlistTitle;
    private ArrayList<Media> playlistFiles = new ArrayList<>();
    //endregion
    //region constructors
    /**
     * Construct an empty instance (default)
     */
    private Playlist() {}
    /**
     * Construct an instance with title
     * @param playlistTitle title of instance
     */
    public Playlist(String playlistTitle) {
        this.playlistTitle = playlistTitle;
    }

    /**
     * Construct an instance with an ID and title
     * @param playlistID ID for instance
     * @param playlistTitle title for instance
     */
    public Playlist(int playlistID, String playlistTitle) {
        this.playlistID = playlistID;
        this.playlistTitle = playlistTitle.trim();
    }
    //endregion
    //region getter and setter shared object (static object)
    /**
     * Get static object (accessible from anywhere)
     * @return sharedObject (static object)
     */
    public static Playlist getSharedObj() {
        return sharedObj;
    }
    /**
     * Set(copy) static objects instance variables from another instance
     * @param object to set(copy) instance variables from
     */
    public void setSharedObj(Playlist object) {
        this.playlistID = object.getPlaylistID();
        this.playlistTitle = object.getPlaylistTitle();
    }
    //endregion
    //region getters and setters
    public int getPlaylistID() {
        return this.playlistID;
    }
    public String getPlaylistTitle() {
        return this.playlistTitle;
    }
    public ArrayList<Media> getPlaylistFiles() {
        return this.playlistFiles;
    }
    public void setPlaylistID(int playlistID) {
        this.playlistID = playlistID;
    }
    public void setPlaylistTitle(String playlistTitle) {
        this.playlistTitle = playlistTitle;
    }
    public void setPlaylistFiles(ArrayList<Media> playlistFiles) {
        this.playlistFiles = playlistFiles;
    }
    //endregion
    //region additional assisting methods
    /**
     * Get instance title formatted for use as value in sql-statement
     * @return instance title formatted
     */
    public String getTitleValues() {
        return "('" + this.playlistTitle + "')";
    }

    /**
     * Reset static object
     */
    public void resetSharedObj() {
        if (!this.playlistFiles.isEmpty()) {
            this.playlistFiles.clear();
        }
        this.playlistTitle = "";
        this.playlistID = 0;
    }

    /**
     * Adds new objects to playlist files array
     * @param mediaFile to add to playlist files array
     */
    public void addToPlaylistFiles(MediaFile mediaFile) {
        Media newMedia = MediaFile.createMedia(mediaFile.getPath());
        this.playlistFiles.add(newMedia);
    }

    /**
     * Default method called when instance is displayed on listview
     * @return text to display representing object on listview
     */
    @Override
    public String toString() {
        return this.playlistTitle;
    }
    //endregion
}
