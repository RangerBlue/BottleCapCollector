package com.km.bottlecapcollector.cloud.database.repository;

import com.km.bottlecapcollector.cloud.database.document.ItemEntity;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Firestore collection item operations.
 * All methods accept a collectionName parameter to support multiple collections.
 */
public interface FirestoreBottleCapRepository {

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
     * Finds all items by collection type within a Firestore collection.
     *
     * @param collectionName the Firestore collection name
     * @param collectionType the collection type to filter by
     * @return a list of items for the specified collection type
     */
    List<ItemEntity> findByCollectionType(String collectionName, String collectionType);

    /**
     * Finds items by collection type with pagination.
     *
     * @param collectionName the Firestore collection name
     * @param collectionType the collection type to filter by
     * @param limit  the maximum number of documents to return
     * @param offset the number of documents to skip
     * @return a paginated list of items for the specified collection type
     */
    List<ItemEntity> findByCollectionType(String collectionName, String collectionType, int limit, int offset);

    /**
     * Finds items by user ID.
     *
     * @param collectionName the Firestore collection name
     * @param userId the user ID
     * @return a list of items belonging to the user
     */
    List<ItemEntity> findByUserId(String collectionName, String userId);

    /**
     * Finds items by user ID with pagination.
     *
     * @param collectionName the Firestore collection name
     * @param userId the user ID
     * @param limit maximum number of results
     * @param offset number of results to skip
     * @return a paginated list of items belonging to the user
     */
    List<ItemEntity> findByUserId(String collectionName, String userId, int limit, int offset);

    /**
     * Counts items by user ID.
     *
     * @param collectionName the Firestore collection name
     * @param userId the user ID
     * @return the count of items belonging to the user
     */
    long countByUserId(String collectionName, String userId);

    /**
     * Finds items by user ID and collection type.
     *
     * @param collectionName the Firestore collection name
     * @param userId the user ID
     * @param collectionType the collection type to filter by
     * @return a list of items belonging to the user for the specified collection type
     */
    List<ItemEntity> findByUserIdAndCollectionType(String collectionName, String userId, String collectionType);

    /**
     * Finds an item by collection type and ID.
     *
     * @param collectionName the Firestore collection name
     * @param collectionType the collection type to filter by
     * @param id the document ID
     * @return an Optional containing the item if found
     */
    Optional<ItemEntity> findByCollectionTypeAndId(String collectionName, String collectionType, String id);

    /**
     * Finds items by name (case-insensitive partial match).
     *
     * @param collectionName the Firestore collection name
     * @param name the name to search for
     * @return a list of matching items
     */
    List<ItemEntity> findByNameContaining(String collectionName, String name);

    /**
     * Finds items by name and collection type (case-insensitive partial match).
     *
     * @param collectionName the Firestore collection name
     * @param name the name to search for
     * @param collectionType the collection type to filter by
     * @return a list of matching items for the specified collection type
     */
    List<ItemEntity> findByNameContainingAndCollectionType(String collectionName, String name, String collectionType);

    /**
     * Finds items that have a specific tag.
     *
     * @param collectionName the Firestore collection name
     * @param tag the tag to search for
     * @return a list of items with the specified tag
     */
    List<ItemEntity> findByTag(String collectionName, String tag);

    /**
     * Finds items that have a specific tag and collection type.
     *
     * @param collectionName the Firestore collection name
     * @param tag the tag to search for
     * @param collectionType the collection type to filter by
     * @return a list of items with the specified tag and collection type
     */
    List<ItemEntity> findByTagAndCollectionType(String collectionName, String tag, String collectionType);

    /**
     * Finds items that have embeddings.
     *
     * @param collectionName the Firestore collection name
     * @return a list of items with embeddings
     */
    List<ItemEntity> findWithEmbeddings(String collectionName);

    /**
     * Finds items that have embeddings by collection type.
     *
     * @param collectionName the Firestore collection name
     * @param collectionType the collection type to filter by
     * @return a list of items with embeddings for the specified collection type
     */
    List<ItemEntity> findWithEmbeddingsByCollectionType(String collectionName, String collectionType);

    /**
     * Finds items that have vision metadata.
     *
     * @param collectionName the Firestore collection name
     * @return a list of items with vision metadata
     */
    List<ItemEntity> findWithVisionMetadata(String collectionName);

