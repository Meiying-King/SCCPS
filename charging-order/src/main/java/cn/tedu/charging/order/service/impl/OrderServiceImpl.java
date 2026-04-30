package cn.tedu.charging.order.service.impl;

import cn.tedu.charging.common.constant.MqttTopicConst;
import cn.tedu.charging.common.pojo.message.DelayCheckMessage;
import cn.tedu.charging.common.pojo.message.StartCheckMessage;
import cn.tedu.charging.common.pojo.param.OrderAddParam;
import cn.tedu.charging.common.protocol.JsonResult;
import cn.tedu.charging.common.protocol.WebSocketResult;
import cn.tedu.charging.common.utils.CronUtil;
import cn.tedu.charging.common.utils.SnowflakeIdGenerator;
import cn.tedu.charging.common.utils.XxlJobTaskUtil;
import cn.tedu.charging.order.amqp.DelayProducer;
import cn.tedu.charging.order.clients.DeviceClient;
import cn.tedu.charging.order.clients.UserClient;
import cn.tedu.charging.order.dao.repository.BillRepository;
import cn.tedu.charging.order.dao.repository.impl.BillRepositoryImpl;
import cn.tedu.charging.order.mqtt.MqttProducer;
import cn.tedu.charging.order.pojo.po.ChargingBillExceptionPO;
import cn.tedu.charging.order.pojo.po.ChargingBillSuccessPO;
import cn.tedu.charging.order.server.points.WebsocketServerPoint;
import cn.tedu.charging.order.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Date;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService {
    @Autowired
    private DeviceClient deviceClient;
    @Autowired
    private UserClient userClient;
    @Autowired
    private SnowflakeIdGenerator generator;
    @Autowired
    private MqttProducer mqttProducer;
    @Autowired
    private DelayProducer delayProducer;
    @Autowired
    private BillRepository billRepository;

    @Override
    public String createOrder(OrderAddParam param) {
        //1. 调用设备(openFeign)检查设备--轻做
        checkGunAvailable(param.getGunId());
        //2. 调用车主(openFeign)检查车主--轻做
        checkUser(param.getUserId(),param.getGunId());
        //3. 生成一个订单编号
        String billId=generateBillId();
        //4. 给设备发送命令开始充电
        //4.1 组织消息对象 orderNo userId gunId
        StartCheckMessage startCheckMessage=new StartCheckMessage();
        startCheckMessage.setOrderNo(billId);
        startCheckMessage.setUserId(param.getUserId());
        startCheckMessage.setGunId(param.getGunId());
        //4.2 发送消息 主题名称路径绑定桩id pileId
        mqttProducer.send(MqttTopicConst.START_GUN_CHECK_PREFIX+param.getPileId(),startCheckMessage);
        //5. 发送延迟消息 为设备无响应兜底检查
        DelayCheckMessage delayCheckMessage=new DelayCheckMessage();
        delayCheckMessage.setOrderNo(billId);
        delayCheckMessage.setUserId(param.getUserId());
        delayCheckMessage.setGunId(param.getGunId());
        delayCheckMessage.setPileId(param.getPileId());
        delayProducer.sendDelay(delayCheckMessage,1000*60);
        // 6. 修改枪状态 正在充电中
        deviceClient.updateGunStatus(param.getGunId(),2);
        //7. 发布定时任务,检查订单充电结果
        //7.1 根据扫码下单 订单情况 计算订单充电最大理论时长,确定定时执行时间 为了测试方便 设置3分钟
        String cronExpression= CronUtil.delayCron(1000*60*3);
        //7.2 利用cron表达式和当前扫码生成订单编号,以及一个固定的执行器order-executor创建任务
        XxlJobTaskUtil.createJobTask(cronExpression,"order-executor",billId);
        return billId;
    }
    @Autowired
    private WebsocketServerPoint websocketServerPoint;
    @Override
    public void orderStatusCheck(String billId) {
        //1.调用仓储层利用billId查询success订单
        ChargingBillSuccessPO success=billRepository.getSuccessByBillId(billId);
        //判断存在
        if (success!=null){
            //判断订单状态是否是1
            if (success.getBillStatus()==1){
                //订单按照异常处理
                log.debug("该订单已到最大充电时间,依然未结束,按照异常处理,订单编号:{}",billId);
                //2.修改订单状态为异常结束 status=3
                billRepository.updateSuccessStatus(success.getId(),3);
                //3.组织生成异常订单po 写入数据库
                ChargingBillExceptionPO exception=new ChargingBillExceptionPO();
                exception.setBillId(billId);
                exception.setBillStarttime(success.getChargingStartTime());
                exception.setCreateTime(new Date());
                exception.setDeleted(0);
                billRepository.saveException(exception);
                //4.通知用户如果还在充电,请结束走人
                WebSocketResult websocketResult=new WebSocketResult<>();
                websocketResult.setState(1);
                websocketResult.setMessage("ok");
                websocketResult.setData("您的订单充电异常,请结账走人");
                try {
                    websocketServerPoint.pushMsg(success.getUserId(),websocketResult);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }else{
                log.debug("该订单已经启动成功且结束,订单编号:{}",billId);
            }
        }else{
            log.debug("该订单没有启动成功,订单编号:{}",billId);
        }
    }

    private void checkUser(Integer userId, Integer gunId) {
        JsonResult<Boolean> result = userClient.checkUser(userId, gunId);
        Integer code = result.getCode();
        if (code!=null&&code!=0){
            log.error("车主检查可充失败");
            throw new RuntimeException("车主检查可充失败");
        }else if(code!=null&&code==0){
            log.debug("调用车主检查可充成功,userId:{},gunId:{}",userId,gunId);
            Boolean available = result.getData();
            if (!available){
                log.error("当前车主不可在该充电枪充电");
                throw new RuntimeException("当前车主不可在该充电枪充电");
            }
        }
    }

    private void checkGunAvailable(Integer gunId) {
        JsonResult<Boolean> result = deviceClient.checkGun(gunId);
        Integer code = result.getCode();
        if (code!=null&&code!=0){
            log.error("设备检查枪状态失败");
            throw new RuntimeException("设备检查枪状态失败");
        }else if(code!=null&&code==0){
            log.debug("设备检查枪状态成功,枪编号:{}",gunId);
            Boolean available = result.getData();
            if (!available){
                log.error("当前枪不能用来充电");
                throw new RuntimeException("当前枪不能用来充电");
            }
        }
    }

    private String generateBillId() {
        return generator.nextId()+"";
    }
}
