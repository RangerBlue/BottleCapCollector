package com.km.bottlecapcollector.cloud.database.item.repository;

import com.google.cloud.firestore.AggregateQuerySnapshot;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.FieldPath;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.common.collect.Lists;
import com.km.bottlecapcollector.cloud.database.item.entity.ItemEntity;
import com.km.bottlecapcollector.color.HSBColorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.km.bottlecapcollector.cloud.database.FirestoreExecutor.execute;

@Repository
@RequiredArgsConstructor
@Slf4j
public class CollectionItemEntityFirestoreRepository implements CollectionItemEntityRepository {
    private static final String COLLECTIONS_ROOT = "collections";
    private static final String ITEMS_SUBCOLLECTION = "items";
    private final Firestore firestore;

    @Override
    public ItemEntity save(String collectionName, ItemEntity item) {
        log.info("Saving item to collection '{}': {}", collectionName, item.getName());

        DocumentReference docRef;
        if (item.getId() == null || item.getId().isEmpty()) {
            docRef = getCollection(collectionName).document();
            item.setId(docRef.getId());
        } else {
            docRef = getCollection(collectionName).document(item.getId());
        }

        execute(docRef.set(item), "save item");
        log.info("Successfully saved item with id: {} to collection '{}'", item.getId(), collectionName);
        return item;
    }

    @Override
    public Optional<ItemEntity> findById(String collectionName, String id) {
        log.trace("Finding item by id: {} in collection '{}'", id, collectionName);

        DocumentSnapshot document = execute(
                getCollection(collectionName).document(id).get(),
                "find item by id: " + id
        );

        return document.exists()
                ? Optional.ofNullable(document.toObject(ItemEntity.class))
                : Optional.empty();
    }

    @Override
    public Optional<ItemEntity> findByIdAndUserId(String collectionName, String id, String userId) {
        log.trace("Finding item by id: {} and userId: {} in collection '{}'", id, userId, collectionName);

        QuerySnapshot querySnapshot = execute(
                getCollection(collectionName)
                        .whereEqualTo(FieldPath.documentId(), id)
                        .whereEqualTo("userId", userId)
                        .limit(1)
                        .get(),
                "find item by id: " + id + " and userId: " + userId
        );

        List<ItemEntity> items = toItemList(querySnapshot);
        return items.isEmpty() ? Optional.empty() : Optional.of(items.getFirst());
    }

    @Override
    public List<ItemEntity> findAll(String collectionName) {
        log.trace("Finding all items in collection '{}'", collectionName);

        QuerySnapshot querySnapshot = execute(
                getCollection(collectionName).get(),
                "find all items"
        );

        return toItemList(querySnapshot);
    }

    @Override
    public List<ItemEntity> findAll(String collectionName, int limit, int offset) {
        log.trace("Finding items in collection '{}' with limit: {}, offset: {}", collectionName, limit, offset);

        QuerySnapshot querySnapshot = execute(
                getCollection(collectionName)
                        .orderBy("createdAt", Query.Direction.DESCENDING)
                        .offset(offset)
                        .limit(limit)
                        .get(),
                "find items with pagination"
        );

        return toItemList(querySnapshot);
    }

    @Override
    public List<ItemEntity> findByUserId(String collectionName, String userId) {
        log.trace("Finding items by user id: {} in collection '{}'", userId, collectionName);

        QuerySnapshot querySnapshot = execute(
                getCollection(collectionName)
                        .whereEqualTo("userId", userId)
                        .get(),
                "find items by userId: " + userId
        );

        return toItemList(querySnapshot);
    }

    @Override
    public List<ItemEntity> findByUserId(String collectionName, String userId, int limit, int offset) {
        log.trace("Finding items by user id: {} in collection '{}' with limit: {}, offset: {}",
                userId, collectionName, limit, offset);

        QuerySnapshot querySnapshot = execute(
                getCollection(collectionName)
                        .whereEqualTo("userId", userId)
                        .orderBy("createdAt", Query.Direction.DESCENDING)
                        .offset(offset)
                        .limit(limit)
                        .get(),
                "find items by userId with pagination: " + userId
        );

        return toItemList(querySnapshot);
    }

