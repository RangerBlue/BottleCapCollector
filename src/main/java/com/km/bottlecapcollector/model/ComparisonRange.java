package com.km.bottlecapcollector.model;

import com.km.bottlecapcollector.opencv.ComparisonMethod;
import jakarta.persistence.*;

@Entity
public class ComparisonRange {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Enumerated(EnumType.STRING)
    private ComparisonMethod methodName;
    private double minValue;
    private double maxValue;

    public ComparisonRange() {
    }

    public ComparisonRange(ComparisonMethod methodName, double minValue, double maxValue) {
        this.methodName = methodName;
        this.minValue = minValue;
        this.maxValue = maxValue;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public ComparisonMethod getMethodName() {
        return methodName;
    }

    public void setMethodName(ComparisonMethod methodName) {
        this.methodName = methodName;
    }

    public double getMinValue() {
        return minValue;
    }

    public void setMinValue(double minValue) {
        this.minValue = minValue;
    }

    public double getMaxValue() {
        return maxValue;
    }

    public void setMaxValue(double maxValue) {
        this.maxValue = maxValue;
    }
}
