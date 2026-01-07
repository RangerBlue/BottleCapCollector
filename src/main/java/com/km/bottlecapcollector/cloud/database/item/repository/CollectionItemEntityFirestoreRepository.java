package com.km.bottlecapcollector.cloud.database.item.repository;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.km.bottlecapcollector.cloud.database.item.entity.ItemEntity;
import com.km.bottlecapcollector.cloud.database.exception.FirestoreException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

/**
 * Implementation of FirestoreBottleCapRepository using Google Cloud Firestore.
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class CollectionItemEntityFirestoreRepository implements CollectionItemEntityRepository {

    private final Firestore firestore;

    private CollectionReference getCollection(String collectionName) {
        return firestore.collection(collectionName);
    }

    @Override
    public ItemEntity save(String collectionName, ItemEntity item) {
        log.info("Saving item to collection '{}': {}", collectionName, item.getName());
        try {
            DocumentReference docRef;
            if (item.getId() == null || item.getId().isEmpty()) {
                docRef = getCollection(collectionName).document();
                item.setId(docRef.getId());
            } else {
                docRef = getCollection(collectionName).document(item.getId());
            }

            ApiFuture<WriteResult> result = docRef.set(item);
            result.get();
            log.info("Successfully saved item with id: {} to collection '{}'", item.getId(), collectionName);
            return item;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FirestoreException("Interrupted while saving item", e);
        } catch (ExecutionException e) {
            throw new FirestoreException("Failed to save item", e.getCause());
        }
    }

    @Override
    public Optional<ItemEntity> findById(String collectionName, String id) {
        log.trace("Finding item by id: {} in collection '{}'", id, collectionName);
        try {
            DocumentSnapshot document = getCollection(collectionName).document(id).get().get();
            if (document.exists()) {
                return Optional.ofNullable(document.toObject(ItemEntity.class));
            }
            return Optional.empty();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FirestoreException("Interrupted while finding item by id", e);
        } catch (ExecutionException e) {
            throw new FirestoreException("Failed to find item by id: " + id, e.getCause());
        }
    }

    @Override
    public Optional<ItemEntity> findByIdAndUserId(String collectionName, String id, String userId) {
        log.trace("Finding item by id: {} and userId: {} in collection '{}'", id, userId, collectionName);
        try {
            QuerySnapshot querySnapshot = getCollection(collectionName)
                    .whereEqualTo(FieldPath.documentId(), id)
                    .whereEqualTo("userId", userId)
                    .limit(1)
                    .get()
                    .get();
            List<ItemEntity> items = toItemList(querySnapshot);
            return items.isEmpty() ? Optional.empty() : Optional.of(items.get(0));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FirestoreException("Interrupted while finding item by id and userId", e);
        } catch (ExecutionException e) {
            throw new FirestoreException("Failed to find item by id: " + id + " and userId: " + userId, e.getCause());
        }
    }

    @Override
    public List<ItemEntity> findAll(String collectionName) {
        log.trace("Finding all items in collection '{}'", collectionName);
        try {
            QuerySnapshot querySnapshot = getCollection(collectionName).get().get();
            return toItemList(querySnapshot);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FirestoreException("Interrupted while finding all items", e);
        } catch (ExecutionException e) {
            throw new FirestoreException("Failed to find all items", e.getCause());
        }
    }

    @Override
    public List<ItemEntity> findAll(String collectionName, int limit, int offset) {
        log.trace("Finding items in collection '{}' with limit: {}, offset: {}", collectionName, limit, offset);
        try {
            QuerySnapshot querySnapshot = getCollection(collectionName)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .offset(offset)
                    .limit(limit)
                    .get()
                    .get();
            return toItemList(querySnapshot);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FirestoreException("Interrupted while finding items with pagination", e);
        } catch (ExecutionException e) {
            throw new FirestoreException("Failed to find items with pagination", e.getCause());
        }
    }

    @Override
    public List<ItemEntity> findByUserId(String collectionName, String userId) {
        log.trace("Finding items by user id: {} in collection '{}'", userId, collectionName);
        try {
            QuerySnapshot querySnapshot = getCollection(collectionName)
                    .whereEqualTo("userId", userId)
                    .get()
                    .get();
            return toItemList(querySnapshot);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FirestoreException("Interrupted while finding items by user id", e);
        } catch (ExecutionException e) {
            throw new FirestoreException("Failed to find items by user id: " + userId, e.getCause());
        }
    }

    @Override
    public List<ItemEntity> findByUserId(String collectionName, String userId, int limit, int offset) {
        log.trace("Finding items by user id: {} in collection '{}' with limit: {}, offset: {}",
                userId, collectionName, limit, offset);
        try {
            QuerySnapshot querySnapshot = getCollection(collectionName)
                    .whereEqualTo("userId", userId)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .offset(offset)
                    .limit(limit)
                    .get()
                    .get();
            return toItemList(querySnapshot);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FirestoreException("Interrupted while finding items by user id with pagination", e);
        } catch (ExecutionException e) {
            throw new FirestoreException("Failed to find items by user id with pagination: " + userId, e.getCause());
        }
    }

    @Override
    public long countByUserId(String collectionName, String userId) {
        log.trace("Counting items by user id: {} in collection '{}'", userId, collectionName);
        try {
            AggregateQuerySnapshot snapshot = getCollection(collectionName)
                    .whereEqualTo("userId", userId)
                    .count()
                    .get()
                    .get();
            return snapshot.getCount();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FirestoreException("Interrupted while counting items by user id", e);
        } catch (ExecutionException e) {
            throw new FirestoreException("Failed to count items by user id: " + userId, e.getCause());
        }
    }

    @Override
    public List<ItemEntity> findByHSBColorRangeAndUserId(
            String collectionName,
            float hueMin, float hueMax,
            float saturationMin, float saturationMax,
            float brightnessMin, float brightnessMax,
            String userId,
            int limit) {
        log.trace("Finding items in collection '{}' by HSB color range for user: {} - H:[{}-{}], S:[{}-{}], B:[{}-{}]",
                collectionName, userId, hueMin, hueMax, saturationMin, saturationMax, brightnessMin, brightnessMax);
        try {
            QuerySnapshot querySnapshot = getCollection(collectionName)
                    .whereEqualTo("userId", userId)
                    .whereGreaterThanOrEqualTo("image.hsbColor.hue", hueMin)
                    .whereLessThanOrEqualTo("image.hsbColor.hue", hueMax)
                    .get()
                    .get();

            List<ItemEntity> allMatches = toItemList(querySnapshot);

            return allMatches.stream()
                    .filter(item -> item.getImage() != null && item.getImage().getHsbColor() != null)
                    .filter(item -> {
                        Float sat = item.getImage().getHsbColor().getSaturation();
                        Float bri = item.getImage().getHsbColor().getBrightness();
                        return sat != null && bri != null
                                && sat >= saturationMin && sat <= saturationMax
                                && bri >= brightnessMin && bri <= brightnessMax;
                    })
                    .limit(limit)
                    .toList();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FirestoreException("Interrupted while finding items by HSB color range and userId", e);
        } catch (ExecutionException e) {
            throw new FirestoreException("Failed to find items by HSB color range and userId: " + userId, e.getCause());
        }
    }



    @Override
    public void deleteById(String collectionName, String id) {
        log.info("Deleting item with id: {} from collection '{}'", id, collectionName);
        try {
            getCollection(collectionName).document(id).delete().get();
            log.info("Successfully deleted item with id: {} from collection '{}'", id, collectionName);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FirestoreException("Interrupted while deleting item", e);
        } catch (ExecutionException e) {
            throw new FirestoreException("Failed to delete item with id: " + id, e.getCause());
        }
    }


    @Override
    public List<ItemEntity> findBySearchTokenAndUserId(String collectionName, String token, String userId, int limit, int offset) {
        log.trace("Finding items by search token: {} and user id: {} in collection '{}' with limit: {}, offset: {}",
                token, userId, collectionName, limit, offset);
        try {
            QuerySnapshot querySnapshot = getCollection(collectionName)
                    .whereArrayContains("searchTokens", token)
                    .whereEqualTo("userId", userId)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .offset(offset)
                    .limit(limit)
                    .get()
                    .get();
            return toItemList(querySnapshot);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FirestoreException("Interrupted while finding items by search token and user id", e);
        } catch (ExecutionException e) {
            throw new FirestoreException("Failed to find items by search token: " + token + " and user id: " + userId, e.getCause());
        }
    }

    @Override
    public long countBySearchTokenAndUserId(String collectionName, String token, String userId) {
        log.trace("Counting items by search token: {} and user id: {} in collection '{}'", token, userId, collectionName);
        try {
            AggregateQuerySnapshot snapshot = getCollection(collectionName)
                    .whereArrayContains("searchTokens", token)
                    .whereEqualTo("userId", userId)
                    .count()
                    .get()
                    .get();
            return snapshot.getCount();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FirestoreException("Interrupted while counting items by search token and user id", e);
        } catch (ExecutionException e) {
            throw new FirestoreException("Failed to count items by search token: " + token + " and user id: " + userId, e.getCause());
        }
    }

    private List<ItemEntity> toItemList(QuerySnapshot querySnapshot) {
        List<ItemEntity> items = new ArrayList<>();
        for (DocumentSnapshot document : querySnapshot.getDocuments()) {
            ItemEntity item = document.toObject(ItemEntity.class);
            if (item != null) {
                items.add(item);
            }
        }
        return items;
    }
}
