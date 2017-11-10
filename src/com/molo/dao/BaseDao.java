package com.molo.dao;

import java.sql.SQLException;
import java.sql.Statement;

import com.molo.dbconnection.DBHandler;

public class BaseDao {
	private DBHandler dbh=null;
	protected Statement request=null;
	public BaseDao(DBHandler dbh) {
		this.dbh=dbh;
		this.request=getRequest();
	}
	public void close() {
		try {
			if(this.request!=null)
				this.request.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public Statement getRequest() {
		try {
			return this.dbh.getRequest();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}
}
