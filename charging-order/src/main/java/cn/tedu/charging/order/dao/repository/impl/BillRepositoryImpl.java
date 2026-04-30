package cn.tedu.charging.order.dao.repository.impl;

import cn.tedu.charging.order.dao.mapper.BillExceptionMapper;
import cn.tedu.charging.order.dao.mapper.BillFailMapper;
import cn.tedu.charging.order.dao.mapper.BillSuccessMapper;
import cn.tedu.charging.order.dao.repository.BillRepository;
import cn.tedu.charging.order.pojo.po.ChargingBillExceptionPO;
import cn.tedu.charging.order.pojo.po.ChargingBillFailPO;
import cn.tedu.charging.order.pojo.po.ChargingBillSuccessPO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
@Slf4j
public class BillRepositoryImpl implements BillRepository {
    @Autowired
    private BillSuccessMapper successMapper;
    @Autowired
    private BillFailMapper failMapper;
    @Autowired
    private BillSuccessMapper billSuccessMapper;
    @Autowired
    private BillFailMapper billFailMapper;
    @Autowired
    private BillExceptionMapper billExceptionMapper;

    @Override
    public void saveFail(ChargingBillFailPO fail) {
        //charging_bill_fail
        failMapper.insert(fail);
    }

    @Override
    public void saveSuccess(ChargingBillSuccessPO success) {
        //charging_bill_success
        successMapper.insert(success);
    }

    @Override
    public Long countSuccess(String orderNo) {
        QueryWrapper queryWrapper=new QueryWrapper();
        queryWrapper.eq("bill_id",orderNo);
        return billSuccessMapper.selectCount(queryWrapper);
    }

    @Override
    public Long countFail(String orderNo) {
        QueryWrapper queryWrapper=new QueryWrapper();
        queryWrapper.eq("bill_id",orderNo);
        return billFailMapper.selectCount(queryWrapper);
    }

    @Override
    public ChargingBillSuccessPO getSuccessByBillId(String billId) {
        //select * from charging_success_bill where bill_id=?
        QueryWrapper queryWrapper=new QueryWrapper();
        queryWrapper.eq("bill_id",billId);
        return billSuccessMapper.selectOne(queryWrapper);
    }

    @Override
    public void updateSuccessStatus(Integer id, Integer status) {
        //update charging_success_bill set bill_status=? where id=?
        ChargingBillSuccessPO successPO=new ChargingBillSuccessPO();
        successPO.setId(id);
        successPO.setBillStatus(status);
        billSuccessMapper.updateById(successPO);
    }

    @Override
    public void saveException(ChargingBillExceptionPO exception) {
        billExceptionMapper.insert(exception);
    }

    @Override
    public void updateSuccess(ChargingBillSuccessPO success) {
        //update success set success除了billId以外的非空字段 where bill_id=?
        //定义一个查询条件queryWrapper 拼接where 使用success的非空定义set
        QueryWrapper queryWrapper=new QueryWrapper<>();
        queryWrapper.eq("bill_id",success.getBillId());
        //调用更新 使用queryWrapper拼接update的where语句,使用success中非空属性拼接set
        success.setBillId(null);
        billSuccessMapper.update(success,queryWrapper);
    }
}
