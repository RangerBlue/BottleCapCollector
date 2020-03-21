package com.km.BottleCapCollector;

import com.km.BottleCapCollector.model.BottleCap;
import com.km.BottleCapCollector.repository.BottleCapRepository;
import com.km.BottleCapCollector.util.ImageHistogramFactory;
import org.junit.jupiter.api.Test;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;


@DataJpaTest
public class BottleCapTests {



    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private BottleCapRepository repository;

    private final Path resourceFolder = Paths.get("src/main/resources/img/");
    private final String img1Name = "captest.jpg";
    private final String img2Name = "captest2.jpg";
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
        Mat hist1 = ImageHistogramFactory.getHistogram(Paths.get(img1));
        Mat hist2 = ImageHistogramFactory.getHistogram(Paths.get(img2));
        assertTrue(ImageHistogramFactory.correlationMethod(hist1,hist2) < ImageHistogramFactory.CORRELATION_BASE,
                "Correlation metric should be less than base metric");

        assertTrue(ImageHistogramFactory.chisquareMethod(hist1,hist2) > ImageHistogramFactory.CHI_SQUARE_BASE,
                "Chi-square metric should be greater than base metric");

        assertTrue(ImageHistogramFactory.intersectionMethod(hist1,hist2) < ImageHistogramFactory.INTERSECTION_BASE,
                "Intersection metric should be less than base metric");

        assertTrue(ImageHistogramFactory.bhattacharyyaMethod(hist1,hist2) > ImageHistogramFactory.BHATTACHARYYA_BASE,
                "Bhattacharyya metric should be greater than base metric");

    }

    @Test
    public void testSaveAndLoadHistogramAsObject() {
        Mat hist1 = ImageHistogramFactory.getHistogram(Paths.get(img1));
        Mat hist2 = ImageHistogramFactory.getHistogram(Paths.get(img2));

        ImageHistogramFactory.storeMatFile(hist1, "captest.jpg", resourceFolder);
        ImageHistogramFactory.storeMatFile(hist2, "captest2.jpg", resourceFolder);

       Mat histFromFile1 = ImageHistogramFactory.loadMat(img1Name+ImageHistogramFactory.OBJECT_PREFIX, resourceFolder);
        Mat histFromFile2 = ImageHistogramFactory.loadMat(img2Name+ImageHistogramFactory.OBJECT_PREFIX,resourceFolder);

        assertEquals(ImageHistogramFactory.correlationMethod(hist1, hist2),
                ImageHistogramFactory.correlationMethod(histFromFile1, histFromFile2));

        try {
            Files.delete(Paths.get(img1+ ImageHistogramFactory.OBJECT_PREFIX));
            Files.delete(Paths.get(img2+ ImageHistogramFactory.OBJECT_PREFIX));
        } catch (IOException e) {
            e.printStackTrace();
        }

        assertFalse(Files.exists(Paths.get(img1+ ImageHistogramFactory.OBJECT_PREFIX)));
        assertFalse(Files.exists(Paths.get(img2+ ImageHistogramFactory.OBJECT_PREFIX)));


    }



}
