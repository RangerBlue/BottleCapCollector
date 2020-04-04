package com.km.BottleCapCollector.service;

import com.km.BottleCapCollector.model.ComparisonRange;
import com.km.BottleCapCollector.model.HistogramResult;
import com.km.BottleCapCollector.repository.ComparisonRangeRepository;
import com.km.BottleCapCollector.util.ComparisonMethod;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;
import java.util.function.Function;

@Service
public class ComparisonRangeService {

    @Autowired
    private ComparisonRangeRepository repository;

    public List<ComparisonRange> getAll() {
        return (List<ComparisonRange>) repository.findAll();
    }

    public List<ComparisonRange> calculateMinMaxValuesOfAllComparisonMethods(List<HistogramResult> list) throws IllegalArgumentException {
        List<ComparisonRange> result = new ArrayList<>();
        result.add(calculateMinMaxValueOfMethod(list, ComparisonMethod.CORRELATION, HistogramResult::getCorrelation));
        result.add(calculateMinMaxValueOfMethod(list, ComparisonMethod.CHI_SQUARE, HistogramResult::getChisquare));
        result.add(calculateMinMaxValueOfMethod(list, ComparisonMethod.INTERSECTION, HistogramResult::getIntersection));
        result.add(calculateMinMaxValueOfMethod(list, ComparisonMethod.BHATTACHARYYA, HistogramResult::getBhattacharyya));
        return result;
    }

    public ComparisonRange calculateMinMaxValueOfMethod(List<HistogramResult> list, ComparisonMethod method, Function<HistogramResult, Double> toMethod) {
        OptionalDouble minValue = list.stream().mapToDouble(v -> toMethod.apply(v)).min();
        if (minValue.isPresent()) {
            OptionalDouble maxValue = list.stream().mapToDouble(v -> toMethod.apply(v)).max();
            ComparisonRange range = new ComparisonRange(method, minValue.getAsDouble(), maxValue.getAsDouble());
            repository.save(range);
            return range;
        } else {
            throw new IllegalArgumentException("There is no data in table");
        }
    }
}
