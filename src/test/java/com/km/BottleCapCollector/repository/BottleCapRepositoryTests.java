package com.km.BottleCapCollector.repository;

import com.km.BottleCapCollector.model.BottleCap;
import org.junit.After;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;


@DataJpaTest
public class BottleCapRepositoryTests {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private BottleCapRepository repository;

    private final Path resourceFolder = Paths.get("src/main/resources/img/");
    private final String img1Name = "captest.jpg";
    private final String img2Name = "captest2.jpg";
    private final String img1 = "src/main/resources/img/captest.jpg";
    private final String img2 = "src/main/resources/img/captest2.jpg";

    @Test
    public void testAddCap(){
        long id = entityManager.persist(new BottleCap("Perła", "img1234.img")).getId();
        BottleCap cap = repository.findById(id).get();
        assertEquals(cap.getCapName(), "Perła");
        assertTrue(cap.getFileLocation().contains("img1234.img"));
    }

    @Test
    public void testFindById() {
        long id = entityManager.persist(new BottleCap("Perła", "img1234.img")).getId();
        BottleCap cap = repository.findById(id).get();
        assertEquals("Perła", cap.getCapName());
        assertTrue(cap.getFileLocation().contains("img1234.img"));
    }

    @Test
    public void testFindByName() {
        entityManager.persist(new BottleCap("Lech", "img321.img"));
        BottleCap cap = repository.findByCapName("Lech").get(0);
        assertEquals("Lech", cap.getCapName());
        assertTrue(cap.getFileLocation().contains("img321.img"));
    }

    @Test
    public void testCount() {
        entityManager.persist(new BottleCap("Perła"));
        entityManager.persist(new BottleCap("Lech"));
        long booksSize = repository.count();
        assertEquals(2, booksSize);
    }

}
