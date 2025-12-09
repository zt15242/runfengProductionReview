package other.rainbow.autoaccount.api;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rkhd.platform.sdk.api.annotations.RequestMethod;
import com.rkhd.platform.sdk.api.annotations.RestApi;
import com.rkhd.platform.sdk.api.annotations.RestBeanParam;
import com.rkhd.platform.sdk.api.annotations.RestMapping;
import com.rkhd.platform.sdk.data.model.Delivery_Note__c;
import com.rkhd.platform.sdk.exception.*;
import com.rkhd.platform.sdk.http.RkhdHttpClient;
import com.rkhd.platform.sdk.log.Logger;
import com.rkhd.platform.sdk.log.LoggerFactory;
import com.rkhd.platform.sdk.model.BatchOperateResult;
import com.rkhd.platform.sdk.model.XObject;
import com.rkhd.platform.sdk.service.XObjectService;
import other.rainbow.autoaccount.common.NeoCrmRkhdService;
import other.rainbow.autoaccount.pojo.ReturnResult;
import other.rainbow.autoaccount.service.GMDG011Controller;

import java.io.IOException;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


@RestApi(baseUrl = "/button")
public class DeliveryNoteReceived {
    private static final Logger logger = LoggerFactory.getLogger();
    private final static XObjectService xs = XObjectService.instance();
    /*
    parm:OrderId
     */
    @RestMapping(value = "/DeliveryNoteReceived", method = RequestMethod.POST)
    public String DeliveryNoteReceived(@RestBeanParam(name = "data") String param) throws AsyncTaskException, ParseException, ApiEntityServiceException {
        logger.info("param="+param);
        JSONObject jsonObject = JSONObject.parseObject(param);
        logger.info("jsonObject="+jsonObject.toJSONString());
        String  id = jsonObject.getString("id");
        logger.info("id="+id);
        ReturnResult result = new ReturnResult();
        try {
            String sqlaccount = "SELECT id,Shipping_Status__c FROM Delivery_Note__c WHERE id= " + id;
            JSONArray accountinfo;
            RkhdHttpClient rkhdclient = RkhdHttpClient.instance();
            accountinfo = NeoCrmRkhdService.xoql(rkhdclient, sqlaccount);
            JSONObject jsstr = accountinfo.getJSONObject(0);
            logger.info("Shipping_Status__c:"+jsstr.getString("Shipping_Status__c"));
            if (Objects.equals(jsstr.getString("Shipping_Status__c"), "1")){
                List<XObject> updateList = new ArrayList<>();
                Delivery_Note__c dn = new Delivery_Note__c();
                dn.setId(Long.parseLong(id));
                dn.setReceiving_Status__c(true);
                dn.setReceiving_Record_OperationTime__c(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
                updateList.add(dn);
                BatchOperateResult batchResult = XObjectService.instance().update(updateList);
                logger.info("batchResult1"+batchResult.getSuccess());
                logger.info("batchResult2"+batchResult.getErrorMessage());
                logger.info("batchResult3"+batchResult.getCode());
                if (batchResult.getSuccess()){
                    result.setIsSuccess(true);
                }else {
                    result.setIsSuccess(false);
                    result.setMessage(batchResult.getErrorMessage());
                }

            }else {
                result.setIsSuccess(false);
                result.setMessage("未发货，不能收货");
                return JSONObject.toJSONString(result);
            }

        } catch (XsyHttpException | InterruptedException | ScriptBusinessException | IOException |
                 ApiEntityServiceException e) {
            result.setIsSuccess(false);
            result.setMessage(e.getMessage());
            throw new RuntimeException(e);
        }
        return JSONObject.toJSONString(result);
    }
    @RestMapping(value = "/ResyncAccount", method = RequestMethod.POST)
    public String ButtonResyncAccount(@RestBeanParam(name = "data") String param) throws AsyncTaskException, ParseException, ApiEntityServiceException {
        logger.info("param="+param);
        JSONObject jsonObject = JSONObject.parseObject(param);
        logger.info("jsonObject="+jsonObject.toJSONString());
        String  id = jsonObject.getString("id");
        logger.info("id="+id);
        String sqlaccount = "SELECT id,Customer_Approval_Status__c,Sync_Status__c,ownerId.name,Language__c,Customer_Group__c,Customer_Short_Name__c,accountName,Group_Name__c " +
                ",Tax_Identification__c,Tax_Number__c,Tax_Number__c,Continent__c,Region__c,Country__c.Code__c," +
                " Province_State__c.Code__c,City__c,Street__c,Postal_Code__c,SAP_Customer_Type__c,Contact_Name__c," +
                " Telephone__c,Email__c,fax,MDG_Customer_Code__c  FROM account WHERE id= " + id;
        JSONArray accountinfo;
        ReturnResult result = new ReturnResult();
        try {
            RkhdHttpClient rkhdclient = RkhdHttpClient.instance();
            accountinfo = NeoCrmRkhdService.xoql(rkhdclient, sqlaccount);

            for (Integer i = 0; i < accountinfo.size(); i++) {
                JSONObject jsstr = accountinfo.getJSONObject(i);
//                if (!Objects.equals(jsstr.getJSONArray("Sync_Status__c").get(0).toString(), "Synchronization Failure")){
//                    result.setMessage("can not resync data");
//                    result.setIsSuccess(false);
//                    return JSONObject.toJSONString(result);
//                }
                logger.info("jsstr.getString(\"MDG_Customer_Code__c\")="+jsstr.getString("MDG_Customer_Code__c"));
                if (jsstr.getString("MDG_Customer_Code__c") !=null && !Objects.equals(jsstr.getString("MDG_Customer_Code__c"), "")){
                    // 触发接口
                    result = GMDG011Controller.sendGMDG011(id,"U");
                }else{
                    result = GMDG011Controller.sendGMDG011(id,"I");
                }
            }
        } catch (XsyHttpException | InterruptedException | ScriptBusinessException | IOException |
                 ApiEntityServiceException e) {
            result.setIsSuccess(false);
            result.setMessage(e.getMessage());
            throw new RuntimeException(e);
        } catch (CustomConfigException e) {
            throw new RuntimeException(e);
        }
        logger.info("最终返回result="+JSONObject.toJSONString(result));
        return JSONObject.toJSONString(result);
    }
    @RestMapping(value = "/ResyncCustomerSalesInfo", method = RequestMethod.POST)
    public String ButtonResyncCustomerSalesInfo (@RestBeanParam(name = "data") String param) throws AsyncTaskException, ParseException, ApiEntityServiceException {
        logger.info("param="+param);
        JSONObject jsonObject = JSONObject.parseObject(param);
        logger.info("jsonObject="+jsonObject.toJSONString());
        String  id = jsonObject.getString("id");
        logger.info("id="+id);
        String sqlCustomerSales = "SELECT id,isSync__c  FROM Customer_Sales_Info__c WHERE id= " + id;
        String sqlctc = "SELECT id,Tax_Category__c  FROM Customer_Tax_Category__c WHERE Sales_View__c= " + id;
        JSONArray sqlCustomerSalesinfo;
        JSONArray ctcinfo;
        ReturnResult result = new ReturnResult();
        try {
            RkhdHttpClient rkhdclient = RkhdHttpClient.instance();
            sqlCustomerSalesinfo = NeoCrmRkhdService.xoql(rkhdclient, sqlCustomerSales);
            ctcinfo = NeoCrmRkhdService.xoql(rkhdclient, sqlctc);

            for (Integer i = 0; i < ctcinfo.size(); i++) {
                JSONObject jsstr = ctcinfo.getJSONObject(i);

                if (jsstr.getJSONArray("Tax_Category__c")==null){
                    result.setIsSuccess(false);
                    result.setMessage("[Customer Tax Category] is required, please fill in before approval");
                    return JSONObject.toJSONString(result);
                }
            }

            for (Integer i = 0; i < sqlCustomerSalesinfo.size(); i++) {
                JSONObject jsstr = sqlCustomerSalesinfo.getJSONObject(i);

                if (Objects.equals(jsstr.getString("isSync__c"), "1")){
                    // 触发接口
                    result = GMDG011Controller.sendCustomerSales(id,"U");
                }else{
                    result = GMDG011Controller.sendCustomerSales(id,"I");
                }
            }


        } catch (XsyHttpException | InterruptedException | ScriptBusinessException | IOException |
                 ApiEntityServiceException e) {
            result.setIsSuccess(false);
            result.setMessage(e.getMessage());
            throw new RuntimeException(e);
        } catch (CustomConfigException e) {
            throw new RuntimeException(e);
        }
        logger.info("最终返回result="+JSONObject.toJSONString(result));
        return JSONObject.toJSONString(result);
    }
}
