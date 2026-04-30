package cn.tedu.charging.order.clients;

import cn.tedu.charging.common.protocol.JsonResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name="charging-user")
public interface UserClient {
    //检查某个车主是否允许在这个枪上充电
    @GetMapping("/user/charge/check")
    JsonResult<Boolean> checkUser(
            @RequestParam("userId") Integer userId,@RequestParam("gunId")Integer gunId);
}
