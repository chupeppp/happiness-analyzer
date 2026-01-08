package ru.university;

import ru.university.database.DatabaseManager;
import ru.university.model.Country;
import ru.university.model.Region;
import ru.university.visualization.ChartGenerator;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;
import java.util.stream.Collectors;
import java.util.function.Consumer;

public class Main {

    private static final String CSV_FILE_NAME = "happiness_data_2015.csv";
    private static final String DB_NAME = "happiness_data.db";
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.0000");

    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setDecimalSeparator('.');
        DECIMAL_FORMAT.setDecimalFormatSymbols(symbols);
    }

    public static void main(String[] args) {
        List<Country> countries = new ArrayList<>();
        Map<String, Region> regionsMap = new HashMap<>();

        // Создаем папку для диаграмм перед началом работы
        createChartsDirectory();

        // Шаг 1: Подготовка CSV файла
        prepareCSVFile();

        // Шаг 2: Парсинг CSV файла
        parseCSVFile(countries, regionsMap);

        // Шаг 3: Работа с базой данных
        DatabaseManager dbManager = new DatabaseManager(DB_NAME);
        dbManager.createTables();

        // Преобразуем Map регионов в List для вставки в БД
        List<Region> regionsList = new ArrayList<>(regionsMap.values());
        dbManager.insertRegions(regionsList);
        dbManager.insertCountries(countries);

        // Шаг 4: Выполнение SQL-запросов и вывод результатов

        // Запрос 1: Топ-10 стран по показателю экономики (GDP per Capita)
        System.out.println("\n===== ЗАПРОС 1: ТОП-10 СТРАН ПО ПОКАЗАТЕЛЮ ЭКОНОМИКИ =====");
        String query1 = "SELECT name, economy FROM countries ORDER BY economy DESC LIMIT 10";
        Map<String, Double> economyData = new LinkedHashMap<>();
        List<String> topEconomyCountries = new ArrayList<>();

        executeAndPrintQuery(dbManager, query1, rs -> {
            try {
                System.out.printf("%-30s | %s%n", "Страна", "GDP per Capita");
                System.out.println("-".repeat(50));
                while (rs.next()) {
                    String countryName = rs.getString("name");
                    double economyValue = rs.getDouble("economy");
                    System.out.printf("%-30s | %s%n", countryName, DECIMAL_FORMAT.format(economyValue));
                    economyData.put(countryName, economyValue);
                    topEconomyCountries.add(countryName);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });

        // Создание диаграммы для первого запроса
        ChartGenerator.createEconomyChart(economyData, "economy_chart.png");

        // Запрос 2: Страна из топ-3 по экономике в регионах 'Latin America and Caribbean' и 'Eastern Asia'
        System.out.println("\n===== ЗАПРОС 2: СТРАНА ИЗ ТОП-3 ПО ЭКОНОМИКЕ В УКАЗАННЫХ РЕГИОНАХ =====");
        // Сначала получаем топ-3 страны по экономике
        String top3Query = "SELECT name FROM countries ORDER BY economy DESC LIMIT 3";

        List<String> top3Countries = new ArrayList<>();
        executeAndPrintQuery(dbManager, top3Query, rs -> {
            try {
                while (rs.next()) {
                    top3Countries.add(rs.getString("name"));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });

        // Создаем список стран с кавычками для SQL запроса
        List<String> top3Quoted = top3Countries.stream()
                .map(c -> "'" + c + "'")
                .collect(Collectors.toList());

        // Теперь ищем среди этих стран те, что находятся в указанных регионах
        String query2 = "SELECT c.name, c.economy, r.name AS region_name " +
                "FROM countries c " +
                "JOIN regions r ON c.region_id = r.id " +
                "WHERE r.name IN ('Latin America and Caribbean', 'Eastern Asia', 'Southeastern Asia') " +
                "AND c.name IN (" + String.join(",", top3Quoted) + ") " +
                "ORDER BY c.economy DESC LIMIT 1";

        // Используем массив из одного элемента вместо локальной переменной
        final boolean[] foundCountry = {false};

        executeAndPrintQuery(dbManager, query2, rs -> {
            try {
                if (rs.next()) {
                    foundCountry[0] = true;
                    System.out.println("Результат:");
                    System.out.println("-".repeat(50));
                    System.out.printf("Страна: %s%n", rs.getString("name"));
                    System.out.printf("Регион: %s%n", rs.getString("region_name"));
                    System.out.printf("Экономический показатель (GDP per Capita): %s%n",
                            DECIMAL_FORMAT.format(rs.getDouble("economy")));
                } else {
                    // Если не найдено стран в точных регионах, расширяем поиск
                    String extendedQuery = "SELECT c.name, c.economy, r.name AS region_name " +
                            "FROM countries c " +
                            "JOIN regions r ON c.region_id = r.id " +
                            "WHERE c.name IN (" + String.join(",", top3Quoted) + ") " +
                            "ORDER BY c.economy DESC LIMIT 1";

                    executeAndPrintQuery(dbManager, extendedQuery, rs2 -> {
                        try {
                            if (rs2.next()) {
                                System.out.println("В указанных регионах нет стран из топ-3 по экономике.");
                                System.out.println("Ближайшая страна из топ-3 по экономике:");
                                System.out.println("-".repeat(50));
                                System.out.printf("Страна: %s%n", rs2.getString("name"));
                                System.out.printf("Регион: %s%n", rs2.getString("region_name"));
                                System.out.printf("Экономический показатель (GDP per Capita): %s%n",
                                        DECIMAL_FORMAT.format(rs2.getDouble("economy")));
                            }
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    });
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });

        // Запрос 3: Страна из регионов 'Western Europe' и 'North America' со средними показателями по всем критериям
        System.out.println("\n===== ЗАПРОС 3: СТРАНА СО СРЕДНИМИ ПОКАЗАТЕЛЯМИ =====");
        // Сначала получаем средние значения по каждому критерию
        String avgQuery = "SELECT AVG(economy) as avg_economy, AVG(family) as avg_family, " +
                "AVG(health) as avg_health, AVG(freedom) as avg_freedom, " +
                "AVG(trust) as avg_trust, AVG(generosity) as avg_generosity, " +
                "AVG(happiness_score) as avg_happiness " +
                "FROM countries c " +
                "JOIN regions r ON c.region_id = r.id " +
                "WHERE r.name IN ('Western Europe', 'North America')";

        // Используем массивы из одного элемента, чтобы обойти ограничение лямбд
        final double[] averages = new double[7];
        executeAndPrintQuery(dbManager, avgQuery, rs -> {
            try {
                if (rs.next()) {
                    averages[0] = rs.getDouble("avg_economy");
                    averages[1] = rs.getDouble("avg_family");
                    averages[2] = rs.getDouble("avg_health");
                    averages[3] = rs.getDouble("avg_freedom");
                    averages[4] = rs.getDouble("avg_trust");
                    averages[5] = rs.getDouble("avg_generosity");
                    averages[6] = rs.getDouble("avg_happiness");

                    System.out.println("Средние показатели по регионам 'Western Europe' и 'North America':");
                    System.out.println("-".repeat(50));
                    System.out.printf("Экономика (GDP per Capita): %s%n", DECIMAL_FORMAT.format(averages[0]));
                    System.out.printf("Семья: %s%n", DECIMAL_FORMAT.format(averages[1]));
                    System.out.printf("Здоровье: %s%n", DECIMAL_FORMAT.format(averages[2]));
                    System.out.printf("Свобода: %s%n", DECIMAL_FORMAT.format(averages[3]));
                    System.out.printf("Доверие к правительству: %s%n", DECIMAL_FORMAT.format(averages[4]));
                    System.out.printf("Щедрость: %s%n", DECIMAL_FORMAT.format(averages[5]));
                    System.out.printf("Общий индекс счастья: %s%n", DECIMAL_FORMAT.format(averages[6]));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });

        // Теперь находим страну, наиболее близкую к средним показателям
        String query3 = "SELECT name, economy, family, health, freedom, trust, generosity, happiness_score " +
                "FROM countries " +
                "WHERE region_id IN (SELECT id FROM regions WHERE name IN ('Western Europe', 'North America'))";

        // Используем массивы из одного элемента для изменения значений внутри лямбды
        final Country[] closestCountry = new Country[1];
        final double[] minDeviation = {Double.MAX_VALUE};

        executeAndPrintQuery(dbManager, query3, rs -> {
            try {
                while (rs.next()) {
                    String countryName = rs.getString("name");
                    double economy = rs.getDouble("economy");
                    double family = rs.getDouble("family");
                    double health = rs.getDouble("health");
                    double freedom = rs.getDouble("freedom");
                    double trust = rs.getDouble("trust");
                    double generosity = rs.getDouble("generosity");
                    double happinessScore = rs.getDouble("happiness_score");

                    // Вычисляем отклонение от средних значений
                    double deviation = Math.abs(economy - averages[0]) +
                            Math.abs(family - averages[1]) +
                            Math.abs(health - averages[2]) +
                            Math.abs(freedom - averages[3]) +
                            Math.abs(trust - averages[4]) +
                            Math.abs(generosity - averages[5]) +
                            Math.abs(happinessScore - averages[6]);

                    if (deviation < minDeviation[0]) {
                        minDeviation[0] = deviation;
                        closestCountry[0] = new Country(0, countryName, null, 0, happinessScore, 0, economy, family, health, freedom, trust, generosity, 0);
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });

        if (closestCountry[0] != null) {
            System.out.println("\nСтрана со средними показателями по всем критериям:");
            System.out.println("-".repeat(50));
            System.out.printf("Страна: %s%n", closestCountry[0].getName());
            System.out.printf("Экономика (GDP per Capita): %s (среднее: %s)%n",
                    DECIMAL_FORMAT.format(closestCountry[0].getEconomy()), DECIMAL_FORMAT.format(averages[0]));
            System.out.printf("Семья: %s (среднее: %s)%n",
                    DECIMAL_FORMAT.format(closestCountry[0].getFamily()), DECIMAL_FORMAT.format(averages[1]));
            System.out.printf("Здоровье: %s (среднее: %s)%n",
                    DECIMAL_FORMAT.format(closestCountry[0].getHealth()), DECIMAL_FORMAT.format(averages[2]));
            System.out.printf("Свобода: %s (среднее: %s)%n",
                    DECIMAL_FORMAT.format(closestCountry[0].getFreedom()), DECIMAL_FORMAT.format(averages[3]));
            System.out.printf("Доверие к правительству: %s (среднее: %s)%n",
                    DECIMAL_FORMAT.format(closestCountry[0].getTrust()), DECIMAL_FORMAT.format(averages[4]));
            System.out.printf("Щедрость: %s (среднее: %s)%n",
                    DECIMAL_FORMAT.format(closestCountry[0].getGenerosity()), DECIMAL_FORMAT.format(averages[5]));
            System.out.printf("Общий индекс счастья: %s (среднее: %s)%n",
                    DECIMAL_FORMAT.format(closestCountry[0].getHappinessScore()), DECIMAL_FORMAT.format(averages[6]));
        }

        // Дополнительная визуализация: Топ-10 стран по общему индексу счастья
        System.out.println("\n===== ДОПОЛНИТЕЛЬНАЯ ВИЗУАЛИЗАЦИЯ: ТОП-10 СТРАН ПО ИНДЕКСУ СЧАСТЬЯ =====");
        String topHappinessQuery = "SELECT name, happiness_score FROM countries ORDER BY happiness_rank LIMIT 10";
        List<String> topCountries = new ArrayList<>();
        List<Double> happinessScores = new ArrayList<>();

        executeAndPrintQuery(dbManager, topHappinessQuery, rs -> {
            try {
                System.out.printf("%-30s | %s%n", "Страна", "Индекс счастья");
                System.out.println("-".repeat(50));
                int rank = 1;
                while (rs.next()) {
                    String countryName = rs.getString("name");
                    double score = rs.getDouble("happiness_score");
                    System.out.printf("%2d. %-27s | %s%n", rank++, countryName, DECIMAL_FORMAT.format(score));
                    topCountries.add(countryName);
                    happinessScores.add(score);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });

        // Создание диаграммы для топ-10 стран по счастью
        ChartGenerator.createTopCountriesChart(topCountries, happinessScores, "happiness_chart.png");

        // Шаг 5: Закрытие соединения с базой данных
        dbManager.closeConnection();

        System.out.println("\nПроект успешно выполнен!");
        System.out.println("\nДиаграммы сохранены в папке: charts/");
        System.out.println("- economy_chart.png");
        System.out.println("- happiness_chart.png");
        System.out.println("\nБаза данных сохранена в файле: " + DB_NAME);
    }

    private static void createChartsDirectory() {
        File dir = new File("charts");
        if (!dir.exists()) {
            dir.mkdirs();
            System.out.println("Папка для диаграмм создана: charts/");
        } else {
            System.out.println("Папка для диаграмм уже существует: charts/");
        }
    }

    private static void prepareCSVFile() {
        try {
            // Копируем CSV файл из ресурсов в корневую директорию для чтения
            InputStream inputStream = Main.class.getClassLoader().getResourceAsStream(CSV_FILE_NAME);
            if (inputStream == null) {
                System.out.println("CSV файл не найден в ресурсах!");
                // Пытаемся найти файл в текущей директории
                File currentFile = new File(CSV_FILE_NAME);
                if (currentFile.exists()) {
                    System.out.println("CSV файл найден в текущей директории");
                } else {
                    System.out.println("CSV файл не найден и в текущей директории!");
                }
                return;
            }

            Path tempFile = Path.of(CSV_FILE_NAME);
            Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("CSV файл успешно скопирован из ресурсов в корневую директорию");
        } catch (IOException e) {
            System.out.println("Ошибка при копировании CSV файла из ресурсов");
            e.printStackTrace();
        }
    }

    private static void parseCSVFile(List<Country> countries, Map<String, Region> regionsMap) {
        try (CSVReader reader = new CSVReader(new FileReader(CSV_FILE_NAME))) {
            // Пропускаем заголовок
            String[] header = reader.readNext();
            System.out.println("Заголовки CSV файла:");
            System.out.println(Arrays.toString(header));

            int regionCounter = 1;
            int countryCounter = 1;

            String[] nextLine;
            int validRows = 0;
            int invalidRows = 0;

            while ((nextLine = reader.readNext()) != null) {
                try {
                    if (nextLine.length < 12) {
                        System.out.println("Пропущена строка с недостаточным количеством данных: " + Arrays.toString(nextLine));
                        invalidRows++;
                        continue;
                    }

                    // Извлекаем данные из CSV
                    String countryName = nextLine[0];
                    String regionName = nextLine[1];
                    int happinessRank = Integer.parseInt(nextLine[2]);
                    double happinessScore = Double.parseDouble(nextLine[3]);
                    double standardError = Double.parseDouble(nextLine[4]);
                    double economy = Double.parseDouble(nextLine[5]);
                    double family = Double.parseDouble(nextLine[6]);
                    double health = Double.parseDouble(nextLine[7]);
                    double freedom = Double.parseDouble(nextLine[8]);
                    double trust = Double.parseDouble(nextLine[9]);
                    double generosity = Double.parseDouble(nextLine[10]);
                    double dystopiaResidual = Double.parseDouble(nextLine[11]);

                    // Проверяем, существует ли регион в мапе
                    Region region = regionsMap.get(regionName);
                    if (region == null) {
                        region = new Region(regionCounter++, regionName);
                        regionsMap.put(regionName, region);
                    }

                    // Создаем объект Country
                    Country country = new Country(
                            countryCounter++,
                            countryName,
                            region,
                            happinessRank,
                            happinessScore,
                            standardError,
                            economy,
                            family,
                            health,
                            freedom,
                            trust,
                            generosity,
                            dystopiaResidual
                    );

                    countries.add(country);
                    validRows++;
                } catch (Exception e) {
                    System.out.println("Ошибка при обработке строки: " + Arrays.toString(nextLine));
                    System.out.println("Причина: " + e.getMessage());
                    invalidRows++;
                }
            }

            System.out.println("\nCSV файл успешно обработан:");
            System.out.println("- Количество корректно обработанных строк: " + validRows);
            System.out.println("- Количество ошибочных строк: " + invalidRows);
            System.out.println("- Всего стран: " + countries.size());
            System.out.println("- Всего уникальных регионов: " + regionsMap.size());
        } catch (IOException | CsvValidationException e) {
            System.out.println("Ошибка при чтении CSV файла");
            e.printStackTrace();
        }
    }

    private static void executeAndPrintQuery(DatabaseManager dbManager, String query, Consumer<ResultSet> resultProcessor) {
        System.out.println("\nВыполняется SQL-запрос:");
        System.out.println(query);

        ResultSet rs = dbManager.executeQuery(query);
        if (rs != null) {
            resultProcessor.accept(rs);
            try {
                rs.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}