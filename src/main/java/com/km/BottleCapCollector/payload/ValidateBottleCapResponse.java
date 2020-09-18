package com.km.BottleCapCollector.payload;

import java.util.ArrayList;

public class ValidateBottleCapResponse {
    boolean isDuplicate;
    ArrayList<Long> similarCapsIDs;

    public ValidateBottleCapResponse(boolean isDuplicate, ArrayList<Long> similarCapsIDs) {
        this.isDuplicate = isDuplicate;
        this.similarCapsIDs = similarCapsIDs;
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
}
