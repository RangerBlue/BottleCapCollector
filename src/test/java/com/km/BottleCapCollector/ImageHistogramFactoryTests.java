package com.km.BottleCapCollector;

import com.km.BottleCapCollector.util.BottleCapMat;
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
    public void testMetricsWithFileUpload() {
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

        assertHistogramsConversion2x2(hist1, hist2, histFromFile1, histFromFile2);

        try {
            Files.delete(Paths.get(img1 + imageHistogramUtil.OBJECT_PREFIX()));
            Files.delete(Paths.get(img2 + imageHistogramUtil.OBJECT_PREFIX()));
        } catch (IOException e) {
            e.printStackTrace();
        }

        assertFalse(Files.exists(Paths.get(img1 + imageHistogramUtil.OBJECT_PREFIX())));
        assertFalse(Files.exists(Paths.get(img2 + imageHistogramUtil.OBJECT_PREFIX())));
    }

    @Test
    public void testMetricsWithoutUpload() throws IOException, ClassNotFoundException {
        Mat hist1 = imageHistogramUtil.calculateHistogram(img1Name, resourceFolder);
        Mat hist2 = imageHistogramUtil.calculateHistogram(img2Name, resourceFolder);
        BottleCapMat byteFromMat1 = imageHistogramUtil.convertMatToBottleCapMat(hist1);
        Mat hist1After = imageHistogramUtil.convertBottleCapMatToMat(byteFromMat1);
        BottleCapMat byteFromMat2 = imageHistogramUtil.convertMatToBottleCapMat(hist2);
        Mat hist2After = imageHistogramUtil.convertBottleCapMatToMat(byteFromMat2);

        assertHistogramsConversion2x2(hist1, hist2, hist1After, hist2After);

    }

    private void assertHistogramsConversion2x2(Mat hist1Before, Mat hist2Before, Mat hist1After, Mat hist2After) {
        assertEquals(imageHistogramUtil.correlationMethod(hist1Before, hist2Before),
                imageHistogramUtil.correlationMethod(hist1After, hist2After), "Results should be the same");
        assertEquals(imageHistogramUtil.CORRELATION_BASE(), imageHistogramUtil.correlationMethod(hist1Before, hist1After),
                "Correlation metric should be equal with base metric");
        assertEquals(imageHistogramUtil.CORRELATION_BASE(), imageHistogramUtil.correlationMethod(hist2Before, hist2After),
                "Correlation metric should be equal with base metric");
        assertEquals(imageHistogramUtil.correlationMethod(hist1Before, hist2After),
                imageHistogramUtil.correlationMethod(hist1After, hist2Before), "Results should be the same");

        assertEquals(imageHistogramUtil.chisquareMethod(hist1Before, hist2Before),
                imageHistogramUtil.chisquareMethod(hist1After, hist2After), "Results should be the same");
        assertEquals(imageHistogramUtil.CHI_SQUARE_BASE(), imageHistogramUtil.chisquareMethod(hist1Before, hist1After),
                "Chi-square metric should be equal with base metric");
        assertEquals(imageHistogramUtil.CHI_SQUARE_BASE(), imageHistogramUtil.chisquareMethod(hist2Before, hist2After),
                "Chi-square metric should be equal with base metric");
        assertEquals(imageHistogramUtil.chisquareMethod(hist1Before, hist2After),
                imageHistogramUtil.chisquareMethod(hist1After, hist2Before), "Results should be the same");

        assertEquals(imageHistogramUtil.intersectionMethod(hist1Before, hist2Before),
                imageHistogramUtil.intersectionMethod(hist1After, hist2After), "Results should be the same");
        assertNotEquals(imageHistogramUtil.INTERSECTION_BASE(), imageHistogramUtil.intersectionMethod(hist1Before, hist1After),
                "Intersection metric should not be equal with base metric");
        assertNotEquals(imageHistogramUtil.INTERSECTION_BASE(), imageHistogramUtil.intersectionMethod(hist2Before, hist2After),
                "Intersection metric should not be equal with base metric");
        assertEquals(imageHistogramUtil.intersectionMethod(hist1Before, hist2After),
                imageHistogramUtil.intersectionMethod(hist1After, hist2Before), "Results should be the same");

        assertEquals(imageHistogramUtil.bhattacharyyaMethod(hist1Before, hist2Before),
                imageHistogramUtil.bhattacharyyaMethod(hist1After, hist2After), "Results should be the same");
        assertEquals(imageHistogramUtil.BHATTACHARYYA_BASE(), imageHistogramUtil.bhattacharyyaMethod(hist1Before, hist1After),
                "Bhattacharyya metric should be equal with base metric");
        assertEquals(imageHistogramUtil.BHATTACHARYYA_BASE(), imageHistogramUtil.bhattacharyyaMethod(hist2Before, hist2After),
                "Bhattacharyya metric should be equal with base metric");
        assertEquals(imageHistogramUtil.bhattacharyyaMethod(hist1Before, hist2After),
                imageHistogramUtil.bhattacharyyaMethod(hist1After, hist2Before), "Results should be the same");
    }

    public static void assertHistogramsConversion1x1(Mat histBefore, Mat histAfter, ImageHistogramUtil imageHistogramUtil) {
        assertEquals(imageHistogramUtil.CORRELATION_BASE(), imageHistogramUtil.correlationMethod(histBefore, histAfter),
                "Correlation metric should be equal with base metric");
        assertEquals(imageHistogramUtil.CORRELATION_BASE(), imageHistogramUtil.correlationMethod(histBefore, histAfter),
                "Correlation metric should be equal with base metric");

        assertEquals(imageHistogramUtil.CHI_SQUARE_BASE(), imageHistogramUtil.chisquareMethod(histBefore, histAfter),
                "Chi-square metric should be equal with base metric");
        assertEquals(imageHistogramUtil.CHI_SQUARE_BASE(), imageHistogramUtil.chisquareMethod(histBefore, histAfter),
                "Chi-square metric should be equal with base metric");

        assertNotEquals(imageHistogramUtil.INTERSECTION_BASE(), imageHistogramUtil.intersectionMethod(histBefore, histAfter),
                "Intersection metric should not be equal with base metric");
        assertNotEquals(imageHistogramUtil.INTERSECTION_BASE(), imageHistogramUtil.intersectionMethod(histBefore, histAfter),
                "Intersection metric should not be equal with base metric");

        assertEquals(imageHistogramUtil.BHATTACHARYYA_BASE(), imageHistogramUtil.bhattacharyyaMethod(histBefore, histAfter),
                "Bhattacharyya metric should be equal with base metric");
        assertEquals(imageHistogramUtil.BHATTACHARYYA_BASE(), imageHistogramUtil.bhattacharyyaMethod(histBefore, histAfter),
                "Bhattacharyya metric should be equal with base metric");
    }

}
