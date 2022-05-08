package com.km.bottlecapcollector.util;

import java.util.Arrays;
import java.util.Objects;

public class CustomMat {
    private byte[] matArray;
    private int cols;
    private int rows;

    public CustomMat(byte[] matArray, int cols, int rows) {
        this.matArray = matArray;
        this.cols = cols;
        this.rows = rows;
    }

    public byte[] getMatArray() {
        return matArray;
    }

    public void setMatArray(byte[] matArray) {
        this.matArray = matArray;
    }

    public int getCols() {
        return cols;
    }

    public void setCols(int cols) {
        this.cols = cols;
    }

    public int getRows() {
        return rows;
    }

    public void setRows(int rows) {
        this.rows = rows;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CustomMat that = (CustomMat) o;
        return cols == that.cols &&
                rows == that.rows &&
                Arrays.equals(matArray, that.matArray);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(cols, rows);
        result = 31 * result + Arrays.hashCode(matArray);
        return result;
    }
}
