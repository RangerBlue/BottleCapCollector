package com.km.bottlecapcollector.repository;

import com.km.bottlecapcollector.model.ComparisonRange;
import com.km.bottlecapcollector.util.ComparisonMethod;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface ComparisonRangeRepository extends CrudRepository<ComparisonRange, Integer> {
    @Query("SELECT range FROM ComparisonRange range WHERE range.methodName = :method")
    public ComparisonRange findComparisonRangeByComparisonMethod(ComparisonMethod method);
}
