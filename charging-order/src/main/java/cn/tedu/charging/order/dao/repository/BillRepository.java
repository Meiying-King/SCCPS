package cn.tedu.charging.order.dao.repository;

import cn.tedu.charging.order.pojo.po.ChargingBillExceptionPO;
import cn.tedu.charging.order.pojo.po.ChargingBillFailPO;
import cn.tedu.charging.order.pojo.po.ChargingBillSuccessPO;

public interface BillRepository {
    void saveFail(ChargingBillFailPO fail);

    void saveSuccess(ChargingBillSuccessPO success);

    Long countSuccess(String orderNo);

    Long countFail(String orderNo);

    ChargingBillSuccessPO getSuccessByBillId(String billId);

    void updateSuccessStatus(Integer id, Integer status);

    void saveException(ChargingBillExceptionPO exception);

    void updateSuccess(ChargingBillSuccessPO success);
}
