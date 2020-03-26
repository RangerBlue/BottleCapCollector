package com.km.BottleCapCollector.repository;

import com.km.BottleCapCollector.model.BottleCap;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

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
    public void testFindById() {
        entityManager.persist(new BottleCap("Perła"));
        long booksSize = repository.count();
        assertEquals(1, booksSize);
        Optional<BottleCap> cap = repository.findById(4l);
        assertEquals("Perła", cap.get().getName());
    }

    @Test
    public void testFindByName() {
        entityManager.persist(new BottleCap("Lech"));
        BottleCap cap = repository.findByName("Lech").get(0);
        assertEquals("Lech", cap.getName());
    }

    @Test
    public void testCount() {
        entityManager.persist(new BottleCap("Perła"));
        entityManager.persist(new BottleCap("Lech"));
        long booksSize = repository.count();
        assertEquals(2, booksSize);
    }

}
