package cn.tedu.charging.order.mqtt;

import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * 把mqttClient注入到这里
 * 实现方法的封装,让业务生产发送消息 和emqx解耦
 */
@Component
@Slf4j
public class MqttProducer {
    @Autowired
    private MqttClient client;

    //底层发送实现,调用者需要传递消息内容 消息目标主题,消息qos和retained
    public void doSend(String topic,byte[] payload,int qos,boolean retained){
        try{
            //1.组织消息对象
            MqttMessage message=new MqttMessage(payload);
            message.setQos(qos);
            message.setRetained(retained);
            //2.发送
            client.publish(topic,message);
        }catch (Exception e){
            log.error("发送消息失败,topic={},qos={},retained={}",topic,qos,retained,e);
            throw new RuntimeException("发送消息失败");
        }
    }
    //包装doSend 暴漏给业务使用
    public boolean send(String topic,Object message){
        //1.处理序列化 设置默认qos=1和retained=true
        byte[] payload=JSON.toJSONString(message).getBytes(StandardCharsets.UTF_8);
        //2.调用doSend 设置默认发送
        try {
            doSend(topic, payload, 1, true);
            return true;
        }catch (Exception e){
            log.error("发送消息失败,topic={},payload={}",topic,payload,e);
            return false;
        }
    }
    //可以根据业务需求重载
    public boolean send(String topic,Object message,Integer qos){
        //1.处理序列化 设置默认qos=1和retained=true
        byte[] payload=JSON.toJSONString(message).getBytes(StandardCharsets.UTF_8);
        //2.调用doSend 设置默认发送
        try {
            doSend(topic, payload, qos, true);
            return true;
        }catch (Exception e){
            log.error("发送消息失败,topic={},payload={}",topic,payload,e);
            return false;
        }
    }
    public boolean send(String topic,Object message,Integer qos,Boolean retained){
        //1.处理序列化 设置默认qos=1和retained=true
        byte[] payload=JSON.toJSONString(message).getBytes(StandardCharsets.UTF_8);
        //2.调用doSend 设置默认发送
        try {
            doSend(topic, payload, qos, retained);
            return true;
        }catch (Exception e){
            log.error("发送消息失败,topic={},payload={}",topic,payload,e);
            return false;
        }
    }
}
