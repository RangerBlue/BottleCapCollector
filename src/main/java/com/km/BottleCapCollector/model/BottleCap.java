package com.km.BottleCapCollector.model;


import org.opencv.core.Mat;
import org.springframework.beans.factory.annotation.Value;

import javax.persistence.*;
import java.nio.file.Paths;
import java.time.LocalDateTime;

@Entity
@Table(name = "CAP")
public class BottleCap {

    @Id
    @Column(name="id")
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private long id;
    private String name;
    private String creationDate;

    public BottleCap(String name) {
        this.name = name;
        this.creationDate = LocalDateTime.now().toString();
    }

    public BottleCap(){
        this.creationDate = LocalDateTime.now().toString();
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    public String getCreationDate() {
        return creationDate;
    }


    @Override
    public String toString() {
        return "BottleCap{" +
                "name='" + name + '\'' +
                ", creationDate=" + creationDate +
                '}';
    }

    @Override
    public int hashCode() {
        return (int) (17 * this.name.hashCode() + 31 * id);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if(obj instanceof BottleCap){
            BottleCap cap = (BottleCap) obj;
            return cap.name.equals(this.name);
        }
        return false;
    }
}
