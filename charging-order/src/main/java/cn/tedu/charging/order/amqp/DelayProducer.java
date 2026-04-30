package cn.tedu.charging.order.amqp;

import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
@Slf4j
public class DelayProducer {
    @Autowired
    private RabbitTemplate rabbitTemplate;
    //让调用者,只关心发送的消息内容和延迟的时间长短
    public void sendDelay(Object msg,Integer expiration){
        //1.序列化,如果是字符串直接getBytes如果不是字符串转成json getBytes
        String bodyStr=null;
        if (!(msg instanceof String)){
            bodyStr= JSON.toJSONString( msg);
        }else{
            bodyStr=(String) msg;
        }
        byte[] body=bodyStr.getBytes(StandardCharsets.UTF_8);
        MessageProperties properties=new MessageProperties();
        properties.setExpiration(expiration.toString());
        Message message=new Message(body,properties);
        //2.发送到延迟交换机
        rabbitTemplate.send("delay_ex","",message);
        log.info("发送延迟消息:{}",msg);
    }
}
