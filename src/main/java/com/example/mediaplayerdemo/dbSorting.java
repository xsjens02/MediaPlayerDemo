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
        initArrangeDB();
    }

    /**
     * Reads folder for mp4-files then adds them to arraylist
     */
    private static void initReadFiles() {
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile() && file.getName().toLowerCase().endsWith("mp4")) {
                    folderFiles.add(file);
                }
            }
        }
    }

    /**
     * Reads database for mp4-files then adds them to arraylist
     */
    private static void initReadDB() {
        Connection dbConnection = com.example.mediaplayerdemo.dbConnection.databaseConnection(com.example.mediaplayerdemo.dbConnection.setProps(), com.example.mediaplayerdemo.dbConnection.URL);
        PreparedStatement getData;
        try {
            getData = dbConnection.prepareCall("SELECT fldPath FROM tblMedia");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        try {
            ResultSet dataResult = getData.executeQuery();
            while (dataResult.next()) {
                String path = dataResult.getString("fldPath").trim();
                File file = new File(path);
                if (file.isFile() && file.getName().toLowerCase().endsWith("mp4")) {
                    dbFiles.add(file);
                }
            }
        } catch (SQLException ignore) {}
        com.example.mediaplayerdemo.dbConnection.databaseClose(dbConnection);
    }

    /**
     * Determines how to align content of database with content of folder
     */
    private static void initArrangeDB() {
        if (!folderFiles.isEmpty() && !dbFiles.isEmpty()) {
            initSortDB();
        } else if (folderFiles.isEmpty()) {
            resetDB("tblMedia");
        } else {
            initSortDB();
        }
    }

    /**
     * Sorts database and folder to align content
     */
    private static void initSortDB() {
        for (File folderFile : folderFiles) {
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
            if (!presenceInDB) {
                MediaFile addFile = new MediaFile(folderFile);
                String sqlSpecifier = "tblMedia (fldPath, fldTitle, fldFormat, fldDuration) VALUES " + addFile.getInsertValues();
                addToDB(sqlSpecifier);
                dbFiles.add(folderFile);
            }
        }

        for (File dbFile : dbFiles) {
            if (!dbFile.isFile()) {
                MediaFile deleteFile = new MediaFile(dbFile);
                String sqlSpecifier = "tblMedia WHERE fldPath=" + deleteFile.getDeleteValues();
                deleteFromDB(sqlSpecifier);
            } else {
                boolean presenceInFolder = false;
                for (File folderFile : folderFiles) {
                    long misMatchVal = getMisMatchVal(dbFile, folderFile);
                    if (misMatchVal == -1) {
                        presenceInFolder = true;
                        break;
                    }
                }
                if (!presenceInFolder) {
                    MediaFile deleteFile = new MediaFile(dbFile);
                    String sqlSpecifier = "tblMedia WHERE fldPath=" + deleteFile.getDeleteValues();
                    deleteFromDB(sqlSpecifier);
                }
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
        Connection dbConnection = com.example.mediaplayerdemo.dbConnection.databaseConnection(com.example.mediaplayerdemo.dbConnection.setProps(), com.example.mediaplayerdemo.dbConnection.URL);
        PreparedStatement deleteData;
        try {
            deleteData = dbConnection.prepareCall("DELETE FROM " + sqlSpecifier);
            deleteData.execute();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            com.example.mediaplayerdemo.dbConnection.databaseClose(dbConnection);
        }
    }

    /**
     * Add data to database
     * @param sqlSpecifier specifies where and what data to add
     */
    public static void addToDB(String sqlSpecifier) {
        Connection dbConnection = com.example.mediaplayerdemo.dbConnection.databaseConnection(com.example.mediaplayerdemo.dbConnection.setProps(), com.example.mediaplayerdemo.dbConnection.URL);
        PreparedStatement addData;
        try {
            addData = dbConnection.prepareCall("INSERT INTO " + sqlSpecifier);
            addData.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            com.example.mediaplayerdemo.dbConnection.databaseClose(dbConnection);
        }
    }

    /**
     * Reset a table in database completely
     * @param resetTable is the table to reset
     */
    public static void resetDB(String resetTable) {
        Connection dbConnection = com.example.mediaplayerdemo.dbConnection.databaseConnection(com.example.mediaplayerdemo.dbConnection.setProps(), com.example.mediaplayerdemo.dbConnection.URL);
        PreparedStatement deleteData;
        try {
            deleteData = dbConnection.prepareCall("DELETE FROM " + resetTable);
            deleteData.execute();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            com.example.mediaplayerdemo.dbConnection.databaseClose(dbConnection);
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
        Connection dbConnection = com.example.mediaplayerdemo.dbConnection.databaseConnection(com.example.mediaplayerdemo.dbConnection.setProps(), com.example.mediaplayerdemo.dbConnection.URL);
        int data = 0;
        PreparedStatement getData;
        try {
            getData = dbConnection.prepareCall("SELECT " + searchField + " FROM " + searchTable + " " + sqlSpecifier);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        try {
            ResultSet tblData = getData.executeQuery();
            while (tblData.next()) {
                data = tblData.getInt(searchField);
            }
        } catch (SQLException ignore) {}
        com.example.mediaplayerdemo.dbConnection.databaseClose(dbConnection);
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
        Connection connection = dbConnection.databaseConnection(com.example.mediaplayerdemo.dbConnection.setProps(), com.example.mediaplayerdemo.dbConnection.URL);
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
        com.example.mediaplayerdemo.dbConnection.databaseClose(connection);
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
        Connection dbConnection = com.example.mediaplayerdemo.dbConnection.databaseConnection(com.example.mediaplayerdemo.dbConnection.setProps(), com.example.mediaplayerdemo.dbConnection.URL);
        PreparedStatement getData;
        try {
            getData = dbConnection.prepareCall("SELECT * FROM " + tblToSearch);
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
            com.example.mediaplayerdemo.dbConnection.databaseClose(dbConnection);
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
