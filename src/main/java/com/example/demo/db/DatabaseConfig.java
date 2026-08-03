package com.example.demo.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConfig {

  private static final String URL = System.getenv("DB_URL");
  private static final String USERNAME = System.getenv("DB_USERNAME");
  private static final String PASSWORD = System.getenv("DB_PASSWORD");

  public static Connection getConnection() throws SQLException {
    if (URL == null || USERNAME == null || PASSWORD == null) {
      throw new IllegalStateException(
          "Database env vars not defined: DB_URL, DB_USERNAME, DB_PASSWORD");
    }
    return DriverManager.getConnection(URL, USERNAME, PASSWORD);
  }
}
