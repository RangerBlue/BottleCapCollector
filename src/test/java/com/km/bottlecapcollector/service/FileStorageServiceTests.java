package com.km.bottlecapcollector.service;

import com.km.bottlecapcollector.model.BottleCap;
import com.km.bottlecapcollector.util.BottleCapPair;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

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
