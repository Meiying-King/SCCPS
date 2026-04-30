package cn.tedu.charging.order.controller;

import cn.tedu.charging.common.pojo.param.OrderAddParam;
import cn.tedu.charging.common.protocol.JsonResult;
import cn.tedu.charging.order.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OrderController {
    @Autowired
    private OrderService orderService;
    //扫码下单
    @PostMapping("/order/create")
    public JsonResult<String> createOrder(@RequestBody OrderAddParam param){
        //调用业务层,返回一个订单编号 orderNo billId 是相同的意义
        String billId=orderService.createOrder(param);
        return JsonResult.ok(billId);
    }
}
