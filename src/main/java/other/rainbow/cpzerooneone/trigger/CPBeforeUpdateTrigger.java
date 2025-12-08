package other.rainbow.cpzerooneone.trigger;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rkhd.platform.sdk.data.model.Account;
import com.rkhd.platform.sdk.data.model.Customer_Payment_Item__c;
import com.rkhd.platform.sdk.data.model.Customer_Payment__c;
import com.rkhd.platform.sdk.exception.ScriptBusinessException;
import com.rkhd.platform.sdk.exception.TriggerContextException;
import com.rkhd.platform.sdk.exception.XsyHttpException;
import com.rkhd.platform.sdk.http.RkhdHttpClient;
import com.rkhd.platform.sdk.log.Logger;
import com.rkhd.platform.sdk.log.LoggerFactory;
import com.rkhd.platform.sdk.model.XObject;
import com.rkhd.platform.sdk.trigger.*;
import other.rainbow.cpzerooneone.common.NeoCrmRkhdService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CPBeforeUpdateTrigger implements Trigger {
    private static final Logger logger = LoggerFactory.getLogger();
    @Override
    public TriggerResponse execute(TriggerRequest triggerRequest) {
        List<DataResult> result = new ArrayList<>();
        String msg ="";
        Boolean success = true;
        try {
            List<XObject> updateList = new ArrayList<>();
            RkhdHttpClient rkhdclient = RkhdHttpClient.instance();          //请求实例
            List<XObject> dataList = triggerRequest.getDataList();
            JSONArray accountinfo;

            // 收集所有需要查询的Customer_Payment__c的ID
            List<String> paymentIds = new ArrayList<>();
            for (XObject xObject : dataList) {
                paymentIds.add(String.valueOf(xObject.getId()));
            }
            // 构造IN查询，一次性查出所有相关的明细
            String sqlaccount = "SELECT Amount2__c, Customer_Payment__c FROM Customer_Payment_Item__c WHERE Is_Active__c = 1 and Customer_Payment__c IN (" +
                    String.join(",", paymentIds) + ")";
            try {
                accountinfo = NeoCrmRkhdService.xoql(rkhdclient, sqlaccount);
            } catch (XsyHttpException | InterruptedException | ScriptBusinessException e) {
                throw new RuntimeException(e);
            }
            // 用Map分组
            Map<String, List<JSONObject>> paymentItemMap = new HashMap<>();
            for (int i = 0; i < accountinfo.size(); i++) {
                JSONObject jsstr = accountinfo.getJSONObject(i);
                String paymentId = jsstr.getString("Customer_Payment__c");
                paymentItemMap.computeIfAbsent(paymentId, k -> new ArrayList<>()).add(jsstr);
            }

            for(XObject xObject : dataList){
                logger.info("NewCP=" + xObject);
                double CPremain = ((Customer_Payment__c) xObject).getPaymentamount__c();

                List<JSONObject> items = paymentItemMap.getOrDefault(xObject.getId(), new ArrayList<>());
                for (int i = 0; i < items.size(); i++) {
                    JSONObject jsstr = items.get(i);
                    CPremain = CPremain - Double.parseDouble(jsstr.getString("Amount2__c"));
                    logger.info("CPremain==="+i+"&&"+CPremain);
                }
                logger.info("最后的CPremain==="+CPremain);
                if (CPremain<0){
                    result.add(new DataResult(false, "The payment amount must be greater than the total amount of Customer Payment Items", xObject));
                    return new TriggerResponse(false, "The payment amount must be greater than the total amount of Customer Payment Items", result);
                }
                result.add(new DataResult(true, "成功", xObject));
            }
            msg = "成功";
        } catch (IOException e) {
            msg = "失败";
            success = false;
//            throw new RuntimeException(e);
        }
        logger.info("result==="+result);
        return new TriggerResponse(success, msg, result);
    }
}


