package com.km.BottleCapCollector.model;

import com.km.BottleCapCollector.util.BottleCapMat;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "CAP")
public class BottleCap {

    @Id
    @Column(name="id")
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private long id;
    private String fileLocation;
    private String capName;
    private String creationDate;
    @Lob
    private byte[] data;
    private int cols;
    private int rows;

    public BottleCap(String name) {
        this.capName = name;
        this.creationDate = LocalDateTime.now().toString();
    }

    public BottleCap(String capName, BottleCapMat mat, String fileLocation) {
        this.capName = capName;
        this.data = mat.getMatArray();
        this.cols = mat.getCols();
        this.rows = mat.getRows();
        this.fileLocation = fileLocation;
        this.creationDate = LocalDateTime.now().toString();
    }

    public BottleCap(String capName, BottleCapMat mat) {
        this.capName = capName;
        this.data = mat.getMatArray();
        this.cols = mat.getCols();
        this.rows = mat.getRows();
        this.creationDate = LocalDateTime.now().toString();
    }

    public BottleCap(String capName, String fileName) {
        this.fileLocation = fileName;
        this.capName = capName;
        this.creationDate = LocalDateTime.now().toString();
    }

    public BottleCap(){
        this.creationDate = LocalDateTime.now().toString();
    }

    public long getId() {
        return id;
    }

    public String getCreationDate() {
        return creationDate;
    }

    public String getFileLocation() {
        return fileLocation;
    }

    public void setFileLocation(String fileLocation) {
        this.fileLocation = fileLocation;
    }

    public String getCapName() {
        return capName;
    }

    public void setCapName(String capName) {
        this.capName = capName;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
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
    public int hashCode() {
        return Objects.hash(id, fileLocation, capName, creationDate);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if(obj instanceof BottleCap){
            BottleCap cap = (BottleCap) obj;
            return cap.getCapName().equals(this.getCapName());
        }
        return false;
    }
}
