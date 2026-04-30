package cn.tedu.charging.cost.controller;

import cn.tedu.charging.common.pojo.param.ProgressCostParam;
import cn.tedu.charging.common.pojo.vo.ProgressCostVO;
import cn.tedu.charging.common.protocol.JsonResult;
import cn.tedu.charging.cost.service.CostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CostController {

    @Autowired
    private CostService costService;

    @PostMapping("/cost/bill/calculate")
    public JsonResult<ProgressCostVO> calculateCost(@RequestBody ProgressCostParam cost){
        //计算价格 控制层要什么,业务层就返回什么
        ProgressCostVO vo = costService.calculateCost(cost);
        return JsonResult.ok(vo);
    }
}
