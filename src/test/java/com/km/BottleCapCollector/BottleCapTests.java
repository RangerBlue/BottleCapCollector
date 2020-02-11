package com.km.BottleCapCollector;

import com.km.BottleCapCollector.model.BottleCap;
import com.km.BottleCapCollector.repository.BottleCapRepository;
import com.km.BottleCapCollector.util.ImageHistogram;
import org.junit.jupiter.api.Test;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;


@DataJpaTest
public class BottleCapTests {



    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private BottleCapRepository repository;

    private final String img1 = "src/main/resources/img/captest.jpg";
    private final String img2 = "src/main/resources/img/captest2.jpg";

    @Test
    public void testFindById() {
        entityManager.persist(new BottleCap("Perła", Paths.get("testPath").toString()));
        long booksSize = repository.count();
        assertEquals(1, booksSize);
        Optional<BottleCap> cap = repository.findById(4l);
        assertEquals("Perła",cap.get().getName());
    }

    @Test
    public void testFindByName() {
        entityManager.persist(new BottleCap("Lech", Paths.get("testPath0").toString()));
        BottleCap cap = repository.findByName("Lech").get(0);
        assertEquals("Lech", cap.getName());
    }

    @Test
    public void testCount() {
        entityManager.persist(new BottleCap("Perła", Paths.get("testPath1").toString()));
        entityManager.persist(new BottleCap("Lech", Paths.get("testPath2").toString()));
        long booksSize = repository.count();
        assertEquals(2, booksSize);
    }

    @Test
    public void testOpenCV() {
        Imgcodecs imageCodecs = new Imgcodecs();
        Mat image1 = imageCodecs.imread(img1);
        Mat image2 = imageCodecs.imread(img2);
        assertNotEquals(image1.size(), image2.size());
    }

    @Test
    public void testMetrics() {
        ImageHistogram hist1 = new ImageHistogram(img1);
        ImageHistogram hist2 = new ImageHistogram(img2);
        assertTrue(hist1.correlationMethod(hist2.getHistImage()) < ImageHistogram.CORRELATION_BASE,
                "Correlation metric should be less than base metric");

        assertTrue(hist1.chisquareMethod(hist2.getHistImage()) > ImageHistogram.CHI_SQUARE_BASE,
                "Chi-square metric should be greater than base metric");

        assertTrue(hist1.intersectionMethod(hist2.getHistImage()) < ImageHistogram.INTERSECTION_BASE,
                "Intersection metric should be less than base metric");

        assertTrue(hist1.bhattacharyyaMethod(hist2.getHistImage()) > ImageHistogram.BHATTACHARYYA_BASE,
                "Bhattacharyya metric should be greater than base metric");

    }

    @Test
    public void testSaveAndLoadHistogramAsObject() {
        ImageHistogram hist1 = new ImageHistogram(img1);
        ImageHistogram hist2 = new ImageHistogram(img2);

        ImageHistogram.storeMatFile(img1);
        ImageHistogram.storeMatFile(img2);
        ImageHistogram histFromFile1 = new ImageHistogram(ImageHistogram.loadMat(img1+ImageHistogram.OBJECT_PREFIX));
        ImageHistogram histFromFile2 = new ImageHistogram(ImageHistogram.loadMat(img2+ImageHistogram.OBJECT_PREFIX));

        assertEquals(hist1.correlationMethod(hist2.getHistImage()),
                histFromFile1.correlationMethod(histFromFile2.getHistImage()));

        try {
            Files.delete(Paths.get(img1+ImageHistogram.OBJECT_PREFIX));
            Files.delete(Paths.get(img2+ImageHistogram.OBJECT_PREFIX));
        } catch (IOException e) {
            e.printStackTrace();
        }

        assertFalse(Files.exists(Paths.get(img1+ImageHistogram.OBJECT_PREFIX)));
        assertFalse(Files.exists(Paths.get(img2+ImageHistogram.OBJECT_PREFIX)));


    }



}
