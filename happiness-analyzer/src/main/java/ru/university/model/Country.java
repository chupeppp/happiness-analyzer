package ru.university.model;

public class Country {
    private int id;
    private String name;
    private Region region;
    private int happinessRank;
    private double happinessScore;
    private double standardError;
    private double economy;
    private double family;
    private double health;
    private double freedom;
    private double trust;
    private double generosity;
    private double dystopiaResidual;

    public Country(int id, String name, Region region, int happinessRank, double happinessScore,
                   double standardError, double economy, double family, double health, double freedom,
                   double trust, double generosity, double dystopiaResidual) {
        this.id = id;
        this.name = name;
        this.region = region;
        this.happinessRank = happinessRank;
        this.happinessScore = happinessScore;
        this.standardError = standardError;
        this.economy = economy;
        this.family = family;
        this.health = health;
        this.freedom = freedom;
        this.trust = trust;
        this.generosity = generosity;
        this.dystopiaResidual = dystopiaResidual;
    }

    // Геттеры и сеттеры
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Region getRegion() {
        return region;
    }

    public void setRegion(Region region) {
        this.region = region;
    }

    public int getHappinessRank() {
        return happinessRank;
    }

    public void setHappinessRank(int happinessRank) {
        this.happinessRank = happinessRank;
    }

    public double getHappinessScore() {
        return happinessScore;
    }

    public void setHappinessScore(double happinessScore) {
        this.happinessScore = happinessScore;
    }

    public double getStandardError() {
        return standardError;
    }

    public void setStandardError(double standardError) {
        this.standardError = standardError;
    }

    public double getEconomy() {
        return economy;
    }

    public void setEconomy(double economy) {
        this.economy = economy;
    }

    public double getFamily() {
        return family;
    }

    public void setFamily(double family) {
        this.family = family;
    }

    public double getHealth() {
        return health;
    }

    public void setHealth(double health) {
        this.health = health;
    }

    public double getFreedom() {
        return freedom;
    }

    public void setFreedom(double freedom) {
        this.freedom = freedom;
    }

    public double getTrust() {
        return trust;
    }

    public void setTrust(double trust) {
        this.trust = trust;
    }

    public double getGenerosity() {
        return generosity;
    }

    public void setGenerosity(double generosity) {
        this.generosity = generosity;
    }

    public double getDystopiaResidual() {
        return dystopiaResidual;
    }

    public void setDystopiaResidual(double dystopiaResidual) {
        this.dystopiaResidual = dystopiaResidual;
    }

    @Override
    public String toString() {
        return "Country{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", region=" + (region != null ? region.getName() : "null") +
                ", happinessRank=" + happinessRank +
                ", happinessScore=" + happinessScore +
                '}';
    }
}