    @Override
    public long countByUserId(String collectionName, String userId) {
        log.trace("Counting items by user id: {} in collection '{}'", userId, collectionName);

        AggregateQuerySnapshot snapshot = execute(
                getCollection(collectionName)
                        .whereEqualTo("userId", userId)
                        .count()
                        .get(),
                "count items by userId: " + userId
        );

        return snapshot.getCount();
    }

    @Override
    public List<ItemEntity> findByHSBColorRangeAndUserId(
            String collectionName,
            float hueMin, float hueMax,
            float saturationMin, float saturationMax,
            float brightnessMin, float brightnessMax,
            String userId,
            int limit) {

        List<String> buckets = HSBColorService.generateBuckets(
                hueMin, hueMax,
                saturationMin, saturationMax,
                brightnessMin, brightnessMax
        );

        if (buckets.isEmpty()) {
            return List.of();
        }

        List<ItemEntity> result = new ArrayList<>();
        for (List<String> chunk : Lists.partition(buckets, 10)) {
            QuerySnapshot snapshot = execute(
                    getCollection(collectionName)
                            .whereEqualTo("userId", userId)
                            .whereIn("image.hsbBucket", chunk)
                            .get(),
                    "find items by HSB bucket chunk for userId: " + userId
            );
            result.addAll(toItemList(snapshot));
        }

        return result.stream()
                .filter(item -> item.getImage() != null && item.getImage().getHsbColor() != null)
                .filter(item -> isWithinSaturationAndBrightnessRange(
                        item, saturationMin, saturationMax, brightnessMin, brightnessMax))
                .limit(limit)
                .toList();
    }

    @Override
    public void deleteById(String collectionName, String id) {
        log.info("Deleting item with id: {} from collection '{}'", id, collectionName);

        execute(
                getCollection(collectionName).document(id).delete(),
                "delete item with id: " + id
        );

        log.info("Successfully deleted item with id: {} from collection '{}'", id, collectionName);
    }

    @Override
    public List<ItemEntity> findBySearchTokenAndUserId(String collectionName, String token, String userId,
                                                       int limit, int offset) {
        log.trace("Finding items by search token: {} and user id: {} in collection '{}' with limit: {}, offset: {}",
                token, userId, collectionName, limit, offset);

        QuerySnapshot querySnapshot = execute(
                getCollection(collectionName)
                        .whereArrayContains("searchTokens", token)
                        .whereEqualTo("userId", userId)
                        .orderBy("createdAt", Query.Direction.DESCENDING)
                        .offset(offset)
                        .limit(limit)
                        .get(),
                "find items by search token: " + token + " and userId: " + userId
        );

        return toItemList(querySnapshot);
    }

    @Override
    public long countBySearchTokenAndUserId(String collectionName, String token, String userId) {
        log.trace("Counting items by search token: {} and user id: {} in collection '{}'",
                token, userId, collectionName);

        AggregateQuerySnapshot snapshot = execute(
                getCollection(collectionName)
                        .whereArrayContains("searchTokens", token)
                        .whereEqualTo("userId", userId)
                        .count()
                        .get(),
                "count items by search token: " + token + " and userId: " + userId
        );

        return snapshot.getCount();
    }

    @Override
    public long countAllByUserId(String userId) {
        log.trace("Counting all items for user: {}", userId);

        try {
            AggregateQuerySnapshot snapshot = execute(
                    firestore.collectionGroup(ITEMS_SUBCOLLECTION)
                            .whereEqualTo("userId", userId)
                            .count()
                            .get(),
                    "count all items for userId: " + userId
            );

            return snapshot.getCount();

        } catch (Exception e) {
            log.info(
                    "No '{}' subcollections exist yet. Returning count = 0 for userId={}",
                    ITEMS_SUBCOLLECTION,
                    userId
            );
            return 0;
        }
    }



    private CollectionReference getCollection(String collectionId) {
        return firestore.collection(COLLECTIONS_ROOT)
                .document(collectionId)
                .collection(ITEMS_SUBCOLLECTION);
    }

    private boolean isWithinSaturationAndBrightnessRange(ItemEntity item,
                                                         float saturationMin, float saturationMax,
                                                         float brightnessMin, float brightnessMax) {
        Float saturation = item.getImage().getHsbColor().getSaturation();
        Float brightness = item.getImage().getHsbColor().getBrightness();

        return saturation != null && brightness != null
                && saturation >= saturationMin && saturation <= saturationMax
                && brightness >= brightnessMin && brightness <= brightnessMax;
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
