package com.km.BottleCapCollector.service;

import com.km.BottleCapCollector.exception.DuplicateCapException;
import com.km.BottleCapCollector.model.ComparisonRange;
import com.km.BottleCapCollector.model.HistogramResult;
import com.km.BottleCapCollector.repository.ComparisonRangeRepository;
import com.km.BottleCapCollector.util.ComparisonMethod;
import com.km.BottleCapCollector.util.ImageHistogramFactory;
import com.km.BottleCapCollector.util.SimilarityModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;
import java.util.Set;
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

    public double calculateSimilarityForCap(List<HistogramResult> histogramCalculation) {
        List<ComparisonRange> range = getAll();
        try {
            double sum = histogramCalculation.stream().parallel()
                    .map(histogramResult -> calculateSimilarityForAllMethods(histogramResult, range))
                    .mapToDouble(HistogramResult::getSimilarity).sum();
            return sum / (histogramCalculation.size());
        } catch (DuplicateCapException e) {
            return 1;
        }
    }

    public SimilarityModel calculateSimilarityModelForCap(List<HistogramResult> histogramCalculation) {
        List<ComparisonRange> range = getAll();
        SimilarityModel model = new SimilarityModel();
        try {
            histogramCalculation.stream().parallel().forEach(histogramResult -> model.addValue(calculateSimilarityForAllMethods(histogramResult, range)));
        } catch (DuplicateCapException e) {
            model.markModelAsDuplicate(histogramCalculation.size());
        } finally {
            Set<HistogramResult> top = model.calculateTopSimilar();
            model.setSimilarCaps(top);
            return model;
        }
    }

    public HistogramResult calculateSimilarityForAllMethods(HistogramResult histogramCalculation, List<ComparisonRange> range) {
        double correlation = calculateSimilarityForCorrelation(histogramCalculation, range.get(0));
        double chisquare = calculateSimilarityForChisquare(histogramCalculation, range.get(1));
        double intersection = calculateSimilarityForIntersection(histogramCalculation, range.get(2));
        double bhattacharyya = calculateSimilarityForBhattacharyya(histogramCalculation, range.get(3));
        histogramCalculation.setSimilarity((correlation + chisquare + intersection + bhattacharyya) / 4);
        return histogramCalculation;
    }

    public double calculateSimilarityForCorrelation(HistogramResult histogramCalculation, ComparisonRange range) {
        double min = range.getMinValue();
        double max = range.getMaxValue();
        double value = histogramCalculation.getCorrelation();
        if (value < ImageHistogramFactory.CORRELATION_BASE && value > 0) {
            return (value - min) / (max - min);
        } else {
            throw new DuplicateCapException("You have already got this picture");
        }
    }


    public double calculateSimilarityForChisquare(HistogramResult histogramCalculation, ComparisonRange range) {
        double min = range.getMinValue();
        double max = range.getMaxValue();
        double value = histogramCalculation.getChisquare();
        if (value > ImageHistogramFactory.CHI_SQUARE_BASE) {
            return (max - value) / (max - min);
        } else {
            throw new DuplicateCapException("You have already got this picture");
        }
    }

    public double calculateSimilarityForIntersection(HistogramResult histogramCalculation, ComparisonRange range) {
        double min = range.getMinValue();
        double max = range.getMaxValue();
        double value = histogramCalculation.getIntersection();
        if (value > 0 && value < ImageHistogramFactory.INTERSECTION_BASE) {
            return (value - min) / (max - min);
        } else {
            throw new DuplicateCapException("You have already got this picture");
        }
    }

    public double calculateSimilarityForBhattacharyya(HistogramResult histogramCalculation, ComparisonRange range) {
        double min = range.getMinValue();
        double max = range.getMaxValue();
        double value = histogramCalculation.getBhattacharyya();
        if (value < 1 && value > ImageHistogramFactory.BHATTACHARYYA_BASE) {
            return (max - value) / (max - min);
        } else {
            throw new DuplicateCapException("You have already got this picture");
        }
    }
}
