package com.km.bottlecapcollector.util;

import com.km.bottlecapcollector.model.CapItem;

import java.util.Objects;

public class HistogramResult {
    private CapItem firstCap;
    private CapItem secondCap;

    private double correlation;
    private double chisquare;
    private double intersection;
    private double bhattacharyya;
    private Double similarity;

    public HistogramResult() {
    }

    public HistogramResult(double correlation, double chisquare, double intersection, double bhattacharyya) {
        this.correlation = correlation;
        this.chisquare = chisquare;
        this.intersection = intersection;
        this.bhattacharyya = bhattacharyya;
    }

    public double getCorrelation() {
        return correlation;
    }

    public double getChisquare() {
        return chisquare;
    }

    public double getIntersection() {
        return intersection;
    }

    public double getBhattacharyya() {
        return bhattacharyya;
    }

    public void setCorrelation(double correlation) {
        this.correlation = correlation;
    }

    public void setChisquare(double chisquare) {
        this.chisquare = chisquare;
    }

    public void setIntersection(double intersection) {
        this.intersection = intersection;
    }

    public void setBhattacharyya(double bhattacharyya) {
        this.bhattacharyya = bhattacharyya;
    }

    public CapItem getFirstCap() {
        return firstCap;
    }

    public void setFirstCap(CapItem firstCap) {
        this.firstCap = firstCap;
    }

    public CapItem getSecondCap() {
        return secondCap;
    }

    public void setSecondCap(CapItem secondCap) {
        this.secondCap = secondCap;
    }

    public double getSimilarity() {
        return similarity;
    }

    public void setSimilarity(double similarity) {
        this.similarity = similarity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HistogramResult that = (HistogramResult) o;
        return  Double.compare(that.correlation, correlation) == 0 &&
                Double.compare(that.chisquare, chisquare) == 0 &&
                Double.compare(that.intersection, intersection) == 0 &&
                Double.compare(that.bhattacharyya, bhattacharyya) == 0 &&
                Double.compare(that.similarity, similarity) == 0 &&
                Objects.equals(firstCap, that.firstCap) &&
                Objects.equals(secondCap, that.secondCap);
    }

    @Override
    public int hashCode() {
        return Objects.hash(firstCap, secondCap, correlation, chisquare, intersection, bhattacharyya, similarity);
    }

    @Override
    public String toString() {
        return "HistogramResult{" +
                "firstCap=" + firstCap.getId() +
                ", secondCap=" + secondCap.getId() +
                ", correlation=" + correlation +
                ", chisquare=" + chisquare +
                ", intersection=" + intersection +
                ", bhattacharyya=" + bhattacharyya +
                ", similarity=" + similarity +
                '}';
    }
}
