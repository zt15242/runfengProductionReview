package other.rainbow.cpzerooneone.trigger.CSI;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rkhd.platform.sdk.data.model.Customer_Sales_Info__c;
import com.rkhd.platform.sdk.exception.ScriptBusinessException;
import com.rkhd.platform.sdk.exception.TriggerContextException;
import com.rkhd.platform.sdk.exception.XsyHttpException;
import com.rkhd.platform.sdk.http.RkhdHttpClient;
import com.rkhd.platform.sdk.log.Logger;
import com.rkhd.platform.sdk.log.LoggerFactory;
import com.rkhd.platform.sdk.model.XObject;
import com.rkhd.platform.sdk.trigger.*;
import other.rainbow.cpzerooneone.common.NeoCrmRkhdService;
import other.rainbow.cpzerooneone.common.ObjectOptionValueRetrieval;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class BeforeUpdateCSITrigger implements Trigger {
    private static final Logger logger = LoggerFactory.getLogger();
    @Override
    public TriggerResponse execute(TriggerRequest triggerRequest) throws ScriptBusinessException {
        String msg = "";
        boolean success = true;
        TriggerContext triggerContext = new TriggerContext();
        RkhdHttpClient rkhdclient = null;
        List<DataResult> result = null;
        try {
            rkhdclient = RkhdHttpClient.instance();
            List<XObject> xObjectList = triggerRequest.getDataList();
            List<XObject> contextList = new ArrayList<>();
            result = new ArrayList<>();
            String sqlcsi = "SELECT id,Country__c,isSync__c,Sync_Status__c,Customer__c,Sales_View_Status__c,Customer__c,Customer__c.MDG_Customer_Code__c FROM Customer_Sales_Info__c WHERE id= " + xObjectList.get(0).getId();
//            String sqlaccount = "SELECT id,Is_Sync__c,Tax_Category__c,Sales_View__c  FROM Customer_Sales_Info__c WHERE id= " + xObjectList.get(0).getId();
            JSONArray csiinfo;
            try {
                csiinfo = NeoCrmRkhdService.xoql(rkhdclient, sqlcsi);
            } catch (XsyHttpException | InterruptedException e) {
                throw new RuntimeException(e);
            }
            for (XObject object : xObjectList) {
                Customer_Sales_Info__c temp = new Customer_Sales_Info__c();
                temp.setId(object.getId());
                temp.setEntityType(object.getAttribute("entityType"));
                result.add(new DataResult(true, "成功", temp));
            }
            for (Integer i = 0; i < csiinfo.size(); i++) {
                JSONObject jsstr = csiinfo.getJSONObject(i);
                Customer_Sales_Info__c temp = new Customer_Sales_Info__c();
                temp.setId(xObjectList.get(0).getId());

                temp.setIsSync__c(Objects.equals(jsstr.getString("isSync__c"), "1"));
                temp.setCountry__c(Long.parseLong(jsstr.getString("Country__c")));
                Map<String, Integer> stringIntegerMap4 = ObjectOptionValueRetrieval.objectInformation2("Customer_Sales_Info__c","Sales_View_Status__c");
                for (Map.Entry<String, Integer> entry : stringIntegerMap4.entrySet()) {
                    if ( csiinfo.getJSONObject(0).getJSONArray("Sales_View_Status__c") !=null){
                        if (Objects.equals(entry.getKey(), csiinfo.getJSONObject(0).getJSONArray("Sales_View_Status__c").get(0))){
                            temp.setSales_View_Status__c(entry.getValue());
                        }
                    }
                }
                Map<String, Integer> stringIntegerMap5 = ObjectOptionValueRetrieval.objectInformation2("Customer_Sales_Info__c","Sync_Status__c");
                for (Map.Entry<String, Integer> entry : stringIntegerMap5.entrySet()) {
                    if ( csiinfo.getJSONObject(0).getJSONArray("Sync_Status__c") !=null){
                        if (Objects.equals(entry.getKey(), csiinfo.getJSONObject(0).getJSONArray("Sync_Status__c").get(0))){
                            temp.setSync_Status__c(entry.getValue());
                        }
                    }
                }
                contextList.add(temp);

            }


            logger.info("xObjectList==" + JSON.toJSONString(xObjectList));
            logger.info("csiinfo==" + JSON.toJSONString(csiinfo));
            logger.info("result==" + result);
            logger.info("contextList==" + JSON.toJSONString(contextList));
            triggerContext.set("oldList", JSON.toJSONString(contextList));
            msg = "成功";
        } catch (TriggerContextException | IOException e) {
            msg = "失败"+e.getMessage();
            success = false;
//            throw new RuntimeException(e);
        }

        return new TriggerResponse(success, msg, result, triggerContext);
    }
}
