package other.rainbow.orderapp.tsts;

import com.rkhd.platform.sdk.exception.CustomConfigException;
import com.rkhd.platform.sdk.exception.XsyHttpException;
import com.rkhd.platform.sdk.http.CommonData;
import com.rkhd.platform.sdk.http.CommonHttpClient;
import com.rkhd.platform.sdk.http.CommonResponse;
import com.rkhd.platform.sdk.log.Logger;
import com.rkhd.platform.sdk.log.LoggerFactory;
import com.rkhd.platform.sdk.service.CustomConfigService;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

public class TextMessageRestReq {

    private static final Logger LOG = LoggerFactory.getLogger();

    private static TextMessageRestReq singleton = new TextMessageRestReq();
    public static TextMessageRestReq instance() {
        return singleton;
    }

    public Boolean sendTextMessageService(String phoneNumber, String textMessage) throws CustomConfigException, XsyHttpException, UnsupportedEncodingException {

        CustomConfigService customConfigService = CustomConfigService.instance();
        Map<String, String> configProperties = null;

        configProperties = customConfigService.getConfigSet("message_properties");
        String url =  "http://wgapi.jzyyun.com:9008/servlet/UserServiceAPI?method=sendSMS&isLongSms=0&username=8178TIdr_0&password=morzl32z" + "&mobile=" + phoneNumber + "&content=" + URLEncoder.encode("【Rainbow】" + textMessage,"utf-8");
        if(configProperties != null) {
            url = configProperties.get("textmessage") + "&mobile=" + phoneNumber + "&content=" + URLEncoder.encode("【Rainbow】" + textMessage,"utf-8");
        }

        CommonData data = CommonData.newBuilder().callString(url)
                .callType("GET")
                .build();
        CommonResponse<Boolean> result = CommonHttpClient.instance(15000,15000).execute(data, (dataString) -> dataString.startsWith("success;"));

        return result.getData();
    }
}
