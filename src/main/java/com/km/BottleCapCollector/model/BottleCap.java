package com.km.BottleCapCollector.model;


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
    private String path;
    private String creationDate;

    public BottleCap(String name) {
        this.name = name;
        this.creationDate = LocalDateTime.now().toString();
    }

    public BottleCap(String name, String path) {
        this.name = name;
        this.path = path;
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

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getCreationDate() {
        return creationDate;
    }


    @Override
    public String toString() {
        return "BottleCap{" +
                "name='" + name + '\'' +
                ", path=" + path +
                ", creationDate=" + creationDate +
                '}';
    }
}
