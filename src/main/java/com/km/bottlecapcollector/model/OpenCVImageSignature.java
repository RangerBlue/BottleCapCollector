package com.km.bottlecapcollector.model;

import com.km.bottlecapcollector.exception.ImageSignatureException;
import com.km.bottlecapcollector.opencv.CustomMat;
import com.km.bottlecapcollector.opencv.ImageHistogramUtil;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import lombok.Data;
import org.opencv.core.Mat;
import org.springframework.web.multipart.MultipartFile;

@Entity
@DiscriminatorValue("OpenCV")
@Data
public class OpenCVImageSignature extends AbstractSignature {
    @Lob
    private byte[] imageData;

    private int imageCols;

    private int imageRows;

    private double intersectionValue;


    @Override
    public AbstractSignature calculateParameters(MultipartFile file) throws ImageSignatureException {
        Mat mat = ImageHistogramUtil.calculateAndReturnMathObject(file);
        CustomMat customMatFile = ImageHistogramUtil.convertMathObjectToBottleCapMat(mat);
        setImageData(customMatFile.getMatArray());
        setImageCols(customMatFile.getCols());
        setImageRows(customMatFile.getRows());
        setIntersectionValue(ImageHistogramUtil.calculateIntersection(mat));
        return this;
    }
}
