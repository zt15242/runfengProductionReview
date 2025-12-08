package other.rainbow.cpzerooneone.trigger.CTC;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rkhd.platform.sdk.data.model.Account;
import com.rkhd.platform.sdk.data.model.Customer_Tax_Category__c;
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

public class BeforeUpdateCTCTrigger implements Trigger {
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
            String sqlaccount = "SELECT id,Is_Sync__c,Tax_Category__c,Sales_View__c  FROM Customer_Tax_Category__c WHERE id= " + xObjectList.get(0).getId();
            JSONArray accountinfo;
            try {
                accountinfo = NeoCrmRkhdService.xoql(rkhdclient, sqlaccount);
            } catch (XsyHttpException | InterruptedException e) {
                throw new RuntimeException(e);
            }
            for (XObject object : xObjectList) {
                Customer_Tax_Category__c temp = new Customer_Tax_Category__c();
                temp.setId(object.getId());
                temp.setEntityType(object.getAttribute("entityType"));
                result.add(new DataResult(true, "成功", temp));
            }
            for (int i = 0; i < accountinfo.size(); i++) {
                JSONObject jsstr = accountinfo.getJSONObject(i);
                Customer_Tax_Category__c temp = new Customer_Tax_Category__c();
                temp.setId(xObjectList.get(0).getId());
                logger.info("Is_Sync__c==" + jsstr.getString("Is_Sync__c"));
                temp.setIs_Sync__c(Objects.equals(jsstr.getString("Is_Sync__c"), "1"));
                Map<String, Integer> stringIntegerMap4 = ObjectOptionValueRetrieval.objectInformation2("Customer_Tax_Category__c","Tax_Category__c");

                for (Map.Entry<String, Integer> entry : stringIntegerMap4.entrySet()) {
                    if (accountinfo.getJSONObject(0).getJSONArray("Tax_Category__c")!=null){
                        if (Objects.equals(entry.getKey(), accountinfo.getJSONObject(0).getJSONArray("Tax_Category__c").get(0))){
                            temp.setTax_Category__c(entry.getValue());
                        }
                    }
                }
                contextList.add(temp);

            }


            logger.info("accountinfo==" + JSON.toJSONString(accountinfo));
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

