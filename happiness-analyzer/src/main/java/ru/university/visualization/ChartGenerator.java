package ru.university.visualization;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.labels.StandardCategoryItemLabelGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Map;

public class ChartGenerator {

    private static final DecimalFormat DECIMAL_FORMAT;

    static {
        DECIMAL_FORMAT = new DecimalFormat("0.00");
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setDecimalSeparator('.');
        DECIMAL_FORMAT.setDecimalFormatSymbols(symbols);
    }

    public static void createEconomyChart(Map<String, Double> economyData, String filename) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        // Добавляем все данные в датасет
        for (Map.Entry<String, Double> entry : economyData.entrySet()) {
            dataset.addValue(entry.getValue(), "Economy", entry.getKey());
        }

        // Создаем диаграмму
        JFreeChart barChart = ChartFactory.createBarChart(
                "Средний GDP per Capita по регионам", // Название диаграммы
                "Country", // Название оси X
                "GDP per Capita", // Название оси Y
                dataset,
                PlotOrientation.VERTICAL,
                true, true, false);

        // Настраиваем внешний вид диаграммы
        barChart.getTitle().setFont(new Font("SansSerif", Font.BOLD, 16));
        barChart.getTitle().setPaint(Color.DARK_GRAY);

        CategoryPlot plot = barChart.getCategoryPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);

        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setSeriesPaint(0, new Color(41, 98, 255)); // Цвет столбцов

        // Исправленные методы для JFreeChart 1.5.4
        renderer.setDefaultItemLabelGenerator(new StandardCategoryItemLabelGenerator("{2}", DECIMAL_FORMAT));
        renderer.setDefaultItemLabelsVisible(true);
        renderer.setDefaultItemLabelPaint(Color.DARK_GRAY);
        renderer.setDefaultItemLabelFont(new Font("SansSerif", Font.PLAIN, 10));

        CategoryAxis domainAxis = plot.getDomainAxis();
        domainAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_45);
        domainAxis.setTickLabelFont(new Font("SansSerif", Font.PLAIN, 10));
        domainAxis.setTickLabelPaint(Color.DARK_GRAY);

        ValueAxis rangeAxis = plot.getRangeAxis();
        rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        rangeAxis.setTickLabelFont(new Font("SansSerif", Font.PLAIN, 10));
        rangeAxis.setTickLabelPaint(Color.DARK_GRAY);

        // Создаем папку для диаграмм, если ее нет
        File dir = new File("charts");
        if (!dir.exists()) {
            dir.mkdirs();
        }

        // Сохраняем диаграмму в файл
        try {
            ChartUtils.saveChartAsPNG(new File("charts/" + filename), barChart, 1000, 600);
            System.out.println("Диаграмма экономики успешно сохранена в файл: charts/" + filename);
        } catch (IOException e) {
            System.out.println("Ошибка при сохранении диаграммы экономики");
            e.printStackTrace();
        }
    }

    public static void createTopCountriesChart(List<String> countries, List<Double> scores, String filename) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        for (int i = 0; i < countries.size(); i++) {
            dataset.addValue(scores.get(i), "Happiness Score", countries.get(i));
        }

        JFreeChart barChart = ChartFactory.createBarChart(
                "Top 10 Happiest Countries (2015)",
                "Country",
                "Happiness Score",
                dataset,
                PlotOrientation.VERTICAL,
                true, true, false);

        barChart.getTitle().setFont(new Font("SansSerif", Font.BOLD, 16));
        barChart.getTitle().setPaint(Color.DARK_GRAY);

        CategoryPlot plot = barChart.getCategoryPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);

        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setSeriesPaint(0, new Color(41, 182, 246)); // Цвет столбцов

        // Исправленные методы для JFreeChart 1.5.4
        renderer.setDefaultItemLabelGenerator(new StandardCategoryItemLabelGenerator("{2}", DECIMAL_FORMAT));
        renderer.setDefaultItemLabelsVisible(true);
        renderer.setDefaultItemLabelPaint(Color.DARK_GRAY);
        renderer.setDefaultItemLabelFont(new Font("SansSerif", Font.PLAIN, 10));

        CategoryAxis domainAxis = plot.getDomainAxis();
        domainAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_45);
        domainAxis.setTickLabelFont(new Font("SansSerif", Font.PLAIN, 10));
        domainAxis.setTickLabelPaint(Color.DARK_GRAY);

        ValueAxis rangeAxis = plot.getRangeAxis();
        rangeAxis.setTickLabelFont(new Font("SansSerif", Font.PLAIN, 10));
        rangeAxis.setTickLabelPaint(Color.DARK_GRAY);

        // Создаем папку для диаграмм, если ее нет
        File dir = new File("charts");
        if (!dir.exists()) {
            dir.mkdirs();
        }

        // Сохраняем диаграмму в файл
        try {
            ChartUtils.saveChartAsPNG(new File("charts/" + filename), barChart, 1000, 600);
            System.out.println("Диаграмма индекса счастья успешно сохранена в файл: charts/" + filename);
        } catch (IOException e) {
            System.out.println("Ошибка при сохранении диаграммы индекса счастья");
            e.printStackTrace();
        }
    }
}