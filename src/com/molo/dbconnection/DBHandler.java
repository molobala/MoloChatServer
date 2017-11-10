package com.molo.dbconnection;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DBHandler {
	private static final String URL="jdbc:mysql://localhost:3306/chat01",
								USER="root",
								PASSWORD="";
	private Connection dbh=null;
	private Statement statement=null;
	public DBHandler() throws SQLException {
		openDriver();
	}
	private void openDriver() throws SQLException{
		/* Chargement du driver JDBC pour MySQL */
		try {
		    Class.forName( "com.mysql.jdbc.Driver" );
		    System.out.println("Driver chargé avec succès !!!");
		    this.dbh=DriverManager.getConnection(URL, USER, PASSWORD);
		    System.out.println("Connexion reussi avec succès à la base de donnée Dictionnaire");
		} catch ( ClassNotFoundException e ) {
		    e.printStackTrace();
		} 
	}
	public Connection getConnexion(){
		return this.dbh;
	}
	public void close(){
		try {
			if(this.dbh!=null){
				this.dbh.close();
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public Statement getRequest() throws SQLException {
		if(this.dbh==null)
			return null;
		if(this.statement==null)
			this.statement=this.dbh.createStatement();
		return this.statement;
	}
}
