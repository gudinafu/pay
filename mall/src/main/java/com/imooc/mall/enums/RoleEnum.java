package com.imooc.mall.enums;

import lombok.Getter;

@Getter
public enum RoleEnum {
    ADMIN(0),  //管理员

    CUSTOMER(1), //普通用户

    ;

    Integer code;

    RoleEnum(Integer code) {
        this.code = code;
    }
}
