package com.km.bottlecapcollector.service;


import com.km.bottlecapcollector.exception.CapNotFoundException;
import com.km.bottlecapcollector.model.BottleCap;
import com.km.bottlecapcollector.repository.BottleCapRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;

@Service
@Slf4j
public class BottleCapService {

    @Autowired
    private BottleCapRepository repository;

    @Caching(evict =
    @CacheEvict(value = "caps", allEntries = true))
    public BottleCap addBottleCap(BottleCap cap) {
        return repository.save(cap);
    }

    @Cacheable(value = "caps")
    public List<BottleCap> getAllBottleCaps() {
        log.trace("Retrieving all caps from database");
        return (List<BottleCap>) repository.findAll();
    }

    public BottleCap getBottleCap(long id) {
        return repository.findById(id).orElseThrow(() -> new CapNotFoundException(id));
    }

    @Caching(evict =
    @CacheEvict(value = "caps", allEntries = true))
    public void deleteBottleCapWithId(Long id) {
        repository.deleteById(id);
    }

    @Profile("admin")
    public void addBottleCapForInitialUpload(File file) {
        repository.save(new BottleCap(file.getName(), file.getName()));
    }

}
