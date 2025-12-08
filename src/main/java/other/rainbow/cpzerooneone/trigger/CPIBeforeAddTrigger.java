package other.rainbow.cpzerooneone.trigger;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rkhd.platform.sdk.data.model.Customer_Payment_Item__c;
import com.rkhd.platform.sdk.data.model.Customer_Payment__c;
import com.rkhd.platform.sdk.exception.ApiEntityServiceException;
import com.rkhd.platform.sdk.exception.ScriptBusinessException;
import com.rkhd.platform.sdk.exception.XsyHttpException;
import com.rkhd.platform.sdk.http.RkhdHttpClient;
import com.rkhd.platform.sdk.log.Logger;
import com.rkhd.platform.sdk.log.LoggerFactory;
import com.rkhd.platform.sdk.model.BatchOperateResult;
import com.rkhd.platform.sdk.model.XObject;
import com.rkhd.platform.sdk.service.XObjectService;
import com.rkhd.platform.sdk.trigger.DataResult;
import com.rkhd.platform.sdk.trigger.Trigger;
import com.rkhd.platform.sdk.trigger.TriggerRequest;
import com.rkhd.platform.sdk.trigger.TriggerResponse;
import other.rainbow.cpzerooneone.common.NeoCrmRkhdService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CPIBeforeAddTrigger implements Trigger {
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
            List<Long> cpIds = new ArrayList<>();
            List<Long> orderIds = new ArrayList<>();
            for (XObject obj : dataList) {
                Long cpId = ((Customer_Payment_Item__c) obj).getCustomer_Payment__c();
                Long orderId = ((Customer_Payment_Item__c) obj).getOrder__c();
                if (cpId != null) { cpIds.add(cpId); }
                if (orderId != null) { orderIds.add(orderId); }
            }
            JSONArray accountinfoAll = new JSONArray();
            JSONArray cpiinfoAll = new JSONArray();
            JSONArray orderinfoAll = new JSONArray();
            JSONArray cpinfoAll = new JSONArray();
            try {
                if (cpIds.size() > 0) {
                    String sqlaccountAll = "SELECT Amount2__c, Customer_Payment__c FROM Customer_Payment_Item__c WHERE Is_Active__c = 1 and Customer_Payment__c IN (" + NeoCrmRkhdService.joinLongInSql(cpIds) + ")";
                    accountinfoAll = NeoCrmRkhdService.xoql(rkhdclient, sqlaccountAll);
                    String sqlcpAll = "SELECT id, Paymentamount__c FROM Customer_Payment__c WHERE id IN (" + NeoCrmRkhdService.joinLongInSql(cpIds) + ")";
                    cpinfoAll = NeoCrmRkhdService.xoql(rkhdclient, sqlcpAll);
                }
                if (orderIds.size() > 0) {
                    String sqlcpiAll = "SELECT Amount2__c, Order__c FROM Customer_Payment_Item__c WHERE Is_Active__c = 1 and Order__c IN (" + NeoCrmRkhdService.joinLongInSql(orderIds) + ")";
                    cpiinfoAll = NeoCrmRkhdService.xoql(rkhdclient, sqlcpiAll);
                    String sqlorderAll = "SELECT id, amount FROM order WHERE id IN (" + NeoCrmRkhdService.joinLongInSql(orderIds) + ")";
                    orderinfoAll = NeoCrmRkhdService.xoql(rkhdclient, sqlorderAll);
                }
            } catch (XsyHttpException | InterruptedException | ScriptBusinessException e) {
                throw new RuntimeException(e);
            }
            Map<Long, List<JSONObject>> cpItemsMap = new HashMap<>();
            for (int i = 0; i < accountinfoAll.size(); i++) {
                JSONObject row = accountinfoAll.getJSONObject(i);
                Long id = row.getLong("Customer_Payment__c");
                cpItemsMap.computeIfAbsent(id, k -> new ArrayList<>()).add(row);
            }
            Map<Long, List<JSONObject>> orderItemsMap = new HashMap<>();
            for (int i = 0; i < cpiinfoAll.size(); i++) {
                JSONObject row = cpiinfoAll.getJSONObject(i);
                Long id = row.getLong("Order__c");
                orderItemsMap.computeIfAbsent(id, k -> new ArrayList<>()).add(row);
            }
            Map<Long, JSONObject> orderMap = new HashMap<>();
            for (int i = 0; i < orderinfoAll.size(); i++) {
                JSONObject row = orderinfoAll.getJSONObject(i);
                orderMap.put(row.getLong("id"), row);
            }
            Map<Long, JSONObject> cpMap = new HashMap<>();
            for (int i = 0; i < cpinfoAll.size(); i++) {
                JSONObject row = cpinfoAll.getJSONObject(i);
                cpMap.put(row.getLong("id"), row);
            }

            for(XObject xObject : dataList){
                logger.info("NewCPI=" + xObject);
                Long cpId = ((Customer_Payment_Item__c) xObject).getCustomer_Payment__c();
                Long orderId = ((Customer_Payment_Item__c) xObject).getOrder__c();
                JSONObject orderObj = orderMap.get(orderId);
                JSONObject cpObj = cpMap.get(cpId);
                double remain = Double.parseDouble(orderObj.getString("amount"));
                double CPremain = Double.parseDouble(cpObj.getString("Paymentamount__c"));
                logger.info("开始remain==="+remain);
                List<JSONObject> orderItems = orderItemsMap.getOrDefault(orderId, new ArrayList<>());
                for (int i = 0; i<orderItems.size(); i++){
                    JSONObject jsstr= orderItems.get(i);
                    remain = remain-Double.parseDouble(jsstr.getString("Amount2__c"));
                    logger.info("中间remain==="+i+"&&"+remain);
                }
                List<JSONObject> cpItems = cpItemsMap.getOrDefault(cpId, new ArrayList<>());
                for (int i = 0; i<cpItems.size(); i++){
                    JSONObject jsstr= cpItems.get(i);
                    CPremain = CPremain-Double.parseDouble(jsstr.getString("Amount2__c"));
                    logger.info("CPremain==="+i+"&&"+CPremain);
                }
                if (((Customer_Payment_Item__c) xObject).getIs_Active__c()){
                    remain = remain-((Customer_Payment_Item__c) xObject).getAmount2__c();
                    CPremain = CPremain-((Customer_Payment_Item__c) xObject).getAmount2__c();
                }
                logger.info("最后remain==="+remain);
                if (remain<0){
                    result.add(new DataResult(false, "The amount must be less than the remaining Amount of the order", xObject));
                    return new TriggerResponse(false, "The amount must be less than the remaining Amount of the order", result);
                }
                if (CPremain<0){
                    result.add(new DataResult(false, "The amount must be less than the remaining Amount of the customer payment", xObject));
                    return new TriggerResponse(false, "The amount must be less than the remaining Amount of the customer payment", result);
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
