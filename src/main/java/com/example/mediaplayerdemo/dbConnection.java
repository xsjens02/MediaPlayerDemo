package com.example.mediaplayerdemo;
import java.sql.*;
import java.util.Properties;

public class dbConnection {
    public static final String USERNAME = "sa";
    public static final String PASSWORD = "Kokopops02";
    public static final String DATABASE_NAME = "dbMediaPlayer";
    public static final String PORT = "1433";
    public static final String URL = "jdbc:sqlserver://localhost:" + PORT + ";databaseName=" + DATABASE_NAME;
    public static final String ENCRYPT = "false";
    public static Connection connection;

    public static Properties setProps() {
        System.out.println("Setting up properties");
        Properties properties = new Properties();
        properties.setProperty("user", USERNAME);
        properties.setProperty("password", PASSWORD);
        properties.setProperty("encrypt", ENCRYPT);
        return properties;
    }

    public static Connection databaseConnection(Properties properties, String URL) {
        try {
            connection = DriverManager.getConnection(URL, properties);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return connection;
    }

    public static void databaseClose(Connection connection) {
        try {
            connection.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        System.out.println("Closing connection to JDBC");
    }
}