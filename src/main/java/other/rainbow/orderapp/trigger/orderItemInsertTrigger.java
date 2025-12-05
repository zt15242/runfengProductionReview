package other.rainbow.orderapp.trigger;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rkhd.platform.sdk.data.model.Order;
import com.rkhd.platform.sdk.data.model.OrderProduct;
import com.rkhd.platform.sdk.exception.ApiEntityServiceException;
import com.rkhd.platform.sdk.exception.ScriptBusinessException;
import com.rkhd.platform.sdk.exception.XsyHttpException;
import com.rkhd.platform.sdk.http.RkhdHttpClient;
import com.rkhd.platform.sdk.model.BatchOperateResult;
import com.rkhd.platform.sdk.model.QueryResult;
import com.rkhd.platform.sdk.model.XObject;
import com.rkhd.platform.sdk.service.MetadataService;
import com.rkhd.platform.sdk.service.XObjectService;
import com.rkhd.platform.sdk.service.XoqlService;
import com.rkhd.platform.sdk.trigger.DataResult;
import com.rkhd.platform.sdk.trigger.Trigger;
import com.rkhd.platform.sdk.trigger.TriggerRequest;
import com.rkhd.platform.sdk.trigger.TriggerResponse;
import com.rkhd.platform.sdk.log.Logger;
import com.rkhd.platform.sdk.log.LoggerFactory;
import other.rainbow.orderapp.common.NeoCrmRkhdService;
import other.rainbow.orderapp.cstss.SqlFormatUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.util. * ;
import java.util.stream.Collectors;

//在新建或更新联系人时，判断是否存在有重名的联系人，有则提示重名，不允许创建
public class orderItemInsertTrigger implements Trigger {
    static Logger logger = LoggerFactory.getLogger();
    private static Map<String,String> houseApiMap;

