package com.km.bottlecapcollector.cloud.database;

import com.google.api.core.ApiFuture;
import com.km.bottlecapcollector.cloud.database.exception.FirestoreException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.concurrent.ExecutionException;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FirestoreExecutor {

    public static <T> T execute(ApiFuture<T> future, String operationDescription) {
        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FirestoreException("Interrupted while trying to " + operationDescription, e);
        } catch (ExecutionException e) {
            throw new FirestoreException("Failed to " + operationDescription, e.getCause());
        }
    }
}
