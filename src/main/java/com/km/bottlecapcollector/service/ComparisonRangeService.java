package com.km.bottlecapcollector.service;

import com.km.bottlecapcollector.exception.DuplicateCapException;
import com.km.bottlecapcollector.model.ComparisonRange;
import com.km.bottlecapcollector.model.OpenCVImageSignature;
import com.km.bottlecapcollector.opencv.HistogramResult;
import com.km.bottlecapcollector.repository.ComparisonRangeRepository;
import com.km.bottlecapcollector.opencv.ComparisonMethod;
import com.km.bottlecapcollector.opencv.ImageHistogramUtil;
import com.km.bottlecapcollector.util.SimilarityModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;

@Service
@Slf4j
public class ComparisonRangeService {

    @Autowired
    private ComparisonRangeRepository repository;

    ImageHistogramUtil imageHistogramUtil = new ImageHistogramUtil();

    public List<ComparisonRange> getAll() {
        return (List<ComparisonRange>) repository.findAll();
    }

    public ComparisonRange getByComparisonMethod(ComparisonMethod method) {
        return repository.findComparisonRangeByComparisonMethod(method);
    }

    public List<ComparisonRange> calculateMinMaxValuesOfAllComparisonMethods(List<HistogramResult> list) throws IllegalArgumentException {
        log.info("Calculating min and max values in calculateMinMaxValuesOfAllComparisonMethods() method");
        List<ComparisonRange> result = new ArrayList<>();
        result.add(calculateMinMaxValueOfMethod(list, ComparisonMethod.CORRELATION, HistogramResult::getCorrelation));
        result.add(calculateMinMaxValueOfMethod(list, ComparisonMethod.CHI_SQUARE, HistogramResult::getChisquare));
        result.add(calculateMinMaxValueOfMethod(list, ComparisonMethod.BHATTACHARYYA, HistogramResult::getBhattacharyya));
        log.info("Calculated values for methods : CORRELATION - MIN:" + result.get(0).getMinValue() + " MAX:" + result.get(0).getMaxValue());
        log.info("Calculated values for methods : CHI_SQUARE - MIN:" + result.get(1).getMinValue() + " MAX:" + result.get(1).getMaxValue());
        log.info("Calculated values for methods : BHATTACHARYYA - MIN:" + result.get(2).getMinValue() + " MAX:" + result.get(2).getMaxValue());

        return result;
    }

