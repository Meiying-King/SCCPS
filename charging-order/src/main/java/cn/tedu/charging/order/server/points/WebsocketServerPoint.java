package cn.tedu.charging.order.server.points;

import com.alibaba.fastjson2.JSON;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 当前这个类,表示在web服务中,一个对外暴露的ws连接终端
 * 这个类负责所有连接客户端的通信全过程
 * 1. 连接是否建立
 * 2. 连接是否关闭
 * 3. 客户端发送什么消息
 * 4. 通信是否存在异常
 */
@Component
@Slf4j
@ServerEndpoint("/ws/server/{userId}")
public class WebsocketServerPoint {
    private static final Map<Integer,Session> SESSIONS=new ConcurrentHashMap<>();
    /**
     * 客户端连接成功时调用的方法
     */
    @OnOpen
    public void onOpen(Session session, @PathParam("userId") Integer userId) throws IOException {
        log.info("客户端连接成功,会话对象id:{},用户id:{}",session.getId(),userId);
        String msg="欢迎用户:"+userId+"来此连接";
        session.getBasicRemote().sendText(msg);
        /*
         每当一个用户建立连接,就是在map中新增userId和session映射
         应该考虑到同一个userId多次建立连接的情况
         */
        //判断当前userId,在SESSIONS中是否已经存在
        boolean contain = SESSIONS.containsKey(userId);
        if (contain){
            log.info("当前用户已经建立连接,userId:{}",userId);
            Session oldSession = SESSIONS.get(userId);
            oldSession.close();
            //从映射map中删除
            SESSIONS.remove(userId);
        }
        //将新的连接,和新的映射保存到map中
        SESSIONS.put(userId,session);
        //打印在线人数
        log.info("当前在线人数:{}",SESSIONS.size());
    }
    /**
     * 客户端断开连接时调用的方法
     */
    @OnClose
    public void onClose(Session session,@PathParam("userId") Integer userId){
        log.info("客户端断开连接,会话对象id:{},用户id:{}",session.getId(),userId);
        //当前用户保存的映射因为断开连接,要上次那户
        SESSIONS.remove(userId);
        log.info("当前在线人数:{}",SESSIONS.size());
    }
    /**
     * 客户端发送消息时调用的方法
     */
    @OnMessage
    public void onMessage(String message,Session session,@PathParam("userId") Integer userId) throws IOException {
        log.info("收到客户端发来的消息:{},会话对象id:{},用户id:{}",message,session.getId(),userId);
        String msg="收到";
        session.getBasicRemote().sendText(msg);
    }
    /**
     * 上述三个方法执行通信的过程中,任何一个出现异常,都会调用
     */
    @OnError
    public void onError(Throwable error,Session session,@PathParam("userId") Integer userId){
        log.error("会话对象id:{},用户id:{} 通信过程异常:{}",session.getId(),userId,error);
    }

    /**
     * 目标: 利用userId业务数据 找到session连接 从而将消息推送出去
     * 1. 入参消息不是String是object,判断这个消息实现类 如果是string直接推,如果是其他类型json在推
     * 2. 利用userId获取session对象
     * 3. 判断如果能获取直接调用api推送 不能获取session 抛异常
     */
    public void pushMsg(Integer userId, Object msg) throws IOException {
        //1.解析入参msg
        String textMsg="";
        if (!(msg instanceof String)){
            //消息数据是其他引用类型,将msg转化成json
            textMsg= JSON.toJSONString(msg);
        }else{
            textMsg=(String)msg;
        }
        //2.拿到当前userId在线的用户session连接
        Session session = SESSIONS.get(userId);
        //判断非空
        //3.非空直接推送 空抛异常
        if(session!=null){
            session.getBasicRemote().sendText(textMsg);
        }else{
            throw new RuntimeException("当前用户不在线,userId:"+userId);
        }
    }
}
