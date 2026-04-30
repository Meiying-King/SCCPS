package cn.tedu.charging.device.dao.repository.impl;

import cn.tedu.charging.common.constant.CacheKeyConst;
import cn.tedu.charging.common.pojo.query.NearStationsQuery;
import cn.tedu.charging.device.dao.mapper.GunMapper;
import cn.tedu.charging.device.dao.mapper.StationMapper;
import cn.tedu.charging.device.dao.repository.DeviceRepository;
import cn.tedu.charging.device.pojo.po.ChargingGunInfoPO;
import cn.tedu.charging.device.pojo.po.ChargingStationPO;
import cn.tedu.charging.device.pojo.po.StationCanalPO;
import cn.tedu.charging.device.pojo.po.StationGeoPO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.geo.*;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.GeoOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Repository
public class DeviceRepositoryImpl implements DeviceRepository {
    @Autowired
    private StationMapper stationMapper;
    @Autowired
    private RedisTemplate redisTemplate;
    @Override
    public List<ChargingStationPO> getStationsPage(Long start, Long rows) {
        //select * from charging_station limit 0,2;
        QueryWrapper<ChargingStationPO> queryWrapper=new QueryWrapper<>();
        //手动在sql最末尾 添加limit关键字
        queryWrapper.last("limit "+start+","+rows);
        return stationMapper.selectList(queryWrapper);
    }
    @Override
    public Long countStations() {
        //select count(0) from charging_station
        return stationMapper.selectCount(null);
    }
    @Override
    public boolean checkWarmUpExists() {
        //exists key
        return redisTemplate.hasKey(CacheKeyConst.GEO_STATIONS);
    }
    @Override
    public List<ChargingStationPO> getAllStations() {
        //select * from charging_station
        return stationMapper.selectList(null);
    }

    @Override
    public void saveGeos(List<ChargingStationPO> stations) {
        //入参是非空的集合 List<StationPO>(id,lng,lat) 映射成List<GeoLocation>
        //1.准备一个操作redis的客户端对象 geo
        GeoOperations geoOperations = redisTemplate.opsForGeo();
        //2.redis命令 geoadd charging:all:stations.geo id1 lng1 lat1 id2 lng2 lat2 ...
        List<RedisGeoCommands.GeoLocation> geoStations=new ArrayList<>();
        //3.循环遍历stationPO集合
        for (ChargingStationPO station : stations) {
            //从每一个StationPO中 只获取3个值,id,lng,lat
            Integer id=station.getId();
            Double lng=station.getStationLng().doubleValue();
            Double lat=station.getStationLat().doubleValue();
            //将每一个stationPO转化成能写入geo的geoLocation
            RedisGeoCommands.GeoLocation geoStation=
                    new RedisGeoCommands.GeoLocation(id,
                            new Point(lng,lat));
            //将geoLocation放到批量的列表中geoStations
            geoStations.add(geoStation);
        }
        //4.写入redis
        geoOperations.add(CacheKeyConst.GEO_STATIONS,geoStations);
    }

    @Override
    public List<StationGeoPO> nearStations(NearStationsQuery query) {
        //1.根据经纬度中心点 半径 查询geo数据 List<GeoLocation>
        //georadius charging:stations:all.geo lng lat radius m withdist withcoord
        GeoOperations<String,Integer> geoOps = redisTemplate.opsForGeo();
        //中心点 手机定位点
        Point center=new Point(query.getLongitude(),query.getLatitude());
        //半径 不添加选项单位 默认是m   添加选项 km 就是使用DistanceUnit.KILOMETERS
        Distance radius=new Distance(query.getRadius());
        //使用中心点和半径 组织一个上层入参封装类型 circle
        Circle circle=new Circle(center,radius);
        //给我组织一个携带withdist和withcoord的arguments对象
        RedisGeoCommands.GeoRadiusCommandArgs args
                = RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs();
        args.includeDistance();//withdist
        args.includeCoordinates();//withcoord
        //调用radius查询
        GeoResults<RedisGeoCommands.GeoLocation<Integer>> allResults = geoOps.radius(CacheKeyConst.GEO_STATIONS, circle, args);
        //从返回对象拿到命中的geo元素集合
        List<GeoResult<RedisGeoCommands.GeoLocation<Integer>>> results = allResults.getContent();        //2.使用geo集合转化StationGeoPO 缺少的数据,从数据库读取
        //判断集合是否为空,不空才封装pos
        if (results!=null&&results.size()>0){
            //循环遍历results,每一个result封装一个stationGeoPO
            List<StationGeoPO> pos=new ArrayList<>();
            for (GeoResult<RedisGeoCommands.GeoLocation<Integer>> result : results) {
                //能够从redis的geo集合获取的属性 stationId(geo元素值 name) point distance
                StationGeoPO po=new StationGeoPO();
                //距离distance
                po.setDistance(new BigDecimal(result.getDistance().getValue()));
                //坐标lng lat x y
                po.setStationLng(result.getContent().getPoint().getX());
                po.setStationLat(result.getContent().getPoint().getY());
                //stationId
                Integer stationId = result.getContent().getName();
                po.setStationId(stationId);
                //从数据库 查询一个完整的station对象，得到stationName和stationStatus
                ChargingStationPO stationPO = getStationById(stationId);
                //stationName和stationStatus
                po.setStationName(stationPO.getStationName());
                po.setStationStatus(stationPO.getStationStatus());
                pos.add(po);
            }
            return pos;
        }else{
            return List.of();
        }

    }

    @Override
    public String getStationName(Integer stationId) {
        return "";
    }

    @Override
    public ChargingStationPO getStationById(Integer stationId) {
        //1.读取缓存 ChargingStationPO(序列化方式 json)
        String stationKey=CacheKeyConst.STATION_DETAIL_PREFIX+stationId;
        ValueOperations<String,ChargingStationPO> valueOps = redisTemplate.opsForValue();
        //GET KEY
        ChargingStationPO stationPO = valueOps.get(stationKey);
        //判断命中
        if (stationPO!=null){
            //缓存命中直接返回结束
            return stationPO;
        }else{
            //缓存没命中,
            stationPO = stationMapper.selectById(stationId);
            //3.将数据库数据回填到缓存中
            if (stationPO==null){
                //考虑穿透,存储null值,但是超时时间设置短1分钟
                //set key value EX 60
                valueOps.set(stationKey,null,60, TimeUnit.SECONDS);
            }else{
                //set key value EX 60*60*24*2
                //存储非空po,设置超时2天
                valueOps.set(stationKey,stationPO,2,TimeUnit.DAYS);
            }
            return stationPO;
        }
    }
    @Autowired
    private GunMapper gunMapper;
    @Override
    public List<ChargingGunInfoPO> getStationGuns(Integer stationId) {
        //是否可以添加缓存 使用什么格式数据保存枪列表 list
        return gunMapper.selectByStationId(stationId);
    }

    @Override
    public Boolean updateGunStatus(Integer gunId, Integer status) {
        return null;
    }

    @Override
    public void saveStation(StationCanalPO stationCanalPO) {

    }

    @Override
    public void updateStation(StationCanalPO before, StationCanalPO after) {

    }

    @Override
    public void deleteStation(StationCanalPO stationCanalPO) {

    }

    @Override
    public Long countGunByIdAndStatus(Integer gunId, Integer status) {
        return 0l;
    }


}
