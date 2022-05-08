package com.km.bottlecapcollector.repository;

import com.km.bottlecapcollector.ImageHistogramFactoryTests;
import com.km.bottlecapcollector.model.BottleCap;
import com.km.bottlecapcollector.util.CustomMat;
import com.km.bottlecapcollector.util.ImageHistogramUtil;
import org.junit.jupiter.api.Test;
import org.opencv.core.Mat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import({ImageHistogramUtil.class})
public class BottleCapRepositoryTests {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private BottleCapRepository repository;
    ImageHistogramUtil imageHistogramUtil = new ImageHistogramUtil();

    private final Path resourceFolder = Paths.get("src/main/resources/img/");
    private final String img1Name = "captest.jpg";

    @Test
    public void testAddCap() throws IOException {
        Mat hist1 = imageHistogramUtil.calculateHistogram(img1Name, resourceFolder);
        BottleCap cap = new BottleCap("Perła", "Description", imageHistogramUtil.convertMatToBottleCapMat(hist1),
                "testLocation", "testID", 1);
        long id = entityManager.persist(cap).getId();
        BottleCap cap1 = repository.findById(id).get();
        assertEquals(cap1.getCapName(), "Perła");
        Mat hist2 = imageHistogramUtil.convertBottleCapMatToMat(new CustomMat(cap1.getData(), cap1.getCols(), cap1.getRows()));
        ImageHistogramFactoryTests.assertHistogramsConversion1x1(hist1, hist2, imageHistogramUtil);
    }

    @Test
    public void testFindById() {
        long id = entityManager.persist(new BottleCap("Perła", "img1234.img")).getId();
        BottleCap cap = repository.findById(id).get();
        assertEquals("Perła", cap.getCapName());
        assertTrue(cap.getFileLocation().contains("img1234.img"));
    }

    @Test
    public void testFindByName() {
        entityManager.persist(new BottleCap("Lech", "img321.img"));
        BottleCap cap = repository.findByCapName("Lech").get(0);
        assertEquals("Lech", cap.getCapName());
        assertTrue(cap.getFileLocation().contains("img321.img"));
    }

    @Test
    public void testCount() {
        entityManager.persist(new BottleCap("Perła"));
        entityManager.persist(new BottleCap("Lech"));
        long booksSize = repository.count();
        assertEquals(2, booksSize);
    }
}
