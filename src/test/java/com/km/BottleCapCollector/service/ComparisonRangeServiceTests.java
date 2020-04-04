package com.km.BottleCapCollector.service;

import com.km.BottleCapCollector.model.BottleCap;
import com.km.BottleCapCollector.model.ComparisonRange;
import com.km.BottleCapCollector.model.HistogramResult;
import com.km.BottleCapCollector.repository.ComparisonRangeRepository;
import com.km.BottleCapCollector.util.ComparisonMethod;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class ComparisonRangeServiceTests {

    @InjectMocks
    private ComparisonRangeService service;

    @Mock
    private ComparisonRangeRepository repository;

    @Test
    public void calculateMethodMaxMinValues() {
        HistogramResult result1 = new HistogramResult(1, 2, 3, 4);
        result1.setFirstCap(new BottleCap());
        result1.setSecondCap(new BottleCap());
        HistogramResult result2 = new HistogramResult(5, 6, 7, 8);
        result2.setFirstCap(new BottleCap());
        result2.setSecondCap(new BottleCap());
        HistogramResult result3 = new HistogramResult(9, 10, 11, 12);
        result3.setFirstCap(new BottleCap());
        result3.setSecondCap(new BottleCap());
        List<HistogramResult> histogramList = new ArrayList<>();
        histogramList.add(result1);
        histogramList.add(result2);
        histogramList.add(result3);

        List<ComparisonRange> result = service.calculateMinMaxValuesOfAllComparisonMethods(histogramList);
        assertEquals(1, result.stream().filter(item ->item.getMethodName().equals(ComparisonMethod.CORRELATION)).findFirst().get().getMinValue());
        assertEquals(9, result.stream().filter(item ->item.getMethodName().equals(ComparisonMethod.CORRELATION)).findFirst().get().getMaxValue());

        assertEquals(2, result.stream().filter(item ->item.getMethodName().equals(ComparisonMethod.CHI_SQUARE)).findFirst().get().getMinValue());
        assertEquals(10, result.stream().filter(item ->item.getMethodName().equals(ComparisonMethod.CHI_SQUARE)).findFirst().get().getMaxValue());

        assertEquals(3, result.stream().filter(item ->item.getMethodName().equals(ComparisonMethod.INTERSECTION)).findFirst().get().getMinValue());
        assertEquals(11, result.stream().filter(item ->item.getMethodName().equals(ComparisonMethod.INTERSECTION)).findFirst().get().getMaxValue());

        assertEquals(4, result.stream().filter(item ->item.getMethodName().equals(ComparisonMethod.BHATTACHARYYA)).findFirst().get().getMinValue());
        assertEquals(12, result.stream().filter(item ->item.getMethodName().equals(ComparisonMethod.BHATTACHARYYA)).findFirst().get().getMaxValue());

    }
}
