package com.vaPaTi.vaPaTi.repository.publication;

import com.vaPaTi.vaPaTi.entity.publication.Publication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PublicationRepository extends JpaRepository<Publication, Long> {

    // Author and campaign (whose owner is the same user) come in the same SELECT, so the listing doesn't grow with its size
    @Query("SELECT p FROM Publication p JOIN FETCH p.user u LEFT JOIN FETCH u.userInfo LEFT JOIN FETCH u.verificationRequest " +
            "JOIN FETCH p.campaign c LEFT JOIN FETCH c.goal " +
            "WHERE p.campaignId = :campaignId AND u.deletedAt IS NULL ORDER BY p.createdAt DESC")
    List<Publication> findAllByCampaignId(@Param("campaignId") Long campaignId);

}
