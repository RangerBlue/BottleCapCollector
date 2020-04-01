package com.km.BottleCapCollector.model;

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

    public BottleCap(String name) {
        this.capName = name;
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
