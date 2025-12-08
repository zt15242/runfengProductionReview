package other.rainbow.cpzerooneone.trigger.CTC;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.rkhd.platform.sdk.data.model.Customer_Tax_Category__c;
import com.rkhd.platform.sdk.data.model.Customer_Tax_Category__c;
import com.rkhd.platform.sdk.exception.*;
import com.rkhd.platform.sdk.http.RkhdHttpClient;
import com.rkhd.platform.sdk.log.Logger;
import com.rkhd.platform.sdk.log.LoggerFactory;
import com.rkhd.platform.sdk.model.XObject;
import com.rkhd.platform.sdk.trigger.*;
import other.rainbow.cpzerooneone.service.CustomerSalescontroller;
import other.rainbow.cpzerooneone.service.GMDG011Controller;

import java.io.IOException;
import java.util.*;

public class AfterUpdateCTCTrigger implements Trigger {
    private static final Logger logger = LoggerFactory.getLogger();
    @Override
    public TriggerResponse execute(TriggerRequest triggerRequest) throws ScriptBusinessException {
        List<DataResult> result = new ArrayList<>();
        String msg ="";
        Boolean success = true;
        //获取beforeUpdate 的list
        TriggerContext triggerContext = triggerRequest.getTriggerContext();
        JSONArray jsonArray = null;
        try {
            jsonArray = JSONArray.parseArray(triggerContext.get("oldList"));
            List<Customer_Tax_Category__c> oldList = jsonArray.toJavaList(Customer_Tax_Category__c.class);
            Map<Long,Customer_Tax_Category__c> oldMap = new HashMap<Long,Customer_Tax_Category__c>();
            for(Customer_Tax_Category__c old :oldList){
                logger.info("old=" + old);
                oldMap.put(old.getId(),old);
            }

            logger.info("jsonArray===="+ JSON.toJSONString(jsonArray));
            logger.info("oldMap=" + oldMap);


            RkhdHttpClient rkhdclient = RkhdHttpClient.instance();          //请求实例
            List<XObject> dataList = triggerRequest.getDataList();
            Map<Long,Customer_Tax_Category__c> newMap= new HashMap<>();
            for(XObject xObject : dataList){
                newMap.put(xObject.getId(), (Customer_Tax_Category__c) xObject);
                result.add(new DataResult(true, "成功", xObject));
                logger.info("((Customer_Tax_Category__c) xObject).getTax_Category__c()=" + ((Customer_Tax_Category__c) xObject).getTax_Category__c());
                logger.info("((Customer_Tax_Category__c) xObject).getIs_Sync__c()=" + ((Customer_Tax_Category__c) xObject).getIs_Sync__c());
                logger.info("(oldMap.get(((Customer_Tax_Category__c) xObject).getId()).getTax_Category__c()=" + oldMap.get(((Customer_Tax_Category__c) xObject).getId()).getTax_Category__c());
                if (!Objects.equals(((Customer_Tax_Category__c) xObject).getTax_Category__c(), oldMap.get(((Customer_Tax_Category__c) xObject).getId()).getTax_Category__c()) && ((Customer_Tax_Category__c) xObject).getIs_Sync__c()){
                    CustomerSalescontroller.sendCustomerSales(((Customer_Tax_Category__c) xObject).getSales_View__c().toString(),"U");
                }
            }
            msg = "成功";
        } catch (TriggerContextException | IOException | InterruptedException | XsyHttpException |
                 ApiEntityServiceException e) {
            msg = "失败"+e.getMessage();
            success = false;
//            throw new RuntimeException(e);
        } catch (CustomConfigException e) {
            throw new RuntimeException(e);
        }
        logger.info("result==="+result);
        return new TriggerResponse(success, msg, result);
    }
}
