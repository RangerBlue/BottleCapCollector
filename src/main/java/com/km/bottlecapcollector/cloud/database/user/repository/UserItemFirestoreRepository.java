package com.km.bottlecapcollector.cloud.database.user.repository;

import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.km.bottlecapcollector.cloud.database.user.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import static com.km.bottlecapcollector.cloud.database.FirestoreExecutor.execute;

@Repository
@RequiredArgsConstructor
@Slf4j
public class UserItemFirestoreRepository implements UserEntityRepository {

    private static final String COLLECTION_NAME = "users";

    private final Firestore firestore;

    @Override
    public UserEntity save(UserEntity user) {
        log.info("Saving user: {}", user.getEmail());

        DocumentReference docRef;
        if (user.getId() == null || user.getId().isEmpty()) {
            docRef = getCollection().document();
            user.setId(docRef.getId());
        } else {
            docRef = getCollection().document(user.getId());
        }

        execute(docRef.set(user), "save user");
        log.info("Successfully saved user with id: {}", user.getId());
        return user;
    }

    @Override
    public Optional<UserEntity> findById(String id) {
        log.trace("Finding user by id: {}", id);

        DocumentSnapshot document = execute(
                getCollection().document(id).get(),
                "find user by id: " + id
        );

        return document.exists()
                ? Optional.ofNullable(document.toObject(UserEntity.class))
                : Optional.empty();
    }

    @Override
    public boolean existsById(String id) {
        log.trace("Checking if user exists with id: {}", id);

        DocumentSnapshot document = execute(
                getCollection().document(id).get(),
                "check user existence for id: " + id
        );

        return document.exists();
    }

    private CollectionReference getCollection() {
        return firestore.collection(COLLECTION_NAME);
    }
}
