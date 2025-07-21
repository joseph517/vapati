package com.vaPaTi.vaPaTi.repository;

import com.vaPaTi.vaPaTi.entity.Publication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PublicationRepository extends JpaRepository<Publication, Long> {

    List<Publication> findAllByUser_Id(Long userId);

}