    /**
     * Finds items that have vision metadata by collection type.
     *
     * @param collectionName the Firestore collection name
     * @param collectionType the collection type to filter by
     * @return a list of items with vision metadata for the specified collection type
     */
    List<ItemEntity> findWithVisionMetadataByCollectionType(String collectionName, String collectionType);

    /**
     * Finds items with similar HSB colors within the specified ranges.
     * Used for pre-filtering before applying embedding-based similarity search.
     *
     * @param collectionName the Firestore collection name
     * @param hueMin        minimum hue value (0.0 - 1.0)
     * @param hueMax        maximum hue value (0.0 - 1.0)
     * @param saturationMin minimum saturation value (0.0 - 1.0)
     * @param saturationMax maximum saturation value (0.0 - 1.0)
     * @param brightnessMin minimum brightness value (0.0 - 1.0)
     * @param brightnessMax maximum brightness value (0.0 - 1.0)
     * @param limit         maximum number of results to return
     * @return a list of items matching the color range
     */
    List<ItemEntity> findByHSBColorRange(
            String collectionName,
            float hueMin, float hueMax,
            float saturationMin, float saturationMax,
            float brightnessMin, float brightnessMax,
            int limit);

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
     * Finds items with similar HSB colors within the specified ranges and collection type.
     *
     * @param collectionName the Firestore collection name
     * @param hueMin        minimum hue value (0.0 - 1.0)
     * @param hueMax        maximum hue value (0.0 - 1.0)
     * @param saturationMin minimum saturation value (0.0 - 1.0)
     * @param saturationMax maximum saturation value (0.0 - 1.0)
     * @param brightnessMin minimum brightness value (0.0 - 1.0)
     * @param brightnessMax maximum brightness value (0.0 - 1.0)
     * @param collectionType the collection type to filter by
     * @param limit         maximum number of results to return
     * @return a list of items matching the color range and collection type
     */
    List<ItemEntity> findByHSBColorRangeAndCollectionType(
            String collectionName,
            float hueMin, float hueMax,
            float saturationMin, float saturationMax,
            float brightnessMin, float brightnessMax,
            String collectionType,
            int limit);

    /**
     * Deletes an item by its ID.
     *
     * @param collectionName the Firestore collection name
     * @param id the document ID
     */
    void deleteById(String collectionName, String id);

    /**
     * Checks if an item exists with the given ID.
     *
     * @param collectionName the Firestore collection name
     * @param id the document ID
     * @return true if the item exists, false otherwise
     */
    boolean existsById(String collectionName, String id);

    /**
     * Counts the total number of items in a collection.
     *
     * @param collectionName the Firestore collection name
     * @return the total count
     */
    long count(String collectionName);

    /**
     * Counts the total number of items by collection type.
     *
     * @param collectionName the Firestore collection name
     * @param collectionType the collection type to filter by
     * @return the count for the specified collection type
     */
    long countByCollectionType(String collectionName, String collectionType);

    /**
     * Finds items that contain the specified search token.
     * Uses Firestore array-contains query for efficient prefix search.
     *
     * @param collectionName the Firestore collection name
     * @param token the search token (lowercase prefix)
     * @return a list of items containing the token
     */
    List<ItemEntity> findBySearchToken(String collectionName, String token);

    /**
     * Finds items that contain the specified search token with pagination.
     *
     * @param collectionName the Firestore collection name
     * @param token the search token (lowercase prefix)
     * @param limit maximum number of results
     * @param offset number of results to skip
     * @return a paginated list of items containing the token
     */
    List<ItemEntity> findBySearchToken(String collectionName, String token, int limit, int offset);

    /**
     * Counts items that contain the specified search token.
     *
     * @param collectionName the Firestore collection name
     * @param token the search token (lowercase prefix)
     * @return the count of matching items
     */
    long countBySearchToken(String collectionName, String token);

    /**
     * Finds items by search token and user ID with pagination.
     *
     * @param collectionName the Firestore collection name
     * @param token the search token (lowercase prefix)
     * @param userId the user ID
     * @param limit maximum number of results
     * @param offset number of results to skip
     * @return a paginated list of items containing the token and belonging to the user
     */
    List<ItemEntity> findBySearchTokenAndUserId(String collectionName, String token, String userId, int limit, int offset);

    /**
     * Counts items by search token and user ID.
     *
     * @param collectionName the Firestore collection name
     * @param token the search token (lowercase prefix)
     * @param userId the user ID
     * @return the count of matching items belonging to the user
     */
    long countBySearchTokenAndUserId(String collectionName, String token, String userId);
}
