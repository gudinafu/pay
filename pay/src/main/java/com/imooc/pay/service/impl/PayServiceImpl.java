package com.imooc.pay.service.impl;

import com.google.gson.Gson;
import com.imooc.pay.consts.PayConsts;
import com.imooc.pay.dao.PayInfoMapper;
import com.imooc.pay.enums.PayPlatformEnum;
import com.imooc.pay.pojo.PayInfo;
import com.imooc.pay.service.IPayService;
import com.lly835.bestpay.config.WxPayConfig;
import com.lly835.bestpay.enums.BestPayPlatformEnum;
import com.lly835.bestpay.enums.BestPayTypeEnum;
import com.lly835.bestpay.enums.OrderStatusEnum;
import com.lly835.bestpay.model.PayRequest;
import com.lly835.bestpay.model.PayResponse;
import com.lly835.bestpay.service.BestPayService;
import com.lly835.bestpay.service.impl.BestPayServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;

@Slf4j
@Service
public class PayServiceImpl implements IPayService {

    @Autowired
    private BestPayService bestPayService;

    @Autowired
    private AmqpTemplate amqpTemplate;

    @Resource
    private PayInfoMapper payInfoMapper;


    /**
     * 发起支付
     *
     * @param orderId 订单ID
     * @param amount  金额
     */
    @Override
    public PayResponse create(String orderId, BigDecimal amount, BestPayTypeEnum bestPayTypeEnum) {

        //写入数据库
        PayInfo payInfo = new PayInfo(Long.valueOf(orderId),
                PayPlatformEnum.getByBestPayTypeEnum(bestPayTypeEnum).getCode(), //支付类型
                OrderStatusEnum.NOTPAY.name(), //未支付状态
                amount);

         payInfoMapper.insertSelective(payInfo);


        PayRequest payRequest = new PayRequest();
        payRequest.setOrderName("8096324-最好的支付sdk");  //订单名称
        payRequest.setOrderId(orderId);    //订单ID
        payRequest.setOrderAmount(amount.doubleValue());//支付金额
        payRequest.setPayTypeEnum(bestPayTypeEnum);//支付类型

        PayResponse payResponse = bestPayService.pay(payRequest);

        //        <xml>
        //            <return_code><![CDATA[SUCCESS]]></return_code>
        //            <return_msg><![CDATA[OK]]></return_msg>
        //            <appid><![CDATA[wxd898fcb01713c658]]></appid>
        //            <mch_id><![CDATA[1483469312]]></mch_id>
        //            <nonce_str><![CDATA[yVqGfhLu2nX1SaiR]]></nonce_str>    ---随机字符串
        //            <sign><![CDATA[E13769100B44983E2863FB6E0571327F]]></sign>     ----签名
        //            <result_code><![CDATA[SUCCESS]]></result_code>
        //            <prepay_id><![CDATA[wx020028372916592d1da037b21342803100]]></prepay_id>   ---预支付ID
        //            <trade_type><![CDATA[NATIVE]]></trade_type>
        //            <code_url><![CDATA[weixin://wxpay/bizpayurl?pr=ABU62vh]]></code_url>   ---二维码文本
        //        </xml>
        log.info("response={}" + payResponse);
        return payResponse;
    }

    /**
     * 异步通知处理
     *
     * @param notifyData
     * @return
     */
    @Override
    public String asyncNotify(String notifyData) {
        //1. 签名检验
        PayResponse payResponse = bestPayService.asyncNotify(notifyData);
        log.info("异步通知 response={}", payResponse);

        //2. 金额校验（从数据库查订单）
        //比较严重（正常情况下是不会发生的）发出告警：钉钉、短信
        PayInfo payInfo = payInfoMapper.selectByOrderNo(Long.parseLong(payResponse.getOrderId()));
        if (payInfo == null) {
            //告警
            throw new RuntimeException("通过orderNo查询到的结果是null");
        }
        //如果订单支付状态不是"已支付"
        if (!payInfo.getPlatformStatus().equals(OrderStatusEnum.SUCCESS.name())) {
            //Double类型比较大小，精度。1.00  1.0
            if (payInfo.getPayAmount().compareTo(BigDecimal.valueOf(payResponse.getOrderAmount())) != 0) {
                //告警
                throw new RuntimeException("异步通知中的金额和数据库里的不一致，orderNo=" + payResponse.getOrderId());
            }

            //3. 修改订单支付状态
            payInfo.setPlatformStatus(OrderStatusEnum.SUCCESS.name());
            payInfo.setPlatformNumber(payResponse.getOutTradeNo());
            payInfoMapper.updateByPrimaryKeySelective(payInfo);
        }

        //支付成功发送MQ消息，mall接受MQ消息
        amqpTemplate.convertAndSend(PayConsts.QUEUE_PAY_NOTIFY, new Gson().toJson(payInfo));

        if (payResponse.getPayPlatformEnum() == BestPayPlatformEnum.WX) {
            //4. 告诉微信不要再通知了
            return "<xml>\n" +
                    "  <return_code><![CDATA[SUCCESS]]></return_code>\n" +
                    "  <return_msg><![CDATA[OK]]></return_msg>\n" +
                    "</xml>";
        } else if (payResponse.getPayPlatformEnum() == BestPayPlatformEnum.ALIPAY) {
            return "success";
        }

        throw new RuntimeException("异步通知中错误的支付平台");

    }

    @Override
    public PayInfo queryByOrderId(String orderId) {
        return payInfoMapper.selectByOrderNo(Long.parseLong(orderId));
    }
}
