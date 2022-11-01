package com.km.bottlecapcollector.repository;

import com.km.bottlecapcollector.ImageHistogramFactoryTests;
import com.km.bottlecapcollector.model.CapItem;
import com.km.bottlecapcollector.model.OpenCVImageSignature;
import com.km.bottlecapcollector.opencv.CustomMat;
import com.km.bottlecapcollector.opencv.ImageHistogramUtil;
import com.km.bottlecapcollector.util.ItemFactoryImpl;
import org.junit.jupiter.api.Test;
import org.opencv.core.Mat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import({ImageHistogramUtil.class})
public class CapItemRepositoryOpenCVTests {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CapItemRepository repository;
    ImageHistogramUtil imageHistogramUtil = new ImageHistogramUtil();

    private final Path resourceFolder = Paths.get("src/main/resources/img/");
    private final String img1Name = "captest.jpg";

    @Test
    public void testAddCap() throws IOException {
        Mat hist1 = imageHistogramUtil.calculateHistogram(img1Name, resourceFolder);
        CapItem cap = new ItemFactoryImpl().getCapItem();
        cap.setName("Perła");
        cap.setDescription("Description");
        MultipartFile file = new MockMultipartFile(img1Name,
                Files.newInputStream(new File("src/main/resources/img/captest.jpg").toPath()));
        cap.getImage().getSignature().calculateParameters(file);
        long id = entityManager.persist(cap).getId();
        CapItem cap1 = repository.findById(id).get();
        assertEquals(cap1.getName(), "Perła");
        OpenCVImageSignature openCVImageSignature = (OpenCVImageSignature) cap.getImage().getSignature();
        Mat hist2 = ImageHistogramUtil.convertBottleCapMatToMat(new CustomMat(openCVImageSignature.getImageData(),
                openCVImageSignature.getImageCols(), openCVImageSignature.getImageRows()));
        ImageHistogramFactoryTests.assertHistogramsConversion1x1(hist1, hist2, imageHistogramUtil);
    }

}
