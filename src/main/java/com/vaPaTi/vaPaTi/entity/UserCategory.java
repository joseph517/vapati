package com.vaPaTi.vaPaTi.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity
@Table(name = "user_category")
@Data
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
}
