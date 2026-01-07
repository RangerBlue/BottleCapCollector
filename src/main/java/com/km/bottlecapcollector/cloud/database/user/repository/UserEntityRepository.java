package com.km.bottlecapcollector.cloud.database.user.repository;

import com.km.bottlecapcollector.cloud.database.user.entity.UserEntity;

import java.util.Optional;

/**
 * Repository interface for Firestore user operations.
 */
public interface UserEntityRepository {

    /**
     * Saves a user document to Firestore.
     *
     * @param user the user to save
     * @return the saved user
     */
    UserEntity save(UserEntity user);

    /**
     * Finds a user by their ID (Google ID).
     *
     * @param id the document ID (Google ID)
     * @return an Optional containing the user if found
     */
    Optional<UserEntity> findById(String id);


    /**
     * Checks if a user exists with the given ID.
     *
     * @param id the document ID
     * @return true if the user exists, false otherwise
     */
    boolean existsById(String id);

}
