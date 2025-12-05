package other.rainbow.orderapp.cstss;

import com.rkhd.platform.sdk.context.ScriptRuntimeContext;
import com.rkhd.platform.sdk.exception.CustomConfigException;
import com.rkhd.platform.sdk.exception.XsyHttpException;
import com.rkhd.platform.sdk.http.CommonData;
import com.rkhd.platform.sdk.http.CommonHttpClient;
import com.rkhd.platform.sdk.http.CommonResponse;
import com.rkhd.platform.sdk.http.handler.ResponseBodyHandler;
import com.rkhd.platform.sdk.log.Logger;
import com.rkhd.platform.sdk.log.LoggerFactory;
import com.rkhd.platform.sdk.service.CustomConfigService;

import java.util.Base64;
import java.util.Map;

public class SapRestReq<T> {

    private static final Logger LOG = LoggerFactory.getLogger();

    private static SapRestReq singleton = new SapRestReq();
    public static SapRestReq instance() {
        return singleton;
    }

    public CommonResponse<T> send_SAP_POST_Service(String bodyStrJson, String endpoint, ResponseBodyHandler<T> handler) throws XsyHttpException, CustomConfigException {

        CustomConfigService customConfigService = CustomConfigService.instance();
        Map<String, String> configProperties = null;

        configProperties = customConfigService.getConfigSet("sap_properties");
        String username = "xsy-crm01";
        String password = "Abcd1234";
        String endpointBase = "http://localhost:8080/";
        // String endpointBase = "http://120.224.116.35:8901";
        String getEnviromentFlag = "";
        String url =  endpointBase + endpoint + getEnviromentFlag;
        Base64.Encoder encoder = Base64.getEncoder();
        LOG.info("base64加密前：" + username + ":" + password);
        String authorization = encoder.encodeToString((username + ":" + password).getBytes());
        LOG.info("base64加密后：" + authorization);
        String authorizationsBase64 = "Basic "+ authorization;
        if(configProperties != null) {
//            username = configProperties.get("username");
//            password = configProperties.get("password");
//            endpointBase = configProperties.get("endpoint");
            // getEnviromentFlag = configProperties.get("env");
            url = configProperties.get("cstss");
            authorizationsBase64 = configProperties.get("Authorization");
        }

        CommonData data = CommonData.newBuilder().callString(url)
                .callType("POST")
                .header("Content-Type", "application/json")
                .header("Authorization", authorizationsBase64)
                .body(bodyStrJson)
                .build();
        CommonResponse<T> result = CommonHttpClient.instance(15000,15000).execute(data,handler);

        return result;
    }

    public static String getEnviromentFlag() {
        ScriptRuntimeContext scriptRuntimeContext = ScriptRuntimeContext.instance();
        String name = scriptRuntimeContext.getEnvInfo().getName();
        return name;
    }
}
