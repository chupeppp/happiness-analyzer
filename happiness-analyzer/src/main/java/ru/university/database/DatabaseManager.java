package ru.university.database;

import ru.university.model.Country;
import ru.university.model.Region;

import java.sql.*;
import java.util.List;

public class DatabaseManager {
    private Connection connection;

    public DatabaseManager(String dbName) {
        try {
            Class.forName("org.sqlite.JDBC");
            // устанавливаем соединение
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbName);
            System.out.println("Подключение к SQLite успешно установлено");
        } catch (ClassNotFoundException e) {
            System.out.println("Драйвер SQLite не найден");
            e.printStackTrace();
        } catch (SQLException e) {
            System.out.println("Ошибка при подключении к SQLite");
            e.printStackTrace();
        }
    }

    public void createTables() {
        // скл-запрос для создания таблицы регионов
        String createRegionsTable = "CREATE TABLE IF NOT EXISTS regions (\n"
                + " id INTEGER PRIMARY KEY,\n"
                + " name TEXT NOT NULL UNIQUE\n"
                + ");";

        // скл-запрос для создания таблицы стран
        String createCountriesTable = "CREATE TABLE IF NOT EXISTS countries (\n"
                + " id INTEGER PRIMARY KEY,\n"
                + " name TEXT NOT NULL,\n"
                + " region_id INTEGER NOT NULL,\n"
                + " happiness_rank INTEGER NOT NULL,\n"
                + " happiness_score REAL NOT NULL,\n"
                + " standard_error REAL NOT NULL,\n"
                + " economy REAL NOT NULL,\n"
                + " family REAL NOT NULL,\n"
                + " health REAL NOT NULL,\n"
                + " freedom REAL NOT NULL,\n"
                + " trust REAL NOT NULL,\n"
                + " generosity REAL NOT NULL,\n"
                + " dystopia_residual REAL NOT NULL,\n"
                + " FOREIGN KEY (region_id) REFERENCES regions(id)\n"
                + ");";

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createRegionsTable);
            stmt.execute(createCountriesTable);
            System.out.println("Таблицы успешно созданы");
        } catch (SQLException e) {
            System.out.println("Ошибка при создании таблиц");
            e.printStackTrace();
        }
    }

    public void insertRegions(List<Region> regions) {
        String sql = "INSERT OR IGNORE INTO regions(id, name) VALUES(?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            for (Region region : regions) {
                pstmt.setInt(1, region.getId());
                pstmt.setString(2, region.getName());
                pstmt.executeUpdate();
            }
            System.out.println("Регионы успешно добавлены в базу данных");
        } catch (SQLException e) {
            System.out.println("Ошибка при добавлении регионов");
            e.printStackTrace();
        }
    }

    public void insertCountries(List<Country> countries) {
        String sql = "INSERT OR REPLACE INTO countries(id, name, region_id, happiness_rank, happiness_score, " +
                "standard_error, economy, family, health, freedom, trust, generosity, dystopia_residual) " +
                "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            for (Country country : countries) {
                pstmt.setInt(1, country.getId());
                pstmt.setString(2, country.getName());
                pstmt.setInt(3, country.getRegion().getId());
                pstmt.setInt(4, country.getHappinessRank());
                pstmt.setDouble(5, country.getHappinessScore());
                pstmt.setDouble(6, country.getStandardError());
                pstmt.setDouble(7, country.getEconomy());
                pstmt.setDouble(8, country.getFamily());
                pstmt.setDouble(9, country.getHealth());
                pstmt.setDouble(10, country.getFreedom());
                pstmt.setDouble(11, country.getTrust());
                pstmt.setDouble(12, country.getGenerosity());
                pstmt.setDouble(13, country.getDystopiaResidual());
                pstmt.executeUpdate();
            }
            System.out.println("Страны успешно добавлены в базу данных");
        } catch (SQLException e) {
            System.out.println("Ошибка при добавлении стран");
            e.printStackTrace();
        }
    }

    public ResultSet executeQuery(String query) {
        try {
            Statement stmt = connection.createStatement();
            return stmt.executeQuery(query);
        } catch (SQLException e) {
            System.out.println("Ошибка при выполнении запроса: " + query);
            e.printStackTrace();
            return null;
        }
    }

    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("Соединение с базой данных закрыто");
            }
        } catch (SQLException e) {
            System.out.println("Ошибка при закрытии соединения");
            e.printStackTrace();
        }
    }
}