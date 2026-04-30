package cn.tedu.charging.order.config;

import cn.tedu.charging.common.utils.SnowflakeIdGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 创建一个容器管理的雪花生成器对象
 * 当前进程运行所在的环境要提供2个参数
 * 1.数据中心
 * 2.机器编号
 */
@Configuration
@Slf4j
public class SnowflakeConfiguration {
    @Value("${snow.flake.datacenterid:1}")
    private Integer datacenterId;
    @Value("${snow.flake.machineid:1}")
    private Integer machineId;

    @Bean
    public SnowflakeIdGenerator snowflakeIdGenerator(){
        log.debug("创建雪花生成器对象,datacenterId:{},machineId:{}",datacenterId,machineId);
        return new SnowflakeIdGenerator(datacenterId,machineId);
    }

}
