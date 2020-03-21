package com.km.BottleCapCollector.repository;

import com.km.BottleCapCollector.model.HistogramResult;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HistogramResultRepository extends CrudRepository<HistogramResult, Long> {
}
