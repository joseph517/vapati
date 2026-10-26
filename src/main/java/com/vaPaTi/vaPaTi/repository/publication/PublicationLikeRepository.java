package com.vaPaTi.vaPaTi.repository.publication;

import com.vaPaTi.vaPaTi.entity.publication.PublicationLike;
import com.vaPaTi.vaPaTi.repository.user.UserRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PublicationLikeRepository extends JpaRepository<PublicationLike, Long> {

    boolean existsByPublication_IdAndUser_Id(Long publicationId, Long userId);

    // Bulk delete: a derived delete would load the row first. Idempotent: returns 0 if there was no like
    @Modifying
    @Query("DELETE FROM PublicationLike pl WHERE pl.publication.id = :publicationId AND pl.user.id = :userId")
    int deleteByPublicationIdAndUserId(@Param("publicationId") Long publicationId, @Param("userId") Long userId);

    // Likes of one publication, without deleted users nor banned or currently suspended ones, for every caller
    @Query("SELECT COUNT(pl) FROM PublicationLike pl JOIN pl.user u " +
            "WHERE pl.publication.id = :publicationId AND u.deletedAt IS NULL AND " + UserRepository.NOT_BLOCKED)
    long countVisibleByPublicationId(@Param("publicationId") Long publicationId, @Param("now") LocalDateTime now);

    // Same rule, for every publication of a campaign in one query: the listing doesn't grow with its size.
    // Publications without visible likes don't come back (count 0)
    @Query("SELECT p.id AS publicationId, COUNT(pl) AS likesCount FROM PublicationLike pl " +
            "JOIN pl.publication p JOIN pl.user u " +
            "WHERE p.campaignId = :campaignId AND u.deletedAt IS NULL AND " + UserRepository.NOT_BLOCKED +
            "GROUP BY p.id")
    List<PublicationLikeCountProjection> countVisibleByCampaignIdGroupedByPublication(
            @Param("campaignId") Long campaignId, @Param("now") LocalDateTime now);

    // Publications of a campaign the caller liked. The caller is never blocked (the JWT filter stops them first)
    @Query("SELECT p.id FROM PublicationLike pl JOIN pl.publication p " +
            "WHERE p.campaignId = :campaignId AND pl.user.id = :userId")
    List<Long> findLikedPublicationIdsByCampaignIdAndUserId(@Param("campaignId") Long campaignId,
                                                            @Param("userId") Long userId);

    interface PublicationLikeCountProjection {
        Long getPublicationId();
        Long getLikesCount();
    }
}
