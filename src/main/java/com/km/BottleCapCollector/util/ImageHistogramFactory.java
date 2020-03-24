package com.km.BottleCapCollector.util;

import com.km.BottleCapCollector.model.HistogramResult;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

@Component
public class ImageHistogramFactory {

    private static final Logger logger = LoggerFactory.getLogger(ImageHistogramFactory.class);


    private static final byte CORRELATION = 0;
    public static final double CORRELATION_BASE = 1;
    private static final byte CHI_SQUARE = 1;
    public static final double CHI_SQUARE_BASE = 0;
    private static final byte INTERSECTION = 2;
    public static final double INTERSECTION_BASE = 18.8947;
    private static final byte BHATTACHARYYA = 3;
    public static final byte BHATTACHARYYA_BASE = 0;
    public static final String OBJECT_PREFIX = "object";


    private static final int hBins = 50, sBins = 60;
    private static final int[] histSize = {hBins, sBins};
    // hue varies from 0 to 179, saturation from 0 to 255
    private static final float[] ranges = {0, 180, 0, 256};
    // Use the 0-th and 1-st channels
    private static final int[] channels = {0, 1};

    private ImageHistogramFactory() {
    }

    public static Mat getHistogram(Path file) {
        Mat hsvImage = new Mat();
        Mat histImage = new Mat();
        Imgproc.cvtColor(Imgcodecs.imread(file.toString()), hsvImage, Imgproc.COLOR_BGR2HSV);
        List<Mat> hsvBaseList = Arrays.asList(hsvImage);
        Imgproc.calcHist(hsvBaseList, new MatOfInt(channels), new Mat(), histImage, new MatOfInt(histSize), new MatOfFloat(ranges), false);
        Core.normalize(histImage, histImage, 0, 1, Core.NORM_MINMAX);

        return histImage;
    }

    /**
     * The higher the metric, the more accurate the match
     * {@value #@CCORRELATION_BASE} is base value
     *
     * @param histImage1
     * @param histImage2
     * @return metric result
     */
    public static double correlationMethod(Mat histImage1, Mat histImage2) {
        return Imgproc.compareHist(histImage1, histImage2, CORRELATION);
    }

    /**
     * The higher the metric, the more accurate the match
     * {@value #CHI_SQUARE_BASE} is base value
     *
     * @param histImage1
     * @param histImage2
     * @return metric result
     */
    public static double chisquareMethod(Mat histImage1, Mat histImage2) {
        return Imgproc.compareHist(histImage1, histImage2, CHI_SQUARE);
    }

    /**
     * The less the result, the better the match
     * {@value #INTERSECTION_BASE} is base value
     *
     * @param histImage1
     * @param histImage2
     * @return metric result
     */
    public static double intersectionMethod(Mat histImage1, Mat histImage2) {
        return Imgproc.compareHist(histImage1, histImage2, INTERSECTION);
    }

    /**
     * The less the result, the better the match
     * {@value #BHATTACHARYYA_BASE} is base value
     *
     * @param histImage1
     * @param histImage2
     * @return metric result
     */
    public static double bhattacharyyaMethod(Mat histImage1, Mat histImage2) {
        return Imgproc.compareHist(histImage1, histImage2, BHATTACHARYYA);
    }

    public static HistogramResult calculateCoefficients(Mat histImage1, Mat histImage2) {
        HistogramResult result = new HistogramResult();
        result.setCorrelation(correlationMethod(histImage1, histImage2));
        result.setChisquare(chisquareMethod(histImage1, histImage2));
        result.setIntersection(intersectionMethod(histImage1, histImage2));
        result.setBhattacharyya(bhattacharyyaMethod(histImage1, histImage2));
        return result;
    }

    public static final Mat loadMat(String name, Path location) {
        try {
            int cols;
            float[] data;
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(new File(location.toFile(), name)))) {
                cols = (int) ois.readObject();
                data = (float[]) ois.readObject();
            }
            Mat mat = new Mat(data.length / cols, cols, CvType.CV_32F);
            mat.put(0, 0, data);
            return mat;
        } catch (IOException | ClassNotFoundException | ClassCastException ex) {
            logger.error("Could not load mat from file " + name);
        }
        return null;
    }


    public static String storeMatFile(Mat mat, String name, Path location) {
        try {
            int cols = mat.cols();
            float[] data = new float[(int) mat.total() * mat.channels()];
            mat.get(0, 0, data);
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(new File(location.toFile(), name + OBJECT_PREFIX)))) {
                oos.writeObject(cols);
                oos.writeObject(data);
            }
        } catch (IOException | ClassCastException ex) {
            logger.error("ERROR: Could not save mat to file: " + name);
        }
        return name + OBJECT_PREFIX;
    }

    public static String calculateAndStoreHistogram(String imageName, Path fileStorageLocation, Path objectStorageLocation) {
        Mat hist = getHistogram(fileStorageLocation.resolve(imageName).normalize());
        return ImageHistogramFactory.storeMatFile(hist, imageName, objectStorageLocation);
    }
}
