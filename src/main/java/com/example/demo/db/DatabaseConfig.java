package com.example.demo.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConfig {

  private static final String URL = System.getenv("NEON_URL");
  private static final String USERNAME = System.getenv("NEON_USERNAME");
  private static final String PASSWORD = System.getenv("NEON_PASSWORD");

  public static Connection getConnection() throws SQLException {
    if (URL == null || USERNAME == null || PASSWORD == null) {
      throw new IllegalStateException(
          "Database env vars not defined: NEON_URL, NEON_USERNAME, NEON_PASSWORD");
    }
    return DriverManager.getConnection(URL, USERNAME, PASSWORD);
  }
}
