package com.imooc.mall.form;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 添加商品
 */
@Data
public class CartAddForm {
    @NotNull
    private Integer productId;
    //是否选中
    private Boolean selected = true;
}
