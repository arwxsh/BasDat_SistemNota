import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;


public class DBConnection {

    private static final String URL = "jdbc:sqlserver://localhost:1433;databaseName=SistemNota;" + "encrypt=true;trustServerCertificate=true;";
    private static final String USER = "DB_Nota";
    private static final String PASS = "6767";

    private static Connection instance = null;

    private DBConnection() {}

    public static Connection getConnection() throws SQLException {
        if (instance == null || instance.isClosed()) {
            try {
                Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
                instance = DriverManager.getConnection(URL, USER, PASS);
            } catch (ClassNotFoundException e) {
                throw new SQLException("Driver SQL Server tidak ditemukan: " + e.getMessage());
            }
        }
        return instance;
    }
}