    public ComparisonRange calculateMinMaxValueOfMethod(List<HistogramResult> list, ComparisonMethod method, Function<HistogramResult, Double> toMethod) {
        OptionalDouble minValue = list.stream().mapToDouble(v -> toMethod.apply(v)).min();
        if (minValue.isPresent()) {
            OptionalDouble maxValue = list.stream().mapToDouble(v -> toMethod.apply(v)).max();
            Optional<ComparisonRange> existingValue = Optional.ofNullable(getByComparisonMethod(method));
            if (existingValue.isPresent()) {
                log.info("There are comparison ranges in table for method " + method.name());
                ComparisonRange rangeToUpdate = existingValue.get();
                if (existingValue.get().getMinValue() > minValue.getAsDouble()) {
                    rangeToUpdate.setMinValue(minValue.getAsDouble());
                    log.info("Updating new minimum for " + method.name() + " with value " + minValue.getAsDouble());
                }
                if (existingValue.get().getMaxValue() < maxValue.getAsDouble()) {
                    rangeToUpdate.setMaxValue(maxValue.getAsDouble());
                    log.info("Updating new maximum for " + method.name() + " with value " + maxValue.getAsDouble());
                }
                repository.save(rangeToUpdate);
                return rangeToUpdate;
            } else {
                log.info("Comparison ranges table is empty, adding new values");
                ComparisonRange range = new ComparisonRange(method, minValue.getAsDouble(), maxValue.getAsDouble());
                repository.save(range);
                return range;
            }
        } else {
            throw new IllegalArgumentException("There is no data to perform calculations");
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

    public SimilarityModel calculateSimilarityModelForCap(List<HistogramResult> histogramCalculation, int capAmount) {
        log.info("Calculating similarity in calculateSimilarityModelForCap() method");
        List<ComparisonRange> range = getAll();
        SimilarityModel model = new SimilarityModel();

        histogramCalculation.stream().parallel().forEach(
                histogramResult -> {
                    try {
                        model.addValue(calculateSimilarityForAllMethods(histogramResult, range));
                    } catch (DuplicateCapException e) {
                        model.setDuplicate(true);
                    }
                });
        Set<HistogramResult> top = model.calculateTopSimilar(capAmount);
        model.setSimilarCaps(top);
        log.info("Calculated similarity for cap ID : " + histogramCalculation.get(0).getFirstCap().getId() +
                " Duplicate " + model.isDuplicate() +
                " | 0-10% " + model.getFrom00To10() +
                "| 10-20% " + model.getFrom10To20() +
                "| 30-40% " + model.getFrom30To40() +
                "| 40-50% " + model.getFrom40To50() +
                "| 50-60% " + model.getFrom50To60() +
                "| 60-70% " + model.getFrom60To70() +
                "| 70-80% " + model.getFrom70To80() +
                "| 80-90% " + model.getFrom80To90() +
                "| 90-100% " + model.getFrom90To100()
        );
        if (!model.isDuplicate()) {
            Set<HistogramResult> similarCaps = model.getSimilarCaps();
            log.info("Similar caps:");
            similarCaps.forEach(histogramResult -> {
                log.info("ID " + histogramResult.getSecondCap().getId() + ", name " + histogramResult.getSecondCap().getName());
            });
        }
        return model;
    }

    public HistogramResult calculateSimilarityForAllMethods(HistogramResult histogramCalculation, List<ComparisonRange> range) {
        double correlation = calculateSimilarityForCorrelation(histogramCalculation, range.get(0));
        double chisquare = calculateSimilarityForChisquare(histogramCalculation, range.get(1));
        double intersection = calculateSimilarityForIntersection(histogramCalculation);
        double bhattacharyya = calculateSimilarityForBhattacharyya(histogramCalculation, range.get(2));
        histogramCalculation.setSimilarity((correlation + chisquare + intersection + bhattacharyya) / 4);
        return histogramCalculation;
    }

    public double calculateSimilarityForCorrelation(HistogramResult histogramCalculation, ComparisonRange range) {
        double min = range.getMinValue();
        double max = range.getMaxValue();
        double value = histogramCalculation.getCorrelation();
        if (value < imageHistogramUtil.CORRELATION_BASE() && value > 0) {
            return (value - min) / (max - min);
        } else {
            log.info("DuplicateCapException in calculateSimilarityForCorrelation method, range min: " + min +
                    " range max: " + max + " value: " + value + " in cap " + histogramCalculation.getFirstCap().getId() +
                    " and cap " + histogramCalculation.getSecondCap().getId());
            throw new DuplicateCapException("You have already got this picture");
        }
    }


    public double calculateSimilarityForChisquare(HistogramResult histogramCalculation, ComparisonRange range) {
        double min = range.getMinValue();
        double max = range.getMaxValue();
        double value = histogramCalculation.getChisquare();
        if (value > imageHistogramUtil.CHI_SQUARE_BASE()) {
            return (max - value) / (max - min);
        } else {
            log.info("DuplicateCapException in calculateSimilarityForChisquare method, range min: " + min +
                    "range max: " + max + "value: " + value + "in cap " + histogramCalculation.getFirstCap().getId() +
                    " and cap " + histogramCalculation.getSecondCap().getId());
            throw new DuplicateCapException("You have already got this picture");
        }
    }

    public double calculateSimilarityForIntersection(HistogramResult histogramCalculation) {
        OpenCVImageSignature secondCapSignature = (OpenCVImageSignature) histogramCalculation.getSecondCap().getImage().getSignature();
        double value = histogramCalculation.getIntersection() / secondCapSignature.getIntersectionValue();
        if (value != 1) {
            return value;
        } else {
            log.info("DuplicateCapException in calculateSimilarityForIntersection method, value: " + value +
                    "in cap " + histogramCalculation.getFirstCap().getId() +
                    " and cap " + histogramCalculation.getSecondCap().getId());
            throw new DuplicateCapException("You have already got this picture");
        }
    }

    public double calculateSimilarityForBhattacharyya(HistogramResult histogramCalculation, ComparisonRange range) {
        double min = range.getMinValue();
        double max = range.getMaxValue();
        double value = histogramCalculation.getBhattacharyya();
        if (value < 1 && value > imageHistogramUtil.BHATTACHARYYA_BASE()) {
            return (max - value) / (max - min);
        } else {
            log.info("DuplicateCapException in calculateSimilarityForBhattacharyya method, range min: " + min +
                    "range max: " + max + "value: " + value + "in cap " + histogramCalculation.getFirstCap().getId() +
                    " and cap " + histogramCalculation.getSecondCap().getId());
            throw new DuplicateCapException("You have already got this picture");
        }
    }
}
