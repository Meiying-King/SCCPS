package cn.tedu.charging.user.utils;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class WeChatLoginUtil {

    @Data
    public static class WxResponse {
        private String openid;
        private String session_key;
        private String unionid;
        private Integer errcode;
        private String errmsg;
    }

    public static String getOpenId(String appId, String secret, String jsCode) {
        try {
            // 构建URL
            String urlStr = String.format(
                    "https://api.weixin.qq.com/sns/jscode2session?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code",
                    appId, secret, jsCode
            );

            System.out.println("请求URL: " + urlStr.replace(secret, "***"));

            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            int responseCode = conn.getResponseCode();
            System.out.println("响应码: " + responseCode);

            if (responseCode == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String inputLine;

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                System.out.println("响应内容: " + response);

                ObjectMapper mapper = new ObjectMapper();
                WxResponse wxResponse = mapper.readValue(response.toString(), WxResponse.class);

                if (wxResponse.getErrcode() == null || wxResponse.getErrcode() == 0) {
                    return wxResponse.getOpenid();
                } else {
                    System.err.println("微信接口错误: " + wxResponse.getErrmsg());
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public static void main(String[] args) {
        String openId = getOpenId("your_app_id", "your_app_secret", "your_js_code");
        System.out.println("openId: " + openId);
    }
}