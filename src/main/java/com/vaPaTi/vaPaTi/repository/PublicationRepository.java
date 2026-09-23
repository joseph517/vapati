package com.vaPaTi.vaPaTi.repository;

import com.vaPaTi.vaPaTi.entity.Publication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PublicationRepository extends JpaRepository<Publication, Long> {

    @Query("SELECT p FROM Publication p JOIN p.user u WHERE u.id = :userId AND u.deletedAt IS NULL")
    List<Publication> findAllByUser_Id(@Param("userId") Long userId);

}
