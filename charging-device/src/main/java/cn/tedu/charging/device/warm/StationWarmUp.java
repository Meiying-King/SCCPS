package cn.tedu.charging.device.warm;

import cn.tedu.charging.device.dao.mapper.StationMapper;
import cn.tedu.charging.device.service.DeviceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 实现场站一次性预热加载的入口
 */
@Component
@Slf4j
public class StationWarmUp implements ApplicationRunner {
    //注入一个业务对象来完成预热功能
    @Autowired
    private DeviceService deviceService;
    /**
     * spring容器在启动的时候 所有的bean对象都遵循一个生命周的基本流程
     * 1. 实例化
     * 2. 属性注入
     * 2. 初始化方法
     * 4. 结束初始化
     * 5. 在容器一直驻留
     * 6. 容器调用destroy销毁
     * run方法的调用时间 是容器运行之后,所有bean对象都创建完毕
     * 属性注入完毕之后调用的
     */
    @Override
    public void run(ApplicationArguments args) throws Exception {
        //调用业务或者数据层 读数据库数据 写redis
        log.debug("开始预热场站数据");
        for (int i = 0; i < 10; i++) {
            deviceService.warmUpDataInit();
        }
    }
}
