package com.imooc.mall.form;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class UserLoginForm {
    @NotBlank //用于字符
    private String username;

    @NotBlank
    private String password;
}
