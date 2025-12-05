package other.rainbow.orderapp.trigger;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rkhd.platform.sdk.context.ScriptRuntimeContext;
import com.rkhd.platform.sdk.data.model.Account_Receivable__c;
import com.rkhd.platform.sdk.data.model.Customer_Sales_Info__c;
import com.rkhd.platform.sdk.data.model.Order;
import com.rkhd.platform.sdk.exception.ApiEntityServiceException;
import com.rkhd.platform.sdk.exception.ScriptBusinessException;
import com.rkhd.platform.sdk.exception.XsyHttpException;
import com.rkhd.platform.sdk.http.RkhdHttpClient;
import com.rkhd.platform.sdk.http.RkhdHttpData;
import com.rkhd.platform.sdk.http.handler.ResponseBodyHandlers;
import com.rkhd.platform.sdk.log.Logger;
import com.rkhd.platform.sdk.log.LoggerFactory;
import com.rkhd.platform.sdk.model.BatchOperateResult;
import com.rkhd.platform.sdk.model.OperateResult;
import com.rkhd.platform.sdk.model.QueryResult;
import com.rkhd.platform.sdk.model.XObject;
import com.rkhd.platform.sdk.service.XObjectService;
import com.rkhd.platform.sdk.service.XoqlService;
import com.rkhd.platform.sdk.trigger.DataResult;
import com.rkhd.platform.sdk.trigger.Trigger;
import com.rkhd.platform.sdk.trigger.TriggerRequest;
import com.rkhd.platform.sdk.trigger.TriggerResponse;
import org.apache.commons.lang.StringUtils;
import other.rainbow.orderapp.button.CancelToSap;
import other.rainbow.orderapp.common.NeoCrmRkhdService;
import other.rainbow.orderapp.cstss.CustomException;
import other.rainbow.orderapp.cstss.SqlFormatUtils;
import other.rainbow.orderapp.tsts.TextMessageRestReq;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import other.rainbow.orderapp.common.ObjectOptionValueRetrieval;

public class orderInsertTrigger implements Trigger {

