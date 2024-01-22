package com.example.mediaplayerdemo;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class FileController implements Initializable {

    private final File folder = new File("src/main/java/MediaFilesFolder");

    @FXML
    private VBox vboxParent;

    @FXML
    private TextField txtSearchField;

    private final ObservableList<MediaFile> fileCollection = FXCollections.observableArrayList();
    @FXML
    private TableView<MediaFile> tableView;
    @FXML
    private TableColumn<MediaFile, String> colTitle;
    @FXML
    private TableColumn<MediaFile, Integer> colDuration;
    @FXML
    private TableColumn<MediaFile, String> colFile;
    @FXML
    private TableColumn<MediaFile, String> colPath;
    private MediaFile selectedObj;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        initializeVariables();
        arrangeTableView();

    }

    /**
     * Arrange tableview to represent database with data from file folder
     */
    private void arrangeTableView() {
        //Fetch data from database
        Connection dbConnection = com.example.mediaplayerdemo.dbConnection.databaseConnection(com.example.mediaplayerdemo.dbConnection.setProps(), com.example.mediaplayerdemo.dbConnection.URL);
        PreparedStatement getData;
        try {
            getData = dbConnection.prepareCall("SELECT * FROM tblMedia");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        //Sort data from database
        try {
            ResultSet tblData = getData.executeQuery();
            while (tblData.next()) {
                int mediaID = tblData.getInt("fldMediaID");
                String title = tblData.getString("fldTitle").trim();
                int duration = tblData.getInt("fldDuration");
                String path = tblData.getString("fldPath").trim();

                //Use data to create object for observable list
                fileCollection.add(new MediaFile(mediaID, title, duration, path));

            }
            //Set cell values of tableview
            colTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
            colDuration.setCellValueFactory(new PropertyValueFactory<>("duration"));
            colFile.setCellValueFactory(new PropertyValueFactory<>("file"));
            colPath.setCellValueFactory(new PropertyValueFactory<>("path"));

            //Set tableview to observable list
            tableView.setItems(fileCollection);
        } catch (SQLException ignore) {}
        com.example.mediaplayerdemo.dbConnection.databaseClose(dbConnection);
    }

    /**
     * Assign selected object when user clicks on tableview
     */
    @FXML
    private void onFileClick() {
        selectedObj = tableView.getSelectionModel().getSelectedItem();
    }

    /**
     * Deletes a selected object from listview and database when user clicks on 'delete' button
     */
    @FXML
    void onDeleteFileClick() {
        //If an object is selected then proceed to delete that object
        if (selectedObj != null) {
            //If the object is present in association table then delete from that table
            if (dbSorting.isDataPresentInDB("tblMediaPlaylist", "fldMediaID", selectedObj.getMediaID())) {
                String sqlSpecifier = "tblMediaPlaylist WHERE fldMediaID=" + selectedObj.getMediaID();
                dbSorting.deleteFromDB(sqlSpecifier);
            }
            //Delete object from listview and database
            String sqlSpecifier = "tblMedia WHERE fldPath='" + selectedObj.getPath() + "'";
            dbSorting.deleteFromDB(sqlSpecifier);
            //Delete file of object from folder
            try {
                Files.delete(Path.of(selectedObj.getPath()));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            //Remove object from observable list
            fileCollection.remove(selectedObj);
        }
        //Reset selected object
        selectedObj = null;
    }

    /**
     * Lets user choose a file from personal drive to add to listview and database when 'add' button is clicked
     */
    @FXML
    void onAddFileClick() {
        //Open drive-window to choose a new file from
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select File");
        File selectedFile = fileChooser.showOpenDialog(null);
        //If a file is selected and all criteria for a file is meet then proceed
        if (selectedFile != null) {
            if (selectedFile.isFile() && selectedFile.getName().toLowerCase().endsWith("mp4")) {
                //If file is not already present in media folder then proceed
                if (!dbSorting.isFilePresentInFolder(folder, selectedFile)) {
                    //Move new file into folder, add new file to listview and database
                    MediaFile newFile = new MediaFile(dbSorting.moveFile(folder, selectedFile));
                    String sqlSpecifier = "tblMedia (fldPath, fldTitle, fldFormat, fldDuration) VALUES " + newFile.getInsertValues();
                    dbSorting.addToDB(sqlSpecifier);
                    fileCollection.add(newFile);
                }
            }
        }
        //Reset selected object
        selectedObj = null;
    }

    /**
     * Passes a selected object to a static object then closes current stage
     */
    @FXML
    void onChooseFileClick() {
        //If an object is selected then proceed
        if (selectedObj != null) {
            //Set the selected object to a static object then accessible by all
            MediaFile.getSharedObj().setSharedObj(selectedObj);
            //Reset selected object
            selectedObj = null;
            //Close current stage
            Stage currentStage = (Stage) vboxParent.getScene().getWindow();
            currentStage.close();
        }
    }

    /**
     * Initialize all relevant variables by reset
     */
    private void initializeVariables() {
        selectedObj = null;
        fileCollection.clear();
        tableView.getItems().clear();
    }
}