package application;

import java.sql.*;

public class dbClass {
	static Connection conn = null;
	
	public static Connection Connect() 
	{
		try 
		{
			conn = DriverManager.getConnection("jdbc:mysql://localhost/aractakip?serverTimezone=UTC", "root", "");
			return conn;
		}catch(Exception e) 
		{
			System.out.println(e.getMessage().toString());
			return null;
		}
	}
	
}
