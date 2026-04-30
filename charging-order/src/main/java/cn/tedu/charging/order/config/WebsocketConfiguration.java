package cn.tedu.charging.order.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

/**
 * 当前配置类,主要作用是注册生成一个端点的管理器.
 * 这个管理器在容器中会扫描并且加载所有的websocket终端端点,
 * 然后整合到容器使用.如果没有这一步,任何websocket客户端都无法
 * 建立连接
 */
@Configuration
public class WebsocketConfiguration {
    @Bean
    public ServerEndpointExporter serverEndpointExporter(){
        return new ServerEndpointExporter();
    }
}