    private static final Logger logger = LoggerFactory.getLogger();
    @Override
    public TriggerResponse execute(TriggerRequest request) throws ScriptBusinessException {
        AtomicReference<Boolean> successFlg = new AtomicReference<>(true);
        AtomicReference<String> message = new AtomicReference<>("");
        ScriptRuntimeContext src = ScriptRuntimeContext.instance();
        long userId = src.getUserId();
        logger.error("uesrId=====" + userId);
        String userName = "";
        if (!"".equals(userId)){
            String userSql = "select id,name,Sales_Org__c from user where id = " + userId;
            logger.error("uesrId=====" + userId);
            try {
                QueryResult userQuery = XoqlService.instance().query(userSql,true);
                if (userQuery.getRecords().size() > 0){
                    logger.error("uesrId=====" + userQuery.getRecords());
                    JSONArray userInfo = JSONArray.parseArray(userQuery.getRecords().toString());
                    if (userInfo.getJSONObject(0).getJSONArray("Sales_Org__c") != null && ("2150 AGROIRIS (BD)".equals(userInfo.getJSONObject(0).getJSONArray("Sales_Org__c").getString(0)) || "7190 AGROTERRUM GHANA".equals(userInfo.getJSONObject(0).getJSONArray("Sales_Org__c").getString(0)) || "7030 Rainbow Agro(Ghana)".equals(userInfo.getJSONObject(0).getJSONArray("Sales_Org__c").getString(0)))) {
                        userName = userInfo.getJSONObject(0).getString("name");
                    }
                }
            } catch (ApiEntityServiceException e) {
                throw new RuntimeException(e);
            }
        }
        logger.error("进来catch1：" + userName);
        List<XObject> dataList = request.getDataList();
        logger.error("进来catch2：");
        List<DataResult> dataResultList = new ArrayList<>();
        try {
            List<CurrencyOption> currencyOption = CurrencyListReq.instance().getGlobalPicks();
            List<PickOption> currency__cList = GlobalPicksReq.instance().getGlobalPicks("Currency__c");
            List<PickOption> payment_Term__cList = GlobalPicksReq.instance().getGlobalPicks("Payment_Term__c");
            List<PickOption> paymentOrderBy__cList = GlobalPicksReq.instance().getGlobalPicks("PaymentOrderBy__c");
            List<PickOption> sales_Org__cList = GlobalPicksReq.instance().getGlobalPicks("SalesOrg__c");
			// 选项列表字段
            logger.error("进来catch3：");
            logger.error("sales_Org__cList：" + sales_Org__cList);
            Map<Integer,String> saleMap = new HashMap<>(); // 存salesorg的code和label
            for (PickOption p : sales_Org__cList){
                saleMap.put(p.getOptionCode(),p.getOptionLabel());
            }
            Long sfId = null;
            Long terId = null;

            String joinAccIds = dataList.stream().map(item -> {
                Order order = (Order) item;
                return String.valueOf(order.getAccountId());
            }).collect(Collectors.joining(","));
            String joinOrderIds = "";
            // 获取订单Id
            String joinOrdIds = dataList.stream().map(item -> {
                Order order = (Order) item;
                return String.valueOf(order.getId());
            }).collect(Collectors.joining(","));

            logger.error("进来catch30：" + joinOrderIds);
            logger.error("joinOrdIds===：" + joinOrdIds);
            logger.error("joinAccIds===：" + joinAccIds);
            Map<Long,List<String>> accOrgMap = new HashMap<>(); // 存放订单的客户ID/销售组织List
            Map<Long,List<String>> infoAccOrgMap = new HashMap<>(); // 存放订单的客户ID/销售组织List
            for (Object item : dataList) {
                Order order = (Order) item;
                if (accOrgMap.containsKey(order.getAccountId())){
                    if (!accOrgMap.get(order.getAccountId()).contains(order.getSales_Org__c())){
                        List<String> tempList = new ArrayList<>();
                        tempList = accOrgMap.get(order.getAccountId());
                        tempList.add(saleMap.get(order.getSales_Org__c()));
                        accOrgMap.put(order.getAccountId(),tempList);
                    } else {
                        List<String> tempList = new ArrayList<>();
                        tempList.add(saleMap.get(order.getSales_Org__c()));
                        accOrgMap.put(order.getAccountId(),tempList);
                    }
                } else {
                    List<String> tempList = new ArrayList<>();
                    tempList.add(saleMap.get(order.getSales_Org__c()));
                    accOrgMap.put(order.getAccountId(),tempList);
                }
//                accOrgMap.put(order.getAccountId(),)
            }
            QueryResult<Customer_Sales_Info__c> querySalesTemp = null;
            QueryResult queryAccRecTemp = null;
            if (!"".equals(joinAccIds)){
                // 创建订单赋值客户应收
                List<Long> accRecId = new ArrayList<>();
                Map<Long,Long> recIdMap = new HashMap<>(); // 存放客户应收ID--客户ID
                Map<Long,List<Long>> accRecMap = new HashMap<>(); // 存放客户ID--客户应收IDList
                Map<String,Long> unCoPdMap = new HashMap<>(); // 存放unicode--post date
                Map<String, BigDecimal> accAmountMap = new HashMap<>(); // 存放unicode--amount
                Map<Long, BigDecimal> accPayMap = new HashMap<>(); // 存放sales org-- amount sum
                Map<String, List<String>> orgUniMap = new HashMap<>(); // 存放sales org--unicodeList
                Map<Long,Map<String, List<String>>> accOrgUniMap = new HashMap<>();
                Map<Long,Map<String, BigDecimal>> sumAccAmountMap = new HashMap<>();
                queryAccRecTemp = XoqlService.instance().query("SELECT id,Posting_Item__c,Document_Number__c,Amount_in_Trans_Crcy__c,Amount_in_Local_Crcy__c,Amount_in_Trans_Crcy__cDomestic,Overdue_Days_Formula__c,uniqueCdoe__c,Posting_Date__c,Sales_Org__c,Customer__c FROM Account_Receivable__c where Customer__c in (" + joinAccIds + ")",true);
                if (queryAccRecTemp.getSuccess() && queryAccRecTemp.getRecords().size() > 0){
                    JSONArray accRecJSON = JSONArray.parseArray(queryAccRecTemp.getRecords().toString());
                    for (int i = 0;i < accRecJSON.size();i++){
                        JSONObject js = accRecJSON.getJSONObject(i);
                        List<Long> accIdList = new ArrayList<>();
                        if (accRecMap.containsKey(js.getLong("Customer__c"))){
                            accIdList = accRecMap.get(js.getLong("Customer__c"));
                        }
                        if (!accIdList.contains(js.getLong("Customer__c"))){
                            accIdList.add(js.getLong("id"));
                        }
                        accRecMap.put(js.getLong("Customer__c"),accIdList);
                        recIdMap.put(js.getLong("id"),js.getLong("Customer__c"));
                    }
                    for (int i = 0;i < accRecJSON.size();i++){
                        JSONObject js = accRecJSON.getJSONObject(i);
                        accRecId.add(js.getLong("id"));
                        if (js.getJSONArray("Sales_Org__c") != null){
                            if (accOrgUniMap.containsKey(js.getLong("Customer__c"))){
                                orgUniMap = accOrgUniMap.get(js.getLong("Customer__c"));
                                // 存放销售组织和uniqueCodeList
                                List<String> tempOrgList = new ArrayList<>();
                                if (orgUniMap.containsKey(js.getJSONArray("Sales_Org__c").getString(0))){
                                    tempOrgList = orgUniMap.get(js.getJSONArray("Sales_Org__c").getString(0));
                                }
                                if (!tempOrgList.contains(js.getString("uniqueCdoe__c"))){
                                    tempOrgList.add(js.getString("uniqueCdoe__c"));
                                }
                                orgUniMap.put(js.getJSONArray("Sales_Org__c").getString(0),tempOrgList);
                            } else {
                                orgUniMap = new HashMap<>();
                                List<String> tempOrgList = new ArrayList<>();
                                tempOrgList.add(js.getString("uniqueCdoe__c"));
                                orgUniMap.put(js.getJSONArray("Sales_Org__c").getString(0),tempOrgList);
                            }
                            logger.error("orgUniMap======" + orgUniMap);
                            accOrgUniMap.put(js.getLong("Customer__c"),orgUniMap);
                            logger.error("accOrgUniMap======" + accOrgUniMap);
                            if (js.getLong("Posting_Date__c") != null && js.getDouble("Amount_in_Local_Crcy__c") != 0){
                                if (sumAccAmountMap.containsKey(js.getLong("Customer__c"))){
                                    accAmountMap = sumAccAmountMap.get(js.getLong("Customer__c"));
                                } else {
                                    accAmountMap = new HashMap<>();
                                }
                                if (unCoPdMap.containsKey("uniqueCdoe__c")){
                                    if (unCoPdMap.get(js.getString("uniqueCdoe__c")) <= js.getLong("Posting_Date__c")){
                                        unCoPdMap.put(js.getString("uniqueCdoe__c"),js.getLong("Posting_Date__c"));
                                        accAmountMap.put(js.getString("uniqueCdoe__c"),new BigDecimal(js.getDouble("Amount_in_Local_Crcy__c").toString()));
                                    }
                                } else {
                                    unCoPdMap.put(js.getString("uniqueCdoe__c"),js.getLong("Posting_Date__c"));
                                    accAmountMap.put(js.getString("uniqueCdoe__c"),new BigDecimal(js.getDouble("Amount_in_Local_Crcy__c").toString()));
                                }
                                sumAccAmountMap.put(js.getLong("Customer__c"),accAmountMap);
                            }
                        }
                    }
                    for (Long accId : accOrgUniMap.keySet()) {
                        if (sumAccAmountMap.containsKey(accId) && !sumAccAmountMap.get(accId).isEmpty()) {
                            for (String unicode : sumAccAmountMap.get(accId).keySet()) {
                                if (accOrgUniMap.containsKey(accId) && !accOrgUniMap.get(accId).isEmpty()) {
                                    for (String org : accOrgUniMap.get(accId).keySet()) {
                                        if (accOrgUniMap.get(accId).get(org).contains(unicode)) {
                                            BigDecimal amount = sumAccAmountMap.get(accId).get(unicode);
                                            if (accPayMap.containsKey(accId)) {
                                                amount = amount.add(accPayMap.get(accId));
                                            }
                                            accPayMap.put(accId, amount);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                // 创建订单赋值SalesInfo
                querySalesTemp = XObjectService.instance().query("SELECT id,Terms_Of_Payment__c,Customer__c,Sales_Org__c,District__c FROM Customer_Sales_Info__c where Sync_Status__c = 3 and Customer__c in (" + joinAccIds + ")",true);
                if (querySalesTemp.getSuccess()){
                    logger.error("sfId=666==" + querySalesTemp.getRecords().size());
                    if (querySalesTemp.getRecords().size() <= 0){
                        logger.error("sfId=666==");
                        message.set("No corresponding Customer Sales Info found.");
                        successFlg.set(false);
                        dataResultList = dataList.stream().map(item -> {
                            Order order = (Order) item;
                            DataResult dataResult = new DataResult();
                            dataResult.setxObject(order);
                            dataResult.setSuccess(false);
                            dataResult.setMsg("No corresponding Customer Sales Info found.");
                            return dataResult;
                        }).collect(Collectors.toList());
                        return new TriggerResponse(successFlg.get(), message.get(), dataResultList);
                    } else {
                        // 将sales info的客户和sales org list放到map里
                        for (Customer_Sales_Info__c info : querySalesTemp.getRecords()){
                            if (infoAccOrgMap.containsKey(info.getCustomer__c())){
                                if (!infoAccOrgMap.get(info.getCustomer__c()).contains(info.getSales_Org__c())){
                                    List<String> tempList = new ArrayList<>();
                                    tempList = infoAccOrgMap.get(info.getCustomer__c());
                                    tempList.add(saleMap.get(info.getSales_Org__c()));
                                    infoAccOrgMap.put(info.getCustomer__c(),tempList);
                                } else {
                                    List<String> tempList = new ArrayList<>();
                                    tempList.add(saleMap.get(info.getSales_Org__c()));
                                    infoAccOrgMap.put(info.getCustomer__c(),tempList);
                                }
                            } else {
                                List<String> tempList = new ArrayList<>();
                                tempList.add(saleMap.get(info.getSales_Org__c()));
                                infoAccOrgMap.put(info.getCustomer__c(),tempList);
                            }
                        }
                        Boolean orgCheck = true;
                        //  遍历订单的客户/org的map
                        for (Long key : accOrgMap.keySet()){
                            // 遍历该客户的org
                            for (String orgKey : accOrgMap.get(key)){
                                //判断该客户是否有salesinfo
                                if (infoAccOrgMap.get(key).size() > 0){
                                    // 判断是否有这个org
                                    if (!infoAccOrgMap.get(key).contains(orgKey)){
                                        orgCheck = false;
                                    }
                                } else {
                                    orgCheck = false;
                                }
                            }
                        }
                        if (!orgCheck){
                            logger.error("sfId=888==");
                            message.set("No corresponding Customer Sales Info found.");
                            successFlg.set(false);
                            dataResultList = dataList.stream().map(item -> {
                                Order order = (Order) item;
                                DataResult dataResult = new DataResult();
                                dataResult.setxObject(order);
                                dataResult.setSuccess(false);
                                dataResult.setMsg("No corresponding Customer Sales Info found.");
                                return dataResult;
                            }).collect(Collectors.toList());
                            return new TriggerResponse(successFlg.get(), message.get(), dataResultList);
                        } else {
                            logger.error("确实能进来===" + joinOrdIds);
                            // 订单更新走这个分支
                            if (joinOrdIds != null && !"".equals(joinOrdIds) && !"null".equals(joinOrdIds)) {
                                logger.error("但是进错了===" + joinOrdIds);
                                QueryResult<JSONObject> queryOrd = XoqlService.instance().query("SELECT id,po,Sales_Org__c,verification_codebackend__c,accountId,accountId.Telephone__c,Customer_Sales_Info__c,poStatus FROM _order where id in (" + joinOrdIds + ")", true);

                                // salesinfo功能
                                if (queryOrd.getSuccess()) {
                                    if (queryOrd.getRecords().size() > 0) {
                                        JSONArray orderJson = JSONArray.parseArray(String.valueOf(queryOrd.getRecords()));
                                        Map<Long, Long> orAccMap = new HashMap<>(); // 存订单ID/客户ID
                                        Map<Long, String> orOrgMap = new HashMap<>(); // 存订单ID/销售组织
										Map<Long, String> SalesPersonMap = new HashMap<>(); // 存订单ID/销售人员选项
                                        Map<Long, List<JSONObject>> accInfoMap = new HashMap<>(); // 存客户ID/销售组织List
                                        List<Long> accIdList = new ArrayList<>(); // 存客户ID
                                        List<Order> upOrList = new ArrayList<>(); // 存需要更新的订单
                                        for (Integer i = 0; i < orderJson.size(); i++) {
                                            JSONObject js = orderJson.getJSONObject(i);
                                            if (js.getLong("Customer_Sales_Info__c") == null) {
                                                orAccMap.put(js.getLong("id"), js.getLong("accountId"));
                                                orOrgMap.put(js.getLong("id"), js.getString("Sales_Org__c"));
                                                accIdList.add(js.getLong("accountId"));
                                            }
											accIdList.add(js.getLong("accountId"));
                                        }
                                        if (accIdList.size() > 0) {
                                            String accIds = SqlFormatUtils.joinLongInSql(accIdList);
                                            String accInSql = "select id,Customer__c,Sales_Org__c,District__c from Customer_Sales_Info__c where Customer__c in (" + accIds + ")";
                                            QueryResult<JSONObject> salesInfo = XoqlService.instance().query(accInSql, true);
                                            if (salesInfo.getSuccess()) {
                                                if (salesInfo.getRecords().size() > 0) {
                                                    for (Integer i = 0; i < salesInfo.getRecords().size(); i++) {
                                                        JSONObject js = salesInfo.getRecords().get(i);
                                                        if (accInfoMap.containsKey(js.getLong("Customer__c"))) {
                                                            List<JSONObject> tempList = new ArrayList<>();
                                                            tempList = accInfoMap.get(js.getLong("Customer__c"));
                                                            tempList.add(js);
                                                            accInfoMap.put(js.getLong("Customer__c"), tempList);
                                                        } else {
                                                            List<JSONObject> tempList = new ArrayList<>();
                                                            tempList.add(js);
                                                            accInfoMap.put(js.getLong("Customer__c"), tempList);
                                                        }
                                                    }
                                                    if (accInfoMap.size() > 0) {
                                                        for (Long key : orAccMap.keySet()) {
                                                            // 判断salesinfo的Map是否有当前客户
                                                            if (accInfoMap.containsKey(orAccMap.get(key))) {
                                                                Order o = new Order();
                                                                o.setId(key);
                                                                if (accPayMap.containsKey(orAccMap.get(key))) {
                                                                    o.setAccountReceivableAmount__c(Double.valueOf(String.valueOf(accPayMap.get(orAccMap.get(key)))));
                                                                }
                                                                logger.error("sfId====" + orAccMap.get(key));
                                                                logger.error("sfId====" + accInfoMap.get(orAccMap.get(key)));
                                                                // 遍历当前客户的所有salesinfo
                                                                for (JSONObject keyOrg : accInfoMap.get(orAccMap.get(key))) {
                                                                    // 判断sales org是否一致，一致赋值sales info的Id
                                                                    if (!"".equals(keyOrg.getString("Sales_Org__c")) && orOrgMap.get(key) != null && keyOrg.getString("Sales_Org__c").equals(orOrgMap.get(key)) && sfId == null) {
                                                                        sfId = keyOrg.getLong("id");
                                                                        terId = keyOrg.getLong("District__c");
                                                                        joinOrderIds = String.valueOf(sfId);
                                                                    }
                                                                }
                                                                logger.error("sfId=1==" + sfId);
                                                                if (sfId != null) {
                                                                    o.setCustomer_Sales_Info__c(sfId);
                                                                    o.setTerritory__c(terId);
                                                                    if (o.getSales_person__c() == null && !"".equals(userName)) {
                                                                        o.setSales_person__c(userName);
                                                                    }
                                                                    upOrList.add(o);
                                                                } else {
                                                                    logger.error("sfId=2==" + sfId);
                                                                    message.set("No corresponding Customer Sales Info found.");
                                                                    successFlg.set(false);
                                                /*dataResultList = dataList.stream().map(item -> {
                                                    Order order = (Order) item;
                                                    DataResult dataResult = new DataResult();
                                                    dataResult.setxObject(order);
                                                    dataResult.setMsg("No corresponding Customer Sales Info found.");
                                                    dataResult.setSuccess(false);
                                                    return dataResult;
                                                }).collect(Collectors.toList());*/
                                                                    for (Object item : dataList) {
                                                                        Order order = (Order) item;
                                                                        DataResult dataResult = new DataResult();
                                                                        dataResult.setxObject(order);
                                                                        dataResult.setMsg("No corresponding Customer Sales Info found.");
                                                                        dataResult.setSuccess(false);
                                                                        dataResultList.add(dataResult);
                                                                    }
                                                                    logger.error("最后返回：" + successFlg.get() + "==" + message.get() + "==" + dataResultList);
                                                                    return new TriggerResponse(successFlg.get(), message.get(), dataResultList);
                                                                }
                                                            } else {
                                                                logger.error("sfId=3==");
                                                                message.set("No corresponding Customer Sales Info found.");
                                                                successFlg.set(false);
                                                                dataResultList = dataList.stream().map(item -> {
                                                                    Order order = (Order) item;
                                                                    DataResult dataResult = new DataResult();
                                                                    dataResult.setxObject(order);
                                                                    dataResult.setMsg("No corresponding Customer Sales Info found.");
                                                                    dataResult.setSuccess(false);
                                                                    return dataResult;
                                                                }).collect(Collectors.toList());
                                                                return new TriggerResponse(successFlg.get(), message.get(), dataResultList);
                                                            }
                                                        }
                                                    }
                                                } else {
                                                    logger.error("sfId=4==");
                                                    message.set("No corresponding Customer Sales Info found.");
                                                    successFlg.set(false);
                                                    dataResultList = dataList.stream().map(item -> {
                                                        Order order = (Order) item;
                                                        DataResult dataResult = new DataResult();
                                                        dataResult.setxObject(order);
                                                        dataResult.setMsg("No corresponding Customer Sales Info found.");
                                                        dataResult.setSuccess(false);
                                                        return dataResult;
                                                    }).collect(Collectors.toList());
                                                    return new TriggerResponse(successFlg.get(), message.get(), dataResultList);
                                                }
                                            }
                                        }
                                        if (upOrList.size() > 0) {
                                            BatchOperateResult update = XObjectService.instance().update(upOrList, true,true);
                                            if (update.getSuccess()) {
                                                logger.error("saleinfo赋值成功");
                                            } else {
                                                logger.error("salesinfo赋值失败" + update.getErrorMessage());
                                            }
                                        }
                                    } else {
                                        logger.error("总不会进来这了吧===" + joinOrdIds);
                                    }
                                }
                            } else {
                                logger.error("进来新建粉之");
                                //订单新建走这个分支
                                Integer salesOrg = null;
                                Long accId = null;
                                RkhdHttpClient rkhdclient = RkhdHttpClient.instance();
                                Map<String,Integer> orgMap = getGlobalPicks("SalesOrg__c",rkhdclient);
                                logger.error("orgMap====" + orgMap);
                                for (XObject obj : dataList){
                                    Order o = (Order)obj;
                                    salesOrg = o.getSales_Org__c();
                                    accId = o.getAccountId();
                                }
                                String accInSql = "select id,Customer__c,Sales_Org__c,District__c from Customer_Sales_Info__c where Customer__c = " + accId ;
                                QueryResult<JSONObject> salesInfo = XoqlService.instance().query(accInSql, true);
                                if (salesInfo.getSuccess()) {
                                    logger.error("查询成功" + salesInfo.getRecords().size());
                                    logger.error("查询成功" + salesInfo.getRecords());
                                    if (salesInfo.getRecords().size() > 0) {
                                        Map<Integer,Long> accInfoMap = new HashMap<>();
                                        Map<Integer,Long> accTerrMap = new HashMap<>();
                                        for (Integer i = 0; i < salesInfo.getRecords().size(); i++) {
                                            JSONObject js = salesInfo.getRecords().get(i);
                                            accInfoMap.put(orgMap.get(js.getJSONArray("Sales_Org__c").getString(0)), js.getLong("id"));
                                            accTerrMap.put(orgMap.get(js.getJSONArray("Sales_Org__c").getString(0)), js.getLong("District__c"));
                                        }
                                        if (accInfoMap.size() > 0) {
                                            // 判断salesinfo的Map是否有当前客户
                                            if (accInfoMap.containsKey(salesOrg)) {
                                                sfId = accInfoMap.get(salesOrg);
                                                terId = accTerrMap.get(salesOrg);
                                                joinOrderIds = sfId.toString();
                                                logger.error("sfId====" + sfId);
                                                logger.error("sfId====" + accInfoMap);
                                                if (sfId != null) {
                                                    logger.error("dataList===" + dataList);
                                                    for (XObject obj: dataList){
                                                        Order o = (Order) obj;
                                                        o.setCustomer_Sales_Info__c(sfId);
                                                        o.setTerritory__c(terId);
                                                        logger.error("sfId====" + o.getSales_person__c() + (o.getSales_person__c() == null));
                                                        if (o.getSales_person__c() == null && !"".equals(userName)) {
                                                            o.setSales_person__c(userName);
                                                        }
                                                    }
                                                    logger.error("dataList===" + dataList);
                                                } else {
                                                    logger.error("sfId=2==" + sfId);
                                                    message.set("No corresponding Customer Sales Info found.");
                                                    successFlg.set(false);
                                            /*dataResultList = dataList.stream().map(item -> {
                                                Order order = (Order) item;
                                                DataResult dataResult = new DataResult();
                                                dataResult.setxObject(order);
                                                dataResult.setMsg("No corresponding Customer Sales Info found.");
                                                dataResult.setSuccess(false);
                                                return dataResult;
                                            }).collect(Collectors.toList());*/
                                                    for (Object item : dataList) {
                                                        Order order = (Order) item;
                                                        DataResult dataResult = new DataResult();
                                                        dataResult.setxObject(order);
                                                        dataResult.setMsg("No corresponding Customer Sales Info found.");
                                                        dataResult.setSuccess(false);
                                                        dataResultList.add(dataResult);
                                                    }
                                                    logger.error("最后返回：" + successFlg.get() + "==" + message.get() + "==" + dataResultList);
                                                    return new TriggerResponse(successFlg.get(), message.get(), dataResultList);
                                                }
                                            } else {
                                                logger.error("sfId=3==");
                                                message.set("No corresponding Customer Sales Info found.");
                                                successFlg.set(false);
                                                dataResultList = dataList.stream().map(item -> {
                                                    Order order = (Order) item;
                                                    DataResult dataResult = new DataResult();
                                                    dataResult.setxObject(order);
                                                    dataResult.setMsg("No corresponding Customer Sales Info found.");
                                                    dataResult.setSuccess(false);
                                                    return dataResult;
                                                }).collect(Collectors.toList());
                                                return new TriggerResponse(successFlg.get(), message.get(), dataResultList);
                                            }
                                        }
                                    } else {
                                        logger.error("sfId=4==");
                                        message.set("No corresponding Customer Sales Info found.");
                                        successFlg.set(false);
                                        dataResultList = dataList.stream().map(item -> {
                                            Order order = (Order) item;
                                            DataResult dataResult = new DataResult();
                                            dataResult.setxObject(order);
                                            dataResult.setMsg("No corresponding Customer Sales Info found.");
                                            dataResult.setSuccess(false);
                                            return dataResult;
                                        }).collect(Collectors.toList());
                                        return new TriggerResponse(successFlg.get(), message.get(), dataResultList);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            // 验证码功能
            /*if (queryOrd.getSuccess()){
                if (queryOrd.getRecords().size() > 0){
                    JSONArray orderJson = JSONArray.parseArray(String.valueOf(queryOrd.getRecords()));
                    Map<Long,String> vcMap = new HashMap<>();
                    List<Order> orderUpList = new ArrayList<>();
                    TextMessageRestReq tmrr = new TextMessageRestReq();
                    for (Integer i = 0; i < orderJson.size();i++){
                        JSONObject js = orderJson.getJSONObject(i);
                        if ("".equals(js.getString("verification_codebackend__c"))){
                            Random random = new Random();
                            int code = random.nextInt(9000) + 1000;
                            logger.error("生成的四位数验证码为: " + code);
                            Order o = new Order();
                            o.setId(js.getLong("id"));
                            o.setVerification_codebackend__c(String.valueOf(code));
                            orderUpList.add(o);
                            logger.error("verification_code===" + orderUpList);
                            vcMap.put(js.getLong("id"),String.valueOf(code));
                            if (!"".equals(js.getString("phone"))){
                                Boolean phoneCheck = tmrr.sendTextMessageService(js.getString("phone"), String.valueOf(code) + " is your verification code.Your order:"+js.getString("po")+".");
                                logger.error("phoneCheck"+phoneCheck);
                            }
                        }
                    }
                    if (orderUpList.size() > 0){
                        BatchOperateResult update = XObjectService.instance().update(orderUpList,false,true);
                        if (update.getSuccess()){
                            logger.info("更新成功");
                            logger.error("更新成功" + orderUpList);
                        } else {
                            logger.error("更新失败：" + update.getErrorMessage());
                        }
                    }
                }
            } else {
                logger.error("joinOrdIds==err:" + queryOrd.getErrorMessage());
            }*/
            if (!"".equals(joinOrderIds) && joinOrderIds != null) {
                QueryResult<Customer_Sales_Info__c> querySales = XObjectService.instance().query("SELECT id,Terms_Of_Payment__c FROM Customer_Sales_Info__c where id in (" + joinOrderIds + ")", true);
                logger.error("进来joinOrderIds：" + joinOrderIds);
                logger.error("进来catch302getSuccess：" + querySales.getSuccess());
                logger.error("进来catch302getRecords：" + querySales.getRecords());
                logger.error("进来catch302getErrorMessage：" + querySales.getErrorMessage());
                if (querySales.getRecords().size() > 0) {
                    if (!querySales.getSuccess()) {
                        throw new CustomException("Failed to obtain Customer Sales Info！" + querySales.getErrorMessage());
                    }
                    if (querySales.getTotalCount() == 0) {
                        throw new CustomException("Get empty Customer Sales Info！");
                    }
                    /*logger.error("生成的四位数验证码为: " + queryOrd.getSuccess());
                    logger.error("生成的四位数验证码为: " + queryOrd.getRecords());*/
                    /*if (2>1){
                        throw new CustomException("测试失败");
                    }
                    for (Object item : dataList) {
                        Order order = (Order) item;
                        DataResult dataResult = new DataResult();
                        dataResult.setxObject(order);
                        dataResult.setMsg("No corresponding Customer Sales Info found.");
                        dataResult.setSuccess(false);
                        dataResultList.add(dataResult);
                    }*/
                    dataResultList = dataList.stream().map(item -> {
                        DataResult dataResult = new DataResult();
                        dataResult.setSuccess(true);
                        try {

                            Order order = (Order) item;
                            logger.error("订单信息====：" + order);
//                            String temp = GlobalPicksReq.instance().getOptionByCode(currency__cList, order.getCurrency__c()).getOptionApiKey();
//                            logger.error("进来temp==：" + temp);
//                            order.setCurrencyUnit(CurrencyListReq.instance().getOptionByLabel(currencyOption, temp).getCode());
                            logger.error("进来CurrencyUnit");
                            Customer_Sales_Info__c customerSalesInfo__c = querySales.getRecords().stream().filter(items -> items.getId().equals(order.getCustomer_Sales_Info__c())).findAny().orElse(null);
                            logger.error("进来customerSalesInfo__c:" + customerSalesInfo__c);
                            if (customerSalesInfo__c == null) {
                                successFlg.set(false);
                                message.set("No corresponding Customer Sales Info found.");
                                dataResult.setxObject(order);
                                dataResult.setSuccess(false);
                                dataResult.setMsg("No corresponding Customer Sales Info found.");
                                return dataResult;
                            }
                            if (customerSalesInfo__c.getTerms_Of_Payment__c() == null) {
                                successFlg.set(false);
                                message.set("Customer Sales Info. Terms Of Payment is empty.");
                                dataResult.setxObject(order);
                                dataResult.setSuccess(false);
                                dataResult.setMsg("Customer Sales Info. Terms Of Payment is empty.");
                                return dataResult;
                            }
                            if (order.getPayment_Term__c() == null) {
                                successFlg.set(false);
                                message.set("Payment Term is empty.");
                                dataResult.setxObject(order);
                                dataResult.setSuccess(false);
                                dataResult.setMsg("Payment Term is empty.");
                                return dataResult;
                            }
                            Integer optionApiKeyorder = Integer.valueOf(GlobalPicksReq.instance().getOptionByLabel(paymentOrderBy__cList, GlobalPicksReq.instance().getOptionByCode(payment_Term__cList, order.getPayment_Term__c()).getOptionApiKey()).getOptionApiKey());
                            Integer optionApiKeyCustomerSalesInfo = Integer.valueOf(GlobalPicksReq.instance().getOptionByLabel(paymentOrderBy__cList, GlobalPicksReq.instance().getOptionByCode(payment_Term__cList, customerSalesInfo__c.getTerms_Of_Payment__c()).getOptionApiKey()).getOptionApiKey());
                            logger.error("进来对比" + optionApiKeyorder + "~~~" + optionApiKeyCustomerSalesInfo);
                            if (optionApiKeyorder > optionApiKeyCustomerSalesInfo) {
                                successFlg.set(false);
                                message.set("The payment term for this order does not meet the customer's requirements.");
                                dataResult.setxObject(order);
                                dataResult.setSuccess(false);
                                dataResult.setMsg("The payment term for this order does not meet the customer's requirements.");
                                return dataResult;
                            }
                            dataResult.setxObject(order);
                            return dataResult;
                        } catch (Exception e) {
                            logger.error("进来catch：" + e.getStackTrace()[0].getLineNumber() + "====" + e.getMessage());
                            successFlg.set(false);
                            message.set(e.getMessage());
                            dataResult.setxObject(item);
                            dataResult.setSuccess(false);
                            dataResult.setMsg(e.getMessage());
                            logger.error("进来catch：" + e.getStackTrace()[0].getLineNumber() + "====" + e.getMessage());
                            return dataResult;
                        }
                    }).collect(Collectors.toList());
                    logger.error("进来dataResultList：" + dataResultList);
                    logger.error("进来dataResultList：" + successFlg.get());
                    logger.error("进来dataResultList：" + message.get());
                    logger.error("进来dataResultList：" + dataResultList);
                    return new TriggerResponse(successFlg.get(), message.get(), dataResultList);
                }else {
                    dataResultList = dataList.stream().map(item -> {
                        Order order = (Order) item;
                        DataResult dataResult = new DataResult();
                        dataResult.setxObject(order);
                        dataResult.setSuccess(true);
                        return dataResult;
                    }).collect(Collectors.toList());
                    logger.error("进来dataResultList===：" + dataResultList);
                }
            } else {
                dataResultList = dataList.stream().map(item -> {
                    Order order = (Order) item;
                    DataResult dataResult = new DataResult();
                    dataResult.setxObject(order);
                    dataResult.setSuccess(true);
                    return dataResult;
                }).collect(Collectors.toList());
                logger.error("进来dataResultList===：" + dataResultList);
            }
            Map<String,String> apiMap = getApiKey("order","cancelReason");
            if (successFlg.get()){
                for (Object item : dataList) {
                    Order order = (Order) item;
                    if (order.getPoStatus() == 3 && !order.getIsClosed__c()){
                        logger.error("进来调接口");
                        logger.error("进来调接口" + getApiKey("order","cancelReason"));
                        order.setIsClosed__c(true);
                        OperateResult up = XObjectService.instance().update(order,true);
                        String orId = "{\"id\" : \"" + order.getId() + "\",\"type\":\"X\",\"reason\":\"" + apiMap.get(order.getCancelReason().toString()) +"\"}";
                        // String orId = "{\"id\" : \"" + order.getId() + "\",\"type\":\"X\",\"reason\":\"" + order.getCancelReason().toString() +"\"}";
                        logger.error("进来调接口" + orId);
                        CancelToSap.toSap(orId);
                    }
                }
            }
            logger.error("进来dataResultList：" + dataResultList);
            logger.error("进来dataResultList：" + successFlg.get());
            logger.error("进来dataResultList：" + message.get());
            return new TriggerResponse(successFlg.get(), message.get(), dataResultList);
        } catch (Exception e) {
            logger.error("进来catch2：" + e.getStackTrace()[0].getLineNumber() + "====" + e.getStackTrace()[0].getClassName() + "====" + e.getMessage());
            successFlg.set(false);
            message.set(e.getMessage());
            logger.error("进来catch2：" + e.getStackTrace()[0].getLineNumber() + "====" + e.getMessage());
            return new TriggerResponse(successFlg.get(), message.get(),new ArrayList<>());
        }
    }
    // 获取选项列表值code
    public static Map<String,Integer> getGlobalPicks(String globalPickApiKey, RkhdHttpClient client) {
        Map<String,Integer> map = new HashMap<String,Integer>();
        try {
            if (client == null) {
                client = RkhdHttpClient.instance();
            }
            RkhdHttpData data = new RkhdHttpData();
            data.setCall_type("GET");
            data.setCallString("/rest/metadata/v2.0/settings/globalPicks/" + globalPickApiKey);
            String responseStr = client.execute(data, ResponseBodyHandlers.ofString());

            if (StringUtils.isNotBlank(responseStr)) {
                logger.info("查询列表结果：" + responseStr);
                JSONObject responseObject = JSONObject.parseObject(responseStr);
                Integer responseCode = responseObject.getIntValue("code");
                if (responseCode.equals(0)) {
                    String resultStr = responseObject.getString("data");
                    JSONObject dataJson = JSONObject.parseObject(resultStr);
                    String records = dataJson.getString("records");
                    JSONArray pickOptions = JSONObject.parseObject(records).getJSONArray("pickOption");
                    for (int i = 0; i < pickOptions.size(); i++) {
                        JSONObject pickOption = pickOptions.getJSONObject(i);
                        map.put(pickOption.getString("optionLabel"),pickOption.getInteger("optionCode"));
                    }
                }
            }
        } catch (Exception e) {
            logger.error(" 报错信息：" + e);
        }
        return map;
    }

    // 获取选项列表的API
    public static Map<String,String> getApiKey(String obj,String apiKey) throws IOException, ScriptBusinessException, XsyHttpException {
        RkhdHttpClient instance = RkhdHttpClient.instance();
        Map<String,String> apiMap = new HashMap<>();
        Map<String, Map<String, String>> kvMap = new HashMap<>();
        Map<String, Map<Integer, String>> vkMap = new HashMap<>();
        Map<String,Map<String,String>> pkMap = NeoCrmRkhdService.getPicklistValueKey(instance,obj,apiKey,kvMap,vkMap);
        logger.error("apikeyMap打印：" + pkMap);
        RkhdHttpData data = RkhdHttpData.newBuilder().callType("GET")
                .callString("/rest/data/v2.0/xobjects/"+ obj +"/description").build();
        JSONObject result = instance.execute(data, ResponseBodyHandlers.ofJSON());
        JSONArray apiJson = new JSONArray();
        for (int i = 0; i < result.getJSONObject("data").getJSONArray("fields").size();i++){
            JSONObject js = result.getJSONObject("data").getJSONArray("fields").getJSONObject(i);
            if (js.getString("apiKey").equals(apiKey)){
                System.out.println(js);
                apiJson = js.getJSONArray("selectitem");
            }
        }
        for (int i = 0;i < apiJson.size();i++){
            apiMap.put(apiJson.getJSONObject(i).getString("value"),apiJson.getJSONObject(i).getString("apiKey"));
        }
        /*Map<String,String> reasonMap = new HashMap<>();
        for (String key : pkMap.get(apiKey).keySet()){
            reasonMap.put(pkMap.get(apiKey).get(key),key);
        }*/
        //return pkMap.get(apiKey);
        return apiMap;
    }
    public static void main(String[] args) throws IOException, XsyHttpException, ScriptBusinessException, ApiEntityServiceException {
//        List<PickOption> sales_Org__cList = GlobalPicksReq.instance().getGlobalPicks("SalesOrg__c");
//        String aaa = GlobalPicksReq.instance().getOptionByCode(sales_Org__cList,2).getOptionLabel();
//        logger.error("aaaa" + sales_Org__cList);
//        logger.error("aaaa" + aaa);
        QueryResult<Customer_Sales_Info__c> querySalesTemp = XObjectService.instance().query("SELECT id,Terms_Of_Payment__c,Customer__c,Sales_Org__c,Sync_Status__c FROM Customer_Sales_Info__c where Sync_Status__c = 3 limit 2",true);
        QueryResult<JSONObject> querySalesTemp1 = XoqlService.instance().query("SELECT id,Terms_Of_Payment__c,Customer__c,Sales_Org__c,Sync_Status__c FROM Customer_Sales_Info__c where Sync_Status__c = 3 limit 2",true);
        System.out.println("Customer_Sales_Info__c===" + querySalesTemp.getRecords());
        System.out.println("Customer_Sales_Info__c===" + querySalesTemp1.getRecords());
        /*String userSql = "select id,name from user where id = 3549067183440875";
        String userName = "";
        try {
            QueryResult userQuery = XoqlService.instance().query(userSql,true);
            if (userQuery.getRecords().size() > 0){
                JSONArray userInfo = JSONArray.parseArray(userQuery.getRecords().toString());
                userName = userInfo.getJSONObject(0).getString("name");
            }
        } catch (ApiEntityServiceException e) {
            throw new RuntimeException(e);
        }
        System.out.println(userName);
        RkhdHttpClient client = RkhdHttpClient.instance();
        RkhdHttpData data = RkhdHttpData.newBuilder().callType("GET")
                .callString("/rest/data/v2.0/xobjects/order/description").build();
        JSONObject result = client.execute(data, ResponseBodyHandlers.ofJSON());
        System.out.println(getApiKey("order","cancelReason"));
        System.out.println(result.getJSONObject("data").getJSONArray("fields"));
        for (int i = 0; i < result.getJSONObject("data").getJSONArray("fields").size();i++){
            JSONObject js = result.getJSONObject("data").getJSONArray("fields").getJSONObject(i);
            if (js.getString("apiKey").equals("cancelReason")){
                System.out.println(js);
            }
        }*/
    }
}
