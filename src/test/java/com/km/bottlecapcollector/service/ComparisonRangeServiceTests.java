package com.km.bottlecapcollector.service;

import com.km.bottlecapcollector.exception.DuplicateCapException;
import com.km.bottlecapcollector.model.CapItem;
import com.km.bottlecapcollector.model.ComparisonRange;
import com.km.bottlecapcollector.model.OpenCVImageSignature;
import com.km.bottlecapcollector.opencv.ComparisonMethod;
import com.km.bottlecapcollector.opencv.HistogramResult;
import com.km.bottlecapcollector.opencv.ImageHistogramUtil;
import com.km.bottlecapcollector.util.*;
import com.km.bottlecapcollector.repository.ComparisonRangeRepository;
import org.junit.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ComparisonRangeServiceTests {

    @InjectMocks
    private ComparisonRangeService service;

    @Mock
    private ComparisonRangeRepository repository;

    @Spy
    private ImageHistogramUtil imageHistogramUtil;

    ItemFactoryGoogleImpl itemFactory = new ItemFactoryGoogleImpl();

    @Test
    public void testCalculateMethodMaxMinValues() {
        HistogramResult result1 = new HistogramResult(1, 2, 3, 4);
        result1.setFirstCap(new CapItem());
        result1.setSecondCap(new CapItem());
        HistogramResult result2 = new HistogramResult(5, 6, 7, 8);
        result2.setFirstCap(new CapItem());
        result2.setSecondCap(new CapItem());
        HistogramResult result3 = new HistogramResult(9, 10, 11, 12);
        result3.setFirstCap(new CapItem());
        result3.setSecondCap(new CapItem());
        List<HistogramResult> histogramList = new ArrayList<>();
        histogramList.add(result1);
        histogramList.add(result2);
        histogramList.add(result3);

        List<ComparisonRange> result = service.calculateMinMaxValuesOfAllComparisonMethods(histogramList);
        assertEquals(1, result.stream().filter(item -> item.getMethodName().equals(ComparisonMethod.CORRELATION)).findFirst().get().getMinValue());
        assertEquals(9, result.stream().filter(item -> item.getMethodName().equals(ComparisonMethod.CORRELATION)).findFirst().get().getMaxValue());

        assertEquals(2, result.stream().filter(item -> item.getMethodName().equals(ComparisonMethod.CHI_SQUARE)).findFirst().get().getMinValue());
        assertEquals(10, result.stream().filter(item -> item.getMethodName().equals(ComparisonMethod.CHI_SQUARE)).findFirst().get().getMaxValue());

        assertEquals(4, result.stream().filter(item -> item.getMethodName().equals(ComparisonMethod.BHATTACHARYYA)).findFirst().get().getMinValue());
        assertEquals(12, result.stream().filter(item -> item.getMethodName().equals(ComparisonMethod.BHATTACHARYYA)).findFirst().get().getMaxValue());
    }

    @Test
    public void testCalculateSimilarityForCorrelationSuccess() {
        HistogramResult histogramResult = new HistogramResult(0.8, 401, 8, 0.3);
        ComparisonRange range = new ComparisonRange(ComparisonMethod.CORRELATION, 0.4, 0.9);
        histogramResult.setFirstCap(itemFactory.getCapItem());
        histogramResult.setSecondCap(itemFactory.getCapItem());
        double result = service.calculateSimilarityForCorrelation(histogramResult, range);
        assertEquals(0.8, result);
    }

    @Test(expected = DuplicateCapException.class)
    public void testCalculateSimilarityForCorrelationFail() {
        HistogramResult histogramResult = new HistogramResult(1.2, 401, 8, 0.3);
        ComparisonRange range = new ComparisonRange(ComparisonMethod.CORRELATION, 0.4, 0.9);
        histogramResult.setFirstCap(itemFactory.getCapItem());
        histogramResult.setSecondCap(itemFactory.getCapItem());
        service.calculateSimilarityForCorrelation(histogramResult, range);
    }

    @Test
    public void testCalculateSimilarityForChisquareSuccess() {
        HistogramResult histogramResult = new HistogramResult(0.8, 401, 8, 0.3);
        ComparisonRange range = new ComparisonRange(ComparisonMethod.CHI_SQUARE, 1, 1001);
        histogramResult.setFirstCap(itemFactory.getCapItem());
        histogramResult.setSecondCap(itemFactory.getCapItem());
        double result = service.calculateSimilarityForChisquare(histogramResult, range);
        assertEquals(0.6, result);
    }

    @Test(expected = DuplicateCapException.class)
    public void testCalculateSimilarityForChisquareFail() {
        HistogramResult histogramResult = new HistogramResult(1.2, -1, 8, 0.3);
        ComparisonRange range = new ComparisonRange(ComparisonMethod.CHI_SQUARE, 0.4, 0.9);
        histogramResult.setFirstCap(itemFactory.getCapItem());
        histogramResult.setSecondCap(itemFactory.getCapItem());
        service.calculateSimilarityForChisquare(histogramResult, range);
    }

    @Test
    public void testCalculateSimilarityForIntersectionSuccess() {
        HistogramResult histogramResult = new HistogramResult(0.8, 401, 7.5, 0.3);
        histogramResult.setFirstCap(itemFactory.getCapItem());
        histogramResult.setSecondCap(itemFactory.getCapItem());
        OpenCVImageSignature openCVImageSignature = (OpenCVImageSignature) histogramResult.getSecondCap().getImage().getSignature();
        openCVImageSignature.setIntersectionValue(10);

        double result = service.calculateSimilarityForIntersection(histogramResult);
        assertEquals(0.75, result);
    }

    @Test(expected = DuplicateCapException.class)
    public void testCalculateSimilarityForIntersectionFail() {
        HistogramResult histogramResult = new HistogramResult(1.2, 401, 10, 0.3);
        histogramResult.setFirstCap(itemFactory.getCapItem());
        histogramResult.setSecondCap(itemFactory.getCapItem());
        OpenCVImageSignature openCVImageSignature = (OpenCVImageSignature) histogramResult.getSecondCap().getImage().getSignature();
        openCVImageSignature.setIntersectionValue(10);
        service.calculateSimilarityForIntersection(histogramResult);
    }

    @Test
    public void testCalculateSimilarityForBhattacharyyaSuccess() {
        HistogramResult histogramResult = new HistogramResult(0.8, 401, 8, 0.3);
        ComparisonRange range = new ComparisonRange(ComparisonMethod.BHATTACHARYYA, 0.1, 0.9);
        histogramResult.setFirstCap(itemFactory.getCapItem());
        histogramResult.setSecondCap(itemFactory.getCapItem());
        double result = service.calculateSimilarityForBhattacharyya(histogramResult, range);
        assertEquals(0.75, result, 0.00001);
    }

    @Test(expected = DuplicateCapException.class)
    public void testCalculateSimilarityForBhattacharyyaFail() {
        HistogramResult histogramResult = new HistogramResult(0.8, 401, 8, 1.2);
        ComparisonRange range = new ComparisonRange(ComparisonMethod.BHATTACHARYYA, 0.1, 0.9);
        histogramResult.setFirstCap(itemFactory.getCapItem());
        histogramResult.setSecondCap(itemFactory.getCapItem());
        service.calculateSimilarityForBhattacharyya(histogramResult, range);
    }

    @Test
    public void testCalculateSimilarityForAllMethods() {
        List<ComparisonRange> range = new ArrayList<>();
        range.add(new ComparisonRange(ComparisonMethod.CORRELATION, 0.4, 0.9));
        range.add(new ComparisonRange(ComparisonMethod.CHI_SQUARE, 1, 1001));
        range.add(new ComparisonRange(ComparisonMethod.BHATTACHARYYA, 0.1, 0.9));

        HistogramResult histogramResult = new HistogramResult(0.8, 401, 8, 0.3);
        histogramResult.setFirstCap(itemFactory.getCapItem());
        histogramResult.setSecondCap(itemFactory.getCapItem());
        OpenCVImageSignature openCVImageSignature = (OpenCVImageSignature) histogramResult.getSecondCap().getImage().getSignature();
        openCVImageSignature.setIntersectionValue(10);
        HistogramResult result = service.calculateSimilarityForAllMethods(histogramResult, range);
        assertEquals(0.7375, result.getSimilarity(), 0.00001);
    }

    @Test()
    public void testCalculateSimilarityForCap() {
        List<HistogramResult> histogramResults = prepareData();
        double result = service.calculateSimilarityForCap(histogramResults);
        assertEquals(0.4509521, result, 0.00001);
    }

    @Test()
    public void testCalculateSimilarityModelForCap() {
        List<HistogramResult> histogramResults = prepareData();
        SimilarityModel model = service.calculateSimilarityModelForCap(histogramResults, SimilarityModel.similarCapAmount);
        assertEquals(0, model.getFrom00To10());
        assertEquals(0, model.getFrom10To20());
        assertEquals(0, model.getFrom20To30());
        assertEquals(2, model.getFrom30To40());
        assertEquals(1, model.getFrom40To50());
        assertEquals(0, model.getFrom50To60());
        assertEquals(0, model.getFrom60To70());
        assertEquals(1, model.getFrom70To80());
        assertEquals(0, model.getFrom80To90());
        assertEquals(0, model.getFrom90To100());
        assertEquals(4, model.getSimilarCaps().size());
        assertFalse(model.isDuplicate());
    }

    @Test()
    @Execution(ExecutionMode.CONCURRENT)
    public void testCalculateSimilarityModelForTwoIdenticalCaps() {
        List<HistogramResult> histogramResults = prepareData();
        HistogramResult histogramResult = new HistogramResult(
                imageHistogramUtil.CORRELATION_BASE(),
                imageHistogramUtil.CHI_SQUARE_BASE(),
                18.8947,
                imageHistogramUtil.BHATTACHARYYA_BASE());
        histogramResult.setFirstCap(histogramResults.get(0).getFirstCap());
        histogramResult.setSecondCap(itemFactory.getCapItem());
        histogramResults.add(histogramResult);
        SimilarityModel model = service.calculateSimilarityModelForCap(histogramResults, SimilarityModel.similarCapAmount);

        assertEquals(0, model.getFrom00To10());
        assertEquals(0, model.getFrom10To20());
        assertEquals(0, model.getFrom20To30());
        assertEquals(0, model.getFrom30To40());
        assertEquals(0, model.getFrom40To50());
        assertEquals(0, model.getFrom50To60());
        assertEquals(0, model.getFrom60To70());
        assertEquals(0, model.getFrom70To80());
        assertEquals(0, model.getFrom80To90());
        assertEquals(4, model.getFrom90To100());
        assertEquals(0, model.getSimilarCaps().size());
        assertTrue(model.isDuplicate());
    }

    @Test()
    public void testSimilarCapsEquals6() {
        List<HistogramResult> histogramResults = prepareData();
        HistogramResult histogramResult = new HistogramResult(0.7, 402, 7, 0.3);
        histogramResult.setFirstCap(histogramResults.get(0).getFirstCap());
        histogramResult.setSecondCap(itemFactory.getCapItem());
        OpenCVImageSignature openCVImageSignature = (OpenCVImageSignature) histogramResult.getSecondCap().getImage().getSignature();
        openCVImageSignature.setIntersectionValue(10);
        histogramResults.add(histogramResult);
        HistogramResult histogramResult1 = new HistogramResult(0.3, 202, 4, 0.6);
        histogramResult1.setFirstCap(histogramResults.get(0).getFirstCap());
        histogramResult1.setSecondCap(itemFactory.getCapItem());
        OpenCVImageSignature openCVImageSignature1 = (OpenCVImageSignature) histogramResult1.getSecondCap().getImage().getSignature();
        openCVImageSignature1.setIntersectionValue(12);
        histogramResults.add(histogramResult1);
        HistogramResult histogramResult2 = new HistogramResult(0.2, 31, 2, 0.1);
        histogramResult2.setFirstCap(histogramResults.get(0).getFirstCap());
        histogramResult2.setSecondCap(itemFactory.getCapItem());
        OpenCVImageSignature openCVImageSignature2 = (OpenCVImageSignature) histogramResult2.getSecondCap().getImage().getSignature();
        openCVImageSignature2.setIntersectionValue(10);
        histogramResults.add(histogramResult2);

        SimilarityModel model = service.calculateSimilarityModelForCap(histogramResults, SimilarityModel.similarCapAmount);

        assertEquals(0, model.getFrom00To10());
        assertEquals(0, model.getFrom10To20());
        assertEquals(0, model.getFrom20To30());
        assertEquals(3, model.getFrom30To40());
        assertEquals(2, model.getFrom40To50());
        assertEquals(0, model.getFrom50To60());
        assertEquals(1, model.getFrom60To70());
        assertEquals(1, model.getFrom70To80());
        assertEquals(0, model.getFrom80To90());
        assertEquals(0, model.getFrom90To100());
        assertEquals(7, model.getSimilarCaps().size());
        assertFalse(model.isDuplicate());
    }

    private List<HistogramResult> prepareData() {
        List<ComparisonRange> range = new ArrayList<>();
        range.add(new ComparisonRange(ComparisonMethod.CORRELATION, 0.4, 0.9));
        range.add(new ComparisonRange(ComparisonMethod.CHI_SQUARE, 1, 1001));
        range.add(new ComparisonRange(ComparisonMethod.BHATTACHARYYA, 0.1, 0.9));
        when(repository.findAll()).thenReturn(range);

        CapItem newCap = itemFactory.getCapItem();

        List<HistogramResult> histogramResults = new ArrayList<>();
        HistogramResult histogramResult = new HistogramResult(0.8, 401, 8, 0.3);
        histogramResult.setFirstCap(newCap);
        histogramResult.setSecondCap(itemFactory.getCapItem());
        OpenCVImageSignature openCVImageSignature = (OpenCVImageSignature) histogramResult.getSecondCap().getImage().getSignature();
        openCVImageSignature.setIntersectionValue(10);
        HistogramResult histogramResult1 = new HistogramResult(0.2, 201, 4, 0.2);
        histogramResult1.setFirstCap(newCap);
        histogramResult1.setSecondCap(itemFactory.getCapItem());
        OpenCVImageSignature openCVImageSignature1 = (OpenCVImageSignature) histogramResult1.getSecondCap().getImage().getSignature();
        openCVImageSignature1.setIntersectionValue(12);
        HistogramResult histogramResult2 = new HistogramResult(0.3, 333, 5, 0.6);
        histogramResult2.setFirstCap(newCap);
        histogramResult2.setSecondCap(itemFactory.getCapItem());
        OpenCVImageSignature openCVImageSignature2 = (OpenCVImageSignature) histogramResult2.getSecondCap().getImage().getSignature();
        openCVImageSignature2.setIntersectionValue(13);
        HistogramResult histogramResult3 = new HistogramResult(0.35, 311, 3, 0.4);
        histogramResult3.setFirstCap(newCap);
        histogramResult3.setSecondCap(itemFactory.getCapItem());
        OpenCVImageSignature openCVImageSignature3 = (OpenCVImageSignature) histogramResult3.getSecondCap().getImage().getSignature();
        openCVImageSignature3.setIntersectionValue(14);

        histogramResults.addAll(Arrays.asList(histogramResult, histogramResult1, histogramResult2, histogramResult3));

        return histogramResults;
    }
}
