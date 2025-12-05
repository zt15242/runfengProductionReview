package other.rainbow.orderapp.button;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rkhd.platform.sdk.api.annotations.RequestMethod;
import com.rkhd.platform.sdk.api.annotations.RestApi;
import com.rkhd.platform.sdk.api.annotations.RestBeanParam;
import com.rkhd.platform.sdk.api.annotations.RestMapping;
import com.rkhd.platform.sdk.context.ScriptRuntimeContext;
import com.rkhd.platform.sdk.exception.ApiEntityServiceException;
import com.rkhd.platform.sdk.exception.ScriptBusinessException;
import com.rkhd.platform.sdk.exception.XsyHttpException;
import com.rkhd.platform.sdk.http.RkhdHttpClient;
import com.rkhd.platform.sdk.log.Logger;
import com.rkhd.platform.sdk.log.LoggerFactory;
import com.rkhd.platform.sdk.service.XObjectService;
import other.rainbow.orderapp.api.ProductSalesInfoSyncToSAP;
import other.rainbow.orderapp.api.SD005;
import other.rainbow.orderapp.common.NeoCrmRkhdService;
import other.rainbow.orderapp.pojo.ReturnResult;

import java.io.IOException;

@RestApi(baseUrl = "/button")
public class ProductCheck {

    private static final Logger logger = LoggerFactory.getLogger();
    private final static XObjectService xs = XObjectService.instance();
    @RestMapping(value = "/ProductCheck", method = RequestMethod.POST)
    public static String toSap(@RestBeanParam(name = "data") String param) throws ScriptBusinessException, IOException, InterruptedException, XsyHttpException, ApiEntityServiceException {
        logger.info("param="+param);
        JSONObject jsonObject = JSONObject.parseObject(param);
        logger.info("jsonObject="+jsonObject.toJSONString());
        String  id = jsonObject.getString("id");
        logger.info("id="+id);
        ReturnResult result = new ReturnResult();
        String type = "";
        try {
            result = ProductSalesInfoSyncToSAP.productSalesInfo(id,"ProductSalesInfoSyncToSAP");

        } catch (ApiEntityServiceException e) {
            result.setIsSuccess(false);
            result.setMessage(e.getMessage());
            throw new RuntimeException(e);
        } catch (ScriptBusinessException | InterruptedException | IOException | XsyHttpException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        logger.info("最终返回result="+JSONObject.toJSONString(result));
        return JSONObject.toJSONString(result);
    }

}
