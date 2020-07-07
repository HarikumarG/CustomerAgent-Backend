package com.web.serverdao;

import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.mysql.jdbc.Connection;
import com.mysql.jdbc.Statement;

public class ServerDao {

	Connection conn;
	public ServerDao() {
		try {
			Class.forName("com.mysql.jdbc.Driver");
			String url = "jdbc:mysql://localhost:3306/customeragent";
			Connection con = (Connection) DriverManager.getConnection(url,"root","");
			this.conn = con;
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}
	//function to update logout time for agent
	public boolean setLogoutTime(String agentname) {
		Statement stmt;
		try {
			stmt = (Statement) conn.createStatement();
			String url = "update userdetails set logout=now(),isasked='false' where name='"+agentname+"'";
			int rowNum = stmt.executeUpdate(url);
			conn.close();
			if(rowNum > 0) {
				return true;
			} else {
				return false;
			}
		} catch (SQLException e) {
			System.out.println(e.getMessage());
			return false;
		}
	}
	//function to get agent who is not yet asked and has last logout time
	public String[] getAgentName() {
		Statement stmt;
		String agentdetails[] = new String[2];
		try {
			stmt = (Statement) conn.createStatement();
			String url = "select * from userdetails where isasked='false' and isAgent='true' order by logout desc limit 1";
			ResultSet rs = stmt.executeQuery(url);
			if(rs.next() != false) {
				agentdetails[0] = rs.getString(1);
				agentdetails[1] = rs.getString(4);
			}
			conn.close();
			return agentdetails;
		} catch (SQLException e) {
			System.out.println(e.getMessage());
			return agentdetails;
		}
	}
	//after getting that agent update is asked attribute to true
	public void updateIsAsked(String agentname) {
		Statement stmt;
		try {
			stmt = (Statement) conn.createStatement();
			String url = "update userdetails set isasked='true' where name='"+agentname+"'";
			int rowNum = stmt.executeUpdate(url);
			conn.close();
			if(rowNum > 0) {
				System.out.println("Update isAsked for "+agentname+" is successful");
			}
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
	}
	
}
