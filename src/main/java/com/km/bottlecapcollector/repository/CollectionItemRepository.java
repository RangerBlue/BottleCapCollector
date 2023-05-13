package com.km.bottlecapcollector.repository;

import com.km.bottlecapcollector.model.CollectionItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface CollectionItemRepository<T extends CollectionItem> extends JpaRepository<T, Long> {

}
