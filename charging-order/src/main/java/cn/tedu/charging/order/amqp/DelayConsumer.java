package cn.tedu.charging.order.amqp;

import cn.tedu.charging.common.pojo.message.DelayCheckMessage;
import cn.tedu.charging.order.service.ConsumerService;
import com.alibaba.fastjson2.JSON;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class DelayConsumer {
    @Autowired
    private ConsumerService consumerService;
    @Autowired
    private RedisTemplate redisTemplate;
    @RabbitListener(queues="dlx_q")
    public void delayConsume(String param, Message message, Channel channel){
        log.info("延迟消息被消费一次:{}",param);
        //反序列化 json转化会DelayCheckMessage
        DelayCheckMessage msg = JSON.parseObject(param, DelayCheckMessage.class);
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
        //调用业务处理消息
        try{
            consumerService.handleCheckNoRes(msg);
        }catch (Exception e){
            log.error("处理延迟消息异常",e);
        }finally {
            //无论业务执行成功还是失败,在finally中手动释放
            //3.手动释放
            redisTemplate.delete(lockKey);
        }
        //根据业务执行结果(抛异常和正常执行) ack nack(系统异常,认为下次有机会成功,但是如果是业务异常,下次依然没有机会成功 不在重新入队)
        try {
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
        } catch (Exception e) {
            log.error("确认消息失败",e);
        }
    }
}
