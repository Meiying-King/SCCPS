package cn.tedu.charging.order.mqtt;

import cn.tedu.charging.common.constant.MqttTopicConst;
import cn.tedu.charging.common.pojo.message.CheckResultMessage;
import cn.tedu.charging.common.pojo.message.ProgressMessage;
import cn.tedu.charging.order.service.ConsumerService;
import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class MqttClientCallback implements MqttCallbackExtended {
    /*
        方法作用: 当mqttClient调用connect并且成功和emqx建立通信之后立即调用
        参数:
        boolean reconnect: 如果本次建立的连接是重新连接,reconnect就是true 第一次连接false
        String serverURI: 连接服务端address tcp://192.168.8.100:1883
     */
    @Override
    public void connectComplete(boolean reconnect, String serverURI) {
        //判断重连 打印日志
        if (reconnect){
            log.info("重新连接成功,serverURI={}",serverURI);
        }else{
            log.info("首次连接成功,serverURI={}",serverURI);
        }
    }
    /*
        方法作用: 当mqttClient调用disconnect方法断开连接后,或者由于网络波动,
        导致和服务端连接通信超时失败,回调就会调用这个方法
        参数:
        Throwable cause: 断开连接的原因
     */
    @Override
    public void connectionLost(Throwable cause) {
        log.info("连接丢失,原因={}",cause.getMessage());
    }

    /**
     * 消费消息的入口 所有当前客户端监听订阅的主题消息都会进入这个方法处理
     * 方法作用: 当mqttClient连接对象订阅了某个主题,一旦这个主题中出现消息,
     * 就会通过这个方法将消息传递给客户端
     * 参数:
     * String topic: 消息来源的topic名称
     * MqttMessage message: 消息对象,包含了消息的所有内容
     */
    @Autowired
    private ConsumerService consumerService;
    @Autowired
    private RedisTemplate redisTemplate;
    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        log.debug("收到消息,主题={},内容={}",topic,message.toString());
        //判断当前消息来源 根据不同主题名称 调用不同业务方法
        if (topic!=null&&topic.equals(MqttTopicConst.GUN_CHECK_RESULT_TOPIC)){
            log.debug("开始设备反馈自检的消费");
            //反序列化 得到msg对象 调用业务层
            byte[] payload = message.getPayload();
            //转回字符串
            String json=new String(payload, StandardCharsets.UTF_8);
            //转回对象
            CheckResultMessage msg = JSON.parseObject(json, CheckResultMessage.class);
            //1.调用业务方法之前,先抢锁 set key value NX EX 5
            ValueOperations valueOps = redisTemplate.opsForValue();
            String lockKey="charging:order:consume:lock"+msg.getOrderNo();
            //2.抢锁
            Boolean haveLock = valueOps.setIfAbsent(lockKey, "", 5, TimeUnit.SECONDS);
            //判断抢锁成功失败结果
            if(!haveLock){
                //抢锁失败,线程放弃执行业务
                log.debug("线程{}抢锁失败,订单{}",Thread.currentThread().getName(),msg.getOrderNo());
                return ;
            }
            //调用业务实现
            try {
                consumerService.handlerCheckResult(msg);
            }catch (Exception e){
                log.error("处理消息异常",e);
            }finally {
                //无论业务执行成功还是失败,在finally中手动释放
                //3.手动释放
                redisTemplate.delete(lockKey);
            }
        }else if (topic!=null&&topic.equals(MqttTopicConst.CHARGING_PROGRESS_TOPIC)) {
            //按照接口文件解析消息 json转化成ProgressMessage
            log.debug("开始设备同步进度消费");
            //反序列化 得到msg对象 调用业务层
            byte[] payload = message.getPayload();
            //转回字符串
            String json=new String(payload, StandardCharsets.UTF_8);
            //转回对象
            ProgressMessage msg = JSON.parseObject(json, ProgressMessage.class);
            //调用消费业务
            consumerService.handleChargingProgress(msg);
        } else {
            log.error("监听到了不属于当前消费业务的消息");
        }
    }
    /*
        方法作用: 当mqttClient调用publish方法发布消息后,如果服务端返回确认,
        就会调用这个方法
        参数:
        IMqttDeliveryToken token: 发布消息的令牌,可以通过这个令牌判断消息是否发布成功
     */
    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        //消息发送结果 complete:fasle 消息发布失败 true 消息发布成功
        boolean complete = token.isComplete();
        if (complete){
            log.info("消息发布成功");
        }else{
            log.info("消息发布失败");
        }
    }
}
