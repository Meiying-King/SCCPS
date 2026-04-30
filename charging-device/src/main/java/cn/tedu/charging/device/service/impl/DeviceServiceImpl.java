package cn.tedu.charging.device.service.impl;

import cn.tedu.charging.common.pojo.query.NearStationsQuery;
import cn.tedu.charging.common.pojo.vo.GunInfoVO;
import cn.tedu.charging.common.pojo.vo.StationDetailVO;
import cn.tedu.charging.common.pojo.vo.StationInfoVO;
import cn.tedu.charging.device.dao.repository.DeviceRepository;
import cn.tedu.charging.device.pojo.po.ChargingGunInfoPO;
import cn.tedu.charging.device.pojo.po.ChargingStationPO;
import cn.tedu.charging.device.pojo.po.StationGeoPO;
import cn.tedu.charging.device.service.DeviceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DeviceServiceImpl implements DeviceService {
    @Autowired
    private DeviceRepository deviceRepository;

    @Override
    public void warmUpDataInit() {
        //1.读取仓储层返回结果,判断是否继续预热
        boolean exists=deviceRepository.checkWarmUpExists();
        if (exists){
            log.debug("数据已经预热,不需要再次预热");
            return;
        }else{
            log.debug("数据开始执行预热");
            //2.获取场站总数和batch 以便于计算循环次数
            Long total=deviceRepository.countStations();
            Long batch=2l;//应该给10000 5000
            if (total>0){
                log.debug("当前场站总数:{},执行循环",total);
                //3.计算循环次数
                Long loopNum=total%batch==0?total/batch:total/batch+1;
                for (long i=0;i<loopNum;i++){
                    //4.计算分页条件
                    Long start=i*batch;
                    Long rows=batch;
                    //查询分页分批数据
                    List<ChargingStationPO> stations=deviceRepository.getStationsPage(start,rows);
                    //5.将这批stations写入到redis 中
                    deviceRepository.saveGeos(stations);
                }
            }else{
                log.debug("当前场站总数:{},不执行循环",total);
            }
        }
    }
    /*@Override
    public void warmUpDataInit() {
        //1.读取数据层的全部 station数据
        List<ChargingStationPO> stations=deviceRepository.getAllStations();
        //判断非空 不能null size>0
        if(stations!=null&&stations.size()>0){
            log.debug("预热查询场站数据非空,场站个数:{}",stations.size());
            //2.将数据预热写入
            deviceRepository.saveGeos(stations);
        }
    }*/
    @Override
    public List<StationInfoVO> nearStations(NearStationsQuery query) {
        //1.调用数据层查询po的返回结果
        List<StationGeoPO> geoStations = deviceRepository.nearStations(query);
        //判断非空
        if (geoStations!=null&&geoStations.size()>0){
            log.debug("查询附近场站非空,个数:{}",geoStations.size());
            //2.将pos转化vos
            //2.1准备一个vos集合
            List<StationInfoVO> vos=new ArrayList<>();
            //2.2循环遍历 转化单个vo放到vos集合里
            for (StationGeoPO geoStation : geoStations) {
                StationInfoVO vo=new StationInfoVO();
                BeanUtils.copyProperties(geoStation,vo);
                //2.3调整vo中参数细节
                //avgPrice平均电价应该调用 计价服务获取当前场站的平均金额 TODO
                vo.setAvgPrice(1.27);
                //distance目前是米单位
                log.debug("距离:{}",geoStation.getDistance());
                //对距离 将米转化千米/1000 小数点保留2为 剩余的做四舍五入
                //distance/1000 取2位小数电 0.18947==0.18
                vo.setDistance(vo.getDistance().divide(BigDecimal.valueOf(1000)).setScale(2,BigDecimal.ROUND_HALF_UP));
                vos.add(vo);
            }
            //对vos中的列表顺序做排序计算 谁的distance数字小,谁靠前,可以流stream().sorted ()
            return vos.stream().sorted((o1, o2) -> o1.getDistance().compareTo(o2.getDistance())).collect(Collectors.toList());
            /*return geoStations.stream().map(geoStation -> {
                StationInfoVO vo=new StationInfoVO();
                BeanUtils.copyProperties(geoStation,vo);
                return vo;
            }).collect(Collectors.toList());*/
        }else{
            log.debug("查询附近场站为空");
            return List.of();
        }
    }

    @Override
    public StationDetailVO detailStation(Integer stationId) {
        //1.调用仓储层查场站
        ChargingStationPO stationPO = deviceRepository.getStationById(stationId);
        //判断非空
        if (stationPO!=null){
            StationDetailVO vo=new StationDetailVO();
            //2.查询场站下的枪列表
            List<ChargingGunInfoPO> gunPos = deviceRepository.getStationGuns(stationId);
            //判断非空
            if (gunPos!=null&&gunPos.size()>0){
                //枪非空,封装gunVos列表,不在使用循环遍历,使用list的stream()流
                List<GunInfoVO> gunVos=gunPos.stream().map(gunPo->{
                    //映射计算就是将gunPO转化gunInfoVO
                    GunInfoVO gunVO=new GunInfoVO();
                    BeanUtils.copyProperties(gunPo,gunVO);
                    return gunVO;
                }).collect(Collectors.toList());
                //将非空的枪信息 赋值给stationDetailVO
                vo.setGunInfoVos(gunVos);
            }
            //3.vo对象数据封装
            vo.setStationId(stationPO.getId());
            vo.setStationName(stationPO.getStationName());
            vo.setStationStatus(stationPO.getStationStatus());
            vo.setAddress(stationPO.getAddress());
            return vo;
        }else{
            log.debug("查询场站为空");
            return null;
        }
    }

    @Override
    public void updateGunStatus(Integer gunId, Integer status) {

    }

    @Override
    public Boolean checkGunAvailable(Integer gunId) {
        return null;
    }




}
