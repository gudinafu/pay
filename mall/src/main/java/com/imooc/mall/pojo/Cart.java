package com.imooc.mall.pojo;

import lombok.Data;

import java.io.Serializable;

@Data
public class Cart implements Serializable{

    private Integer productId;

    private Integer quantity;

    private Boolean productSelected;

    public Cart() {
    }

    public Cart(Integer productId, Integer quantity, Boolean productSelected) {
        this.productId = productId;
        this.quantity = quantity;
        this.productSelected = productSelected;
    }
}
