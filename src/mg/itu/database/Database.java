package mg.itu.database;

import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;
import java.io.InputStream;

public class Database {
    
    private static String driver;
    private static String url;
    private static String username;
    private static String password;
    private static String propertiesFileName = "D:\\Studies\\ITU\\S5\\INF301_Architechture-Logiciel\\projet\\football\\_db.properties";


    // static initializer block  
    // ensures the configuration is loaded only once 
    static 
    { loadDatabaseConfig(); } 

    private static void loadDatabaseConfig() {
        String configPath = System.getProperty("config.file", propertiesFileName);
        try (InputStream input = new FileInputStream(configPath)) {
            Properties prop = new Properties();
            prop.load(input);

            driver = prop.getProperty("db.driver");
            url = prop.getProperty("db.url");
            username = prop.getProperty("db.username");
            password = prop.getProperty("db.password");

            Class.forName(driver);
            System.out.println("Connection running successfully!");
        } 
        
        catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to load database configuration", e);
        }
    }

    public static Connection getConnection() {
        Connection con = null;
        
        try 
        { con = DriverManager.getConnection(url, username, password); } 
        catch (Exception e) 
        { e.printStackTrace(); }

        return con;
    }
}
