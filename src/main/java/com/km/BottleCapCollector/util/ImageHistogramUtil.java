package com.km.BottleCapCollector.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

@Component
@Scope("singleton")
public class ImageHistogramUtil {

    private static final Logger logger = LogManager.getLogger(ImageHistogramUtil.class);


    private final byte CORRELATION = 0;
    private final double CORRELATION_BASE = 1;
    private final byte CHI_SQUARE = 1;
    private final double CHI_SQUARE_BASE = 0;
    private final byte INTERSECTION = 2;
    private final byte BHATTACHARYYA = 3;
    private final byte BHATTACHARYYA_BASE = 0;
    private final String OBJECT_PREFIX = "object";


    private final int hBins = 50, sBins = 60;
    private final int[] histSize = {hBins, sBins};
    // hue varies from 0 to 179, saturation from 0 to 255
    private final float[] ranges = {0, 180, 0, 256};
    // Use the 0-th and 1-st channels
    private final int[] channels = {0, 1};

    public ImageHistogramUtil() {
    }


    public double CORRELATION_BASE() {
        return this.CORRELATION_BASE;
    }

    public double CHI_SQUARE_BASE() {
        return this.CHI_SQUARE_BASE;
    }

    public byte BHATTACHARYYA_BASE() {
        return this.BHATTACHARYYA_BASE;
    }

    public String OBJECT_PREFIX() {
        return this.OBJECT_PREFIX;
    }


    public Mat calculateHistogram(String file, Path folder) {
        Path location = folder.resolve(file).normalize();
        Mat hsvImage = new Mat();
        Mat histImage = new Mat();
        Imgproc.cvtColor(Imgcodecs.imread(location.toString()), hsvImage, Imgproc.COLOR_BGR2HSV);
        List<Mat> hsvBaseList = Arrays.asList(hsvImage);
        Imgproc.calcHist(hsvBaseList, new MatOfInt(channels), new Mat(), histImage, new MatOfInt(histSize), new MatOfFloat(ranges), false);
        Core.normalize(histImage, histImage, 0, 1, Core.NORM_MINMAX);
        return histImage;
    }

    public Mat calculateHistogram(MultipartFile file) throws IOException {
        logger.info("Calculating Mat object from multipart file...");
        Mat hsvImage = new Mat();
        Mat histImage = new Mat();
        Mat inputImage = Imgcodecs.imdecode(new MatOfByte(file.getBytes()), Imgcodecs.CV_LOAD_IMAGE_UNCHANGED);
        Imgproc.cvtColor(inputImage, hsvImage, Imgproc.COLOR_BGR2HSV);
        List<Mat> hsvBaseList = Arrays.asList(hsvImage);
        Imgproc.calcHist(hsvBaseList, new MatOfInt(channels), new Mat(), histImage, new MatOfInt(histSize), new MatOfFloat(ranges), false);
        Core.normalize(histImage, histImage, 0, histImage.rows(), Core.NORM_MINMAX);
        return histImage;
    }

    /**
     * The higher the metric, the more accurate the match
     * {@link #CORRELATION_BASE} is base value
     *
     * @param histImage1
     * @param histImage2
     * @return metric result
     */
    public double correlationMethod(Mat histImage1, Mat histImage2) {
        return Imgproc.compareHist(histImage1, histImage2, CORRELATION);
    }

    /**
     * The higher the metric, the more accurate the match
     * {@link #CHI_SQUARE_BASE} is base value
     *
     * @param histImage1
     * @param histImage2
     * @return metric result
     */
    public double chisquareMethod(Mat histImage1, Mat histImage2) {
        return Imgproc.compareHist(histImage1, histImage2, CHI_SQUARE);
    }

    /**
     * The result closer to metric, the better the match
     *
     * @param histImage1
     * @param histImage2
     * @return metric result
     */
    public double intersectionMethod(Mat histImage1, Mat histImage2) {
        return Imgproc.compareHist(histImage1, histImage2, INTERSECTION);
    }

    /**
     * The less the result, the better the match
     * {@link #BHATTACHARYYA_BASE} is base value
     *
     * @param histImage1
     * @param histImage2
     * @return metric result
     */
    public double bhattacharyyaMethod(Mat histImage1, Mat histImage2) {
        return Imgproc.compareHist(histImage1, histImage2, BHATTACHARYYA);
    }

    public HistogramResult calculateCoefficients(Mat histImage1, Mat histImage2) {
        HistogramResult result = new HistogramResult();
        result.setCorrelation(correlationMethod(histImage1, histImage2));
        result.setChisquare(chisquareMethod(histImage1, histImage2));
        result.setIntersection(intersectionMethod(histImage1, histImage2));
        result.setBhattacharyya(bhattacharyyaMethod(histImage1, histImage2));
        return result;
    }

    public double calculateIntersection(Mat histImage1) {
        return intersectionMethod(histImage1, histImage1);
    }

    public final Mat loadMat(String name, Path location) {
        try {
            int cols;
            float[] data;
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(new File(location.toFile(), name + OBJECT_PREFIX)))) {
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


    public String storeMatFile(Mat mat, String name, Path location) {
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

    public BottleCapMat convertMatToBottleCapMat(Mat mat) throws IOException {
        logger.info("Converting Mat object to BottleCapMat object ...");
        float[] image = new float[(int) (mat.total() * mat.elemSize())];
        mat.get(0, 0, image);
        ByteArrayOutputStream bas = new ByteArrayOutputStream();
        DataOutputStream ds = new DataOutputStream(bas);
        for (float f : image)
            ds.writeFloat(f);
        byte[] bytes = bas.toByteArray();
        return new BottleCapMat(bytes, mat.cols(), mat.rows());
    }

    public Mat convertBottleCapMatToMat(BottleCapMat data) throws IOException {
        ByteArrayInputStream bas = new ByteArrayInputStream(data.getMatArray());
        DataInputStream ds = new DataInputStream(bas);
        float[] fArr = new float[data.getMatArray().length / 4];
        for (int i = 0; i < fArr.length; i++) {
            fArr[i] = ds.readFloat();
        }
        Mat imageMat = new Mat(data.getRows(), data.getCols(), CvType.CV_32FC1);
        imageMat.put(0, 0, fArr);
        return imageMat;
    }

    public String calculateAndStoreHistogram(String imageName, Path fileStorageLocation, Path objectStorageLocation) {
        Mat hist = calculateHistogram(imageName, fileStorageLocation);
        return storeMatFile(hist, imageName, objectStorageLocation);
    }
}
