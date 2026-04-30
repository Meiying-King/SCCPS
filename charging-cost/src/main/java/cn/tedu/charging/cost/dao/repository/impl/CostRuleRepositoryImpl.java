package cn.tedu.charging.cost.dao.repository.impl;

import cn.tedu.charging.cost.dao.mapper.CostRuleMapper;
import cn.tedu.charging.cost.dao.repository.CostRuleRepository;
import cn.tedu.charging.cost.pojo.po.ChargingCostRulePO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Slf4j
public class CostRuleRepositoryImpl implements CostRuleRepository {
    @Autowired
    private CostRuleMapper costRuleMapper;
    @Autowired
    private RedisTemplate redisTemplate;
    //方案3
    @Override
    public ChargingCostRulePO getCostRule(Integer stationId, Integer hour) {
        //操作list的对象
        ListOperations<String,ChargingCostRulePO> listOps = redisTemplate.opsForList();
        //1.组织一个key值,查询这个key对应的集合,查询所有元素(一个场站计价规则不会太多)
        String stationRulesKey="charging:stations:rules:"+stationId;
        //读取缓存list 底层命令 lrange key 0 -1
        List<ChargingCostRulePO> rules = listOps.range(stationRulesKey, 0, -1);
        //判断命中
        if (rules==null||rules.size()==0){
            log.debug("未命中缓存,查询数据库,stationId={}",stationId);
            //2.利用场站id 查询数据库
            QueryWrapper<ChargingCostRulePO> queryWrapper=new QueryWrapper<>();
            queryWrapper.eq("station_id",stationId);
            rules = costRuleMapper.selectList(queryWrapper);
            //3.把数据库返回结果放到缓存中 lpush key member member...
            if (rules!=null&&rules.size()>0){
                listOps.leftPushAll(stationRulesKey,rules);
            }else{
                log.error("数据库没有数据,stationId={}",stationId);
            }

        }
        //4.从最终的rules中筛选满足时间条件的规则
        if (rules!=null&&rules.size()>0){
            //当我们编写完筛选的规则,每个规则元素都会计算筛选规则,满足筛选条件的留下(最多一个)
            return rules.stream().filter(rule->{
                //每一个列表中的rule都要利用hour判断是否满足时间
                return rule.getStartTime()<=hour&&rule.getEndTime()>hour;
            }).findFirst().orElse(null);
        }else{
            return null;
        }
    }
    //方案2: 查询某个场站下的所有规则,然后利用时间小时数做筛选过滤
    /*@Override
    public ChargingCostRulePO getCostRule(Integer stationId, Integer hour) {
        //1.利用场站id查询规则集合
        QueryWrapper<ChargingCostRulePO> queryWrapper=new QueryWrapper<>();
        queryWrapper.eq("station_id",stationId);
        List<ChargingCostRulePO> rules = costRuleMapper.selectList(queryWrapper);
        //2.在集合中筛选满足条件的规则 start_time<=hour&&end_time>hour 可以使用stream() api
        if (rules!=null&&rules.size()>0){
            //当我们编写完筛选的规则,每个规则元素都会计算筛选规则,满足筛选条件的留下(最多一个)
            return rules.stream().filter(rule->{
                //每一个列表中的rule都要利用hour判断是否满足时间
                return rule.getStartTime()<=hour&&rule.getEndTime()>hour;
            }).findFirst().orElse(null);
        }else{
            return null;
        }
    }*/
    /**
     * name   start  end  power_fee gun_type station_id
     * 尖上午   08     12     1.50   快1     5     #场站id是5,时间段[08,12) 按照1.5元/度
     * 尖下午   14     18     1.50   快1      5     #场站id是5,时间段[14,18) 按照1.5元/度
     * 全天峰   18     24     1.40   快1      5     #场站id是5,时间段[18,24) 按照1.4元/度
     * 全天平   12     14     1.30   快1     5     #场站id是5,时间段[12,14) 按照1.3元/度
     * 全天谷   00     08     1.00   快1      5     #场站id是5,时间段[00,08) 按照1.0元/度
     */

    //方案1: 拼接where条件
    /*public ChargingCostRulePO getCostRule(Integer stationId, Integer hour) {

        QueryWrapper<ChargingCostRulePO> queryWrapper=new QueryWrapper<>();
        queryWrapper.eq("station_id",stationId)
                .le("start_time",hour)
                .gt("end_time",hour);
        return costRuleMapper.selectOne(queryWrapper);
    }*/
}
