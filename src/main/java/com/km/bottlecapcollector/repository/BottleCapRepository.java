package com.km.bottlecapcollector.repository;

import com.km.bottlecapcollector.model.BottleCap;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BottleCapRepository extends CrudRepository<BottleCap, Long> {

    List<BottleCap> findByCapName(String name);

}
