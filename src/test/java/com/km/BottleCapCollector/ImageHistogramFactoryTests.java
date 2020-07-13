package com.km.BottleCapCollector;

import com.km.BottleCapCollector.util.ImageHistogramUtil;
import org.junit.jupiter.api.Test;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertFalse;

@DataJpaTest
@Import(ImageHistogramUtil.class)
public class ImageHistogramFactoryTests {

    @Autowired
    ImageHistogramUtil imageHistogramUtil;
    
    private final Path resourceFolder = Paths.get("src/main/resources/img/");
    private final String img1Name = "captest.jpg";
    private final String img2Name = "captest2.jpg";
    private final String img1 = "src/main/resources/img/captest.jpg";
    private final String img2 = "src/main/resources/img/captest2.jpg";

    @Test
    public void testOpenCV() {
        Mat image1 = Imgcodecs.imread(img1);
        Mat image2 = Imgcodecs.imread(img2);
        assertNotEquals(image1.size(), image2.size());
    }

    @Test
    public void testMetrics() {
        Mat hist1 = imageHistogramUtil.calculateHistogram(img1Name, resourceFolder);
        Mat hist2 = imageHistogramUtil.calculateHistogram(img2Name, resourceFolder);
        assertTrue(imageHistogramUtil.correlationMethod(hist1, hist2) < imageHistogramUtil.CORRELATION_BASE(),
                "Correlation metric should be less than base metric");

        assertTrue(imageHistogramUtil.chisquareMethod(hist1, hist2) > imageHistogramUtil.CHI_SQUARE_BASE(),
                "Chi-square metric should be greater than base metric");

        assertTrue(imageHistogramUtil.intersectionMethod(hist1, hist2) < imageHistogramUtil.INTERSECTION_BASE(),
                "Intersection metric should be less than base metric");

        assertTrue(imageHistogramUtil.bhattacharyyaMethod(hist1, hist2) > imageHistogramUtil.BHATTACHARYYA_BASE(),
                "Bhattacharyya metric should be greater than base metric");

    }

    @Test
    public void testSaveAndLoadHistogramAsObject() {
        Mat hist1 = imageHistogramUtil.calculateHistogram(img1Name, resourceFolder);
        Mat hist2 = imageHistogramUtil.calculateHistogram(img2Name, resourceFolder);

        imageHistogramUtil.storeMatFile(hist1, "captest.jpg", resourceFolder);
        imageHistogramUtil.storeMatFile(hist2, "captest2.jpg", resourceFolder);

        Mat histFromFile1 = imageHistogramUtil.loadMat(img1Name, resourceFolder);
        Mat histFromFile2 = imageHistogramUtil.loadMat(img2Name, resourceFolder);

        assertEquals(imageHistogramUtil.correlationMethod(hist1, hist2),
        imageHistogramUtil.correlationMethod(histFromFile1, histFromFile2));

        try {
            Files.delete(Paths.get(img1 + imageHistogramUtil.OBJECT_PREFIX()));
            Files.delete(Paths.get(img2 + imageHistogramUtil.OBJECT_PREFIX()));
        } catch (IOException e) {
            e.printStackTrace();
        }

        assertFalse(Files.exists(Paths.get(img1 + imageHistogramUtil.OBJECT_PREFIX())));
        assertFalse(Files.exists(Paths.get(img2 + imageHistogramUtil.OBJECT_PREFIX())));
    }
}
