package com.jobcom.config;
import java.sql.*;
public final class Db {
 private Db(){}
 public static Connection getConnection() throws SQLException {
  String url=System.getenv().getOrDefault("JOBCOM_DB_URL","jdbc:mysql://127.0.0.1:3306/jobcom?useSSL=false&serverTimezone=UTC");
  String user=System.getenv().getOrDefault("JOBCOM_DB_USER","root");
  try{Class.forName("com.mysql.cj.jdbc.Driver");}
  catch(ClassNotFoundException e){throw new SQLException("MySQL Connector/J is missing from the deployed application (WEB-INF/lib).",e);}
  return DriverManager.getConnection(url,user,System.getenv().getOrDefault("JOBCOM_DB_PASSWORD",""));
 }
}
