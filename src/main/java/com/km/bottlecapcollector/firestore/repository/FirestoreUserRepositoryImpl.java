package com.km.bottlecapcollector.firestore.repository;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.km.bottlecapcollector.firestore.document.FirestoreUser;
import com.km.bottlecapcollector.firestore.exception.FirestoreException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

/**
 * Implementation of FirestoreUserRepository using Google Cloud Firestore.
 */
@Repository
@Slf4j
public class FirestoreUserRepositoryImpl implements FirestoreUserRepository {

    private static final String COLLECTION_NAME = "users";

    private final Firestore firestore;

    public FirestoreUserRepositoryImpl(Firestore firestore) {
        this.firestore = firestore;
    }

    private CollectionReference getCollection() {
        return firestore.collection(COLLECTION_NAME);
    }

    @Override
    public FirestoreUser save(FirestoreUser user) {
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
    public Optional<FirestoreUser> findById(String id) {
        log.trace("Finding user by id: {}", id);
        try {
            DocumentSnapshot document = getCollection().document(id).get().get();
            if (document.exists()) {
                return Optional.ofNullable(document.toObject(FirestoreUser.class));
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
    public Optional<FirestoreUser> findByEmail(String email) {
        log.trace("Finding user by email: {}", email);
        try {
            QuerySnapshot querySnapshot = getCollection()
                    .whereEqualTo("email", email)
                    .limit(1)
                    .get()
                    .get();
            List<QueryDocumentSnapshot> documents = querySnapshot.getDocuments();
            if (!documents.isEmpty()) {
                return Optional.ofNullable(documents.get(0).toObject(FirestoreUser.class));
            }
            return Optional.empty();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FirestoreException("Interrupted while finding user by email", e);
        } catch (ExecutionException e) {
            throw new FirestoreException("Failed to find user by email: " + email, e.getCause());
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

    @Override
    public void deleteById(String id) {
        log.info("Deleting user with id: {}", id);
        try {
            getCollection().document(id).delete().get();
            log.info("Successfully deleted user with id: {}", id);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FirestoreException("Interrupted while deleting user", e);
        } catch (ExecutionException e) {
            throw new FirestoreException("Failed to delete user with id: " + id, e.getCause());
        }
    }
}
