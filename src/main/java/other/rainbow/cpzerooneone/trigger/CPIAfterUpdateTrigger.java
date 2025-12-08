package other.rainbow.cpzerooneone.trigger;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rkhd.platform.sdk.data.model.*;
import com.rkhd.platform.sdk.exception.ApiEntityServiceException;
import com.rkhd.platform.sdk.exception.ScriptBusinessException;
import com.rkhd.platform.sdk.exception.TriggerContextException;
import com.rkhd.platform.sdk.exception.XsyHttpException;
import com.rkhd.platform.sdk.http.CommonHttpClient;
import com.rkhd.platform.sdk.http.RkhdHttpClient;
import com.rkhd.platform.sdk.http.RkhdHttpData;
import com.rkhd.platform.sdk.log.Logger;
import com.rkhd.platform.sdk.log.LoggerFactory;
import com.rkhd.platform.sdk.model.BatchOperateResult;
import com.rkhd.platform.sdk.model.OperateResult;
import com.rkhd.platform.sdk.model.QueryResult;
import com.rkhd.platform.sdk.model.XObject;
import com.rkhd.platform.sdk.service.MetadataService;
import com.rkhd.platform.sdk.service.XObjectService;
import com.rkhd.platform.sdk.trigger.*;
import other.rainbow.cpzerooneone.common.ObjectOptionValueRetrieval;
import other.rainbow.cpzerooneone.common.NeoCrmRkhdService;
import other.rainbow.cpzerooneone.common.PickOption;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

public class CPIAfterUpdateTrigger implements Trigger {

