package com.km.BottleCapCollector.service;


import com.km.BottleCapCollector.model.BottleCap;
import com.km.BottleCapCollector.repository.BottleCapRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;
import java.util.Optional;

@Service
public class BottleCapService {

    private static final Logger logger = LogManager.getLogger(BottleCapService.class);

    @Autowired
    private BottleCapRepository repository;

    @Caching(evict =
    @CacheEvict(value = "caps", allEntries = true))
    public BottleCap addBottleCap(BottleCap cap) {
        return repository.save(cap);
    }

    @Cacheable(value = "caps")
    public List<BottleCap> getAllBottleCaps() {
        logger.info("Entering getAllBottleCap method in service layer");
        return (List<BottleCap>) repository.findAll();
    }

    public BottleCap getBottleCap(long id) {
        Optional<BottleCap> cap = repository.findById(id);
        if (cap.isPresent()) {
            return cap.get();
        } else {
            throw new IllegalArgumentException("Couldn't find record with given id");
        }
    }

    public void deleteBottleCapWithId(Long id) {
        repository.deleteById(id);
    }

    @Profile("admin")
    public void addBottleCapForInitialUpload(File file) {
        repository.save(new BottleCap(file.getName(), file.getName()));
    }

}