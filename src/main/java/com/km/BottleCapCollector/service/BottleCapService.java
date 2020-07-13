package com.km.BottleCapCollector.service;


import com.km.BottleCapCollector.model.BottleCap;
import com.km.BottleCapCollector.repository.BottleCapRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;
import java.util.Optional;

@Service
public class BottleCapService {

    @Autowired
    private BottleCapRepository repository;

    public BottleCap addBottleCap(BottleCap cap){
        return repository.save(cap);
    }

    public List<BottleCap> getAllBottleCaps(){
        return (List<BottleCap>)repository.findAll();
    }

    public BottleCap getBottleCap(long id){
        Optional<BottleCap> cap = repository.findById(id);
        if(cap.isPresent()){
            return cap.get();
        } else{
            throw new IllegalArgumentException("Couldn't find record with given id");
        }
    }

    public void deleteBottleCapWithId(Long id){
        repository.deleteById(id);
    }

    //TODO move it into addBottleCapMethod
    public boolean isDuplicate(BottleCap newCap){
        return getAllBottleCaps().stream().anyMatch((old) -> old.equals(newCap));
    }

    @Profile("admin")
    public void addBottleCapForInitialUpload(File file){
        repository.save(new BottleCap(file.getName(), file.getName()));
    }

}