    private static final Logger logger = LoggerFactory.getLogger();
    @Override
    public TriggerResponse execute(TriggerRequest triggerRequest) {
        List<DataResult> result = new ArrayList<>();
        JSONArray oldInfo = null;
        try {
            oldInfo = JSONArray.parseArray(triggerRequest.getTriggerContext().get("oldInfo"));
            logger.error("oldInfo===" + oldInfo);
        } catch (TriggerContextException e) {
            throw new RuntimeException(e);
        }
        String msg ="";
        Boolean success = true;
        try {
            List<XObject> updateList = new ArrayList<>();
            RkhdHttpClient rkhdclient = RkhdHttpClient.instance();          //请求实例
            List<XObject> dataList = triggerRequest.getDataList();
            // zyh   优化   ↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓
            JSONArray newList = JSONArray.parseArray(dataList.toString());
            List<Long> ordUpIdList = new ArrayList<>();
            List<Long> ordInIdList = new ArrayList<>();
            List<Long> cpiInIdList = new ArrayList<>();
            for (int i = 0;i < newList.size();i++){
                for (int j = 0;j < oldInfo.size();j++){
                    logger.error("进来判断====" + newList.getJSONObject(i).getBoolean("Is_Active__c") + "===" + oldInfo.getJSONObject(j).getBoolean("Is_Active__c"));
                    if (oldInfo.getJSONObject(j).getString("id").equals(newList.getJSONObject(i).getString("id"))){
                        if (!newList.getJSONObject(i).getBoolean("Is_Active__c") && newList.getJSONObject(i).getBoolean("Is_Active__c") != oldInfo.getJSONObject(j).getBoolean("Is_Active__c")){
                            logger.error("变为false");
                            ordUpIdList.add(newList.getJSONObject(i).getLong("Order__c"));
                            cpiInIdList.add(newList.getJSONObject(i).getLong("id"));
                        }
                        if (newList.getJSONObject(i).getBoolean("Is_Active__c") && newList.getJSONObject(i).getBoolean("Is_Active__c") != oldInfo.getJSONObject(j).getBoolean("Is_Active__c")){
                            logger.error("变为true");
                            ordInIdList.add(newList.getJSONObject(i).getLong("Order__c"));
                        }
                    }
                }
            }
            if (ordUpIdList.size() > 0){
                String ordIds = NeoCrmRkhdService.joinLongInSql(ordUpIdList);
                String cpiIds = NeoCrmRkhdService.joinLongInSql(cpiInIdList);
                logger.error("查询删除===" + cpiIds + "===" + ordIds);
                String paySql = "select id,orderId from paymentApplication where customer_Payment_Item__c in (" + cpiIds + ")";
                QueryResult payQuery = XObjectService.instance().query(paySql,true);
                if (payQuery.getSuccess()){
                    logger.error("查询删除===" + payQuery.getRecords());
                    if (payQuery.getRecords().size() > 0){
                        JSONArray jsonArray = JSONArray.parseArray(payQuery.getRecords().toString());
                        // 取消生效
                        for (int i = 0;i < jsonArray.size();i++){
                            JSONObject js = jsonArray.getJSONObject(i);
                            String url = "/rest/data/v2.0/xobjects/paymentApplication/actions/deactivation?dataId=" + js.getString("id");
                            RkhdHttpData commonData = new RkhdHttpData();
                            commonData.setCallString(url);
                            commonData.setCall_type("PATCH");
                            RkhdHttpClient rkhdHttpClient = new RkhdHttpClient();
                            String delRes = rkhdHttpClient.performRequest(commonData);
                            logger.error("删除结果====" + delRes);
                        }
                        logger.error("查询删除===" + payQuery.getRecords());
                        BatchOperateResult payRes = XObjectService.instance().delete(payQuery.getRecords(),true);
                        if (!payRes.getSuccess()){
                            logger.error("payRes===" + payRes.getErrorMessage());
                        }
                    }
                }
            }
            if (ordInIdList.size() > 0){
                String ordIds = NeoCrmRkhdService.joinLongInSql(ordInIdList);
                String paySql = "select id,orderId from paymentApplication where orderId in(" + ordIds + ")";
                QueryResult payQuery = XObjectService.instance().query(paySql,true);
                /*if (payQuery.getSuccess()){
                    if (payQuery.getRecords().size() > 0){
                        BatchOperateResult payRes = XObjectService.instance().delete(payQuery.getRecords(),true);
                    }
                }*/
                List<XObject> updateListpa = new ArrayList<>();
                List<XObject> updateListpi = new ArrayList<>();

                List<Long> cpIds = new ArrayList<>();
                List<Long> itemIds = new ArrayList<>();
                for (XObject obj : dataList) {
                    cpIds.add(((Customer_Payment_Item__c) obj).getCustomer_Payment__c());
                    itemIds.add(obj.getId());
                }
                String cpQuery = "SELECT id, remaining_Amount__c,Paymentamount__c,amountAll__c,Status__c,Has_order__c,order_Activation__c,currencyUnit FROM Customer_Payment__c WHERE id IN (" + NeoCrmRkhdService.joinLongInSql(cpIds) + ")";
                String itemQuery = "SELECT id, Customer_Payment__c,Order__c,Amount2__c,Customer_Payment__c.Customer__c FROM Customer_Payment_Item__c WHERE id IN (" + NeoCrmRkhdService.joinLongInSql(itemIds) + ")";
                JSONArray cpRecords;
                JSONArray itemRecords;
                try {
                    cpRecords = NeoCrmRkhdService.xoql(rkhdclient, cpQuery);
                    itemRecords = NeoCrmRkhdService.xoql(rkhdclient, itemQuery);
                } catch (XsyHttpException | InterruptedException | ScriptBusinessException e) {
                    throw new RuntimeException(e);
                }
                Map<Long, JSONObject> cpInfoMap = new java.util.HashMap<>();
                for (int i = 0; i < cpRecords.size(); i++) {
                    JSONObject obj = cpRecords.getJSONObject(i);
                    cpInfoMap.put(obj.getLong("id"), obj);
                }
                Map<Long, JSONObject> itemInfoMap = new java.util.HashMap<>();
                for (int i = 0; i < itemRecords.size(); i++) {
                    JSONObject obj = itemRecords.getJSONObject(i);
                    itemInfoMap.put(obj.getLong("id"), obj);
                }

                for(XObject xObject : dataList){
                    logger.info("NewCPI=" + xObject);
                Long cpId = ((Customer_Payment_Item__c) xObject).getCustomer_Payment__c();
                rkhdclient = RkhdHttpClient.instance();
                result = new ArrayList<>();
                    JSONObject jsstr = cpInfoMap.get(cpId);
                    JSONObject jsstr2 = itemInfoMap.get(xObject.getId());
//                    if (((Customer_Payment_Item__c) xObject).getAmount2__c() > Double.parseDouble(jsstr.getString("remaining_Amount__c"))){
//                        return new TriggerResponse(false, "The amount must be less than the remaining Amount of the customer payment", result);
//                    }
                    if (Double.parseDouble(jsstr.getString("amountAll__c"))>Double.parseDouble(jsstr.getString("Paymentamount__c"))){
                        result.add(new DataResult(false, "The payment amount must be greater than the total amount of Customer Payment Items", xObject));
                        return new TriggerResponse(false, "The payment amount must be greater than the total amount of Customer Payment Items", result);
                    }
                    logger.info("amountAll__c==="+jsstr.getString("amountAll__c"));
                    logger.info("Paymentamount__c==="+jsstr.getString("Paymentamount__c"));

                    if (jsstr.getJSONArray("Status__c")!=null && Objects.equals(jsstr.getJSONArray("Status__c").get(0),"Approved")  ) {
                        PaymentApplication pa = new PaymentApplication();
                        PaymentItem pi = new PaymentItem();
                        logger.info("orderActivation===" + jsstr.getString("order_Activation__c"));
                        String orderActivation = jsstr.getString("order_Activation__c");
                        if (Objects.equals(orderActivation, "1")) {
                            // 收款单
                            pa.setAccountId(Long.parseLong(jsstr2.getString("Customer_Payment__c.Customer__c")));
                            pa.setTransactionDate(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
                            pa.setAmount(Double.parseDouble(jsstr2.getString("Amount2__c")));
                            List<PickOption> currencyUnit_List = ObjectOptionValueRetrieval.instance().getGlobalPicks("currencyUnit");

                            pa.setCurrencyUnit(ObjectOptionValueRetrieval.instance().getOptionByLabel(currencyUnit_List,jsstr.getJSONArray("currencyUnit").get(0).toString()).getOptionCode());
                            pi.setCurrencyUnit(ObjectOptionValueRetrieval.instance().getOptionByLabel(currencyUnit_List,jsstr.getJSONArray("currencyUnit").get(0).toString()).getOptionCode());

                            pa.setEntityType(MetadataService.instance().getBusiType("paymentApplication", "defaultBusiType").getId());
                            pa.setCustomer_Payment_Item__c(xObject.getId());
                            updateListpa.add(pa);
                            logger.info("updateListpa" + updateListpa);
                            BatchOperateResult batchResult = XObjectService.instance().insert(updateListpa,false,true);
                            logger.info("batchResult=" + JSONObject.toJSONString(batchResult));
                            Long Paid = batchResult.getOperateResults().stream().map(OperateResult::getDataId).collect(Collectors.toList()).get(0);
                            String url = "";
                            url = "/rest/data/v2.0/xobjects/paymentApplication/actions/activation?dataId=" + Paid;
                            logger.info("url=" + url);
                            String encoded = "eHN5LWNybTAxOkFiY2QxMjM0";
                            // 创建 "Basic " 开头的字符串
                            String hed = "Basic " + encoded;
                            CommonHttpClient commonHttpClient = CommonHttpClient.instance(15000, 15000);
                            RkhdHttpData commonData = new RkhdHttpData();
                            commonData.setCallString(url);
                            commonData.setCall_type("PATCH");
                            RkhdHttpClient rkhdHttpClient2 = new RkhdHttpClient();
                            String result2 = rkhdHttpClient2.performRequest(commonData);

                            logger.info("接口返回result: " + JSON.toJSONString(result2));
                            logger.info("batchResult" + batchResult.getSuccess());
                            logger.info("batchResult" + batchResult.getCode());
                            logger.info("batchResult" + batchResult.getErrorMessage());
                            //收款单明细
                            // TODO 获取id
                            logger.info("pa.getId()==" + batchResult.getOperateResults().stream().map(OperateResult::getDataId).collect(Collectors.toList()));
                            pi.setPaymentId(batchResult.getOperateResults().stream().map(OperateResult::getDataId).collect(Collectors.toList()).get(0));
                            pi.setOrderId(Long.parseLong(jsstr2.getString("Order__c")));
                            pi.setEntityType(MetadataService.instance().getBusiType("paymentItem", "defaultBusiType").getId());
                            pi.setAmount(Double.parseDouble(jsstr2.getString("Amount2__c")));
                            pi.setTransactionDate(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
                            updateListpi.add(pi);
                            logger.info("updateListpi" + updateListpi);

                        }
                    }


                    Customer_Payment__c cp = new Customer_Payment__c();
                    //result.add(new DataResult(true, "成功", xObject));

                    if (((Customer_Payment_Item__c) xObject).getOrder__c()!= null){
                        cp.setId(((Customer_Payment_Item__c) xObject).getCustomer_Payment__c());
                        cp.setHas_order__c(true);
                        if (((Customer_Payment_Item__c) xObject).getOrderIsActive__c()){
                            cp.setOrder_Activation__c(true);
                        }
                    }
                    updateList.add(cp);

                }
                BatchOperateResult batchResult2 = XObjectService.instance().insert(updateListpi,false,true);
                logger.info("batchResult2" + batchResult2.getSuccess());
                logger.info("batchResult2" + batchResult2.getCode());
                logger.info("batchResult2" + batchResult2.getErrorMessage());
                BatchOperateResult batchResult = XObjectService.instance().update(updateList,false,true);
                logger.info("batchResult=" + batchResult);
            }
            // zyh   优化   ↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑
            for(XObject xObject : dataList){
                logger.info("NewCPI=" + xObject);
                Customer_Payment__c cp = new Customer_Payment__c();
                result.add(new DataResult(true, "成功", xObject));

                if (((Customer_Payment_Item__c) xObject).getOrder__c()!= null){
                    cp.setId(((Customer_Payment_Item__c) xObject).getCustomer_Payment__c());
                    cp.setHas_order__c(true);
                    if (((Customer_Payment_Item__c) xObject).getOrderIsActive__c()){
                        cp.setOrder_Activation__c(true);
                    }
                }
                updateList.add(cp);
            }
            BatchOperateResult batchResult = XObjectService.instance().update(updateList,false,true);
            logger.info("batchResult=" + batchResult);
            msg = "成功";
        } catch (IOException |
                 ApiEntityServiceException e) {
            msg = "失败";
            success = false;
            throw new RuntimeException(e);
        } catch (XsyHttpException e) {
            throw new RuntimeException(e);
        }
        logger.info("result==="+result);
        return new TriggerResponse(success, msg, result);
    }

    public static void main(String[] args) throws ApiEntityServiceException {
        String paySql = "select id,orderId from paymentApplication where customer_Payment_Item__c in (3699113730000997)";
        QueryResult payQuery = XObjectService.instance().query(paySql,true);
        logger.info("ooooo==" + payQuery.getSuccess());
        logger.info("ooooo==" + payQuery.getRecords());
        for (int i = 0;i < payQuery.getRecords().size();i++){
            PaymentApplication pa = new PaymentApplication();
            pa = (PaymentApplication)payQuery.getRecords().get(i);
            OperateResult result = XObjectService.instance().delete((PaymentApplication)payQuery.getRecords().get(i),true);
        }
    }
}
