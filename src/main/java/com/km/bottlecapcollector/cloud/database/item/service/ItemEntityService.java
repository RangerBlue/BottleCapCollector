package com.km.bottlecapcollector.cloud.database.item.service;

import com.km.bottlecapcollector.cloud.database.item.entity.ItemEntity;
import com.km.bottlecapcollector.cloud.database.item.repository.CollectionItemEntityRepository;
import com.km.bottlecapcollector.cloud.database.exception.FirestoreDocumentNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;


@Service
@Slf4j
@RequiredArgsConstructor
public class ItemEntityService {

    private final CollectionItemEntityRepository repository;

    public ItemEntity save(String collectionKey, ItemEntity item) {
        log.debug("Saving item to collection: {}", collectionKey);
        return repository.save(collectionKey, item);
    }

    public Optional<ItemEntity> findById(String collectionKey, String id) {
        log.trace("Finding item by id: {} in collection: {}", id, collectionKey);
        return repository.findById(collectionKey, id);
    }

    public ItemEntity findByIdOrThrow(String collectionKey, String id) {
        return findById(collectionKey, id)
                .orElseThrow(() -> new FirestoreDocumentNotFoundException(collectionKey, id));
    }

    public Optional<ItemEntity> findByIdAndUserId(String collectionKey, String id, String userId) {
        log.trace("Finding item by id: {} and userId: {} in collection: {}", id, userId, collectionKey);
        return repository.findByIdAndUserId(collectionKey, id, userId);
    }

    public ItemEntity findByIdAndUserIdOrThrow(String collectionKey, String id, String userId) {
        return findByIdAndUserId(collectionKey, id, userId)
                .orElseThrow(() -> new FirestoreDocumentNotFoundException(collectionKey, id));
    }


    public List<ItemEntity> findByUserId(String collectionKey, String userId, int limit, int offset) {
        log.trace("Finding items by userId: {} in collection: {} with limit: {}, offset: {}", userId, collectionKey, limit, offset);
        return repository.findByUserId(collectionKey, userId, limit, offset);
    }

    public long countByUserId(String collectionKey, String userId) {
        log.trace("Counting items by userId: {} in collection: {}", userId, collectionKey);
        return repository.countByUserId(collectionKey, userId);
    }

    public List<ItemEntity> findBySearchTokenAndUserId(String collectionKey, String searchToken, String userId, int limit, int offset) {
        log.trace("Finding items by searchToken: {} and userId: {} in collection: {}", searchToken, userId, collectionKey);
        return repository.findBySearchTokenAndUserId(collectionKey, searchToken, userId, limit, offset);
    }

    public long countBySearchTokenAndUserId(String collectionKey, String searchToken, String userId) {
        log.trace("Counting items by searchToken: {} and userId: {} in collection: {}", searchToken, userId, collectionKey);
        return repository.countBySearchTokenAndUserId(collectionKey, searchToken, userId);
    }

    public List<ItemEntity> findByHSBColorRangeAndUserId(String collectionKey,
                                                          float hueMin, float hueMax,
                                                          float satMin, float satMax,
                                                          float briMin, float briMax,
                                                          String userId, int limit) {
        log.trace("Finding items by HSB color range for userId: {} in collection: {}", userId, collectionKey);
        return repository.findByHSBColorRangeAndUserId(collectionKey, hueMin, hueMax, satMin, satMax, briMin, briMax, userId, limit);
    }

    public void deleteById(String collectionKey, String id) {
        log.debug("Deleting item by id: {} from collection: {}", id, collectionKey);
        repository.deleteById(collectionKey, id);
    }

    public void deleteByIdAndUserId(String collectionKey, String id, String userId) {
        log.debug("Deleting item by id: {} and userId: {} from collection: {}", id, userId, collectionKey);
        if (findByIdAndUserId(collectionKey, id, userId).isEmpty()) {
            throw new FirestoreDocumentNotFoundException(collectionKey, id);
        }
        repository.deleteById(collectionKey, id);
    }
}
