package cn.tedu.charging.order.service;

import cn.tedu.charging.common.pojo.message.CheckResultMessage;
import cn.tedu.charging.common.pojo.message.DelayCheckMessage;
import cn.tedu.charging.common.pojo.message.ProgressMessage;

public interface ConsumerService {
    //设备无响应延迟消费
    void handleCheckNoRes(DelayCheckMessage msg);
    //设备自检反馈的消费
    void handlerCheckResult(CheckResultMessage msg);
    //设备同步充电进度,处理进度的业务方法
    void handleChargingProgress(ProgressMessage msg);
}
