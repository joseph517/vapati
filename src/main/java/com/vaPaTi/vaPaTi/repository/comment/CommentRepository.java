package com.vaPaTi.vaPaTi.repository.comment;

import com.vaPaTi.vaPaTi.entity.comment.Comment;
import com.vaPaTi.vaPaTi.repository.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    // ADMIN: every comment of the publication whose author is not deleted. The author comes in the same SELECT,
    // so a page doesn't grow with its size. Order is fixed here: the Pageable comes unsorted
    @Query(value = "SELECT c FROM Comment c JOIN FETCH c.user u LEFT JOIN FETCH u.userInfo " +
            "LEFT JOIN FETCH u.verificationRequest " +
            "WHERE c.publicationId = :publicationId AND u.deletedAt IS NULL " +
            "ORDER BY c.createdAt ASC, c.id ASC",
            countQuery = "SELECT COUNT(c) FROM Comment c JOIN c.user u " +
                    "WHERE c.publicationId = :publicationId AND u.deletedAt IS NULL")
    Page<Comment> findAllByPublicationId(@Param("publicationId") Long publicationId, Pageable pageable);

    // Non-admins: same, without banned or currently suspended authors
    @Query(value = "SELECT c FROM Comment c JOIN FETCH c.user u LEFT JOIN FETCH u.userInfo " +
            "LEFT JOIN FETCH u.verificationRequest " +
            "WHERE c.publicationId = :publicationId AND u.deletedAt IS NULL AND " + UserRepository.NOT_BLOCKED +
            "ORDER BY c.createdAt ASC, c.id ASC",
            countQuery = "SELECT COUNT(c) FROM Comment c JOIN c.user u " +
                    "WHERE c.publicationId = :publicationId AND u.deletedAt IS NULL AND " + UserRepository.NOT_BLOCKED)
    Page<Comment> findVisibleByPublicationId(@Param("publicationId") Long publicationId,
                                             @Param("now") LocalDateTime now, Pageable pageable);
}
