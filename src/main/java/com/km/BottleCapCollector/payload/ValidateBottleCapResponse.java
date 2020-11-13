package com.km.BottleCapCollector.payload;

import java.util.ArrayList;

public class ValidateBottleCapResponse {
    boolean isDuplicate;
    ArrayList<Long> similarCapsIDs;
    ArrayList<String> similarCapsURLs;

    public ValidateBottleCapResponse(boolean isDuplicate, ArrayList<Long> similarCapsIDs, ArrayList<String> similarCapsURLs) {
        this.isDuplicate = isDuplicate;
        this.similarCapsIDs = similarCapsIDs;
        this.similarCapsURLs = similarCapsURLs;
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
}
