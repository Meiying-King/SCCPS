package cn.tedu.charging.device.controller;

import cn.tedu.charging.common.pojo.query.NearStationsQuery;
import cn.tedu.charging.common.pojo.vo.StationDetailVO;
import cn.tedu.charging.common.pojo.vo.StationInfoVO;
import cn.tedu.charging.common.protocol.JsonResult;
import cn.tedu.charging.device.service.DeviceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 设备相关http入口 业务包含
 * operater
 * station
 * pile
 * gun
 * author: xiaoxw
 * date: 2025/8/29
 * version: 1.0
 */
@RestController
@Slf4j
public class DeviceController {
    @Autowired
    private DeviceService deviceService;

    /**
     * 查询附近的充电站
     * @param query 充电站查询参数 经纬度 半径
     * @return 充电站列表
     */
    @GetMapping("/device/station/near")
    public JsonResult<List<StationInfoVO>> nearStations(NearStationsQuery query){
        //控制层返回给前端什么数据,就调用业务层获取什么数据
        List<StationInfoVO> vos = deviceService.nearStations(query);
        return JsonResult.ok(vos);
    }
    //查询某个充电站详情包括站场信息以及站场关联的枪数据
    @GetMapping("/device/station/detail/{stationId}")
    public JsonResult<StationDetailVO> detailStation(@PathVariable Integer stationId){
        StationDetailVO vo = deviceService.detailStation(stationId);
        return JsonResult.ok(vo);
    }
    //订单调用设备检查枪是否可用
    @GetMapping("/device/gun/check")
    public JsonResult<Boolean> checkGun(@RequestParam("gunId") Integer gunId){
        //调用业务 查询枪状态,1空闲 2使用中 3故障 4离线 5其他 轻做
        return JsonResult.ok(true);
    }

    //修改枪状态的方法
    @PostMapping("/device/gun/error")
    public JsonResult<Boolean> updateGunStatus(
            @RequestParam("gunId")Integer gunId){
        //TODO
        return JsonResult.ok();
    }
    //补充第二个接口 修改枪转改
    @GetMapping("/device/gun/update")
    JsonResult<Boolean> updateGunStatus(
            @RequestParam("gunId") Integer gunId,
            @RequestParam("status") Integer status){
        //调用业务 调用仓储层 将该枪状态改成传递的值
        return JsonResult.ok(true);
    }
}
