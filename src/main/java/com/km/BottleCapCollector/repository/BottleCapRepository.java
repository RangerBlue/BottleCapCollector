package com.km.BottleCapCollector.repository;

import com.km.BottleCapCollector.model.BottleCap;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BottleCapRepository extends CrudRepository<BottleCap, Long> {

    List<BottleCap> findByCapName(String name);

}
