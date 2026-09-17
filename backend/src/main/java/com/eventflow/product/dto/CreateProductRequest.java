package com.eventflow.product.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = false)
public class CreateProductRequest {

    private String name;
    private String description;
    private BigDecimal price;
    private Boolean active;

    @JsonIgnore
    private boolean activeProvided;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Boolean getActive() {
        return active;
    }

    @JsonSetter("active")
    public void setActive(Boolean active) {
        this.active = active;
        this.activeProvided = true;
    }

    public boolean isActiveProvided() {
        return activeProvided;
    }
}
