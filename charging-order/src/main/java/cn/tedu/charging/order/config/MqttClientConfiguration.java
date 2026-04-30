package cn.tedu.charging.order.config;

import cn.tedu.charging.common.constant.MqttTopicConst;
import cn.tedu.charging.order.mqtt.MqttClientCallback;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * 当前配置类的任务,就是读取属性构造一个可以连接emqx的java客户端对象
 * 将这个客户端对象 驻留到内存中
 */
@Configuration
@Slf4j
public class MqttClientConfiguration {
    @Value("${charging.emqx.address}")
    private String address;
    @Value("${charging.emqx.username}")
    private String username;
    @Value("${charging.emqx.password}")
    private String password;
    //将回调对象注入到配置类
    @Autowired
    private MqttClientCallback callback;
    @Bean
    public MqttClient mqttClient(){
        log.info("正在创建mqttClient对象");
        //初始化赋值null准备在try里赋值
        MqttClient mqttClient=null;
        try {
            //1.将mqttClient对象实例化 new
            mqttClient=new MqttClient(address,"emqx_demo_"+ UUID.randomUUID().toString(),new MemoryPersistence());
            //2.对连接对象提供各种连接的选项配置,必须权限身份的用户名密码
            MqttConnectOptions options=new MqttConnectOptions();
            //2.1设置用户名密码
            options.setUserName(username);
            options.setPassword(password.toCharArray());
            //当设置为 true 时，客户端与 emqx 建立新连接时会清除所有之前的会话信息（包括未完成的消息、订阅等）
            //options.setCleanSession(true);//清空会话
            //设置心跳间隔时间（秒），用于检测客户端是否在线
            //options.setKeepAliveInterval(30);
            //设置连接超时时间（秒）;设置等待建立连接的最大时长
            //options.setConnectionTimeout(30);
            //设置是否自动重新连接 当设置为 true 时，如果连接意外断开，客户端会自动尝试重新连接
            //options.setAutomaticReconnect(true);
            //设置回调函数
            mqttClient.setCallback(callback);
            //最终需要建立连接
            //options准备遗嘱消息
            options.setWill("will-topic","我是最后一条消息".getBytes(StandardCharsets.UTF_8),1,true);
            mqttClient.connect(options);
            //订单消费者,拿到设备自检反馈的主题
            mqttClient.subscribe("$share/order/"+ MqttTopicConst.GUN_CHECK_RESULT_TOPIC,1);
            //订单消费者,共享订阅 设备充电同步进度
            mqttClient.subscribe("$share/order/"+ MqttTopicConst.CHARGING_PROGRESS_TOPIC,1);
            //也可以按照范围订阅 charging/device/#
        }catch (Exception e){
            log.error("创建mqttClient对象失败,异常信息:{}",e.getMessage());
        }
        return mqttClient;
    }
}
