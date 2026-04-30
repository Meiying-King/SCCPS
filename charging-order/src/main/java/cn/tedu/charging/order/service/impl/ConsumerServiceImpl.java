package cn.tedu.charging.order.service.impl;

import cn.tedu.charging.common.pojo.message.CheckResultMessage;
import cn.tedu.charging.common.pojo.message.DelayCheckMessage;
import cn.tedu.charging.common.pojo.message.ProgressData;
import cn.tedu.charging.common.pojo.message.ProgressMessage;
import cn.tedu.charging.common.pojo.param.ProgressCostParam;
import cn.tedu.charging.common.pojo.vo.ProgressCostVO;
import cn.tedu.charging.common.protocol.JsonResult;
import cn.tedu.charging.common.protocol.WebSocketResult;
import cn.tedu.charging.common.utils.SnowflakeIdGenerator;
import cn.tedu.charging.common.utils.TimeConverterUtil;
import cn.tedu.charging.order.clients.CostClient;
import cn.tedu.charging.order.dao.repository.BillRepository;
import cn.tedu.charging.order.dao.repository.ProcessEsRepository;
import cn.tedu.charging.order.pojo.po.ChargingBillFailPO;
import cn.tedu.charging.order.pojo.po.ChargingBillSuccessPO;
import cn.tedu.charging.order.pojo.po.ChargingProgressEsPO;
import cn.tedu.charging.order.server.points.WebsocketServerPoint;
import cn.tedu.charging.order.service.ConsumerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketMessage;

import java.io.IOException;
import java.util.Date;

