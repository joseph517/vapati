package com.vaPaTi.vaPaTi.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.Where;

import java.time.LocalDateTime;

@Entity
@Table(name = "verification_request")
@SQLDelete(sql = "UPDATE verification_request SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
@EqualsAndHashCode(exclude = "user")
@ToString(exclude = "user")
public class VerificationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
    @JsonManagedReference
    private User user;

    @Column(name = "dni_front", nullable = false)
    private String dniFront;
    @Column(name = "dni_back", nullable = false)
    private String dniBack;

    @Column(name = "selfie_user", nullable = false)
    private String selfieUser;

    @Column(name = "status")
    private String status;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // Getters y setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getDniFront() {
        return dniFront;
    }

    public void setDniFront(String dniFront) {
        this.dniFront = dniFront;
    }

    public String getDniBack() {
        return dniBack;
    }

    public void setDniBack(String dniBack) {
        this.dniBack = dniBack;
    }

    public String getSelfieUser() {
        return selfieUser;
    }

    public void setSelfieUser(String selfieUser) {
        this.selfieUser = selfieUser;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

}
