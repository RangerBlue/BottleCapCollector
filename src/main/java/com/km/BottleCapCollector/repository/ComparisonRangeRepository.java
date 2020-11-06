package com.km.BottleCapCollector.repository;

import com.km.BottleCapCollector.model.ComparisonRange;
import com.km.BottleCapCollector.util.ComparisonMethod;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface ComparisonRangeRepository extends CrudRepository<ComparisonRange, Integer> {
    @Query("SELECT range FROM ComparisonRange range WHERE range.methodName = :method")
    public ComparisonRange findComparisonRangeByComparisonMethod(ComparisonMethod method);
}
