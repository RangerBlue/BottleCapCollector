package com.km.bottlecapcollector.service;

import com.km.bottlecapcollector.model.CapItem;
import com.km.bottlecapcollector.repository.CapItemRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class BottleCapServiceTests {
    @InjectMocks
    private BottleCapService service;

    @Mock
    private CapItemRepository repository;


    @Test
    public void addCapTest() {
        CapItem cap0 = new CapItem();
        cap0.setName("Pinta");
        service.addBottleCap(cap0);
        verify(repository, times(1)).save(cap0);
    }

    @Test
    public void getCapByIdTest() {
        String capName = "Pinta";
        long id = 1L;
        CapItem cap0 = new CapItem();
        cap0.setName(capName);

        when(repository.findById(id)).thenReturn(Optional.of(cap0));
        CapItem cap = service.getCapItem(id);
        assertEquals(capName, cap.getName());
        verify(repository, times(1)).findById(id);
    }

    @Test
    public void getAllBottleCaps() {
        List<CapItem> list = new ArrayList<>();
        CapItem cap0 = new CapItem();
        cap0.setName("Pinta");
        CapItem cap1 = new CapItem();
        cap1.setName("Funky Fluid");
        CapItem cap2 = new CapItem();
        cap2.setName("Deer Beer");
        list.add(cap0);
        list.add(cap1);
        list.add(cap2);

        when(repository.findAll()).thenReturn(list);
        List<CapItem> result = service.getAllCapItems();

        assertEquals(3, result.size());
        verify(repository, times(1)).findAll();
    }

}
