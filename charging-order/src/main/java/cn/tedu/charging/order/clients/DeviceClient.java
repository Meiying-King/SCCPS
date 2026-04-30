package cn.tedu.charging.order.clients;

import cn.tedu.charging.common.protocol.JsonResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name="charging-device")
public interface DeviceClient {
    //传递枪编号 查询枪是否可以用来下单充电
    @GetMapping("/device/gun/check")
    JsonResult<Boolean> checkGun(@RequestParam("gunId") Integer gunId);

    //补充第二个接口 修改枪转改
    @GetMapping("/device/gun/update")
    JsonResult<Boolean> updateGunStatus(
            @RequestParam("gunId") Integer gunId,
            @RequestParam("status") Integer status);
}