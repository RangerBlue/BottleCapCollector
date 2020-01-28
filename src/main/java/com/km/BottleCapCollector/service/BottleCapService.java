package com.km.BottleCapCollector.service;


import com.km.BottleCapCollector.model.BottleCap;
import com.km.BottleCapCollector.repository.BottleCapRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class BottleCapService {

    @Autowired
    private BottleCapRepository repository;

    public void addBottleCap(BottleCap cap){
        repository.save(cap);
    }

    public List<BottleCap> getAllBottleCaps(){
        return (List<BottleCap>)repository.findAll();
    }

    public BottleCap getBottleCap(Long id){
        Optional<BottleCap> cap = repository.findById(id);
        if(cap.isPresent()){
            return cap.get();
        } else{
            throw new IllegalArgumentException("Couldn't find record with given id");
        }
    }

}