package com.km.bottlecapcollector.cloud.database.repository;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.km.bottlecapcollector.cloud.database.document.ItemEntity;
import com.km.bottlecapcollector.cloud.exception.FirestoreException;
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
@Slf4j
public class FirestoreBottleCapRepositoryImpl implements FirestoreBottleCapRepository {

    private final Firestore firestore;

    public FirestoreBottleCapRepositoryImpl(Firestore firestore) {
        this.firestore = firestore;
    }

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
    public Optional<ItemEntity> findByCollectionTypeAndId(String collectionName, String collectionKey, String id) {
        log.trace("Finding item by collection key: {} and id: {} in collection '{}'", collectionKey, id, collectionName);
        try {
            DocumentSnapshot document = getCollection(collectionName).document(id).get().get();
            if (document.exists()) {
                ItemEntity item = document.toObject(ItemEntity.class);
                if (item != null && collectionKey.equals(item.getCollectionKey())) {
                    return Optional.of(item);
                }
            }
            return Optional.empty();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FirestoreException("Interrupted while finding item by collection key and id", e);
        } catch (ExecutionException e) {
            throw new FirestoreException("Failed to find item by collection key: " + collectionKey + " and id: " + id, e.getCause());
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
    public List<ItemEntity> findByCollectionType(String collectionName, String collectionType) {
        log.trace("Finding items by collection type: {} in collection '{}'", collectionType, collectionName);
        try {
            QuerySnapshot querySnapshot = getCollection(collectionName)
                    .whereEqualTo("collectionType", collectionType)
                    .get()
                    .get();
            return toItemList(querySnapshot);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FirestoreException("Interrupted while finding items by collection type", e);
        } catch (ExecutionException e) {
            throw new FirestoreException("Failed to find items by collection type: " + collectionType, e.getCause());
        }
    }

    @Override
    public List<ItemEntity> findByCollectionType(String collectionName, String collectionType, int limit, int offset) {
        log.trace("Finding items by collection type: {} in collection '{}' with limit: {}, offset: {}", collectionType, collectionName, limit, offset);
        try {
            QuerySnapshot querySnapshot = getCollection(collectionName)
                    .whereEqualTo("collectionType", collectionType)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .offset(offset)
                    .limit(limit)
                    .get()
                    .get();
            return toItemList(querySnapshot);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FirestoreException("Interrupted while finding items by collection type with pagination", e);
        } catch (ExecutionException e) {
            throw new FirestoreException("Failed to find items by collection type with pagination: " + collectionType, e.getCause());
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
    public List<ItemEntity> findByUserIdAndCollectionType(String collectionName, String userId, String collectionType) {
        log.trace("Finding items by user id: {} and collection type: {} in collection '{}'", userId, collectionType, collectionName);
        try {
            QuerySnapshot querySnapshot = getCollection(collectionName)
                    .whereEqualTo("userId", userId)
                    .whereEqualTo("collectionType", collectionType)
                    .get()
                    .get();
            return toItemList(querySnapshot);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FirestoreException("Interrupted while finding items by user id and collection type", e);
        } catch (ExecutionException e) {
            throw new FirestoreException("Failed to find items by user id: " + userId + " and collection type: " + collectionType, e.getCause());
        }
    }

    @Override
    public List<ItemEntity> findByNameContaining(String collectionName, String name) {
        log.trace("Finding items by name containing: {} in collection '{}'", name, collectionName);
        try {
            String searchName = name.toLowerCase();
            QuerySnapshot querySnapshot = getCollection(collectionName)
                    .whereGreaterThanOrEqualTo("name", searchName)
                    .whereLessThanOrEqualTo("name", searchName + "\uf8ff")
                    .get()
                    .get();
            return toItemList(querySnapshot);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FirestoreException("Interrupted while finding items by name", e);
        } catch (ExecutionException e) {
            throw new FirestoreException("Failed to find items by name: " + name, e.getCause());
        }
    }

    @Override
    public List<ItemEntity> findByNameContainingAndCollectionType(String collectionName, String name, String collectionType) {
        log.trace("Finding items by name containing: {} and collection type: {} in collection '{}'", name, collectionType, collectionName);
        try {
            String searchName = name.toLowerCase();
            QuerySnapshot querySnapshot = getCollection(collectionName)
                    .whereEqualTo("collectionType", collectionType)
                    .get()
                    .get();

            return toItemList(querySnapshot).stream()
                    .filter(item -> item.getName() != null && item.getName().toLowerCase().contains(searchName))
                    .toList();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FirestoreException("Interrupted while finding items by name and collection type", e);
        } catch (ExecutionException e) {
            throw new FirestoreException("Failed to find items by name: " + name + " and collection type: " + collectionType, e.getCause());
        }
    }

    @Override
    public List<ItemEntity> findByTag(String collectionName, String tag) {
        log.trace("Finding items by tag: {} in collection '{}'", tag, collectionName);
        try {
            QuerySnapshot querySnapshot = getCollection(collectionName)
                    .whereArrayContains("tags", tag)
                    .get()
                    .get();
            return toItemList(querySnapshot);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FirestoreException("Interrupted while finding items by tag", e);
        } catch (ExecutionException e) {
            throw new FirestoreException("Failed to find items by tag: " + tag, e.getCause());
        }
    }

    @Override
    public List<ItemEntity> findByTagAndCollectionType(String collectionName, String tag, String collectionType) {
        log.trace("Finding items by tag: {} and collection type: {} in collection '{}'", tag, collectionType, collectionName);
        try {
            QuerySnapshot querySnapshot = getCollection(collectionName)
                    .whereArrayContains("tags", tag)
                    .whereEqualTo("collectionType", collectionType)
                    .get()
                    .get();
            return toItemList(querySnapshot);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FirestoreException("Interrupted while finding items by tag and collection type", e);
        } catch (ExecutionException e) {
            throw new FirestoreException("Failed to find items by tag: " + tag + " and collection type: " + collectionType, e.getCause());
        }
    }

    @Override
    public List<ItemEntity> findWithEmbeddings(String collectionName) {
        log.trace("Finding items with embeddings in collection '{}'", collectionName);
        try {
            QuerySnapshot querySnapshot = getCollection(collectionName)
                    .whereNotEqualTo("embedding", null)
                    .get()
                    .get();
            return toItemList(querySnapshot);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FirestoreException("Interrupted while finding items with embeddings", e);
        } catch (ExecutionException e) {
            throw new FirestoreException("Failed to find items with embeddings", e.getCause());
        }
    }

    @Override
    public List<ItemEntity> findWithEmbeddingsByCollectionType(String collectionName, String collectionType) {
        log.trace("Finding items with embeddings by collection type: {} in collection '{}'", collectionType, collectionName);
        try {
            QuerySnapshot querySnapshot = getCollection(collectionName)
                    .whereEqualTo("collectionType", collectionType)
                    .get()
                    .get();

            return toItemList(querySnapshot).stream()
                    .filter(item -> item.getEmbedding() != null)
                    .toList();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FirestoreException("Interrupted while finding items with embeddings by collection type", e);
        } catch (ExecutionException e) {
            throw new FirestoreException("Failed to find items with embeddings by collection type: " + collectionType, e.getCause());
        }
    }

    @Override
    public List<ItemEntity> findWithVisionMetadata(String collectionName) {
        log.trace("Finding items with vision metadata in collection '{}'", collectionName);
        try {
            QuerySnapshot querySnapshot = getCollection(collectionName)
                    .whereNotEqualTo("visionMetadata", null)
                    .get()
                    .get();
            return toItemList(querySnapshot);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FirestoreException("Interrupted while finding items with vision metadata", e);
        } catch (ExecutionException e) {
            throw new FirestoreException("Failed to find items with vision metadata", e.getCause());
        }
    }

    @Override
    public List<ItemEntity> findWithVisionMetadataByCollectionType(String collectionName, String collectionType) {
        log.trace("Finding items with vision metadata by collection type: {} in collection '{}'", collectionType, collectionName);
        try {
            QuerySnapshot querySnapshot = getCollection(collectionName)
                    .whereEqualTo("collectionType", collectionType)
                    .get()
                    .get();

            return toItemList(querySnapshot).stream()
                    .filter(item -> item.getVisionMetadata() != null)
                    .toList();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FirestoreException("Interrupted while finding items with vision metadata by collection type", e);
        } catch (ExecutionException e) {
            throw new FirestoreException("Failed to find items with vision metadata by collection type: " + collectionType, e.getCause());
        }
    }

    @Override
    public List<ItemEntity> findByHSBColorRange(
            String collectionName,
            float hueMin, float hueMax,
            float saturationMin, float saturationMax,
            float brightnessMin, float brightnessMax,
            int limit) {
        log.trace("Finding items in collection '{}' by HSB color range - H:[{}-{}], S:[{}-{}], B:[{}-{}]",
                collectionName, hueMin, hueMax, saturationMin, saturationMax, brightnessMin, brightnessMax);
        try {
            QuerySnapshot querySnapshot = getCollection(collectionName)
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
            throw new FirestoreException("Interrupted while finding items by HSB color range", e);
        } catch (ExecutionException e) {
            throw new FirestoreException("Failed to find items by HSB color range", e.getCause());
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
    public List<ItemEntity> findByHSBColorRangeAndCollectionType(
            String collectionName,
            float hueMin, float hueMax,
            float saturationMin, float saturationMax,
            float brightnessMin, float brightnessMax,
            String collectionType,
            int limit) {
        log.trace("Finding items in collection '{}' by HSB color range and collection type: {} - H:[{}-{}], S:[{}-{}], B:[{}-{}]",
                collectionName, collectionType, hueMin, hueMax, saturationMin, saturationMax, brightnessMin, brightnessMax);
        try {
            QuerySnapshot querySnapshot = getCollection(collectionName)
                    .whereEqualTo("collectionType", collectionType)
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
            throw new FirestoreException("Interrupted while finding items by HSB color range and collection type", e);
        } catch (ExecutionException e) {
            throw new FirestoreException("Failed to find items by HSB color range and collection type", e.getCause());
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
    public boolean existsById(String collectionName, String id) {
        log.trace("Checking if item exists with id: {} in collection '{}'", id, collectionName);
        try {
            DocumentSnapshot document = getCollection(collectionName).document(id).get().get();
            return document.exists();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FirestoreException("Interrupted while checking item existence", e);
        } catch (ExecutionException e) {
            throw new FirestoreException("Failed to check item existence for id: " + id, e.getCause());
        }
    }

    @Override
    public long count(String collectionName) {
        log.trace("Counting all items in collection '{}'", collectionName);
        try {
            AggregateQuerySnapshot snapshot = getCollection(collectionName).count().get().get();
            return snapshot.getCount();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FirestoreException("Interrupted while counting items", e);
        } catch (ExecutionException e) {
            throw new FirestoreException("Failed to count items", e.getCause());
        }
    }

    @Override
    public long countByCollectionType(String collectionName, String collectionType) {
        log.trace("Counting items by collection type: {} in collection '{}'", collectionType, collectionName);
        try {
            AggregateQuerySnapshot snapshot = getCollection(collectionName)
                    .whereEqualTo("collectionType", collectionType)
                    .count()
                    .get()
                    .get();
            return snapshot.getCount();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FirestoreException("Interrupted while counting items by collection type", e);
        } catch (ExecutionException e) {
            throw new FirestoreException("Failed to count items by collection type: " + collectionType, e.getCause());
        }
    }

    @Override
    public List<ItemEntity> findBySearchToken(String collectionName, String token) {
        log.trace("Finding items by search token: {} in collection '{}'", token, collectionName);
        try {
            QuerySnapshot querySnapshot = getCollection(collectionName)
                    .whereArrayContains("searchTokens", token)
                    .get()
                    .get();
            return toItemList(querySnapshot);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FirestoreException("Interrupted while finding items by search token", e);
        } catch (ExecutionException e) {
            throw new FirestoreException("Failed to find items by search token: " + token, e.getCause());
        }
    }

    @Override
    public List<ItemEntity> findBySearchToken(String collectionName, String token, int limit, int offset) {
        log.trace("Finding items by search token: {} in collection '{}' with limit: {}, offset: {}",
                token, collectionName, limit, offset);
        try {
            QuerySnapshot querySnapshot = getCollection(collectionName)
                    .whereArrayContains("searchTokens", token)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .offset(offset)
                    .limit(limit)
                    .get()
                    .get();
            return toItemList(querySnapshot);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FirestoreException("Interrupted while finding items by search token with pagination", e);
        } catch (ExecutionException e) {
            throw new FirestoreException("Failed to find items by search token with pagination: " + token, e.getCause());
        }
    }

    @Override
    public long countBySearchToken(String collectionName, String token) {
        log.trace("Counting items by search token: {} in collection '{}'", token, collectionName);
        try {
            AggregateQuerySnapshot snapshot = getCollection(collectionName)
                    .whereArrayContains("searchTokens", token)
                    .count()
                    .get()
                    .get();
            return snapshot.getCount();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FirestoreException("Interrupted while counting items by search token", e);
        } catch (ExecutionException e) {
            throw new FirestoreException("Failed to count items by search token: " + token, e.getCause());
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
