package cn.tedu.test.charging.cron;

import cn.tedu.charging.common.utils.CronUtil;
import cn.tedu.charging.common.utils.XxlJobTaskUtil;

public class CronTest {
    public static void main(String[] args) {
        //直接使用cronUtil 工具类生成cron表达式
        String cronExpression = CronUtil.delayCron(1000 * 60 * 60 * 18);
        System.out.println(cronExpression);
        //准备一个 orderNo参数和执行器名称
        String orderNo = "20220301";
        String executorName = "order-executor";
        XxlJobTaskUtil.createJobTask(cronExpression,executorName,orderNo);
    }
}
