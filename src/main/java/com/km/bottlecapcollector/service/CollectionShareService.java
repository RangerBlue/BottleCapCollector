package com.km.bottlecapcollector.service;

import com.km.bottlecapcollector.api.handler.exception.AppForbiddenException;
import com.km.bottlecapcollector.api.handler.exception.AppResourceNotFoundException;
import com.km.bottlecapcollector.api.model.response.CollectionShareResponse;
import com.km.bottlecapcollector.api.model.response.ShareCollectionResponse;
import com.km.bottlecapcollector.api.model.response.SharedCollectionResponse;
import com.km.bottlecapcollector.cloud.database.user.entity.CollectionShareEntity;
import com.km.bottlecapcollector.cloud.database.user.entity.SharedCollectionEntity;
import com.km.bottlecapcollector.cloud.database.user.entity.UserCollectionEntity;
import com.km.bottlecapcollector.cloud.database.user.entity.UserEntity;
import com.km.bottlecapcollector.cloud.database.user.repository.UserEntityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class CollectionShareService {

    private final UserEntityRepository userRepository;

    /**
     * Shares a collection with another user by email.
     * Updates both the owner's sharesGranted list and the target user's sharedWithMe list.
     *
     * @param ownerUserId the owner's user ID
     * @param collectionKey the collection to share
     * @param targetEmail the email of the user to share with
     * @return response indicating success or failure
     */
    public ShareCollectionResponse shareCollection(String ownerUserId, String collectionKey, String targetEmail) {
        log.info("User {} sharing collection {} with email {}", ownerUserId, collectionKey, targetEmail);

        UserEntity owner = userRepository.findById(ownerUserId)
                .orElseThrow(() -> new AppResourceNotFoundException("User not found"));

        validateOwnership(owner, collectionKey);

        Optional<UserEntity> targetUserOpt = userRepository.findByEmail(targetEmail);
        if (targetUserOpt.isEmpty()) {
            log.info("Target user with email {} not found, share not created", targetEmail);
            return ShareCollectionResponse.builder()
                    .shared(false)
                    .message("User not found")
                    .build();
        }

        UserEntity targetUser = targetUserOpt.get();

        if (targetUser.getId().equals(ownerUserId)) {
            log.info("Cannot share collection with yourself");
            return ShareCollectionResponse.builder()
                    .shared(false)
                    .message("Cannot share collection with yourself")
                    .build();
        }

        if (isAlreadySharedWith(owner, collectionKey, targetUser.getId())) {
            log.info("Collection {} already shared with user {}", collectionKey, targetUser.getId());
            return ShareCollectionResponse.builder()
                    .shared(false)
                    .message("Collection already shared with this user")
                    .build();
        }

        Instant now = Instant.now();
        String collectionName = owner.getCollectionName(collectionKey);

        CollectionShareEntity share = CollectionShareEntity.builder()
                .collectionKey(collectionKey)
                .sharedWithUserId(targetUser.getId())
                .sharedWithEmail(targetEmail)
                .sharedAt(now)
                .build();

        if (owner.getSharesGranted() == null) {
            owner.setSharesGranted(new ArrayList<>());
        }
        owner.getSharesGranted().add(share);
        owner.setUpdatedAt(now);
        userRepository.save(owner);

        SharedCollectionEntity sharedCollection = SharedCollectionEntity.builder()
                .collectionKey(collectionKey)
                .collectionName(collectionName)
                .ownerUserId(ownerUserId)
                .ownerName(owner.getName())
                .sharedAt(now)
                .build();

        if (targetUser.getSharedWithMe() == null) {
            targetUser.setSharedWithMe(new ArrayList<>());
        }
        targetUser.getSharedWithMe().add(sharedCollection);
        targetUser.setUpdatedAt(now);
        userRepository.save(targetUser);

        log.info("Successfully shared collection {} with user {}", collectionKey, targetUser.getId());
        return ShareCollectionResponse.builder()
                .shared(true)
                .message("Collection shared successfully")
                .build();
    }

    /**
     * Revokes a share for a collection.
     * Updates both the owner's sharesGranted list and the target user's sharedWithMe list.
     *
     * @param ownerUserId the owner's user ID
     * @param collectionKey the collection
     * @param targetUserId the user to revoke share from
     */
    public void revokeShare(String ownerUserId, String collectionKey, String targetUserId) {
        log.info("User {} revoking share of collection {} from user {}", ownerUserId, collectionKey, targetUserId);

        UserEntity owner = userRepository.findById(ownerUserId)
                .orElseThrow(() -> new AppResourceNotFoundException("User not found"));

        validateOwnership(owner, collectionKey);

        if (owner.getSharesGranted() != null) {
            boolean removed = owner.getSharesGranted().removeIf(
                    share -> share.getCollectionKey().equals(collectionKey)
                            && share.getSharedWithUserId().equals(targetUserId)
            );
            if (removed) {
                owner.setUpdatedAt(Instant.now());
                userRepository.save(owner);
                log.info("Removed share from owner's sharesGranted list");
            }
        }

        userRepository.findById(targetUserId).ifPresent(targetUser -> {
            if (targetUser.getSharedWithMe() != null) {
                boolean removed = targetUser.getSharedWithMe().removeIf(
                        shared -> shared.getCollectionKey().equals(collectionKey)
                                && shared.getOwnerUserId().equals(ownerUserId)
                );
                if (removed) {
                    targetUser.setUpdatedAt(Instant.now());
                    userRepository.save(targetUser);
                    log.info("Removed share from target user's sharedWithMe list");
                }
            }
        });

        log.info("Successfully revoked share of collection {} from user {}", collectionKey, targetUserId);
    }

    /**
     * Gets all shares for a collection (who it's shared with).
     *
     * @param ownerUserId the owner's user ID
     * @param collectionKey the collection
     * @return list of shares
     */
    public List<CollectionShareResponse> getSharesForCollection(String ownerUserId, String collectionKey) {
        log.trace("Getting shares for collection {} owned by {}", collectionKey, ownerUserId);

        UserEntity owner = userRepository.findById(ownerUserId)
                .orElseThrow(() -> new AppResourceNotFoundException("User not found"));

        validateOwnership(owner, collectionKey);

        if (owner.getSharesGranted() == null) {
            return new ArrayList<>();
        }

        return owner.getSharesGranted().stream()
                .filter(share -> share.getCollectionKey().equals(collectionKey))
                .map(share -> CollectionShareResponse.builder()
                        .userId(share.getSharedWithUserId())
                        .email(share.getSharedWithEmail())
                        .sharedAt(share.getSharedAt())
                        .build())
                .toList();
    }

    /**
     * Gets all collections shared with a user.
     *
     * @param userId the user ID
     * @return list of shared collections
     */
    public List<SharedCollectionResponse> getCollectionsSharedWithUser(String userId) {
        log.trace("Getting collections shared with user {}", userId);

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new AppResourceNotFoundException("User not found"));

        if (user.getSharedWithMe() == null) {
            return new ArrayList<>();
        }

        return user.getSharedWithMe().stream()
                .map(shared -> SharedCollectionResponse.builder()
                        .collectionKey(shared.getCollectionKey())
                        .collectionName(shared.getCollectionName())
                        .ownerUserId(shared.getOwnerUserId())
                        .ownerName(shared.getOwnerName())
                        .sharedAt(shared.getSharedAt())
                        .build())
                .toList();
    }

    /**
     * Resolves the owner userId for a collection that a user wants to access.
     * Returns the userId to use for querying items:
     * - If user owns the collection, returns the user's own ID
     * - If collection is shared with user, returns the owner's ID
     * - If no access, throws AppForbiddenException
     *
     * @param userId the user requesting access
     * @param collectionKey the collection to access
     * @return the owner's userId to use for queries
     * @throws AppForbiddenException if user has no access
     */
    public String resolveCollectionOwner(String userId, String collectionKey) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new AppResourceNotFoundException("User not found"));

        // Check if user owns this collection
        if (user.getCollections() != null) {
            boolean owns = user.getCollections().stream()
                    .anyMatch(c -> c.getCollectionKey().equals(collectionKey));
            if (owns) {
                return userId;
            }
        }

        // Check if collection is shared with user
        if (user.getSharedWithMe() != null) {
            Optional<SharedCollectionEntity> shared = user.getSharedWithMe().stream()
                    .filter(s -> s.getCollectionKey().equals(collectionKey))
                    .findFirst();
            if (shared.isPresent()) {
                return shared.get().getOwnerUserId();
            }
        }

        throw new AppForbiddenException("You do not have access to this collection");
    }

    /**
     * Checks if a user has read access to a collection.
     * A user has read access if they are the owner or the collection is shared with them.
     *
     * @param userId the user ID
     * @param collectionKey the collection
     * @param ownerUserId the owner's user ID
     * @return true if the user has read access
     */
    public boolean hasReadAccess(String userId, String collectionKey, String ownerUserId) {
        if (userId.equals(ownerUserId)) {
            return true;
        }

        UserEntity user = userRepository.findById(userId).orElse(null);
        if (user == null || user.getSharedWithMe() == null) {
            return false;
        }

        return user.getSharedWithMe().stream()
                .anyMatch(shared -> shared.getCollectionKey().equals(collectionKey)
                        && shared.getOwnerUserId().equals(ownerUserId));
    }


    private void validateOwnership(UserEntity user, String collectionKey) {
        if (user.getCollections() == null) {
            throw new AppForbiddenException("You do not own this collection");
        }

        boolean owns = user.getCollections().stream()
                .anyMatch(c -> c.getCollectionKey().equals(collectionKey));

        if (!owns) {
            throw new AppForbiddenException("You do not own this collection");
        }
    }

    private boolean isAlreadySharedWith(UserEntity owner, String collectionKey, String targetUserId) {
        if (owner.getSharesGranted() == null) {
            return false;
        }
        return owner.getSharesGranted().stream()
                .anyMatch(share -> share.getCollectionKey().equals(collectionKey)
                        && share.getSharedWithUserId().equals(targetUserId));
    }
}