@Service
@Slf4j
public class ConsumerServiceImpl implements ConsumerService {
    @Autowired
    private BillRepository billRepository;
    @Autowired
    private WebsocketServerPoint websocketServerPoint;
    @Autowired
    private CostClient costClient;
    @Autowired
    private SnowflakeIdGenerator snowflakeIdGenerator;
    @Autowired
    private ProcessEsRepository processEsRepository;
    @Override
    public void handlerCheckResult(CheckResultMessage msg) {
        //准备一个即将给前端推送的消息统一格式
        WebSocketResult<String> websocketResult=new WebSocketResult<>();
        //1.读取result自检结果
        Boolean result=msg.getResult();
        if (result){
            //如果自检成功 则更新订单状态为 已自检成功
            log.info("订单{}的充电自检成功",msg.getOrderNo());
            //检查成功是否存在
            Long count=billRepository.countSuccess(msg.getOrderNo());
            if (count>0){
                log.warn("订单{}的充电自检成功,但之前已存在成功记录",msg.getOrderNo());
                return;
            }
            //2.组织成功po写入数据库
            saveBillSuccess(msg);
            //3.组织通知用户马上开充的消息,state 1 2 3只有等于1的时候才使用data封装String
            websocketResult.setState(1);
            websocketResult.setMessage("ok");
            websocketResult.setData("您的订单马上开始充电");
        }else{
            //如果自检失败 则更新订单状态为 自检失败
            log.info("订单{}的充电自检失败,失败原因:{}",msg.getOrderNo(),msg.getFailDesc());
            Long count=billRepository.countFail(msg.getOrderNo());
            if (count>0){
                log.warn("订单{}的充电自检失败,但之前已存在失败记录",msg.getOrderNo());
                return;
            }
            //4.组织失败po写入数据库
            saveBillFail(msg);
            //5.组织通知用户换枪走人
            websocketResult.setState(1);
            websocketResult.setMessage("ok");
            websocketResult.setData("对不起,您的订单设备启动失败,请换枪走人,送您一张优惠券http://30f329");
            //UNDO 调用优惠券的业务接口,给该用户生成一张可领取的券码
            //6.调用设备服务 修改枪状态为故障
        }
        //实现消息推送
        try {
            websocketServerPoint.pushMsg(msg.getUserId(),websocketResult);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void handleChargingProgress(ProgressMessage msg) {
        //1.检查充电当前情况是否满足安全要求--温度 轻做,永远返回true 没问题
        Boolean tempCheck=checkTemperature(msg.getTemperature());
        //2.调用计价中心计算充电金额
        ProgressCostVO progressCostVO=calculateCost(msg);
        //3.组织用户订单充电详情 记录到ES 中
        saveChargingProgress(msg,progressCostVO);
        //4.根据充电进度信息 推送用户展示充电详情
        sendProgress2User(msg,progressCostVO);
        //判断是否充满 如果没充满,继续,如果充满了,停止充电结束订单
        if (msg.getIsFull()){
            log.debug("订单充电结束");
            //5.更新订单状态
            updateSuccess(msg);
            //6.生成用户消息 提示结束 UNDO
            //7.给设备发送命令 断电 UNDO
        }
    }

    private void sendProgress2User(ProgressMessage msg, ProgressCostVO progressCostVO) {
        //1.组织一个消息对象,将progressData封装
        //1.1创建对象准备封装
        WebSocketResult<ProgressData> messageResult=new WebSocketResult<ProgressData>();
        messageResult.setState(3);//如果想让前端按照充电进度的数据展示 状态码需要时3 1字符串数据 2欠费 3充电进度
        messageResult.setMessage("充电进度详情");
        //1.2progressData 封装结果
        ProgressData data=new ProgressData();
        //总度数,总金额,单次度数,单价从入参拿出来封装
        data.setTotalCapacity(msg.getTotalCapacity());
        data.setTotalCost(progressCostVO.getTotalCost());
        data.setChargingCapacity(progressCostVO.getChargingCapacity());
        data.setOneElectricityCost(progressCostVO.getPowerFee());
        //利用时间工具 将消息中totalTime毫秒数 计算充电小时 分钟 秒单位
        data.setHours(TimeConverterUtil.getHour(msg.getTotalTime()).intValue());//一般情况下 某个车辆充电时到不了小时
        data.setMinutes(TimeConverterUtil.getMinute(msg.getTotalTime()).intValue());
        data.setSeconds(TimeConverterUtil.getSecond(msg.getTotalTime()).intValue());
        messageResult.setData(data);
        //2.使用终端推送 msg携带的用户id就是推送的目标
        try {
            websocketServerPoint.pushMsg(msg.getUserId(),messageResult);
        } catch (IOException e) {
            log.error("推送充电进度消息失败,用户id:{}",msg.getUserId(),e);
            throw new RuntimeException(e);
        }
    }

    private void saveChargingProgress(ProgressMessage msg, ProgressCostVO progressCostVO) {
        //1.创建一个PO对象
        ChargingProgressEsPO progressEsPO=new ChargingProgressEsPO();
        //1.1 能从消息拷贝的,先拷贝
        if (msg!=null){
            BeanUtils.copyProperties(msg,progressEsPO);
        }else{
            log.error("设备同步充电进度消息为空,无法保存进度数据");
            throw new RuntimeException("设备同步进度消息不可用");
        }
        //1.2补充单次充电度数 以及截止到目前的总金额 capacity totaoCost
        if (progressCostVO!=null){
            BeanUtils.copyProperties(progressCostVO,progressEsPO);
        }else{
            log.error("订单调用计价结果为空,无法保存进度数据");
            throw new RuntimeException("订单调用计价结果不可用");
        }
        //1.3生成雪花算法id 保证每条进度数据 是时序的,添加一个前缀避免和某个订单的编号重复的
        String id="P"+snowflakeIdGenerator.nextId();
        progressEsPO.setId(id);
        //2.写入es
        processEsRepository.save(progressEsPO);
    }

    private ProgressCostVO calculateCost(ProgressMessage msg) {
        //1.使用入参的消息 组织一个调用计价的param
        ProgressCostParam param=new ProgressCostParam();
        if (msg!=null){
            BeanUtils.copyProperties(msg,param);
        }else{
            log.error("设备同步充电进度消息为空,无法调用计价服务");
            throw new RuntimeException("设备同步进度消息不可用");
        }
        //2.发起调用得到结果
        JsonResult<ProgressCostVO> result = costClient.calculateBill(param);
        //3.解析结果 openFeign调用 什么条件下的结果是正常可用的 code==0
        if (result!=null&&result.getCode()!=null&&result.getCode()==0){
            log.debug("订单调用计价结果正常,结果的值:{}",result.getData());
            return result.getData();//progressCostVO
        }else{
            log.error("订单调用计价中心失败,失败原因:{}",result.getMessage());
            throw new RuntimeException("调用计价服务失败");
        }
    }

    private void updateSuccess(ProgressMessage msg) {
        //在结束充电的时候,更新数据很多 状态 结束充电时间 充电度数等
        //1.组织一个更新的po对象,
        ChargingBillSuccessPO success=new ChargingBillSuccessPO();
        success.setUpdateTime(new Date());
        success.setBillStatus(2);//正常结束
        success.setChargingCapacity(msg.getTotalCapacity().intValue());
        success.setChargingDuration(msg.getTotalTime().intValue());
        success.setChargingEndTime(new Date());
        success.setBillId(msg.getOrderNo());//更新查询的条件
        //2.交给仓储层处理更新
        billRepository.updateSuccess(success);
    }

    private Boolean checkTemperature(Double temperature) {
        //要查看设备温度在哪一个范围 0~99c正常 99~200 报警 200~500 立刻断电
        if (temperature<10000d){
            log.debug("温度满足安全条件,正常");
            return true;
        }else {
            log.error("温度不满足安全条件,当前温度为:{}",temperature);
            return false;
        }
    }

    private void saveBillFail(CheckResultMessage msg) {
        //1.组织一个失败订单
        ChargingBillFailPO fail=new ChargingBillFailPO();
        fail.setBillId(msg.getOrderNo());
        fail.setGunId(msg.getGunId());
        fail.setUserId(msg.getUserId());
        fail.setCreateTime(new Date());
        fail.setUpdateTime(fail.getCreateTime());
        fail.setDeleted(0);//逻辑删除
        //失败原因
        fail.setFailDesc(msg.getFailDesc());
        //2.写入数据库
        billRepository.saveFail(fail);
    }

    private void saveBillSuccess(CheckResultMessage msg) {
        //1.准备一个success对象
        ChargingBillSuccessPO success=new ChargingBillSuccessPO();
        //1.1success组织完整属性 可以当前启动阶段结果中补充的属性
        success.setBillId(msg.getOrderNo());
        success.setGunId(msg.getGunId());
        success.setUserId(msg.getUserId());
        success.setBillStatus(1);
        success.setChargingStartTime(new Date());
        success.setCreateTime(success.getChargingStartTime());
        success.setUpdateTime(success.getChargingStartTime());
        success.setDeleted(0);//逻辑删除
        //1.2operatorId stationId 可以通过gunId查询 冗余字段
        //2.调用仓储层写入 insert
        billRepository.saveSuccess(success);
    }

    @Override
    public void handleCheckNoRes(DelayCheckMessage msg) {
        //1.利用消息数据orderNo检查成功
        String billId=msg.getOrderNo();
        Long sccuessCount = billRepository.countSuccess(billId);
        if (sccuessCount>0){
            log.debug("订单{}的充电自检成功,已存在成功记录,不再处理",billId);
            return;
        }
        //2.检查失败
        Long failCount = billRepository.countFail(billId);
        if (failCount>0){
            log.debug("订单{}的充电自检失败,已存在失败记录,不再处理",billId);
            return;
        }
        //3.组织失败数据写入失败信息 原因:设备无响应
        ChargingBillFailPO fail=new ChargingBillFailPO();
        fail.setBillId(msg.getOrderNo());
        fail.setGunId(msg.getGunId());
        fail.setUserId(msg.getUserId());
        fail.setCreateTime(new Date());
        fail.setUpdateTime(fail.getCreateTime());
        fail.setDeleted(0);//逻辑删除
        fail.setFailDesc("设备无响应");
        //2.写入数据库
        billRepository.saveFail(fail);
        //手动组织一个失败对象写入数据库
        //TODO 4.通知设备服务device检查设备的情况,根据实际情况判断是否要修改记录设备状态数据
        //5.组织消息让用户知道情况(到当前为止,用户已经等待了1分钟了)
        WebSocketResult websocketResult=new WebSocketResult<>();
        websocketResult.setState(1);
        websocketResult.setMessage("ok");
        websocketResult.setData("久等了,您的订单启动失败,sorry");
        try {
            websocketServerPoint.pushMsg(msg.getUserId(),websocketResult);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


}
