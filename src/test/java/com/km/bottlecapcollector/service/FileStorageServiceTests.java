package com.km.bottlecapcollector.service;

import com.km.bottlecapcollector.model.BottleCap;
import com.km.bottlecapcollector.model.CapItem;
import com.km.bottlecapcollector.util.BottleCapPair;
import com.km.bottlecapcollector.util.ImageHistogramUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class FileStorageServiceTests {



    @Test
    public void calculateEachWithEach() {
        List<CapItem> list = new ArrayList<>();
        CapItem cap0 = new CapItem();
        cap0.setName("Pinta");
        CapItem cap1 = new CapItem();
        cap1.setName("Funky Fluid");
        CapItem cap2 = new CapItem();
        cap2.setName("Deer Beer");
        CapItem cap3 = new CapItem();
        cap3.setName("Zakladowy");
        CapItem cap4 = new CapItem();
        cap4.setName("Dziki Wschod");
        list.add(cap0);
        list.add(cap1);
        list.add(cap2);
        list.add(cap3);
        list.add(cap4);

        List<BottleCapPair> result = ImageHistogramUtil.calculateEachWithEach(list);
        assertEquals(10, result.size());
        assertEquals(4, result.stream().filter(pair -> pair.getFirstCap().equals(cap0) || pair.getSecondCap().equals(cap0)).count());
    }

}
