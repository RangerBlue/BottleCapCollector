package com.km.BottleCapCollector;

import com.km.BottleCapCollector.model.BottleCap;
import com.km.BottleCapCollector.repository.BottleCapRepository;
import org.junit.jupiter.api.Test;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.nio.file.Paths;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;


@DataJpaTest
public class BottleCapTests {



    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private BottleCapRepository repository;

    @Test
    public void testFindById() {
        entityManager.persist(new BottleCap("Perła", Paths.get("testPath").toString()));
        long booksSize = repository.count();
        assertEquals(1, booksSize);
        Optional<BottleCap> cap = repository.findById(4l);
        assertEquals("Perła",cap.get().getName());
    }

    @Test
    public void testFindByName() {
        entityManager.persist(new BottleCap("Lech", Paths.get("testPath0").toString()));
        BottleCap cap = repository.findByName("Lech").get(0);
        assertEquals("Lech", cap.getName());
    }

    @Test
    public void testCount() {
        entityManager.persist(new BottleCap("Perła", Paths.get("testPath1").toString()));
        entityManager.persist(new BottleCap("Lech", Paths.get("testPath2").toString()));
        long booksSize = repository.count();
        assertEquals(2, booksSize);
    }

    @Test
    public void testOpenCV() {
        Imgcodecs imageCodecs = new Imgcodecs();
        Mat image1 = imageCodecs.imread(Paths.get("src/main/resources/img/captest.jpg").toString());
        Mat image2 = imageCodecs.imread("src/main/resources/img/captest2.jpg");
        assertNotEquals(image1.size(), image2.size());
    }




}
