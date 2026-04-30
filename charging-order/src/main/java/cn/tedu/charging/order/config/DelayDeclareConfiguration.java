package cn.tedu.charging.order.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DelayDeclareConfiguration {
    private static final String DELAY_EX="delay_ex";
    private static final String DELAY_Q="delay_q";
    private static final String DLX_EX="dlx_ex";
    private static final String DLX_Q="dlx_q";
    private static final String DLX_RK="dlx_rk";
    //声明延迟交换机
    @Bean
    public Exchange delayExchange(){
        return ExchangeBuilder.fanoutExchange(DELAY_EX).build();
    }
    //声明延迟队列 绑定死信交换机 绑定死信路由
    @Bean
    public Queue delayQueue(){
        return QueueBuilder
                .nonDurable(DELAY_Q)
                .deadLetterExchange(DLX_EX)
                .deadLetterRoutingKey(DLX_RK).build();
    }
    //延迟交换机和延迟队列绑定关系
    @Bean
    public Binding delayBinding(){
        return BindingBuilder.bind(delayQueue()).to(delayExchange()).with("").noargs();
    }
    //声明死信交换机 死信队列绑定关系
    @Bean
    public Exchange dlxExchange(){
        return ExchangeBuilder.directExchange(DLX_EX).build();
    }
    @Bean
    public Queue dlxQueue(){
        return QueueBuilder.nonDurable(DLX_Q).build();
    }
    @Bean
    public Binding dlxBinding(){
        return BindingBuilder
                .bind(dlxQueue())
                .to(dlxExchange()).with(DLX_RK).noargs();
    }
}
