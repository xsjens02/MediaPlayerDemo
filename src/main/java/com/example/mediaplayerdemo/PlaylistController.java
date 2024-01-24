package com.example.mediaplayerdemo;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.ResourceBundle;

public class PlaylistController implements Initializable {
    //region FXML annotations
    @FXML
    private VBox vboxParent;
    @FXML
    private Button btnBuild;
    @FXML
    private TextField txtTitlePrompt;
    @FXML
    private ListView<Playlist> listViewPlaylist;
    @FXML
    private ListView<MediaFile> listViewFile;
    //endregion
    //region instances variables
    private final ObservableList<Playlist> playlistCollection = FXCollections.observableArrayList();
    private final ObservableList<MediaFile> fileCollection = FXCollections.observableArrayList();
    private Playlist selectedObjPlaylist;
    private MediaFile selectedObjFile;
    //endregion
    //region initialize
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        initializeVariables();
        arrangePlaylistView();
    }
    //endregion
    //region control handlers
    /**
     * Assign selected object (playlist) when user clicks on listview of playlists
     */
    @FXML
    private void playlistClick() {
        //Hide unrelated function
        showTitlePrompt(false);
        //Assign selected object (playlist) to object selected in listview of playlists
        selectedObjPlaylist = listViewPlaylist.getSelectionModel().getSelectedItem();
        //Clear file listview
        clearFiles();
        //If selected object (playlist) is assigned then proceed
        if (selectedObjPlaylist != null) {
            //If selected object (playlist) contains any files then arrange listview of files
            if (dbSorting.isDataPresentInDB("tblMediaPlaylist", "fldPlaylistID", selectedObjPlaylist.getPlaylistID())) {
                arrangeFileView(selectedObjPlaylist.getPlaylistID());
            } else {
                fileCollection.clear();
            }
        }
    }

    /**
     * Assign selected object (file) when user clicks on listview of files
     */
    @FXML
    private void onFileClick() {
        //Hide unrelated function
        showTitlePrompt(false);
        //Assign selected object (playlist) to object selected in listview of files
        selectedObjFile = listViewFile.getSelectionModel().getSelectedItem();
    }

    /**
     * Deletes a selected object (playlist) from listview and database when user clicks on 'delete' button
     */
    @FXML
    private void onDeleteListClick() {
        //If an object is selected then proceed to delete that object
        if (selectedObjPlaylist != null) {
            //If the object is present in association table then delete from that table
            if (!fileCollection.isEmpty()) {
                String sqlSpecifier = "tblMediaPlaylist WHERE fldPlaylistID=" + selectedObjPlaylist.getPlaylistID();
                dbSorting.deleteFromDB(sqlSpecifier);
            }
            //Delete object from listview and database
            String sqlSpecifier = "tblPlaylist WHERE fldPlaylistID=" + selectedObjPlaylist.getPlaylistID();
            dbSorting.deleteFromDB(sqlSpecifier);
            //Remove object from observable list
            playlistCollection.remove(selectedObjPlaylist);
            //Clear corresponding file listview of deleted object
            clearFiles();
            //Reset all selected objects
            resetSelectedObjects();
        }
    }

    /**
     * Shows a text field with prompt and button to generate new playlist
     */
    @FXML
    private void onCreateListClick() {
        resetSelectedObjects();
        clearFiles();
        showTitlePrompt(true);
    }

    /**
     * Adds a new playlist in listview and database when user clicks on 'build' button
     */
    @FXML
    private void onBuildListClick() {
        //Fetch title from text field
        String title = txtTitlePrompt.getText();
        //If title meets criteria then proceed
        if (!title.isEmpty() && title.length() <= 30) {
            //Generate new playlist object
            Playlist newPlaylist = new Playlist(title);
            //Add object to database
            String sqlSpecifierAdd = "tblPlaylist (fldPlaylistTitle) VALUES " + newPlaylist.getTitleValues();
            dbSorting.addToDB(sqlSpecifierAdd);
            //Fetch ID of object from database and then add to listview
            String sqlSpecifierFetch = "WHERE fldPlaylistTitle=" + newPlaylist.getTitleValues();
            newPlaylist.setPlaylistID(dbSorting.getIntDataFromDB("tblPlaylist", "fldPlaylistID", sqlSpecifierFetch));
            playlistCollection.add(newPlaylist);
        } else {
            txtTitlePrompt.setPromptText("Error! Please try again!");
        }
    }

    /**
     * Passes a selected object to a static object then closes current stage
     */
    @FXML
    private void onPlayListClick() {
        //If an object is selected then proceed
        if (selectedObjPlaylist != null) {
            //Set the selected object to a static object then accessible by all
            Playlist.getSharedObj().setSharedObj(selectedObjPlaylist);
            //If selected object has content then add that content to static object
            if (!fileCollection.isEmpty()) {
                for (MediaFile mediaFile : fileCollection) {
                    Playlist.getSharedObj().addToPlaylistFiles(mediaFile);
                }
            }
            //Reset all
            clearFiles();
            resetSelectedObjects();
            //Close current stage
            Stage currentStage = (Stage) vboxParent.getScene().getWindow();
            currentStage.close();
        }
    }

    /**
     * Deletes a selected object (file) from listview and database when user clicks on 'delete' button
     */
    @FXML
    private void onDeleteFileClick() {
        //If an object (file) is selected then proceed to delete that object
        if (selectedObjFile != null) {
            //Delete object from listview and database
            String sqlSpecifier = "tblMediaPlaylist WHERE fldMediaID=" + selectedObjFile.getMediaID() + " and fldPlaylistID=" + selectedObjPlaylist.getPlaylistID();
            dbSorting.deleteFromDB(sqlSpecifier);
            fileCollection.remove(selectedObjFile);
            //Reset selected object
            selectedObjFile = null;
        }
    }

    /**
     * Lets user choose a file from fileView.fxml to add to listview and database when 'add' button is clicked
     */
    @FXML
    private void onAddFileClick() {
        //If an object (playlist) is selected then proceed to add to that object
        if (selectedObjPlaylist != null) {
            //Open fileView.fxml to choose a file to add to playlist
            try {
                new SceneSwitch("FileView.fxml", "Select File");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            //If a file is selected then proceed
            if (MediaFile.getSharedObj() != null) {
                //If the playlist contains no files the give file position 1
                if (!dbSorting.isDataPresentInDB("tblMediaPlaylist", "fldPlaylistID", selectedObjPlaylist.getPlaylistID())) {
                    MediaFile.getSharedObj().setPlaylistPosition(1);
                    //Else receive position from last object in observable list (sorted) and add 1
                } else {
                    MediaFile lastObjectInCollection = fileCollection.get(fileCollection.size() - 1);
                    MediaFile.getSharedObj().setPlaylistPosition(lastObjectInCollection.getPlaylistPosition() + 1);
                }
                //Add object to listview and database
                String sqlSpecifier = "tblMediaPlaylist (fldPlaylistID, fldMediaID, fldPosition) VALUES " + getAddFileValues();
                dbSorting.addToDB(sqlSpecifier);
                fileCollection.add(MediaFile.getSharedObj());
                //Reset selected object
                selectedObjFile = null;
            }
        }
    }
    //endregion
    //region additional assisting methods
    /**
     * Arrange listview to represent database with data of playlists
     */
    public void arrangePlaylistView() {
        //Fetch data from database
        Connection connection = dbConnection.databaseConnection(dbConnection.setProps(), dbConnection.URL);
        PreparedStatement getData;
        try {
            getData = connection.prepareCall("SELECT * FROM tblPlaylist");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        //Sort data from database
        try {
            ResultSet tblData = getData.executeQuery();
            while (tblData.next()) {
                int playlistID = tblData.getInt("fldPlaylistID");
                String playlistTitle = tblData.getString("fldPlaylistTitle");

                //Use data to create object for observable list
                playlistCollection.add(new Playlist(playlistID, playlistTitle));
            }
            //Set listview of playlists to observable list
            listViewPlaylist.setItems(playlistCollection);
        } catch (SQLException ignore) {}
        dbConnection.databaseClose(connection);
    }

    /**
     * Arrange listview to represent database with data of files from specific playlists
     * @param playlistID is the playlist identification to search for files in database
     */
    private void arrangeFileView(int playlistID) {
        //Reset selected object (file)
        selectedObjFile = null;
        //Fetch data from database
        Connection connection = dbConnection.databaseConnection(dbConnection.setProps(), dbConnection.URL);
        PreparedStatement getData;
        try {
            getData = connection.prepareCall("SELECT fldMediaID, fldPosition FROM tblMediaPlaylist WHERE fldPlaylistID=" + playlistID);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        //Sort data from database
        try {
            ResultSet tblData = getData.executeQuery();
            while (tblData.next()) {
                int mediaID = tblData.getInt("fldMediaID");
                int position = tblData.getInt("fldPosition");

                //Use data to create object for observable list
                fileCollection.add(new MediaFile(mediaID, position));
                //Sort observable list by file position in playlist
                fileCollection.sort(Comparator.comparing(MediaFile::getPlaylistPosition));

                //Set listview of playlists to observable list
                listViewFile.setItems(fileCollection);
            }
        } catch (SQLException ignore) {}
        dbConnection.databaseClose(connection);
    }

    /**
     * Generates add values to use in sql-statement when adding a file to a playlist
     * @return add values to use in sql-statement
     */
    private String getAddFileValues() {
        return "('" + selectedObjPlaylist.getPlaylistID() + "', '" + MediaFile.getSharedObj().getMediaID() + "', '" + MediaFile.getSharedObj().getPlaylistPosition() + "')";
    }

    /**
     * Shows and hides text field and button to generate new playlist
     * @param showPrompt boolean condition to show;true or hide;false
     */
    private void showTitlePrompt(boolean showPrompt) {
        txtTitlePrompt.setVisible(showPrompt);
        btnBuild.setVisible(showPrompt);
    }

    /**
     * Reset all relevant variables
     */
    private void resetSelectedObjects() {
        selectedObjPlaylist = null;
        selectedObjFile = null;
    }

    /**
     * Reset observable list for files and corresponding listview
     */
    private void clearFiles() {
        fileCollection.clear();
        listViewFile.getItems().clear();
    }

    /**
     * Initialize all relevant variables by reset
     */
    private void initializeVariables() {
        showTitlePrompt(false);
        resetSelectedObjects();
        playlistCollection.clear();
        listViewPlaylist.getItems().clear();
        fileCollection.clear();
        listViewFile.getItems().clear();
        Playlist.getSharedObj().resetSharedObj();
    }
    //endregion
}