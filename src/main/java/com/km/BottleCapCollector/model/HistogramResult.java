package com.km.BottleCapCollector.model;

import javax.persistence.*;

@Entity
@Table(name = "HISTOGRAMRESULT")
public class HistogramResult {
    @Id
    @Column(name="id")
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private long id;

    @OneToOne
    private BottleCap firstCap;

    @OneToOne
    private BottleCap secondCap;

    private double correlation;
    private double chisquare;
    private double intersection;
    private double bhattacharyya;

    public HistogramResult() {
    }

    public HistogramResult(double correlation, double chisquare, double intersection, double bhattacharyya) {
        this.correlation = correlation;
        this.chisquare = chisquare;
        this.intersection = intersection;
        this.bhattacharyya = bhattacharyya;
    }

    public long getId() {
        return id;
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

    public BottleCap getFirstCap() {
        return firstCap;
    }

    public void setFirstCap(BottleCap firstCap) {
        this.firstCap = firstCap;
    }

    public BottleCap getSecondCap() {
        return secondCap;
    }

    public void setSecondCap(BottleCap secondCap) {
        this.secondCap = secondCap;
    }
}
