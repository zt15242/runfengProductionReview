package other.rainbow.orderapp.api;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rkhd.platform.sdk.api.annotations.RequestMethod;
import com.rkhd.platform.sdk.api.annotations.RestApi;
import com.rkhd.platform.sdk.api.annotations.RestBeanParam;
import com.rkhd.platform.sdk.api.annotations.RestMapping;
import com.rkhd.platform.sdk.context.ScriptRuntimeContext;
import com.rkhd.platform.sdk.data.model.Delivery_Note__c;
import com.rkhd.platform.sdk.data.model.Order;
import com.rkhd.platform.sdk.exception.ApiEntityServiceException;
import com.rkhd.platform.sdk.exception.AsyncTaskException;
import com.rkhd.platform.sdk.exception.ScriptBusinessException;
import com.rkhd.platform.sdk.exception.XsyHttpException;
import com.rkhd.platform.sdk.http.RkhdHttpClient;
import com.rkhd.platform.sdk.log.Logger;
import com.rkhd.platform.sdk.log.LoggerFactory;
import com.rkhd.platform.sdk.model.BatchOperateResult;
import com.rkhd.platform.sdk.model.XObject;
import com.rkhd.platform.sdk.service.XObjectService;
import other.rainbow.orderapp.common.NeoCrmRkhdService;
import other.rainbow.orderapp.pojo.ReturnResult;

import java.io.IOException;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

// 产品同步发送致SAP
@RestApi(baseUrl = "/button")
public class ProductSyncToSAPApi {
    private static final Logger logger = LoggerFactory.getLogger();
    private final static XObjectService xs = XObjectService.instance();
    /*
    parm:OrderId
     */
    @RestMapping(value = "/ResyncProduct", method = RequestMethod.POST)
    public String ButtonResyncAccount(@RestBeanParam(name = "data") String param) throws AsyncTaskException, ParseException, ApiEntityServiceException {
        logger.info("param="+param);
        JSONObject jsonObject = JSONObject.parseObject(param);
        logger.info("jsonObject="+jsonObject.toJSONString());
        String  id = jsonObject.getString("id");
        logger.info("id="+id);
        ReturnResult result = new ReturnResult();
        try {
            String sqlaccount = "SELECT id,Shipping_Status__c,Shipper__c,Shipping_Record_Operation_Time__c FROM order WHERE id= " + id;
            JSONArray accountinfo;
            RkhdHttpClient rkhdclient = RkhdHttpClient.instance();
            accountinfo = NeoCrmRkhdService.xoql(rkhdclient, sqlaccount);
            JSONObject jsstr = accountinfo.getJSONObject(0);
            ScriptRuntimeContext src = ScriptRuntimeContext.instance();
            long userId = src.getUserId();

            List<XObject> updateList = new ArrayList<>();
            Order dn = new Order();
            dn.setId(Long.parseLong(id));
            dn.setShipper__c(userId);
            dn.setShipping_Status__c(true);
            dn.setShipping_Record_Operation_Time__c(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());

            updateList.add(dn);
            BatchOperateResult batchResult = XObjectService.instance().update(updateList);

            SD005 tosap = new SD005(id,"U",true,false,"","");
            result = tosap.returnResult;

        } catch (
                ApiEntityServiceException e) {
            result.setIsSuccess(false);
            result.setMessage(e.getMessage());
            throw new RuntimeException(e);
        } catch (ScriptBusinessException | InterruptedException | IOException | XsyHttpException e) {
            throw new RuntimeException(e);
        }
        logger.info("最终返回result="+JSONObject.toJSONString(result));
        return JSONObject.toJSONString(result);
    }
}
