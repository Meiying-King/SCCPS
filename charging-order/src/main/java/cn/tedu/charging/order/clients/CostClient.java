package cn.tedu.charging.order.clients;

import cn.tedu.charging.common.pojo.param.ProgressCostParam;
import cn.tedu.charging.common.pojo.vo.ProgressCostVO;
import cn.tedu.charging.common.protocol.JsonResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 订单调用 charging-cost服务的接口
 */
@FeignClient(name="charging-cost")
public interface CostClient {
    /**
     * post 提交
     * path /cost/bill/calculate
     * param @RequestBody ProgressCostParam
     * return JsonResult<ProgressCostVO>
     */
    @PostMapping("/cost/bill/calculate")
    JsonResult<ProgressCostVO> calculateBill(@RequestBody ProgressCostParam param);
}
