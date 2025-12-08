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

public class BeforeAddCSITrigger implements Trigger {
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
            for (XObject object : xObjectList) {
                Customer_Sales_Info__c temp = new Customer_Sales_Info__c();
//                temp.setId(object.getId());
                temp.setEntityType(ObjectOptionValueRetrieval.getEntityTypesId(rkhdclient,"Customer_Sales_Info__c","defaultBusiType"));
                result.add(new DataResult(true, "成功", temp));
            }
            for (Integer i = 0; i < xObjectList.size(); i++) {
                Customer_Sales_Info__c temp = new Customer_Sales_Info__c();
                temp.setUnique_Attribute__c(((Customer_Sales_Info__c)xObjectList.get(i)).getName());
                logger.info("Country__c==" + ((Customer_Sales_Info__c)xObjectList.get(i)).getCountry__c());
                temp.setCountry__c(((Customer_Sales_Info__c)xObjectList.get(i)).getCountry__c());
                contextList.add(temp);

            }

            logger.info("xObjectList==" + JSON.toJSONString(xObjectList));
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
