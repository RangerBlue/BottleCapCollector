package com.km.bottlecapcollector.cloud.database.user.repository;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.km.bottlecapcollector.cloud.database.user.entity.UserEntity;
import com.km.bottlecapcollector.cloud.database.exception.FirestoreException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.ExecutionException;

/**
 * Implementation of FirestoreUserRepository using Google Cloud Firestore.
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class UserItemFirestoreRepository implements UserEntityRepository {

    private static final String COLLECTION_NAME = "users";

    private final Firestore firestore;

    private CollectionReference getCollection() {
        return firestore.collection(COLLECTION_NAME);
    }

    @Override
    public UserEntity save(UserEntity user) {
        log.info("Saving user: {}", user.getEmail());
        try {
            DocumentReference docRef;
            if (user.getId() == null || user.getId().isEmpty()) {
                docRef = getCollection().document();
                user.setId(docRef.getId());
            } else {
                docRef = getCollection().document(user.getId());
            }

            ApiFuture<WriteResult> result = docRef.set(user);
            result.get();
            log.info("Successfully saved user with id: {}", user.getId());
            return user;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FirestoreException("Interrupted while saving user", e);
        } catch (ExecutionException e) {
            throw new FirestoreException("Failed to save user", e.getCause());
        }
    }

    @Override
    public Optional<UserEntity> findById(String id) {
        log.trace("Finding user by id: {}", id);
        try {
            DocumentSnapshot document = getCollection().document(id).get().get();
            if (document.exists()) {
                return Optional.ofNullable(document.toObject(UserEntity.class));
            }
            return Optional.empty();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FirestoreException("Interrupted while finding user by id", e);
        } catch (ExecutionException e) {
            throw new FirestoreException("Failed to find user by id: " + id, e.getCause());
        }
    }


    @Override
    public boolean existsById(String id) {
        log.trace("Checking if user exists with id: {}", id);
        try {
            DocumentSnapshot document = getCollection().document(id).get().get();
            return document.exists();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FirestoreException("Interrupted while checking user existence", e);
        } catch (ExecutionException e) {
            throw new FirestoreException("Failed to check user existence for id: " + id, e.getCause());
        }
    }

}
