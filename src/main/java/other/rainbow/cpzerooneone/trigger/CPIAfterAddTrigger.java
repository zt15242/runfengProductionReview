package other.rainbow.cpzerooneone.trigger;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rkhd.platform.sdk.data.model.Customer_Payment_Item__c;
import com.rkhd.platform.sdk.data.model.Customer_Payment__c;
import com.rkhd.platform.sdk.data.model.PaymentApplication;
import com.rkhd.platform.sdk.data.model.PaymentItem;
import com.rkhd.platform.sdk.exception.ApiEntityServiceException;
import com.rkhd.platform.sdk.exception.CustomConfigException;
import com.rkhd.platform.sdk.exception.ScriptBusinessException;
import com.rkhd.platform.sdk.exception.XsyHttpException;
import com.rkhd.platform.sdk.http.CommonHttpClient;
import com.rkhd.platform.sdk.http.CommonResponse;
import com.rkhd.platform.sdk.http.RkhdHttpClient;
import com.rkhd.platform.sdk.http.RkhdHttpData;
import com.rkhd.platform.sdk.log.Logger;
import com.rkhd.platform.sdk.log.LoggerFactory;
import com.rkhd.platform.sdk.model.BatchOperateResult;
import com.rkhd.platform.sdk.model.OperateResult;
import com.rkhd.platform.sdk.model.XObject;
import com.rkhd.platform.sdk.service.CustomConfigService;
import com.rkhd.platform.sdk.service.MetadataService;
import com.rkhd.platform.sdk.service.XObjectService;
import com.rkhd.platform.sdk.trigger.DataResult;
import com.rkhd.platform.sdk.trigger.Trigger;
import com.rkhd.platform.sdk.trigger.TriggerRequest;
import com.rkhd.platform.sdk.trigger.TriggerResponse;
import other.rainbow.cpzerooneone.common.ObjectOptionValueRetrieval;
import other.rainbow.cpzerooneone.common.NeoCrmRkhdService;
import other.rainbow.cpzerooneone.common.PickOption;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class CPIAfterAddTrigger implements Trigger {
    private static final Logger logger = LoggerFactory.getLogger();
    @Override
    public TriggerResponse execute(TriggerRequest triggerRequest) {
        List<DataResult> result = new ArrayList<>();
        String msg ="";
        Boolean success = true;
        
        try {
            RkhdHttpClient rkhdclient = RkhdHttpClient.instance();

            List<XObject> updateList = new ArrayList<>();
                  //请求实例
            List<XObject> dataList = triggerRequest.getDataList();
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
                Long cpId = ((Customer_Payment_Item__c) xObject).getCustomer_Payment__c();
                logger.info("NewCPI=" + xObject);
                JSONObject jsstr = cpInfoMap.get(cpId);
                JSONObject jsstr2 = itemInfoMap.get(xObject.getId());
//                if (((Customer_Payment_Item__c) xObject).getAmount2__c() > Double.parseDouble(jsstr.getString("remaining_Amount__c"))){
//                    return new TriggerResponse(false, "The amount must be less than the remaining Amount of the customer payment", result);
//                }

                if (Double.parseDouble(jsstr.getString("amountAll__c"))>Double.parseDouble(jsstr.getString("Paymentamount__c"))){
                    result.add(new DataResult(false, "The payment amount must be greater than the total amount of Customer Payment Items", xObject));
                    return new TriggerResponse(false, "The payment amount must be greater than the total amount of Customer Payment Items", result);
                }
                logger.info("amountAll__c==="+jsstr.getString("amountAll__c"));
                logger.info("Paymentamount__c==="+jsstr.getString("Paymentamount__c"));
                List<XObject> updateListpa = new ArrayList<>();
                List<XObject> updateListpi = new ArrayList<>();
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
                        BatchOperateResult batchResult2 = XObjectService.instance().insert(updateListpi,false,true);
                        logger.info("batchResult2" + batchResult2.getSuccess());
                        logger.info("batchResult2" + batchResult2.getCode());
                        logger.info("batchResult2" + batchResult2.getErrorMessage());
                    }
                }


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
                BatchOperateResult batchResult = XObjectService.instance().update(updateList,false,true);
                logger.info("batchResult=" + batchResult);
            }
            msg = "成功";
        } catch
            (IOException |
                 ApiEntityServiceException e) {
            msg = "失败";
            success = false;
        } catch (XsyHttpException e) {
            throw new RuntimeException(e);
        }
        logger.info("result==="+result);
        return new TriggerResponse(success, msg, result);
    }
}
