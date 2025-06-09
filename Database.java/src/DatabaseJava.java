import java.sql.*;
import java.util.*;

class Database {
    public static void main(String[] arg) {
        try {
            // Load SQLite JDBC driver
            Class.forName("org.sqlite.JDBC");

            // Define correct database path (Adjust for Windows/Linux)
            String dbURL = "jdbc:sqlite:C:\\sqllite\\university.db"; // Windows Example
            // String dbURL = "jdbc:sqlite:/home/user/sqlite/university.db"; // Linux/Mac Example

            // Establish connection
            /*Connection con = DriverManager.getConnection(dbURL);
            Statement stm = con.createStatement();
            ResultSet rs = stm.executeQuery("SELECT * FROM dept");*/
            
            
            Connection con=DriverManager.getConnection(dbURL);
            PreparedStatement ste=con.prepareStatement("SELECT *FROM dept where deptno=?");
            Scanner sc=new Scanner(System.in);
            System.out.println("Enter dept no: ");
            int dno=sc.nextInt();
            ste.setInt(1, dno);
            ResultSet rs=ste.executeQuery();
            // Read and display database records
            while (rs.next()) {
                rs.getInt("deptno"); // Ensure column name is correct in your DB
                String dname = rs.getString("dname"); // Ensure column name is correct in your DB

                //System.out.println(dno + " " + dname);
                System.out.println(rs.getInt("deptno") + " " + dname);
            }

            // Close resources
            rs.close();
            ste.close();
            con.close();
        } catch (ClassNotFoundException e) {
            System.out.println("SQLite JDBC Driver not found. Make sure it's added to the classpath.");
            e.printStackTrace();
        } catch (SQLException e) {
            System.out.println("Database connection error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
