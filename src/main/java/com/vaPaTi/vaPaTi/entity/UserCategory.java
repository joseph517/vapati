package com.vaPaTi.vaPaTi.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity
@Table(name = "user_category")
@EqualsAndHashCode(exclude = "user")
@ToString(exclude = "user")
public class UserCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

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


    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }
}
