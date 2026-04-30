package cn.tedu.charging.order.timer;

import cn.tedu.charging.order.service.OrderService;
import com.xxl.job.core.context.XxlJobContext;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class OrderCheckTimer {
    @Autowired
    private OrderService orderService;
    /**
     * 检查某张订单在最大充电时间之后是否正常结束
     */
    @XxlJob("order-status-check")
    public void orderStatusCheck(){
        //1.解析参数 拿到每个任务中传递的orderNo
        String billId=XxlJobContext.getXxlJobContext().getJobParam();
        //2.执行业务
        log.debug("定时任务开始执行,订单编号:{}",billId);
        orderService.orderStatusCheck(billId);
    }
}
