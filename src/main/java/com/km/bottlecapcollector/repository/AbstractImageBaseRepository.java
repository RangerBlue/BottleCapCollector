package com.km.bottlecapcollector.repository;

import com.km.bottlecapcollector.model.AbstractImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface AbstractImageBaseRepository<T extends AbstractImage> extends JpaRepository<T, Long> {
}
