package com.vaPaTi.vaPaTi.dtos;

import java.util.ArrayList;
import java.util.List;

public class CreateUserDTO {

    private Boolean isActive;

    private List<Long> categoryIds = new ArrayList<>();

    // Getters y setters
    public Boolean getActive() {
        return isActive;
    }

    public void setActive(Boolean active) {
        isActive = active;
    }

    public List<Long> getCategoryIds() {
        return categoryIds;
    }

    public void setCategoryIds(List<Long> categoryIds) {
        this.categoryIds = categoryIds;
    }
}
