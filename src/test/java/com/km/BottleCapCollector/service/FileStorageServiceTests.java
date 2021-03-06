package com.km.BottleCapCollector.service;

import com.km.BottleCapCollector.model.BottleCap;
import com.km.BottleCapCollector.util.HistogramResult;
import com.km.BottleCapCollector.util.BottleCapPair;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class FileStorageServiceTests {

    @InjectMocks
    private FileStorageService service;


    @Test
    public void calculateEachWithEach() {
        List<BottleCap> list = new ArrayList<>();
        BottleCap cap0 = new BottleCap("Pinta");
        BottleCap cap1 = new BottleCap("Funky Fluid");
        BottleCap cap2 = new BottleCap("Deer Beer");
        BottleCap cap3 = new BottleCap("Zakladowy");
        BottleCap cap4 = new BottleCap("Dziki Wschod");
        list.add(cap0);
        list.add(cap1);
        list.add(cap2);
        list.add(cap3);
        list.add(cap4);

        List<BottleCapPair> result = service.calculateEachWithEach(list);
        assertEquals(10, result.size());
        assertEquals(4, result.stream().filter(pair -> pair.getFirstCap().equals(cap0) || pair.getSecondCap().equals(cap0)).count());
    }

}
