package com.km.BottleCapCollector.util;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.util.Arrays;
import java.util.List;

public class ImageHistogram {
    private static final byte CORRELATION = 0;
    public static final double CORRELATION_BASE = 1;
    private static final byte CHI_SQUARE = 1;
    public static final double CHI_SQUARE_BASE = 0;
    private static final byte INTERSECTION = 2;
    public static final double INTERSECTION_BASE = 18.8947;
    private static final byte BHATTACHARYYA = 3;
    public static final byte BHATTACHARYYA_BASE = 0;

    private Mat hsvImage ;
    private Mat histImage;

    private static final int hBins = 50, sBins = 60;
    private static final int[] histSize = { hBins, sBins };
    // hue varies from 0 to 179, saturation from 0 to 255
    private static final float[] ranges = { 0, 180, 0, 256 };
    // Use the 0-th and 1-st channels
    private static final int[] channels = { 0, 1 };

    public ImageHistogram(String path) {
        hsvImage = new Mat();
        histImage = new Mat();
        Imgproc.cvtColor( Imgcodecs.imread(path), hsvImage, Imgproc.COLOR_BGR2HSV );
        List<Mat> hsvBaseList = Arrays.asList(hsvImage);
        Imgproc.calcHist(hsvBaseList, new MatOfInt(channels), new Mat(), histImage, new MatOfInt(histSize), new MatOfFloat(ranges), false);
        Core.normalize(histImage, histImage, 0, 1, Core.NORM_MINMAX);
    }

    public Mat getHistImage() {
        return histImage;
    }

    public void setHistImage(Mat histImage) {
        this.histImage = histImage;
    }

    /**
     * The higher the metric, the more accurate the match
     * {@value #@CCORRELATION_BASE} is base value
     * @param histImage
     * @return metric result
     */
    public double correlationMethod(Mat histImage){
        return Imgproc.compareHist( this.histImage, histImage, CORRELATION );
    }

    /**
     * The higher the metric, the more accurate the match
     * {@value #CHI_SQUARE_BASE} is base value
     * @param histImage
     * @return metric result
     */
    public double chisquareMethod(Mat histImage){
        return Imgproc.compareHist( this.histImage, histImage, CHI_SQUARE );
    }

    /**
     * The less the result, the better the match
     * {@value #INTERSECTION_BASE} is base value
     * @param histImage
     * @return metric result
     */
    public double intersectionMethod(Mat histImage){
        return Imgproc.compareHist( this.histImage, histImage, INTERSECTION );
    }

    /**
     * The less the result, the better the match
     * {@value #BHATTACHARYYA_BASE} is base value
     * @param histImage
     * @return metric result
     */
    public double bhattacharyyaMethod(Mat histImage){
        return Imgproc.compareHist( this.histImage, histImage, BHATTACHARYYA);
    }
}
