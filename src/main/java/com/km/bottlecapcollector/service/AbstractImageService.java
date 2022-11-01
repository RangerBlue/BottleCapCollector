package com.km.bottlecapcollector.service;

import com.km.bottlecapcollector.model.AbstractImage;
import com.km.bottlecapcollector.model.RectangleOpenCVGoogleImage;
import com.km.bottlecapcollector.model.RoundOpenCVGoogleImage;
import com.km.bottlecapcollector.repository.AbstractImageRepository;
import com.km.bottlecapcollector.repository.RectangleOpenCVGoogleImageRepository;
import com.km.bottlecapcollector.repository.RoundOpenCVGoogleImageRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
@AllArgsConstructor
public class AbstractImageService {

    private final RoundOpenCVGoogleImageRepository roundOpenCVGoogleImageRepository;
    private final RectangleOpenCVGoogleImageRepository rectangleOpenCVGoogleImageRepository;

    private final AbstractImageRepository abstractImageBaseRepository;


    public List<? extends AbstractImage> getAllGoogleDriveImages() {
        List<RectangleOpenCVGoogleImage> rectanglePictures = rectangleOpenCVGoogleImageRepository.findAll();
        List<RoundOpenCVGoogleImage> roundPictures = roundOpenCVGoogleImageRepository.findAll();
        return Stream.concat(rectanglePictures.stream(), roundPictures.stream()).collect(Collectors.toList());
    }

    public void updateGoogleDriveImage(AbstractImage image) {
        abstractImageBaseRepository.save(image);
    }
}
