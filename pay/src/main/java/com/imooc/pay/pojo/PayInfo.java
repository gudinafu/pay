package com.imooc.pay.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PayInfo implements Serializable{

    private Integer id;
    //用户ID
    private Integer userId;
    //订单号
    private Long orderNo;
    //(1-支付宝,2-微信)
    private Integer payPlatform;
    //第三方支付的流水号
    private String platformNumber;
    //支付状态
    private String platformStatus;
    //支付金额
    private BigDecimal payAmount;

    private Date createTime;

    private Date updateTime;

    public PayInfo(Long orderNo, Integer payPlatform, String platformStatus, BigDecimal payAmount) {
        this.orderNo = orderNo;
        this.payPlatform = payPlatform;
        this.platformStatus = platformStatus;
        this.payAmount = payAmount;
    }
}
