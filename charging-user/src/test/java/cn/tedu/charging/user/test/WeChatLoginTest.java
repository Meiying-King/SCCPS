package cn.tedu.charging.user.test;

import cn.tedu.charging.common.constant.AppAuthConst;
import cn.tedu.charging.user.utils.WeChatLoginUtil;

public class WeChatLoginTest {
    public static void main(String[] args) {
        String openId = WeChatLoginUtil.getOpenId(AppAuthConst.APP_ID, AppAuthConst.APP_SECRET, "0b1owt000hscqV1X3H200mWIHt0owt0C");
        if (openId != null) {
            System.out.println("openId = " + openId);
        }else {
            System.out.println("获取openId失败");
        }
    }
}
