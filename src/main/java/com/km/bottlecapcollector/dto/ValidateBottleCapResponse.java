package com.km.bottlecapcollector.dto;

import java.util.ArrayList;

public class ValidateBottleCapResponse {
    boolean isDuplicate;
    ArrayList<Long> similarCapsIDs;
    ArrayList<String> similarCapsURLs;
    int[] similarityDistribution;


    public ValidateBottleCapResponse(boolean isDuplicate, ArrayList<Long> similarCapsIDs,
                                     ArrayList<String> similarCapsURLs, int[] similarityDistribution) {
        this.isDuplicate = isDuplicate;
        this.similarCapsIDs = similarCapsIDs;
        this.similarCapsURLs = similarCapsURLs;
        this.similarityDistribution = similarityDistribution;
    }

    public boolean isDuplicate() {
        return isDuplicate;
    }

    public void setDuplicate(boolean duplicate) {
        isDuplicate = duplicate;
    }

    public ArrayList<Long> getSimilarCapsIDs() {
        return similarCapsIDs;
    }

    public void setSimilarCapsIDs(ArrayList<Long> similarCapsIDs) {
        this.similarCapsIDs = similarCapsIDs;
    }

    public ArrayList<String> getSimilarCapsURLs() {
        return similarCapsURLs;
    }

    public void setSimilarCapsURLs(ArrayList<String> similarCapsURLs) {
        this.similarCapsURLs = similarCapsURLs;
    }

    public int[] getSimilarityDistribution() {
        return similarityDistribution;
    }

    public void setSimilarityDistribution(int[] similarityDistribution) {
        this.similarityDistribution = similarityDistribution;
    }
}
