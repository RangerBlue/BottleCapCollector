package com.km.bottlecapcollector.model;

import com.km.bottlecapcollector.util.CustomMat;
import com.km.bottlecapcollector.util.ImageHistogramUtil;
import lombok.Data;
import org.opencv.core.Mat;
import org.springframework.web.multipart.MultipartFile;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Lob;
import java.io.IOException;

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
    public void calculateParameters(MultipartFile file) throws IOException {
        Mat mat = ImageHistogramUtil.calculateAndReturnMathObject(file);
        CustomMat customMatFile = ImageHistogramUtil.convertMathObjectToBottleCapMat(mat);
        setImageData(customMatFile.getMatArray());
        setImageCols(customMatFile.getCols());
        setImageRows(customMatFile.getRows());
        setIntersectionValue(ImageHistogramUtil.calculateIntersection(mat));
    }
}
