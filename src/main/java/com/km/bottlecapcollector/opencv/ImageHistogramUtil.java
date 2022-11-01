package com.km.bottlecapcollector.opencv;

import com.km.bottlecapcollector.exception.ImageSignatureException;
import com.km.bottlecapcollector.model.CapItem;
import com.km.bottlecapcollector.model.OpenCVImageSignature;
import com.km.bottlecapcollector.util.BottleCapPair;
import lombok.extern.slf4j.Slf4j;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class ImageHistogramUtil {

    private static final byte CORRELATION = 0;
    private static final double CORRELATION_BASE = 1;
    private static final byte CHI_SQUARE = 1;
    private static final double CHI_SQUARE_BASE = 0;
    private static final byte INTERSECTION = 2;
    private static final byte BHATTACHARYYA = 3;
    private static final byte BHATTACHARYYA_BASE = 0;
    private final String OBJECT_PREFIX = "object";


    private static final int hBins = 50, sBins = 60;
    private static final int[] histSize = {hBins, sBins};
    // hue varies from 0 to 179, saturation from 0 to 255
    private static final float[] ranges = {0, 180, 0, 256};
    // Use the 0-th and 1-st channels
    private static final int[] channels = {0, 1};

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

    public static Mat calculateHistogram(MultipartFile file) {
        log.info("Calculating Mat object from multipart file...");
        Mat hsvImage = new Mat();
        Mat histImage = new Mat();
        Mat inputImage;
        try {
            inputImage = Imgcodecs.imdecode(new MatOfByte(file.getBytes()), Imgcodecs.CV_LOAD_IMAGE_UNCHANGED);
        } catch (IOException e){
            throw new ImageSignatureException("OpenCV processing exception", e);
        }

        Imgproc.cvtColor(inputImage, hsvImage, Imgproc.COLOR_BGR2HSV);
        List<Mat> hsvBaseList = Arrays.asList(hsvImage);
        Imgproc.calcHist(hsvBaseList, new MatOfInt(channels), new Mat(), histImage, new MatOfInt(histSize), new MatOfFloat(ranges), false);
        Core.normalize(histImage, histImage, 0, 1, Core.NORM_MINMAX);
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
    public static double correlationMethod(Mat histImage1, Mat histImage2) {
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
    public static double chisquareMethod(Mat histImage1, Mat histImage2) {
        return Imgproc.compareHist(histImage1, histImage2, CHI_SQUARE);
    }

    /**
     * The result closer to metric, the better the match
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
     * {@link #BHATTACHARYYA_BASE} is base value
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

    public static double calculateIntersection(Mat histImage1) {
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
            log.error("Could not load mat from file " + name);
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
            log.error("ERROR: Could not save mat to file: " + name);
        }
        return name + OBJECT_PREFIX;
    }

    public static CustomMat convertMatToBottleCapMat(Mat mat) throws ImageSignatureException{
        log.info("Converting Mat object to BottleCapMat object ...");
        float[] image = new float[(int) (mat.total() * mat.elemSize())];
        mat.get(0, 0, image);
        ByteArrayOutputStream bas = new ByteArrayOutputStream();
        DataOutputStream ds = new DataOutputStream(bas);
        try {
            for (float f : image)
                ds.writeFloat(f);
        } catch (IOException e){
            throw new ImageSignatureException("OpenCV processing exception", e);
        }

        byte[] bytes = bas.toByteArray();
        return new CustomMat(bytes, mat.cols(), mat.rows());
    }

    public static Mat convertBottleCapMatToMat(CustomMat data) throws IOException {
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

    public CustomMat calculateAndReturnMathObjectAsBottleCapMat(MultipartFile file) throws ImageSignatureException {
        log.info("Entering calculateAndReturnMathObjectAsBottleCapMat method with multipart file ");
        Mat mat = calculateHistogram(file);
        return convertMathObjectToBottleCapMat(mat);
    }

    public static CustomMat convertMathObjectToBottleCapMat(Mat mat) throws ImageSignatureException {
        log.info("Entering convertMathObjectToBottleCapMat method with multipart file ");
        return convertMatToBottleCapMat(mat);
    }

    public static Mat calculateAndReturnMathObject(MultipartFile file) throws ImageSignatureException {
        log.info("Entering calculateAndReturnMathObject method with multipart file ");
        return calculateHistogram(file);
    }

    public static List<HistogramResult> calculateEachWithEachCap(List<CapItem> caps) {
        List<BottleCapPair> dataToProcess = calculateEachWithEach(caps);
        log.info("Processed " + caps.size() + " caps with " + dataToProcess.size() + " pair output");
        List<HistogramResult> results = dataToProcess.stream().parallel().map(bottleCapPair -> prepareHistogram(bottleCapPair)).collect(Collectors.toList());
        log.info("Created " + results.size() + " histogram results");
        return results;
    }

    public static double calculateIntersectionMethod(Mat mat) {
        return calculateIntersection(mat);
    }

    public static HistogramResult prepareHistogram(BottleCapPair pair) {
        Mat histFromFile1 = null;
        Mat histFromFile2 = null;
        OpenCVImageSignature firstCapSignature = (OpenCVImageSignature) pair.getFirstCap().getImage().getSignature();
        OpenCVImageSignature secondCapSignature = (OpenCVImageSignature) pair.getSecondCap().getImage().getSignature();
        try {
            histFromFile1 = convertBottleCapMatToMat(new CustomMat(firstCapSignature.getImageData(),
                    firstCapSignature.getImageCols(), firstCapSignature.getImageRows()));
            histFromFile2 = convertBottleCapMatToMat(new CustomMat(secondCapSignature.getImageData(),
                    secondCapSignature.getImageCols(), secondCapSignature.getImageRows()));
        } catch (IOException e) {
            log.info("Exception occurred during preparing histogram: " + e.getStackTrace());
        }

        HistogramResult result = calculateCoefficients(histFromFile1, histFromFile2);
        result.setFirstCap(pair.getFirstCap());
        result.setSecondCap(pair.getSecondCap());
        return result;
    }

    public static HistogramResult prepareHistogram(Mat firstCapMat, BottleCapPair pair) {
        CapItem secondCap = pair.getSecondCap();
        Mat secondCapMat = null;
        OpenCVImageSignature secondCapSignature = null;
        try {
            secondCapSignature = (OpenCVImageSignature) secondCap.getImage().getSignature();
            secondCapMat = convertBottleCapMatToMat(
                    new CustomMat(secondCapSignature.getImageData(),
                            secondCapSignature.getImageCols(),
                            secondCapSignature.getImageRows()));
        } catch (IOException e) {
            log.info("Exception occurred during conversion : " + e.getStackTrace());
        }
        HistogramResult result = calculateCoefficients(firstCapMat, secondCapMat);
        result.setFirstCap(pair.getFirstCap());
        result.setSecondCap(pair.getSecondCap());
        return result;
    }

    public static List<BottleCapPair> calculateEachWithEach(List<CapItem> inputList) {
        int inputListSize = inputList.size();
        int outputListSize = (inputList.size() * (inputList.size() - 1) / 2);
        List<BottleCapPair> outputList = new ArrayList<>(outputListSize);
        for (int i = 1; i < inputListSize; i++) {
            outputList.addAll(calculateEach(inputList.subList(0, i + 1), i));
        }
        return outputList;
    }

    private static List<BottleCapPair> calculateEach(List<CapItem> inputList, int size) {
        List<BottleCapPair> result = new ArrayList<>();
        for (int i = 0; i <= size - 1; i++) {
            result.add(new BottleCapPair(inputList.get(i), inputList.get(size)));
        }

        return result;
    }

    public static List<HistogramResult> calculateOneAgainstAllCaps(CapItem cap, List<CapItem> inputList) {
        log.info("Entering calculateOneAgainstAllCaps method with cap with ID : " + cap.getId() + " and name "
                + cap.getName() + " against " + inputList.size() + " items");
        log.info("Creating BottleCap pairs");
        List<BottleCapPair> outputList = inputList.stream().parallel().map(bottleCap -> new BottleCapPair(cap, bottleCap)).collect(Collectors.toList());
        log.info("Preparing histograms");
        return outputList.stream().map(pair -> ImageHistogramUtil.prepareHistogram(pair)).collect(Collectors.toList());
    }
}