    {
        try {
            houseApiMap = getApiKey("order", "wareHouse__c");
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ScriptBusinessException e) {
            throw new RuntimeException(e);
        }
    }
    @Override public TriggerResponse execute(TriggerRequest request) throws ScriptBusinessException {

        List < XObject > xObjects = request.getDataList();
        List < DataResult > dataResults = new ArrayList < DataResult > ();
        Logger logger = LoggerFactory.getLogger();
        Long recordId = null;
        Long orderId = null;
        Map<String, BigDecimal> appingNumMap = new HashMap<>();
        List<String> wlhList = new ArrayList<>();
        List<Long> recIdList = new ArrayList<>(); // 存放产品Id
        List<Long> ordIdList = new ArrayList<>(); // 存放订单Id
        List<String> tradNameList = new ArrayList<>(); // 存放tradeName
        Map<Long,String> proGoodMap = new HashMap<>();
        Map<Long,String> proTrdNameMap = new HashMap<>();
        Map<Long,BigDecimal> proNumMap = new HashMap<>();
        Map<Long, BigDecimal> proRateMap = new HashMap<>();
        Map<String, BigDecimal> goodQtyMap = new HashMap<>();
        for (XObject xObject: xObjects) {
            try {
                logger.error("xObjects：" + xObjects);
                logger.error("xObject：" + xObject);
                recordId = xObject.getAttribute("productId");
                recIdList.add(recordId);
                orderId = xObject.getAttribute("orderId");
                logger.error("记录ID：" + recIdList);
                logger.error("记录ID：" + orderId);
                dataResults.add(new DataResult(true, null, xObject));
            } catch(Exception e) {
                return new TriggerResponse(false, "Save failed：" + e.getMessage(), dataResults);
            }
        }
        JSONArray orderItemJson = null;
        String sql = "select id,orderId,orderId.Delivery_Plant__c,productId.External_matnr__c,Box__c,quantity,productId.Trade_Name__c,orderId.wareHouse__c,orderId.Sales_Org__c,Converted_quantity__c,productId.Trade_Name__c from orderProduct where orderId = " + orderId;
        QueryResult queryResult = null;
        try {
            queryResult = XoqlService.instance().query(sql, true);
        } catch (ApiEntityServiceException e) {
            throw new RuntimeException(e);
        }
        if (queryResult.getSuccess() && queryResult.getRecords().size() > 0) {
            orderItemJson = JSONArray.parseArray(queryResult.getRecords().toString());
            for (int i = 0; i < orderItemJson.size(); i++) {
                JSONObject jsstr = orderItemJson.getJSONObject(i);
                wlhList.add(jsstr.getString("productId.External_matnr__c"));
            }
        }
        if (!wlhList.isEmpty()) {
            try {
                appingNumMap = checkApprovalingNum(wlhList, orderId);
            } catch (ApiEntityServiceException e) {
                throw new RuntimeException(e);
            }
            logger.error("appingNumMap===" + appingNumMap);
        }
        if (recIdList.size() > 0){
            try {
                String sqlDN = "select id from Delivery_Note__c where Order__c = " + orderId;
                QueryResult dnInfo = XoqlService.instance().query(sqlDN,true);
                logger.error("dnInfo=="+dnInfo);
                List<OrderProduct> upOpList = new ArrayList<>();
                if (dnInfo.getSuccess() && dnInfo.getRecords().isEmpty()) {
                    for (XObject xObject: xObjects) {
                        OrderProduct op = (OrderProduct) xObject;
                        op.setUnshippedNumbers__c(op.getQuantity());
                        upOpList.add(op);
                    }
                }
                if (upOpList.size() > 0){
                    BatchOperateResult update = XObjectService.instance().update(upOpList,false,true);
                    if (!update.getSuccess()){
                        logger.error("赋值未发货数量失败====" + update.getErrorMessage());
                    }
                }
                String result = checkMatnr(recIdList,orderId);
                logger.error("结1果："+result);
                if ("".equals(result)){
                    // dataResults.add(new DataResult(true, null, null));

                    String wareHouse = "";
                    String plant = "";
                    List<Long> goodsIdList = new ArrayList<>();
                    String ordSql = "select id,wareHouse__c,Delivery_Plant__c,Sales_Org__c from _order where id = " + orderId;
                    QueryResult ordQuery = XoqlService.instance().query(ordSql,true);
                    if (ordQuery.getSuccess() && ordQuery.getRecords().size() > 0){
                        JSONArray ordJSON = JSONArray.parseArray(ordQuery.getRecords().toString());
                        if (ordJSON.getJSONObject(0).getJSONArray("Delivery_Plant__c") != null && ordJSON.getJSONObject(0).getJSONArray("wareHouse__c") != null) {
                            plant = ordJSON.getJSONObject(0).getJSONArray("Delivery_Plant__c").getString(0);
                            wareHouse = ordJSON.getJSONObject(0).getJSONArray("wareHouse__c").getString(0);
                        }
                    }
                    logger.error("plant===" + plant);
                    logger.error("wareHouse===" + wareHouse);
                    String proIds = SqlFormatUtils.joinLongInSql(recIdList);
                    String proSql = "select id,goods,goodsUnit.conversionRate,Trade_Name__c from product where id in (" + proIds + ")";
                    QueryResult proQuery = XoqlService.instance().query(proSql,true);
                    if (proQuery.getSuccess() && proQuery.getRecords().size() > 0){
                        JSONArray proJSON = JSONArray.parseArray(proQuery.getRecords().toString());
                        for (int i = 0;i < proJSON.size();i++){
                            JSONObject proJS = proJSON.getJSONObject(i);
                            goodsIdList.add(proJS.getLong("goods"));
                            if ("7030 Rainbow Agro(Ghana)".equals(plant)) {
                                proGoodMap.put(proJS.getLong("id"), proJS.getString("goods") + "-" + proJS.getString("Trade_Name__c"));
                                tradNameList.add(proJS.getString("goods") + "-" + proJS.getString("Trade_Name__c"));
                            } else {
                                proGoodMap.put(proJS.getLong("id"), proJS.getString("goods"));
                                tradNameList.add(proJS.getString("goods"));
                            }
                            proTrdNameMap.put(proJS.getLong("id"),proJS.getString("Trade_Name__c"));
                            proRateMap.put(proJS.getLong("id"), BigDecimal.valueOf(proJS.getDouble("goodsUnit.conversionRate")));
                        }
                    }
                    logger.error("proGoodMap===" + proGoodMap);
                    logger.error("proRateMap===" + proRateMap);
                    logger.error("tradNameList===" + tradNameList);
                    if (goodsIdList.size() > 0){
                        String goodIds = SqlFormatUtils.joinLongInSql(goodsIdList);
                        String invSql = "select id,product__c.External_matnr__c,product__c.Trade_Name__c,Goods__c,Storage_Location__c,plant__c,Inventory_Quantity__c,availableQty__c,brandName__c from Product_Inventory__c where Goods__c in (" + goodIds + ")";
                        QueryResult invQuery = XoqlService.instance().query(invSql,true);
                        if (invQuery.getSuccess() && invQuery.getRecords().size() > 0){
                            JSONArray invJSON = JSONArray.parseArray(invQuery.getRecords().toString());
                            logger.error("invJSON===" + invJSON);
                            for (int i = 0;i < invJSON.size();i++){
                                JSONObject invJS = invJSON.getJSONObject(i);
                                logger.error("tradeNameKey======" + (invJS.getString("Goods__c") + "-" + invJS.getString("brandName__c")));
                                if (invJS.getJSONArray("Storage_Location__c") != null){
                                    logger.error("tradeNameKey======" + invJS.getJSONArray("Storage_Location__c").getString(0) + "===" + wareHouse);
                                }

                                if (!"".equals(plant) && !"".equals(wareHouse) && (tradNameList.contains(invJS.getString("Goods__c") + "-" + invJS.getString("brandName__c")) || tradNameList.contains(invJS.getString("Goods__c"))) && invJS.getJSONArray("plant__c") != null && plant.equals(invJS.getJSONArray("plant__c").getString(0)) && invJS.getJSONArray("Storage_Location__c") != null && wareHouse.equals(invJS.getJSONArray("Storage_Location__c").getString(0))){
                                    logger.error("invJS===" + invJS);
                                    if ("7030 Rainbow Agro(Ghana)".equals(plant)) {
                                        if (invJS.getDouble("availableQty__c") != null && invJS.getLong("Goods__c") != null) {
                                            String mapKey = invJS.getString("product__c.External_matnr__c") + "-"+wareHouse+"-"+ invJS.getString("product__c.Trade_Name__c");
                                            logger.info("mapKey1：" + mapKey);
                                            BigDecimal tempNum = new BigDecimal(0);
                                            if (goodQtyMap.containsKey(invJS.getString("Goods__c") + "-" + invJS.getString("brandName__c"))) {
                                                tempNum = goodQtyMap.get(invJS.getString("Goods__c") + "-" + invJS.getString("brandName__c"));
                                            }
                                            if (appingNumMap.containsKey(mapKey)){
                                                tempNum = tempNum.add(BigDecimal.valueOf(invJS.getDouble("availableQty__c")).subtract(appingNumMap.get(mapKey)));
                                            }else {
                                                tempNum = tempNum.add(BigDecimal.valueOf(invJS.getDouble("availableQty__c")));
                                            }

                                            goodQtyMap.put(invJS.getString("Goods__c") + "-" + invJS.getString("brandName__c"), tempNum);
                                        }
                                    } else {
                                        if (invJS.getDouble("availableQty__c") != null && invJS.getLong("Goods__c") != null) {
                                            String mapKey2 = invJS.getString("product__c.External_matnr__c") + "-"+wareHouse;
                                            logger.info("mapKey2：" + mapKey2);
                                            BigDecimal tempNum = new BigDecimal(0);
                                            if (goodQtyMap.containsKey(invJS.getString("Goods__c"))) {
                                                tempNum = goodQtyMap.get(invJS.getString("Goods__c"));
                                            }
                                            if (appingNumMap.containsKey(mapKey2)){
                                                logger.error("1" );
                                                tempNum = tempNum.add(BigDecimal.valueOf(invJS.getDouble("availableQty__c")).subtract(appingNumMap.get(mapKey2)));
                                            }else {
                                                logger.error("2" );
                                                tempNum = tempNum.add(BigDecimal.valueOf(invJS.getDouble("availableQty__c")));
                                            }
                                            goodQtyMap.put(invJS.getString("Goods__c"), tempNum);
                                        }                                    }
                                }
                            }
                        }
                    }
                    logger.error("goodQtyMap===" + goodQtyMap);
                    if (proGoodMap.size() > 0 && goodQtyMap.size() > 0){
                        for (Long key : proGoodMap.keySet()){
                            if (goodQtyMap.containsKey(proGoodMap.get(key))){
                                proNumMap.put(key,goodQtyMap.get(proGoodMap.get(key)));
                            }
                        }
                    }
                    logger.error("proNumMap===" + proNumMap);
                    if (proNumMap.size() > 0 && !"7020 Rainbow Agro(Nigeria)".equals(plant)){
                        List<OrderProduct> avaiList = new ArrayList<>();
                        for (XObject xObject : request.getDataList()){
                            logger.error("upopppppppppppppppppppppppppppppp==进来了");
                            OrderProduct op = (OrderProduct) xObject;
                            if (proNumMap.containsKey(op.getProductId())) {
                                logger.error("upopppppppppppppppppppppppppppppp==进来了2");
                                op.setAvailableQty__c(Double.valueOf((proNumMap.get(op.getProductId()).divide(proRateMap.get(op.getProductId()),2)).toString()));
                                logger.error("upopppppppppppppppppppppppppppppp==进来了3");
                            } else {
                                op.setAvailableQty__c(0.00);
                            }
                            logger.error("upopppppppppppppppppppppppppppppp==" + op);
                            avaiList.add(op);
                        }
                        BatchOperateResult update = XObjectService.instance().update(avaiList,false,true);
                        if (!update.getSuccess()){
                            logger.error("更新有效数量失败===" + update.getErrorMessage());
                        }
                    }
                    return new TriggerResponse(true, null, dataResults);
                } else {
                    // dataResults.add(new DataResult(false, result, null));
                    logger.error("结果："+result);
                    return new TriggerResponse(false, result, new ArrayList<>());
                }
            } catch (Exception e){
                return new TriggerResponse(false, "Save failed：" + e.getMessage(), dataResults);

            }
        }
        return new TriggerResponse(true, null, dataResults);
    }
    public static String checkMatnr(List<Long> recIdList,Long orderId){
        String result = "";
        if (recIdList.size() > 0){
            try {
                List < DataResult > dataResults = new ArrayList < DataResult > ();
                Logger logger = LoggerFactory.getLogger();
                RkhdHttpClient rkhdclient = RkhdHttpClient.instance();
                // 将 List<Long> 转换为字符串，使用逗号分隔
                String recIdListStr = recIdList.stream()
                        .map(String::valueOf) // 将 Long 转换为 String
                        .collect(Collectors.joining(",")); // 连接为一个字符串
                String sqlPro = "select id,External_matnr__c from product where id IN(" + recIdListStr + ")";
                String sqlOrder = "select id,Transfer__c from _order where id = " + orderId + " and entityType = " + MetadataService.instance().getBusiType("Order", "RoadSale__c").getId();

                JSONArray proInfo = NeoCrmRkhdService.xoql(rkhdclient, sqlPro);
                JSONArray orderProInfo = NeoCrmRkhdService.xoql(rkhdclient, sqlOrder);
                logger.error("proInfo=="+proInfo);
                logger.error("orderProInfo=="+orderProInfo);
                // 用于存放调拨单ID
                Long tranId = null;
                // 用于存放物料号
                List<String> matnrList = new ArrayList<>();
                if (orderProInfo.size() > 0){
                    // 判断订单是否存在调拨单
                    if (orderProInfo.getJSONObject(0).getLong("Transfer__c") != null){
                        // 赋值调拨单ID
                        tranId = orderProInfo.getJSONObject(0).getLong("Transfer__c");
                    }
                    logger.error("调拨单ID：" + tranId);
                    if (tranId != null){
                        // 通过调拨单查询调拨单明细
                        String tranSql = "select id,goods__c,goods__c.External_matnr__c,goods__c.External_Id__c from Transfer_Item__c where transfer__c = " + tranId;
                        JSONArray tranInfo = NeoCrmRkhdService.xoql(rkhdclient, tranSql);
                        logger.error("调拨查询结果：" + tranInfo);
                        if (tranInfo.size() > 0){
                            for (Integer i = 0;i < tranInfo.size();i++){
                                // 将调拨单明细中商品的物料号插入到List里
                                matnrList.add(tranInfo.getJSONObject(i).getString("goods__c.External_Id__c"));
                            }
                        }
                    }
                    logger.error("调拨单明细物料号：" + matnrList);
                    // 便利排查订单明细
                    for (Integer i = 0;i < proInfo.size();i++){
                        // 判断订单明细中的产品的物料号是否为空
                        if (!"".equals(proInfo.getJSONObject(i).getString("External_matnr__c"))){
                            // 判断订单明细的物料号是否是调拨明细中的物料号
                            if (matnrList.size() > 0 && !matnrList.contains(proInfo.getJSONObject(i).getString("External_matnr__c"))){
                                logger.error("调拨单不存在：" + proInfo.getJSONObject(i).getString("External_matnr__c"));
                                //TODO 订单明细中的产品物料号必须是调拨明细单中的物料
                                result = "The product material number in the order details must be the material in the transfer details.";
                                // throw new RuntimeException("The product material number in the order details must be the material in the transfer details.");
//                                return new TriggerResponse(false, result,null);
                                return result;
                            }
                        }
                    }
                }
            } catch (IOException e) {
                result += e.getMessage();
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                result += e.getMessage();
                throw new RuntimeException(e);
            } catch (XsyHttpException e) {
                result += e.getMessage();
                throw new RuntimeException(e);
            } catch (ScriptBusinessException e) {
                result += e.getMessage();
                throw new RuntimeException(e);
            } catch (ApiEntityServiceException e) {
                throw new RuntimeException(e);
            }
        }
        return result;
    }
    public static Map<String,BigDecimal> checkApprovalingNum(List<String> wlhList , Long ordId) throws ApiEntityServiceException {
        Map<String,BigDecimal> wlhMap = new HashMap<>();
        String wlhCode = SqlFormatUtils.joinInSql(wlhList);
            String sql = "select id,quantity,Converted_quantity__c,orderId,orderId.po,orderId.Sales_Org__c,orderId.Status__c,orderId.Delivery_Plant__c,orderId.wareHouse__c,productId.External_matnr__c,productId.Trade_Name__c from orderProduct where orderId.Status__c = 2 and productId.External_matnr__c IN( " + wlhCode + ") and entityType <> " + MetadataService.instance().getBusiType("Order", "orderReturn__c").getId() + " and orderId <> " + ordId;
        QueryResult ordItemQuery = XoqlService.instance().query(sql,true);
        if (ordItemQuery.getSuccess() && ordItemQuery.getRecords().size() > 0){
            JSONArray ordItemJson = JSONArray.parseArray(ordItemQuery.getRecords().toString());
            logger.error("ordItemJson===" + ordItemJson);
            for (int i = 0;i < ordItemJson.size();i++){
                JSONObject jsstr = ordItemJson.getJSONObject(i);
                String mapKey = "";
                if (
                        jsstr.getJSONArray("orderId.Delivery_Plant__c").get(0).equals("7030 Rainbow Agro(Ghana)")) {
                    mapKey = jsstr.getString("productId.External_matnr__c") + "-" + jsstr.getJSONArray("orderId.wareHouse__c").get(0) + "-" + jsstr.getString("productId.Trade_Name__c");
                } else {
                    mapKey = jsstr.getString("productId.External_matnr__c") + "-" + jsstr.getJSONArray("orderId.wareHouse__c").get(0);
                }
                if (wlhMap.containsKey(mapKey)){
                    if (jsstr.getDouble("quantity") > 0) {
                        BigDecimal tempNum = wlhMap.get(mapKey).add(BigDecimal.valueOf(jsstr.getDouble("quantity")));
                        wlhMap.put(mapKey,tempNum);
                    }
                } else {
                    if (jsstr.getDouble("quantity") > 0) {
                        wlhMap.put(mapKey, BigDecimal.valueOf(jsstr.getDouble("quantity")));
                    }
                }
            }
        }
        return wlhMap;
    }
    public static Map<String,String> getApiKey(String obj,String apiKey) throws IOException, ScriptBusinessException {
        RkhdHttpClient instance = RkhdHttpClient.instance();
        Map<String, Map<String, String>> kvMap = new HashMap<>();
        Map<String, Map<Integer, String>> vkMap = new HashMap<>();
        Map<String,Map<String,String>> pkMap = NeoCrmRkhdService.getPicklistValueKey(instance,obj,apiKey,kvMap,vkMap);
        // logger.error("apikeyMap打印：" + pkMap);
        return pkMap.get(apiKey);
    }
    public static void main(String[] args) throws ScriptBusinessException {
        /*TriggerRequest req = new TriggerRequest();
        XObject ot = new OrderProduct();
        ot.setId(Long.getLong("3611778914010143"));
        List<XObject> list1 = new ArrayList<>();
        list1.add(ot);
        req.setDataList(list1);
        orderItemInsertTrigger trigger = new orderItemInsertTrigger();
        trigger.c(req);*/
        Long a = Long.valueOf("3611778914010128");
        List<Long> aList = new ArrayList<>();
        aList.add(a);
        Long b = Long.valueOf("3635587658089517");
        orderItemInsertTrigger.checkMatnr(aList,b);
    }
}