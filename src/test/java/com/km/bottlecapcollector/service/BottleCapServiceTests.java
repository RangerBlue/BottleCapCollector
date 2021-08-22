package com.km.bottlecapcollector.service;

import com.km.bottlecapcollector.model.BottleCap;
import com.km.bottlecapcollector.repository.BottleCapRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class BottleCapServiceTests {
    @InjectMocks
    private BottleCapService service;

    @Mock
    private BottleCapRepository repository;

    @Test
    public void addCapTest() {
        BottleCap cap0 = new BottleCap("Pinta");
        service.addBottleCap(cap0);
        verify(repository, times(1)).save(cap0);
    }

    @Test
    public void getCapByIdTest() {
        when(repository.findById(1L)).thenReturn(java.util.Optional.of(new BottleCap("Pinta", "file123.jpg")));
        BottleCap cap = service.getBottleCap(1);
        assertEquals("Pinta", cap.getCapName());
        assertTrue(cap.getFileLocation().contains("file123.jpg"));
        verify(repository, times(1)).findById(1L);
    }

    @Test
    public void getAllBottleCaps() {
        List<BottleCap> list = new ArrayList<>();
        BottleCap cap0 = new BottleCap("Pinta");
        BottleCap cap1 = new BottleCap("Funky Fluid");
        BottleCap cap2 = new BottleCap("Deer Beer");
        list.add(cap0);
        list.add(cap1);
        list.add(cap2);

        when(repository.findAll()).thenReturn(list);
        List<BottleCap> result = service.getAllBottleCaps();

        assertEquals(3, result.size());
        verify(repository, times(1)).findAll();
    }

}
