package com.example.mediaplayerdemo;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class dbSorting {
    //region instance variables
    private static File folder;
    private static final ArrayList<File> folderFiles = new ArrayList<>();
    private static final ArrayList<File> dbFiles = new ArrayList<>();
    //endregion
    //region database initialization
    /**
     * Initializes a database to align content with database
     * @param folderPath path to folder containing content to align with database
     */
    public static void initializeDB(String folderPath) {
        folder = new File(folderPath);
        initReadFiles();
        initReadDB();
        initSortDB();
    }

    /**
     * Reads folder for mp4-files then adds them to arraylist
     */
    private static void initReadFiles() {
        //Fetch data from folder
        File[] files = folder.listFiles();
        //If file is valid then proceed to process file
        if (files != null) {
            for (File file : files) {
                //If file is valid to database then add to array of folder files
                if (file.isFile() && file.getName().toLowerCase().endsWith("mp4")) {
                    folderFiles.add(file);
                    //Else delete file from folder
                } else {
                    try {
                        Files.delete(Path.of(file.getPath()));
                    } catch (IOException ignore) {}
                }
            }
        }
    }

    /**
     * Reads database for mp4-files then adds them to arraylist
     */
    private static void initReadDB() {
        //Fetch data from database
        Connection connection = dbConnection.databaseConnection(dbConnection.setProps(), dbConnection.URL);
        PreparedStatement getData;
        try {
            getData = connection.prepareCall("SELECT fldMediaID, fldPath FROM tblMedia");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        //Sort data from database
        try {
            ResultSet dataResult = getData.executeQuery();
            while (dataResult.next()) {
                int id = dataResult.getInt("fldMediaID");
                String path = dataResult.getString("fldPath");
                //If record from database contains a path then process path
                if (path != null) {
                    path = path.trim();
                    //Create a new file with path
                    File file = new File(path);
                    //If file is valid add to array of database files
                    if (file.isFile() && file.getName().toLowerCase().endsWith("mp4")) {
                        dbFiles.add(file);
                        //Else delete record from database
                    } else {
                        //If record id is present in association table delete from that table first
                        if (isDataPresentInDB("tblMediaPlaylist", "fldMediaID", id)) {
                            String sqlSpecifier = "tblMediaPlaylist WHERE fldMediaID=" + id;
                            deleteFromDB(sqlSpecifier);
                        }
                        //Delete from database
                        String sqlSpecifier = "tblMedia WHERE fldMediaID=" + id;
                        deleteFromDB(sqlSpecifier);
                    }
                    //Else delete record from database
                } else {
                    //If record id is present in association table delete from that table first
                    if (isDataPresentInDB("tblMediaPlaylist", "fldMediaID", id)) {
                        String sqlSpecifier = "tblMediaPlaylist WHERE fldMediaID=" + id;
                        deleteFromDB(sqlSpecifier);
                    }
                    //Delete from database
                    String sqlSpecifier = "tblMedia WHERE fldMediaID=" + id;
                    deleteFromDB(sqlSpecifier);
                }
            }
        } catch (SQLException ignore) {}
        dbConnection.databaseClose(connection);
    }

    /**
     * Sorts database and folder to align content
     */
    private static void initSortDB() {
        //Fetch folder file from array
        for (File folderFile : folderFiles) {
            //Check if file is present in database
            boolean presenceInDB = false;
            for (File dbFile : dbFiles) {
                if (dbFile.isFile()) {
                    long misMatchVal = getMisMatchVal(folderFile, dbFile);
                    if (misMatchVal == -1) {
                        presenceInDB = true;
                        break;
                    }
                }
            }
            //If file is not present add file to database
            if (!presenceInDB) {
                MediaFile addFile = new MediaFile(folderFile);
                String sqlSpecifier = "tblMedia (fldPath, fldTitle, fldFormat, fldDuration) VALUES " + addFile.getInsertValuesSQL();
                addToDB(sqlSpecifier);
                dbFiles.add(folderFile);
                //If file is present check if title in database correspond with actual file title
            } else {
                MediaFile updateFile = new MediaFile(folderFile);
                String actualTitle = updateFile.getTitle();
                String sqlSpecifierGet = "WHERE fldPath=" + updateFile.getPathValueSQL();
                String dbTitle = getStringDataFromDB("tblMedia", "fldTitle", sqlSpecifierGet);
                //If database title is not empty check if it matches with actual file title
                if (dbTitle != null) {
                    dbTitle = dbTitle.trim();
                    //If title from database does not correspond with actual file title then update in database
                    if (!actualTitle.equals(dbTitle)) {
                        String sqlSpecifierUpdate = "WHERE fldPath=" + updateFile.getPathValueSQL();
                        updateStringInDB("tblMedia", "fldTitle", actualTitle, sqlSpecifierUpdate);
                    }
                } else {
                    //Else update empty title field in database to be title of file
                    String sqlSpecifierUpdate = "WHERE fldPath=" + updateFile.getPathValueSQL();
                    updateStringInDB("tblMedia", "fldTitle", actualTitle, sqlSpecifierUpdate);
                }
            }
        }

        //Fetch database file from array
        for (File dbFile : dbFiles) {
            //Check if file is present in folder
            boolean presenceInFolder = false;
            for (File folderFile : folderFiles) {
                long misMatchVal = getMisMatchVal(dbFile, folderFile);
                if (misMatchVal == -1) {
                    presenceInFolder = true;
                    break;
                }
            }
            //If file is not present delete file from database
            if (!presenceInFolder) {
                MediaFile deleteFile = new MediaFile(dbFile);
                //If file is present in association table delete from that table first
                if (isDataPresentInDB("tblMediaPlaylist", "fldMediaID", deleteFile.getMediaID())) {
                    String sqlSpecifier = "tblMediaPlaylist WHERE fldMediaID=" + deleteFile.getMediaID();
                    deleteFromDB(sqlSpecifier);
                }
                //Delete from database
                String sqlSpecifier = "tblMedia WHERE fldPath=" + deleteFile.getPathValueSQL();
                deleteFromDB(sqlSpecifier);
            }
        }
    }
    //endregion
    //region database simple functions
    /**
     * Delete data from database
     * @param sqlSpecifier specifies where adn what data to delete
     */
    public static void deleteFromDB(String sqlSpecifier) {
        Connection connection = dbConnection.databaseConnection(dbConnection.setProps(), dbConnection.URL);
        PreparedStatement deleteData;
        try {
            deleteData = connection.prepareCall("DELETE FROM " + sqlSpecifier);
            deleteData.execute();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            dbConnection.databaseClose(connection);
        }
    }

    /**
     * Add data to database
     * @param sqlSpecifier specifies where and what data to add
     */
    public static void addToDB(String sqlSpecifier) {
        Connection connection = dbConnection.databaseConnection(dbConnection.setProps(), dbConnection.URL);
        PreparedStatement addData;
        try {
            addData = connection.prepareCall("INSERT INTO " + sqlSpecifier);
            addData.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            dbConnection.databaseClose(connection);
        }
    }

    /**
     * Update String data in database
     * @param table to update data in
     * @param field what data to update
     * @param newValue the new value to update in
     * @param sqlSpecifier where to update data
     */
    public static void updateStringInDB(String table, String field, String newValue, String sqlSpecifier) {
        Connection connection = dbConnection.databaseConnection(dbConnection.setProps(), dbConnection.URL);
        PreparedStatement addData;
        try {
            addData = connection.prepareCall("UPDATE " + table + " SET " + field + "='" + newValue + "' " + sqlSpecifier);
            addData.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            dbConnection.databaseClose(connection);
        }
    }

    /**
     * Reset a table in database completely
     * @param resetTable is the table to reset
     */
    public static void resetDB(String resetTable) {
        Connection connection = dbConnection.databaseConnection(dbConnection.setProps(), dbConnection.URL);
        PreparedStatement deleteData;
        try {
            deleteData = connection.prepareCall("DELETE FROM " + resetTable);
            deleteData.execute();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            dbConnection.databaseClose(connection);
        }
    }

    /**
     * Search for integer data in database
     * @param searchTable is the table in database that is searched through
     * @param searchField is the field where to retrieve the integer data from
     * @param sqlSpecifier is the specifier to where in table the data occurs
     * @return integer data (return 0 if no data found)
     */
    public static int getIntDataFromDB(String searchTable, String searchField, String sqlSpecifier) {
        Connection connection = dbConnection.databaseConnection(dbConnection.setProps(), dbConnection.URL);
        int data = 0;
        PreparedStatement getData;
        try {
            getData = connection.prepareCall("SELECT " + searchField + " FROM " + searchTable + " " + sqlSpecifier);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        try {
            ResultSet tblData = getData.executeQuery();
            while (tblData.next()) {
                data = tblData.getInt(searchField);
            }
        } catch (SQLException ignore) {}
        dbConnection.databaseClose(connection);
        return data;
    }

    /**
     * Search for String data in database
     * @param searchTable is the table in database that is searched through
     * @param searchField is the field where to retrieve the String data from
     * @param sqlSpecifier is the specifier to where in table the data occurs
     * @return String data (return empty if no data found)
     */
    public static String getStringDataFromDB(String searchTable, String searchField, String sqlSpecifier) {
        Connection connection = dbConnection.databaseConnection(dbConnection.setProps(), dbConnection.URL);
        String data = "";
        PreparedStatement getData;
        try {
            getData = connection.prepareCall("SELECT " + searchField + " FROM " + searchTable + " " + sqlSpecifier);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        try {
            ResultSet tblData = getData.executeQuery();
            while (tblData.next()) {
                data = tblData.getString(searchField);
            }
        } catch (SQLException ignore) {}
        dbConnection.databaseClose(connection);
        return data;
    }

    /**
     * Search and check database for presence of specific data
     * @param tblToSearch is the table in database that is searched through
     * @param searchField is the field where the specific data can occur
     * @param searchData is the data to search for in table
     * @return true if data is present in database
     */
    public static boolean isDataPresentInDB(String tblToSearch, String searchField, int searchData) {
        boolean isDataPresent = false;
        Connection connection = dbConnection.databaseConnection(dbConnection.setProps(), dbConnection.URL);
        PreparedStatement getData;
        try {
            getData = connection.prepareCall("SELECT * FROM " + tblToSearch);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        try {
            ResultSet tblData = getData.executeQuery();
            while (tblData.next()) {
                int value = tblData.getInt(searchField);
                if (value == searchData) {
                    isDataPresent = true;
                    break;
                }
            }
        } catch (SQLException ignore) {}
        finally {
            dbConnection.databaseClose(connection);
        }
        return isDataPresent;
    }
    //endregion
    //region file simple functions
    /**
     * Move a file to a new location
     * @param destinationFolder is the location that the file is moved to
     * @param file is the specific file copied
     * @return the file on the new location
     */
    public static File moveFile(File destinationFolder, File file) {
        File sourceFile = new File(file.getPath());
        File destinationFile = new File(destinationFolder.getPath(), sourceFile.getName());
        try {
            Files.copy(sourceFile.toPath(),destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            Files.delete(Path.of(file.getPath()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return destinationFile;
    }

    /**
     * Search and check a folder for presence of a specific file
     * @param folderToSearch is the folder that is searched through
     * @param searchFile is the file to search is present in the folder
     * @return true if file is present in folder
     */
    public static boolean isFilePresentInFolder(File folderToSearch, File searchFile) {
        File[] files = folderToSearch.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile() && file.getName().toLowerCase().endsWith("mp4")) {
                    long misMatchVal = getMisMatchVal(file, searchFile);
                    if (misMatchVal == -1) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Check if two separate files are identical
     * @param firstFile to compare
     * @param secondFile to compare
     * @return '-1' if they are identical
     */
    private static long getMisMatchVal(File firstFile, File secondFile) {
        long misMatchVal;
        try {
            misMatchVal = Files.mismatch(firstFile.toPath(), secondFile.toPath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return misMatchVal;
    }
    //endregion
}
