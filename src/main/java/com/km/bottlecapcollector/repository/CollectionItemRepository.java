package com.km.bottlecapcollector.repository;

import com.km.bottlecapcollector.model.CollectionItem;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface CollectionItemRepository<T extends CollectionItem> extends CrudRepository <T, Long>{

}
