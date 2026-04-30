package cn.tedu.charging.order.config;

import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class XxlJobConfig {
    @Value("${xxl.job.admin.addresses}")
    private String adminAddresses;

    // xxl-job访问令牌
    @Value("${xxl.job.admin.accessToken}")
    private String accessToken;

    // xxl-job执行器应用名称
    @Value("${xxl.job.executor.appname}")
    private String appname;

    // xxl-job执行器地址 和ip和port关系 ip 127.0.0.1 port 19999 address 127.0.0.1:19999
    @Value("${xxl.job.executor.address}")
    private String address;

    // xxl-job执行器IP
    @Value("${xxl.job.executor.ip}")
    private String ip;

    // xxl-job执行器端口号
    @Value("${xxl.job.executor.port}")
    private int port;

    // xxl-job执行器日志路径
    @Value("${xxl.job.executor.logpath}")
    private String logPath;

    // xxl-job执行器日志保留天数
    @Value("${xxl.job.executor.logretentiondays}")
    private int logRetentionDays;
    //把连接调度中心的执行器对象创建成容器bean来管理
    @Bean
    public XxlJobSpringExecutor xxlJobExecutor(){
        XxlJobSpringExecutor executor = new XxlJobSpringExecutor();
        //给对象赋值 使用注入的属性
        executor.setAdminAddresses(adminAddresses);
        executor.setAccessToken(accessToken);
        executor.setAppname(appname);
        executor.setAddress(address);
        executor.setIp(ip);
        executor.setPort(port);
        executor.setLogPath(logPath);
        executor.setLogRetentionDays(logRetentionDays);
        return executor;
    }
}
