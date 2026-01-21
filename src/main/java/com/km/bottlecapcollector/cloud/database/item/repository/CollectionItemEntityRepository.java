package com.km.bottlecapcollector.cloud.database.item.repository;

import com.google.cloud.firestore.Query;
import com.km.bottlecapcollector.cloud.database.item.entity.ItemEntity;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Firestore collection item operations.
 * All methods accept a collectionName parameter to support multiple collections.
 */
public interface CollectionItemEntityRepository {

    /**
     * Saves a collection item document to Firestore.
     *
     * @param collectionName the Firestore collection name
     * @param item the collection item to save
     * @return the saved item with generated ID if new
     */
    ItemEntity save(String collectionName, ItemEntity item);

    /**
     * Finds a collection item by its ID.
     *
     * @param collectionName the Firestore collection name
     * @param id the document ID
     * @return an Optional containing the item if found
     */
    Optional<ItemEntity> findById(String collectionName, String id);

    /**
     * Finds a collection item by its ID and user ID.
     * This is the preferred method for fetching items as it ensures
     * the query is filtered by userId at the database level for security.
     *
     * @param collectionName the Firestore collection name
     * @param id the document ID
     * @param userId the user ID for ownership verification
     * @return an Optional containing the item if found and owned by the user
     */
    Optional<ItemEntity> findByIdAndUserId(String collectionName, String id, String userId);

    /**
     * Finds all items in a collection.
     *
     * @param collectionName the Firestore collection name
     * @return a list of all items
     */
    List<ItemEntity> findAll(String collectionName);

    /**
     * Finds all items in a collection with pagination.
     *
     * @param collectionName the Firestore collection name
     * @param limit  the maximum number of documents to return
     * @param offset the number of documents to skip
     * @return a paginated list of items
     */
    List<ItemEntity> findAll(String collectionName, int limit, int offset);

    /**
     * Finds items by user ID.
     *
     * @param collectionName the Firestore collection name
     * @param userId the user ID
     * @return a list of items belonging to the user
     */
    List<ItemEntity> findByUserId(String collectionName, String userId);

    /**
     * Finds items by user ID with pagination and sorting.
     *
     * @param collectionName the Firestore collection name
     * @param userId the user ID
     * @param limit maximum number of results
     * @param offset number of results to skip
     * @param sortDirection the sort direction for createdAt field
     * @return a paginated list of items belonging to the user
     */
    List<ItemEntity> findByUserId(String collectionName, String userId, int limit, int offset, Query.Direction sortDirection);

    /**
     * Counts items by user ID.
     *
     * @param collectionName the Firestore collection name
     * @param userId the user ID
     * @return the count of items belonging to the user
     */
    long countByUserId(String collectionName, String userId);

    /**
     * Finds items with similar HSB colors within the specified ranges for a specific user.
     * Used for pre-filtering before applying embedding-based similarity search.
     *
     * @param collectionName the Firestore collection name
     * @param hueMin        minimum hue value (0.0 - 1.0)
     * @param hueMax        maximum hue value (0.0 - 1.0)
     * @param saturationMin minimum saturation value (0.0 - 1.0)
     * @param saturationMax maximum saturation value (0.0 - 1.0)
     * @param brightnessMin minimum brightness value (0.0 - 1.0)
     * @param brightnessMax maximum brightness value (0.0 - 1.0)
     * @param userId        the user ID to filter by
     * @param limit         maximum number of results to return
     * @return a list of items matching the color range belonging to the user
     */
    List<ItemEntity> findByHSBColorRangeAndUserId(
            String collectionName,
            float hueMin, float hueMax,
            float saturationMin, float saturationMax,
            float brightnessMin, float brightnessMax,
            String userId,
            int limit);

    /**
     * Deletes an item by its ID.
     *
     * @param collectionName the Firestore collection name
     * @param id the document ID
     */
    void deleteById(String collectionName, String id);


    /**
     * Finds items by search token and user ID with pagination and sorting.
     *
     * @param collectionName the Firestore collection name
     * @param token the search token (lowercase prefix)
     * @param userId the user ID
     * @param limit maximum number of results
     * @param offset number of results to skip
     * @param sortDirection the sort direction for createdAt field
     * @return a paginated list of items containing the token and belonging to the user
     */
    List<ItemEntity> findBySearchTokenAndUserId(String collectionName, String token, String userId, int limit, int offset, Query.Direction sortDirection);

    /**
     * Counts items by search token and user ID.
     *
     * @param collectionName the Firestore collection name
     * @param token the search token (lowercase prefix)
     * @param userId the user ID
     * @return the count of matching items belonging to the user
     */
    long countBySearchTokenAndUserId(String collectionName, String token, String userId);

    /**
     * Counts all items for a user across all collections using collection group query.
     *
     * @param userId the user ID
     * @return the total count of items belonging to the user across all collections
     */
    long countAllByUserId(String userId);
}
