package cn.tedu.charging.cost.service.impl;

import cn.tedu.charging.common.pojo.param.ProgressCostParam;
import cn.tedu.charging.common.pojo.vo.ProgressCostVO;
import cn.tedu.charging.cost.dao.repository.CostRuleRepository;
import cn.tedu.charging.cost.pojo.po.ChargingCostRulePO;
import cn.tedu.charging.cost.service.CostService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Calendar;
import java.util.Date;

@Service
@Slf4j
public class CostServiceImpl implements CostService {
    @Autowired
    private CostRuleRepository costRuleRepository;
    @Autowired
    private RedisTemplate redisTemplate;
    @Override
    public ProgressCostVO calculateCost(ProgressCostParam param) {
        //1.拿到当前系统时间的小时数 比如 10:58 小时数就是10 9:21 就是9 23:56 就是23
        log.debug("获取当前系统时间小时数开始");
        Integer hourNum=getCurrentHour();
        log.debug("当前系统时间小时数为:{}",hourNum);
        //2.根据当前条件 stationId(场站不同) gunId(枪类型不同) hourNum(时间不同)查询计价规则
        log.debug("根据当前条件获取计价规则开始");
        ChargingCostRulePO costRule=getCostRule(param.getStationId(),param.getGunId(),hourNum);
        log.debug("根据当前条件获取计价规则结束,规则为:{}",costRule);
        //3.利用已知条件 计算单次度数
        BigDecimal onceCapacity=calculateOnceCapacity(param);
        //4.有了电单价,有了单次充电度数,可以累加计算总金额
        BigDecimal totalCost=calculateTotalCost(param,costRule.getPowerFee(),onceCapacity);
        //5.组织vo返回
        ProgressCostVO vo=new ProgressCostVO();
        vo.setTotalCost(totalCost.doubleValue());
        vo.setChargingCapacity(onceCapacity.doubleValue());
        vo.setPowerFee(costRule.getPowerFee().doubleValue());
        return vo;
    }

    private BigDecimal calculateTotalCost(ProgressCostParam param,BigDecimal powerFee,BigDecimal onceCapacity) {
        //准备好累加的redis操作相关数据和对象 incr
        ValueOperations<String,Double> valueOps = redisTemplate.opsForValue();
        //每张订单对于总金额从开始累加到结尾 使用订单编号绑定key值
        String totalCostKey="charging:order:total:cost:"+param.getOrderNo();
        //1.使用单价乘以 单次度数 得到本次金额 multiply
        log.debug("本次充电同步单价:{}",powerFee);
        log.debug("本次充电同步单次度数:{}",onceCapacity);
        BigDecimal onceCost=powerFee.multiply(onceCapacity);
        log.debug("本次充电同步的单次价格:{}",onceCost);
        //2.在redis做累加 redis支持的是double不支持bigDecimal
        //incrby key onceCost
        Double totalCost = valueOps.increment(totalCostKey, onceCost.doubleValue());
        log.debug("本次充电同步为止的总金额:{}",totalCost);
        return new BigDecimal(totalCost+"");
    }

    private BigDecimal calculateOnceCapacity(ProgressCostParam param) {
        //从redisTemplate拿到操作读写string的对象
        ValueOperations valueOps = redisTemplate.opsForValue();
        //使用订单编号 定义这个key值
        String orderLastTotalCapacityKey="charging:order:last:total:"+param.getOrderNo();
        //1. 使用本次总度数 覆盖上次总度数 获取上次总度数 set GET
        Double lastCapacity = (Double) valueOps.getAndSet(orderLastTotalCapacityKey, param.getTotalCapacity());
        //判断,如果上次总度数没有值 说明本地同步是第一次
        if (lastCapacity==null){
            //第一次同步度数,上次总度数按0计算
            lastCapacity=0d;
        }
        //2.使用本次总度数,减去上次的总度数 考虑精度问题,请使用bigDecimal计算
        BigDecimal currentTotalCapacity=new BigDecimal(param.getTotalCapacity()+"");
        log.debug("本次总度数,current:{}",currentTotalCapacity);
        BigDecimal lastTotalCapacity=new BigDecimal(lastCapacity+"");
        log.debug("上次总度数,last:{}",lastTotalCapacity);
        //bigDecimal支持数字计算 减法subtract
        BigDecimal onceCapacity=currentTotalCapacity.subtract(lastTotalCapacity);
        log.debug("单次充电度数,once:{}",onceCapacity);
        //如果计算的单次度数小于0 是负数,异常
        if (onceCapacity.doubleValue()<0){
            log.error("单次充电度数异常,请检查");
            throw new RuntimeException("单次充电度数异常,请检查");
        }
        return onceCapacity;
    }

    private ChargingCostRulePO getCostRule(Integer stationId, Integer gunId, Integer hourNum) {
        //数据库 查询计价规则的条件 stationId gunType hour 目前没有枪类型,但是使用gunId是可以查询到枪类型
        //假设类型已经准备好了 gunType=1 具体查询的时候,默认固定(轻做)
        ChargingCostRulePO costRule = costRuleRepository.getCostRule(stationId, hourNum);
        if (costRule==null){
            log.error("当前条件没有查询到对应计价规则");
            throw new RuntimeException("计价规则为空,条件不正确");
        }
        return costRule;
    }

    private Integer getCurrentHour() {
        //方案1.Date
        //new Date().getHours();
        //方案2.Calendar
        //Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        return LocalDateTime.now().getHour();
    }
}
