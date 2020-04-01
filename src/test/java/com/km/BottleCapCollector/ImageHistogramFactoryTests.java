package com.km.BottleCapCollector;

import com.km.BottleCapCollector.util.ImageHistogramFactory;
import org.junit.jupiter.api.Test;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertFalse;

@DataJpaTest
public class ImageHistogramFactoryTests {

    private final Path resourceFolder = Paths.get("src/main/resources/img/");
    private final String img1Name = "captest.jpg";
    private final String img2Name = "captest2.jpg";
    private final String img1 = "src/main/resources/img/captest.jpg";
    private final String img2 = "src/main/resources/img/captest2.jpg";

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
        assertTrue(ImageHistogramFactory.correlationMethod(hist1, hist2) < ImageHistogramFactory.CORRELATION_BASE,
                "Correlation metric should be less than base metric");

        assertTrue(ImageHistogramFactory.chisquareMethod(hist1, hist2) > ImageHistogramFactory.CHI_SQUARE_BASE,
                "Chi-square metric should be greater than base metric");

        assertTrue(ImageHistogramFactory.intersectionMethod(hist1, hist2) < ImageHistogramFactory.INTERSECTION_BASE,
                "Intersection metric should be less than base metric");

        assertTrue(ImageHistogramFactory.bhattacharyyaMethod(hist1, hist2) > ImageHistogramFactory.BHATTACHARYYA_BASE,
                "Bhattacharyya metric should be greater than base metric");

    }

    @Test
    public void testSaveAndLoadHistogramAsObject() {
        Mat hist1 = ImageHistogramFactory.getHistogram(Paths.get(img1));
        Mat hist2 = ImageHistogramFactory.getHistogram(Paths.get(img2));

        ImageHistogramFactory.storeMatFile(hist1, "captest.jpg", resourceFolder);
        ImageHistogramFactory.storeMatFile(hist2, "captest2.jpg", resourceFolder);

        Mat histFromFile1 = ImageHistogramFactory.loadMat(img1Name + ImageHistogramFactory.OBJECT_PREFIX, resourceFolder);
        Mat histFromFile2 = ImageHistogramFactory.loadMat(img2Name + ImageHistogramFactory.OBJECT_PREFIX, resourceFolder);

        assertEquals(ImageHistogramFactory.correlationMethod(hist1, hist2),
                ImageHistogramFactory.correlationMethod(histFromFile1, histFromFile2));

        try {
            Files.delete(Paths.get(img1 + ImageHistogramFactory.OBJECT_PREFIX));
            Files.delete(Paths.get(img2 + ImageHistogramFactory.OBJECT_PREFIX));
        } catch (IOException e) {
            e.printStackTrace();
        }

        assertFalse(Files.exists(Paths.get(img1 + ImageHistogramFactory.OBJECT_PREFIX)));
        assertFalse(Files.exists(Paths.get(img2 + ImageHistogramFactory.OBJECT_PREFIX)));
    }
}