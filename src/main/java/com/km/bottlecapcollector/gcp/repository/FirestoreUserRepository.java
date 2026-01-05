package com.km.bottlecapcollector.gcp.repository;

import com.km.bottlecapcollector.gcp.document.FirestoreUser;

import java.util.Optional;

/**
 * Repository interface for Firestore user operations.
 */
public interface FirestoreUserRepository {

    /**
     * Saves a user document to Firestore.
     *
     * @param user the user to save
     * @return the saved user
     */
    FirestoreUser save(FirestoreUser user);

    /**
     * Finds a user by their ID (Google ID).
     *
     * @param id the document ID (Google ID)
     * @return an Optional containing the user if found
     */
    Optional<FirestoreUser> findById(String id);

    /**
     * Finds a user by their email address.
     *
     * @param email the email address
     * @return an Optional containing the user if found
     */
    Optional<FirestoreUser> findByEmail(String email);

    /**
     * Checks if a user exists with the given ID.
     *
     * @param id the document ID
     * @return true if the user exists, false otherwise
     */
    boolean existsById(String id);

    /**
     * Deletes a user by their ID.
     *
     * @param id the document ID
     */
    void deleteById(String id);
}
