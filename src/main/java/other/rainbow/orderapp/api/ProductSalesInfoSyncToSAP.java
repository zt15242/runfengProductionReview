/**
 * @ClassName ProductSalesInfoSyncToSAP
 * @Auther Chi-Lynne张宇恒
 * @Discription 提交审批后调用库存接口
 **/
package other.rainbow.orderapp.api;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rkhd.platform.sdk.creekflow.approvalevent.ApprovalEvent;
import com.rkhd.platform.sdk.creekflow.approvalevent.ApprovalEventRequest;
import com.rkhd.platform.sdk.creekflow.approvalevent.ApprovalEventResponse;
import com.rkhd.platform.sdk.data.model.Goods;
import com.rkhd.platform.sdk.data.model.Order;
import com.rkhd.platform.sdk.data.model.OrderProduct;
import com.rkhd.platform.sdk.data.model.Product_Inventory__c;
import com.rkhd.platform.sdk.exception.ApiEntityServiceException;
import com.rkhd.platform.sdk.exception.CustomConfigException;
import com.rkhd.platform.sdk.exception.ScriptBusinessException;
import com.rkhd.platform.sdk.exception.XsyHttpException;
import com.rkhd.platform.sdk.http.*;
import com.rkhd.platform.sdk.http.handler.ResponseBodyHandlers;
import com.rkhd.platform.sdk.log.Logger;
import com.rkhd.platform.sdk.log.LoggerFactory;
import com.rkhd.platform.sdk.model.BatchOperateResult;
import com.rkhd.platform.sdk.model.OperateResult;
import com.rkhd.platform.sdk.model.QueryResult;
import com.rkhd.platform.sdk.service.CustomConfigService;
import com.rkhd.platform.sdk.service.MetadataService;
import com.rkhd.platform.sdk.service.XObjectService;
import com.rkhd.platform.sdk.service.XoqlService;
import org.apache.commons.lang.StringUtils;
import other.rainbow.orderapp.common.NeoCrmRkhdService;
import other.rainbow.orderapp.common.ObjectOptionValueRetrieval;
import other.rainbow.orderapp.cstss.SqlFormatUtils;
import other.rainbow.orderapp.pojo.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

// 提交审批后调用库存接口
public class ProductSalesInfoSyncToSAP implements ApprovalEvent {
    static Map<String,Long> goodIdMap = new HashMap<>();
    static Map<String,Long> proIdMap = new HashMap<>(); // 20250109   zyh   增加产品
    static Long tempId = null;
    @Override
    public ApprovalEventResponse execute(ApprovalEventRequest approvalEventRequest) throws ScriptBusinessException {
        logger.warn("开始执行通过后事件");
        // 实体apikey
        approvalEventRequest.getEntityApiKey();
        //数据Id
        approvalEventRequest.getDataId();
        //待处理任务Id
        approvalEventRequest.getUsertaskLogId();

        ApprovalEventResponse response = new ApprovalEventResponse();

        try {
            //提交后事件实现
            ReturnResult result = productSalesInfo(approvalEventRequest.getDataId().toString(),"ProductSalesInfoSyncToSAP");

            if (result.getIsSuccess()){
                response.setSuccess(true);
                response.setMsg("通过后事件执行成功");
            }else {
                response.setSuccess(false);
                response.setMsg(result.getMessage());
            }
        } catch (Exception e) {
            logger.error("通过后事件执行失败:" + e.getMessage());
            response.setSuccess(false);
            response.setMsg("通过后事件执行失败" + e.getMessage());
        }

        return response;
    }
    private static final Logger logger = LoggerFactory.getLogger();
    //    public static final String END_POINT = "productNumber";
    public static final String END_POINT = "ProductSalesInfoSyncToSAP";
    //private static final String URL="http://120.224.116.35:8901/sap/inventoryinfo/sync";
    private static final String URL="";

    private static Map<String,String> plantApiMap;

    {
        try {
            plantApiMap = getApiKey("order","Delivery_Plant__c");
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ScriptBusinessException e) {
            throw new RuntimeException(e);
        }
    }

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


        public static  ReturnResult productSalesInfo1(String Id, String changeType) throws Exception {
        long longID = Long.parseLong(Id);
        RoundingMode roundingMode = RoundingMode.HALF_UP;
        tempId = longID;
        if (plantApiMap == null){
            try {
                plantApiMap = getApiKey("order","Delivery_Plant__c");
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (ScriptBusinessException e) {
                throw new RuntimeException(e);
            }
        }
        if (houseApiMap == null){
            try {
                houseApiMap = getApiKey("order", "wareHouse__c");
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (ScriptBusinessException e) {
                throw new RuntimeException(e);
            }
        }
        BigDecimal cbMoney = new BigDecimal(0);
        BigDecimal cbUSMoney = new BigDecimal(0);
        BigDecimal freeCbMoney = new BigDecimal(0);
        /*CommonHttpClient commonHttpClient= CommonHttpClient.instance();
        CommonData commonData=new CommonData();
        commonData.addHeader("Content-Type","application/json");
        commonData.setCallString(url);
        commonData.setCall_type("POST");*/


        RkhdHttpClient rkhdclient = RkhdHttpClient.instance();
        reqresultinfo reqresultinfo = new reqresultinfo();
        List<items> itemsList = new ArrayList<>();
        List<reqresultinfo> reqresultinfoList = new ArrayList<>();
        String errorMessage = "";
        Boolean isSuccess = true;
        String numStr = "";
        // TODO
        String sqlOrder = "select id,amount,huilv__c,Rate__c,entityType,currencyUnit from _order where id = "+longID;
        QueryResult<Order> ordResult = XObjectService.instance().query(sqlOrder,true);
        if(!ordResult.getSuccess()) {
            throw new Exception("Order retrieval error");
        }
        String sqlOrderItem = "select id,material_Number__c,unitPrice,orderId.Rate__c,orderId.Sales_Org__c,Converted_quantity__c,productId,productId.goods,productId.goods.factory__c,orderId.Delivery_Plant__c,quantity,productId.External_matnr__c,productId.goods,orderId.wareHouse__c,productId.Trade_Name__c from orderProduct where orderId = "+Id;
        JSONArray orderItemInfo = NeoCrmRkhdService.xoql(rkhdclient,sqlOrderItem);
        if(orderItemInfo == null) {
            logger.warn("orderinfo: " +orderItemInfo.toJSONString());
            throw new Exception("No order line data available");
        }

        // 存放返回值物料号（KEY）和订单明细数量=返回JSON
        Map<String,BigDecimal> itemMap = new HashMap<>();
        Map<String,BigDecimal> itemOrdQtyMap = new HashMap<>();
        Map<String,String> itemOrdBradMap = new HashMap<>();
        Map<String,BigDecimal> itemQtyRateMap = new HashMap<>();
        Map<String,Long> ordItemMap = new HashMap<>();
        // 存放商品ID（key）和商品库存表JSON
        Map<String, JSON> proInvMap = new HashMap<>();
        List<String> goodsIds = new ArrayList<>();
        // 存放物料号，用于查找审批中的订单明细
        List<String> wlhList = new ArrayList<>();
        Map<String,BigDecimal> wlhNumMap = new HashMap<>();
        // 需要的金额
        BigDecimal needMoney = new BigDecimal(0);
        //存物料号/需要的金额
        Map<String,BigDecimal> needMap = new HashMap<>();
//        BigDecimal needMoney = new BigDecimal(0);
        for(Integer i = 0 ;i<orderItemInfo.size(); i++){
            JSONObject jsstr= orderItemInfo.getJSONObject(i);
            if(jsstr == null) {
                // logger.error("zyhTest:"+jsstr.toJSONString());
                continue;
            }
            // 20241224   zyh   给商品赋值
            goodIdMap.put(jsstr.getString("productId.External_matnr__c"),jsstr.getLong("productId.goods"));
            proIdMap.put(jsstr.getString("productId.External_matnr__c"),jsstr.getLong("productId")); // 20250109   zyh   增加产品
            items hos = new items();
            //工厂
//            hos.setWerks(jsstr.getString("productId.goods"));
            if (jsstr.getJSONArray("orderId.Delivery_Plant__c") != null){
                hos.setWerks(plantApiMap.get(jsstr.getJSONArray("orderId.Delivery_Plant__c").getString(0)));
            } else {
                hos.setWerks("");
            }
//            hos.setWERKS("测试werks");
            //物料号
            hos.setMatnr(jsstr.getString("productId.External_matnr__c"));
//            hos.setMATNR("测试matnr");
            itemsList.add(hos);
            if (jsstr.getJSONArray("orderId.wareHouse__c") != null) {
                if (jsstr.getJSONArray("orderId.Sales_Org__c").get(0).equals("7190 AGROTERRUM GHANA") ||
                        jsstr.getJSONArray("orderId.Sales_Org__c").get(0).equals("7030 Rainbow Agro(Ghana)")) {
//                    itemMap.put(jsstr.getString("productId.External_matnr__c") + "-" + houseApiMap.get(jsstr.getJSONArray("orderId.wareHouse__c").getString(0)) + "-" + jsstr.getString("productId.Trade_Name__c"), BigDecimal.valueOf(jsstr.getDouble("Converted_quantity__c")));

                    // hql 20251027 新增
                    if (itemMap.get(jsstr.getString("productId.External_matnr__c") + "-" + houseApiMap.get(jsstr.getJSONArray("orderId.wareHouse__c").getString(0)) + "-" + jsstr.getString("productId.Trade_Name__c")) != null){
                        BigDecimal qty = itemMap.get(jsstr.getString("productId.External_matnr__c") + "-" + houseApiMap.get(jsstr.getJSONArray("orderId.wareHouse__c").getString(0)) + "-" + jsstr.getString("productId.Trade_Name__c"));
                        itemMap.put(jsstr.getString("productId.External_matnr__c") + "-" + houseApiMap.get(jsstr.getJSONArray("orderId.wareHouse__c").getString(0)) + "-" + jsstr.getString("productId.Trade_Name__c"), qty.add(BigDecimal.valueOf(jsstr.getDouble("Converted_quantity__c"))));
                    }else {
                        itemMap.put(jsstr.getString("productId.External_matnr__c") + "-" + houseApiMap.get(jsstr.getJSONArray("orderId.wareHouse__c").getString(0)) + "-" + jsstr.getString("productId.Trade_Name__c"), BigDecimal.valueOf(jsstr.getDouble("Converted_quantity__c")));
                    }
                    if (itemOrdQtyMap.get(jsstr.getString("productId.External_matnr__c") + "-" + houseApiMap.get(jsstr.getJSONArray("orderId.wareHouse__c").getString(0)) + "-" + jsstr.getString("productId.Trade_Name__c")) != null){
                        BigDecimal ordQty = itemOrdQtyMap.get(jsstr.getString("productId.External_matnr__c") + "-" + houseApiMap.get(jsstr.getJSONArray("orderId.wareHouse__c").getString(0)) + "-" + jsstr.getString("productId.Trade_Name__c"));
                        itemOrdQtyMap.put(jsstr.getString("productId.External_matnr__c") + "-" + houseApiMap.get(jsstr.getJSONArray("orderId.wareHouse__c").getString(0)) + "-" + jsstr.getString("productId.Trade_Name__c"), ordQty.add(BigDecimal.valueOf(jsstr.getDouble("quantity"))));
                    } else {
                        itemOrdQtyMap.put(jsstr.getString("productId.External_matnr__c") + "-" + houseApiMap.get(jsstr.getJSONArray("orderId.wareHouse__c").getString(0)) + "-" + jsstr.getString("productId.Trade_Name__c"), BigDecimal.valueOf(jsstr.getDouble("quantity")));
                    }

                    itemQtyRateMap.put(jsstr.getString("productId.External_matnr__c") + "-" + houseApiMap.get(jsstr.getJSONArray("orderId.wareHouse__c").getString(0)) + "-" + jsstr.getString("productId.Trade_Name__c"), BigDecimal.valueOf(jsstr.getDouble("Converted_quantity__c")).divide(new BigDecimal(jsstr.getDouble("quantity")),2,roundingMode));
                    ordItemMap.put(jsstr.getString("productId.External_matnr__c") + "-" + houseApiMap.get(jsstr.getJSONArray("orderId.wareHouse__c").getString(0)) + "-" + jsstr.getString("productId.Trade_Name__c"), jsstr.getLong("id"));
                } else {
//                    itemMap.put(jsstr.getString("productId.External_matnr__c") + "-" + houseApiMap.get(jsstr.getJSONArray("orderId.wareHouse__c").getString(0)), BigDecimal.valueOf(jsstr.getDouble("Converted_quantity__c")));
                    // hql 20251027 新增
                    if (itemMap.get(jsstr.getString("productId.External_matnr__c") + "-" + houseApiMap.get(jsstr.getJSONArray("orderId.wareHouse__c").getString(0))) != null){
                        BigDecimal qty = itemMap.get(jsstr.getString("productId.External_matnr__c") + "-" + houseApiMap.get(jsstr.getJSONArray("orderId.wareHouse__c").getString(0)));
                        itemMap.put(jsstr.getString("productId.External_matnr__c") + "-" + houseApiMap.get(jsstr.getJSONArray("orderId.wareHouse__c").getString(0)), qty.add(BigDecimal.valueOf(jsstr.getDouble("Converted_quantity__c"))));
                    }else {
                        itemMap.put(jsstr.getString("productId.External_matnr__c") + "-" + houseApiMap.get(jsstr.getJSONArray("orderId.wareHouse__c").getString(0)), BigDecimal.valueOf(jsstr.getDouble("Converted_quantity__c")));
                    }
                    if (itemOrdQtyMap.get(jsstr.getString("productId.External_matnr__c") + "-" + houseApiMap.get(jsstr.getJSONArray("orderId.wareHouse__c").getString(0))) != null){
                        BigDecimal ordQty = itemOrdQtyMap.get(jsstr.getString("productId.External_matnr__c") + "-" + houseApiMap.get(jsstr.getJSONArray("orderId.wareHouse__c").getString(0)));
                        itemOrdQtyMap.put(jsstr.getString("productId.External_matnr__c") + "-" + houseApiMap.get(jsstr.getJSONArray("orderId.wareHouse__c").getString(0)), ordQty.add(BigDecimal.valueOf(jsstr.getDouble("quantity"))));
                    } else {
                        itemOrdQtyMap.put(jsstr.getString("productId.External_matnr__c") + "-" + houseApiMap.get(jsstr.getJSONArray("orderId.wareHouse__c").getString(0)), BigDecimal.valueOf(jsstr.getDouble("quantity")));
                    }
                    itemOrdBradMap.put(jsstr.getString("productId.External_matnr__c") + "-" + houseApiMap.get(jsstr.getJSONArray("orderId.wareHouse__c").getString(0)), jsstr.getString("productId.Trade_Name__c"));
                    itemQtyRateMap.put(jsstr.getString("productId.External_matnr__c") + "-" + houseApiMap.get(jsstr.getJSONArray("orderId.wareHouse__c").getString(0)), BigDecimal.valueOf(jsstr.getDouble("Converted_quantity__c")).divide(new BigDecimal(jsstr.getDouble("quantity")),2,roundingMode));
                    ordItemMap.put(jsstr.getString("productId.External_matnr__c") + "-" + houseApiMap.get(jsstr.getJSONArray("orderId.wareHouse__c").getString(0)), jsstr.getLong("id"));
                }
            } else {
                throw new Exception("No warehouse");
            }
            wlhList.add(jsstr.getString("productId.External_matnr__c"));
            if (jsstr.getString("productId.goods") != null && !jsstr.getString("productId.goods").isEmpty()) {
                goodsIds.add(jsstr.getString("productId.goods"));
            }
            logger.info("bankinfo: " +itemsList.size());
            // logger.info("bankinfo: " +itemsList.get(0));
        }
        if (!wlhList.isEmpty()){
            wlhNumMap = checkNum(wlhList);
            // 如果有进行中的库存
            /* if (wlhNumMap.size() > 0){
                // 20241224   zyh   库存比较   start
                logger.info("itemMap===" + itemMap);
                logger.info("wlhNumMap===" + wlhNumMap);
                for (String key : itemMap.keySet()){
                    if (wlhNumMap.get(key) != null){
                        BigDecimal num = wlhNumMap.get(key).add(itemMap.get(key));
                        // wlhNumMap.put(key,num);
                        logger.error("添加审批中后数量：" + itemMap);*/
            // 如果库存小于0的话记录str
                        /*if (num.compareTo(new BigDecimal(0)) < 0){
                            numStr += key + "Order need " + itemMap.get(key) + "，Warehouse have " + num + "；";
                        }*/
                    /*} else {
                        numStr += key + "Order need " + itemMap.get(key) + "，Warehouse is null；";
                    }*/
                /*}
                logger.info("numStr===" + numStr);
                // 20241224   zyh   库存比较   end
            }*/
        }



        /*JSONObject parameter = new JSONObject();
        JSONObject header = new JSONObject();
        header.put("token",tokenStr);
        parameter.put("head",header);
        parameter.put("body",requestBody);*/

        HttpResult result = new HttpResult();
        ReturnResult returnResult = new ReturnResult();
        try{
            // 同步数据7
            result =  crmToSap(itemsList);
            logger.info("ceshi1212:" + result.getResult().toString());
            logger.info("ceshi1212:" + result);
            logger.info("接口返回result: " + JSON.toJSONString(result));
            returnResult.setIsSuccess(true);
            String response = result.getResult();
            JSONObject esbinfo = JSONObject.parseObject(response).getJSONObject("esbinfo");
            JSONArray resultinfo = JSONObject.parseObject(response).getJSONArray("resultinfo");
            JSONObject responseJson = (JSONObject) JSONObject.parse(result.getResult());
         /*   logger.error("测试1212：" + responseJson.toString());
            logger.error("测试1216001：" + esbinfo.toString());
            logger.error("测试1216002：" + resultinfo.toString());*/
            // 获取返回状态值
            String returnStatus = esbinfo.getString("returnstatus");
//            String returnStatus = "S";
//            String partner = responseJson.getJSONArray("resultinfo").getJSONObject(0).getString("partner");
            List<respesbinfo> infoList = new ArrayList<>();
            Order acc = new Order();
//            List<XObject> updateList = new ArrayList<>();
//            List<XObject> updateList2 = new ArrayList<>();
//            int StatusCode = ((JSONObject) JSONObject.parse(result.getResult())).getString("");
//            // TODO 成功方式判断待定
//            isSuccess = StatusCode == 500;
            logger.error("isSuccess+++" + isSuccess);
            if (isSuccess){
                logger.error("isSuccess+++");
                logger.error("resultinfo.size===" + resultinfo.size());
                if (responseJson.getJSONArray("resultinfo").size() > 0){
                    // 20241224   zyh   计算毛利率   start
                    Map<String,BigDecimal> cbMap = new HashMap<>(); // 成本Map，存储物料号/单价
                    Map<String,BigDecimal> cbUSMap = new HashMap<>(); // 成本Map，存储物料号/单价
                    Map<String,BigDecimal> kcMap = new HashMap<>(); // 库存Map，存储物料号/单价
                    for (int i = 0;i < resultinfo.size();i++){
                        String matnr = resultinfo.getJSONObject(i).getString("matnr");
                        String mapKey = resultinfo.getJSONObject(i).getString("matnr") + "-" + resultinfo.getJSONObject(i).getString("werks") + "_" + resultinfo.getJSONObject(i).getString("lgort");
                        if ("7190 AGROTERRUM GHANA".equals(orderItemInfo.getJSONObject(0).getJSONArray("orderId.Sales_Org__c").getString(0)) ||
                                "7030 Rainbow Agro(Ghana)".equals(orderItemInfo.getJSONObject(0).getJSONArray("orderId.Sales_Org__c").getString(0))){
                            mapKey = resultinfo.getJSONObject(i).getString("matnr") + "-" + resultinfo.getJSONObject(i).getString("werks") + "_" + resultinfo.getJSONObject(i).getString("lgort") + "-" + resultinfo.getJSONObject(i).getString("atwrt");
                        }
                        logger.error("clabs===" + resultinfo.getJSONObject(i).getString("clabs"));
                        BigDecimal mapValue = BigDecimal.valueOf(Double.valueOf(resultinfo.getJSONObject(i).getString("clabs").trim()));
                        if (!"".equals(resultinfo.getJSONObject(i).getString("omeng"))){
                            if (itemQtyRateMap.containsKey(mapKey)){
                                logger.error("20250627==1==" + mapValue);
                                logger.error("20250627==2==" + resultinfo.getJSONObject(i).getString("omeng"));
                                mapValue = mapValue.subtract(BigDecimal.valueOf(Double.valueOf(resultinfo.getJSONObject(i).getString("omeng"))));
                                logger.error("20250627==3==" + mapValue);
                                logger.error("20250627==4==" + itemQtyRateMap.get(mapKey));
                            }
                        }
                        if (kcMap.containsKey(mapKey)){
                            BigDecimal tempNum = kcMap.get(mapKey).add(mapValue);
                            kcMap.put(mapKey,tempNum);
                        } else {
                            kcMap.put(mapKey,mapValue);
                        }
                        logger.error("kcMap===" + kcMap);
                        logger.error("matnr+++" + matnr);
                        logger.error("resultinfo+++" + resultinfo.getJSONObject(i));
                        // TODO 成本价改成vmkum
                        BigDecimal verpr = new BigDecimal(0);
                        if (numStr != null && !"".equals(resultinfo.getJSONObject(i).getString("verpr"))) {
                            verpr = new BigDecimal(resultinfo.getJSONObject(i).getString("verpr").trim());
                        }
                        logger.error("verpr+++" + verpr);
                        logger.error("verpr+1++" + resultinfo.getJSONObject(i).getString("verpr"));
                        cbMap.put(matnr,verpr);
                        // TODO 成本价改成verpr
                        BigDecimal vmkum = new BigDecimal(0);
                        if (numStr != null && !"".equals(resultinfo.getJSONObject(i).getString("vmkum"))) {
                            vmkum = new BigDecimal(resultinfo.getJSONObject(i).getString("vmkum").trim());
                        }
                        logger.error("vmkum+++" + vmkum);
                        logger.error("vmkum+1++" + resultinfo.getJSONObject(i).getString("vmkum"));
                        cbUSMap.put(matnr,vmkum);

                    }
                    // if (wlhNumMap.size() > 0){
                    if (itemMap.size() > 0){
                        List<OrderProduct> opList = new ArrayList<>();
                        for (String str : itemMap.keySet()){ // wlhNumMap===>itemMap↓↓↓↓↓↓↓↓↓
                            OrderProduct op = new OrderProduct();
                            op.setId(ordItemMap.get(str));
                            logger.error("库存对比：" + str);
                            logger.error("库存对比：" + str + kcMap.containsKey(str));
                            BigDecimal appingNum = new BigDecimal(0);
                            BigDecimal excAppingNum = new BigDecimal(0);
                            if (kcMap.containsKey(str)){
                                logger.error("库存对比：" + str + kcMap.containsKey(str) + kcMap.get(str));
                                logger.error("库存对比：" + wlhNumMap);
                                if (wlhNumMap.containsKey(str)) {
                                    logger.error("库存对比：" + kcMap.get(str).subtract(wlhNumMap.get(str)).compareTo(new BigDecimal(0)));
                                    logger.error("库存对比：" + kcMap.get(str).subtract(wlhNumMap.get(str)));
                                    logger.error("库存对比：" + kcMap.get(str) + "===" + wlhNumMap.get(str));
                                    if (kcMap.get(str).subtract(wlhNumMap.get(str).add(itemMap.get(str))).compareTo(new BigDecimal(0)) < 0) {
                                        if ("7190 AGROTERRUM GHANA".equals(orderItemInfo.getJSONObject(0).getJSONArray("orderId.Sales_Org__c").getString(0)) ||
                                                "7030 Rainbow Agro(Ghana)".equals(orderItemInfo.getJSONObject(0).getJSONArray("orderId.Sales_Org__c").getString(0))) {
                                            String[] tradName = str.split("-");
                                            // numStr += str + " Order need " + itemOrdQtyMap.get(str) + " PC " + tradName[tradName.length - 1] + "，there are " + wlhNumMap.get(str) + " PC in progress，Warehouse have " + kcMap.get(str).divide(itemQtyRateMap.get(str), 1, roundingMode) + " PC；";
                                            numStr += str + " Order need " + itemOrdQtyMap.get(str) + " PC " + tradName[tradName.length - 1] + "，there are " + wlhNumMap.get(str).divide(itemQtyRateMap.get(str),1,roundingMode) + " PC in progress，Warehouse have " + kcMap.get(str).divide(itemQtyRateMap.get(str), 1, roundingMode) + " PC；";
                                        } else {
                                            // numStr += str + "-" + itemOrdBradMap.get(str) + " Order need " + itemOrdQtyMap.get(str) + " PC，there are " + wlhNumMap.get(str) + " PC in progress，Warehouse have " + kcMap.get(str).divide(itemQtyRateMap.get(str), 1, roundingMode) + " PC；";
                                            numStr += str + "-" + itemOrdBradMap.get(str) + " Order need " + itemOrdQtyMap.get(str) + " PC，there are " + wlhNumMap.get(str).divide(itemQtyRateMap.get(str),1,roundingMode) + " PC in progress，Warehouse have " + kcMap.get(str).divide(itemQtyRateMap.get(str), 1, roundingMode) + " PC；";
                                        }
                                    }
                                    appingNum = wlhNumMap.get(str).divide(itemQtyRateMap.get(str),1,roundingMode);
                                    excAppingNum = (kcMap.get(str).subtract(wlhNumMap.get(str))).divide(itemQtyRateMap.get(str),1,roundingMode);
                                } else {
                                    if (kcMap.get(str).subtract(itemMap.get(str)).compareTo(new BigDecimal(0)) < 0) {
                                        if ("7190 AGROTERRUM GHANA".equals(orderItemInfo.getJSONObject(0).getJSONArray("orderId.Sales_Org__c").getString(0)) ||
                                                "7030 Rainbow Agro(Ghana)".equals(orderItemInfo.getJSONObject(0).getJSONArray("orderId.Sales_Org__c").getString(0))) {
                                            String[] tradName = str.split("-");
                                            numStr += str + " Order need " + itemOrdQtyMap.get(str) + " PC " + tradName[tradName.length - 1] + "，there are 0.0 PC in progress，Warehouse have " + kcMap.get(str).divide(itemQtyRateMap.get(str), 1, roundingMode) + " PC；";
                                        } else {
                                            numStr += str + "-" + itemOrdBradMap.get(str) + " Order need " + itemOrdQtyMap.get(str) + " PC，there are 0.0 PC in progress，Warehouse have " + kcMap.get(str).divide(itemQtyRateMap.get(str), 1, roundingMode) + " PC；";
                                        }
                                    }
                                    excAppingNum = kcMap.get(str).divide(itemQtyRateMap.get(str),1,roundingMode);
                                }
                            } else {
                                logger.error("20250217==" + numStr);
                                if ("7190 AGROTERRUM GHANA".equals(orderItemInfo.getJSONObject(0).getJSONArray("orderId.Sales_Org__c").getString(0)) ||
                                        "7030 Rainbow Agro(Ghana)".equals(orderItemInfo.getJSONObject(0).getJSONArray("orderId.Sales_Org__c").getString(0))) {
                                    String[] tradName = str.split("-");
                                    numStr += str + " Order need " + itemOrdQtyMap.get(str) + " PC " + tradName[tradName.length - 1] + "，there are 0.0 PC in progress，Warehouse is null ；";
                                } else {
                                    numStr += str + "-" + itemOrdBradMap.get(str) + " Order need " + itemOrdQtyMap.get(str) + " PC，there are 0.0 PC in progress，Warehouse is null ；";
                                }
                            }

                        }
                        if (StringUtils.isNotBlank(numStr)){
                            returnResult.setMessage(numStr);
                            returnResult.setIsSuccess(false);
                            return returnResult;
                        }
                    }
                }else {
//                    returnResult.setMessage(responseJson.toJSONString());
                    returnResult.setMessage(esbinfo.getString("returnmsg"));
                    returnResult.setIsSuccess(false);
                    isSuccess = false;
                    errorMessage =responseJson.toJSONString();
//                    // Todo 错误邮件发送未写Utility.notificationUser(accList[0].OwnerId,Id);
                }
            }else {
                returnResult.setMessage(esbinfo.getString("returnmsg"));
                returnResult.setIsSuccess(false);
                isSuccess = false;
                errorMessage =responseJson.toJSONString();
//                // Todo 错误邮件发送未写Utility.notificationUser(accList[0].OwnerId,Id);
            }


        }catch (Exception e){
            logger.error("错误进来catch了：" + e.getMessage() + "行号：" + e.getStackTrace()[0].getLineNumber() + "类命：" + e.getStackTrace()[0].getClassName());
            logger.debug("错误进来catch了：" + e.getMessage() + "行号：" + e.getStackTrace()[0].getLineNumber() + "类命：" + e.getStackTrace()[0].getClassName());
            isSuccess = false;
            errorMessage = e.getMessage();
            returnResult.setMessage(e.getMessage());
            returnResult.setIsSuccess(false);
        }


        /*List<String> loglist = new ArrayList<>();
        loglist.add(Id);
        ObjectOptionValueRetrieval.insertInterfaceLog("ProductSalesInfoSyncToSAP", "CRM",
                "SAP", END_POINT, JSON.toJSONString(itemsList), isSuccess, false,
                result.getResult(), errorMessage, "POST",loglist,
                "查询产品库存信息", "CRM查询SAP库存信息");*/
        return returnResult;
    }

    /**
     * @MethodName ProductSalesInfoSyncToSAP
     * @Auther Chi-Lynne
     * @Discription 提交后调用接口查询库存
     **/
    public static  ReturnResult productSalesInfo(String Id, String changeType) throws Exception {
        long longID = Long.parseLong(Id);
        RoundingMode roundingMode = RoundingMode.HALF_UP;
        tempId = longID;
        if (plantApiMap == null){
            try {
                plantApiMap = getApiKey("order","Delivery_Plant__c");
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (ScriptBusinessException e) {
                throw new RuntimeException(e);
            }
        }
        if (houseApiMap == null){
            try {
                houseApiMap = getApiKey("order", "wareHouse__c");
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (ScriptBusinessException e) {
                throw new RuntimeException(e);
            }
        }
        BigDecimal cbMoney = new BigDecimal(0);
        BigDecimal cbUSMoney = new BigDecimal(0);
        BigDecimal freeCbMoney = new BigDecimal(0);
        /*CommonHttpClient commonHttpClient= CommonHttpClient.instance();
        CommonData commonData=new CommonData();
        commonData.addHeader("Content-Type","application/json");
        commonData.setCallString(url);
        commonData.setCall_type("POST");*/


        RkhdHttpClient rkhdclient = RkhdHttpClient.instance();
        reqresultinfo reqresultinfo = new reqresultinfo();
        List<items> itemsList = new ArrayList<>();
        List<reqresultinfo> reqresultinfoList = new ArrayList<>();
        String errorMessage = "";
        Boolean isSuccess = true;
        String numStr = "";
        // TODO
        String sqlOrder = "select id,amount,huilv__c,Rate__c,entityType,currencyUnit from _order where id = "+longID;
        QueryResult<Order> ordResult = XObjectService.instance().query(sqlOrder,true);
        if(!ordResult.getSuccess()) {
            throw new Exception("Order retrieval error");
        }
        String sqlOrderItem = "select id,material_Number__c,unitPrice,orderId.Rate__c,orderId.Sales_Org__c,Converted_quantity__c,productId,productId.goods,productId.goods.factory__c,orderId.Delivery_Plant__c,quantity,productId.External_matnr__c,productId.goods,orderId.wareHouse__c,productId.Trade_Name__c from orderProduct where orderId = "+Id;
        JSONArray orderItemInfo = NeoCrmRkhdService.xoql(rkhdclient,sqlOrderItem);
        if(orderItemInfo == null) {
            logger.warn("orderinfo: " +orderItemInfo.toJSONString());
            throw new Exception("No order line data available");
        }

        // 存放返回值物料号（KEY）和订单明细数量=返回JSON
        Map<String,BigDecimal> itemMap = new HashMap<>();
        Map<String,BigDecimal> itemOrdQtyMap = new HashMap<>();
        Map<String,String> itemOrdBradMap = new HashMap<>();
        Map<String,BigDecimal> itemQtyRateMap = new HashMap<>();
        Map<String,Long> ordItemMap = new HashMap<>();
        // 存放商品ID（key）和商品库存表JSON
        Map<String, JSON> proInvMap = new HashMap<>();
        List<String> goodsIds = new ArrayList<>();
        // 存放物料号，用于查找审批中的订单明细
        List<String> wlhList = new ArrayList<>();
        Map<String,BigDecimal> wlhNumMap = new HashMap<>();
        // 需要的金额
        BigDecimal needMoney = new BigDecimal(0);
        //存物料号/需要的金额
        Map<String,BigDecimal> needMap = new HashMap<>();
//        BigDecimal needMoney = new BigDecimal(0);
        for(Integer i = 0 ;i<orderItemInfo.size(); i++){
            JSONObject jsstr= orderItemInfo.getJSONObject(i);
            if(jsstr == null) {
                // logger.error("zyhTest:"+jsstr.toJSONString());
                continue;
            }
            // 20241224   zyh   给商品赋值
            goodIdMap.put(jsstr.getString("productId.External_matnr__c"),jsstr.getLong("productId.goods"));
            proIdMap.put(jsstr.getString("productId.External_matnr__c"),jsstr.getLong("productId")); // 20250109   zyh   增加产品
            items hos = new items();
            //工厂
//            hos.setWerks(jsstr.getString("productId.goods"));
            if (jsstr.getJSONArray("orderId.Delivery_Plant__c") != null){
                hos.setWerks(plantApiMap.get(jsstr.getJSONArray("orderId.Delivery_Plant__c").getString(0)));
            } else {
                hos.setWerks("");
            }
//            hos.setWERKS("测试werks");
            //物料号
            hos.setMatnr(jsstr.getString("productId.External_matnr__c"));
//            hos.setMATNR("测试matnr");
            itemsList.add(hos);
            if (jsstr.getJSONArray("orderId.wareHouse__c") != null) {
                if (jsstr.getJSONArray("orderId.Sales_Org__c").get(0).equals("7190 AGROTERRUM GHANA") ||
                        jsstr.getJSONArray("orderId.Sales_Org__c").get(0).equals("7030 Rainbow Agro(Ghana)")) {
//                    itemMap.put(jsstr.getString("productId.External_matnr__c") + "-" + houseApiMap.get(jsstr.getJSONArray("orderId.wareHouse__c").getString(0)) + "-" + jsstr.getString("productId.Trade_Name__c"), BigDecimal.valueOf(jsstr.getDouble("Converted_quantity__c")));
                    // hql 20251027 新增
                    if (itemMap.get(jsstr.getString("productId.External_matnr__c") + "-" + houseApiMap.get(jsstr.getJSONArray("orderId.wareHouse__c").getString(0)) + "-" + jsstr.getString("productId.Trade_Name__c")) != null){
                        BigDecimal qty = itemMap.get(jsstr.getString("productId.External_matnr__c") + "-" + houseApiMap.get(jsstr.getJSONArray("orderId.wareHouse__c").getString(0)) + "-" + jsstr.getString("productId.Trade_Name__c"));
                        itemMap.put(jsstr.getString("productId.External_matnr__c") + "-" + houseApiMap.get(jsstr.getJSONArray("orderId.wareHouse__c").getString(0)) + "-" + jsstr.getString("productId.Trade_Name__c"), qty.add(BigDecimal.valueOf(jsstr.getDouble("Converted_quantity__c"))));
                    }else {
                        itemMap.put(jsstr.getString("productId.External_matnr__c") + "-" + houseApiMap.get(jsstr.getJSONArray("orderId.wareHouse__c").getString(0)) + "-" + jsstr.getString("productId.Trade_Name__c"), BigDecimal.valueOf(jsstr.getDouble("Converted_quantity__c")));
                    }
                    if (itemOrdQtyMap.get(jsstr.getString("productId.External_matnr__c") + "-" + houseApiMap.get(jsstr.getJSONArray("orderId.wareHouse__c").getString(0)) + "-" + jsstr.getString("productId.Trade_Name__c")) != null){
                        BigDecimal ordQty = itemOrdQtyMap.get(jsstr.getString("productId.External_matnr__c") + "-" + houseApiMap.get(jsstr.getJSONArray("orderId.wareHouse__c").getString(0)) + "-" + jsstr.getString("productId.Trade_Name__c"));
                        itemOrdQtyMap.put(jsstr.getString("productId.External_matnr__c") + "-" + houseApiMap.get(jsstr.getJSONArray("orderId.wareHouse__c").getString(0)) + "-" + jsstr.getString("productId.Trade_Name__c"), ordQty.add(BigDecimal.valueOf(jsstr.getDouble("quantity"))));
                    } else {
                        itemOrdQtyMap.put(jsstr.getString("productId.External_matnr__c") + "-" + houseApiMap.get(jsstr.getJSONArray("orderId.wareHouse__c").getString(0)) + "-" + jsstr.getString("productId.Trade_Name__c"), BigDecimal.valueOf(jsstr.getDouble("quantity")));
                    }
                    itemQtyRateMap.put(jsstr.getString("productId.External_matnr__c") + "-" + houseApiMap.get(jsstr.getJSONArray("orderId.wareHouse__c").getString(0)) + "-" + jsstr.getString("productId.Trade_Name__c"), BigDecimal.valueOf(jsstr.getDouble("Converted_quantity__c")).divide(new BigDecimal(jsstr.getDouble("quantity")),2,roundingMode));
                    ordItemMap.put(jsstr.getString("productId.External_matnr__c") + "-" + houseApiMap.get(jsstr.getJSONArray("orderId.wareHouse__c").getString(0)) + "-" + jsstr.getString("productId.Trade_Name__c"), jsstr.getLong("id"));
                } else {
//                    itemMap.put(jsstr.getString("productId.External_matnr__c") + "-" + houseApiMap.get(jsstr.getJSONArray("orderId.wareHouse__c").getString(0)), BigDecimal.valueOf(jsstr.getDouble("Converted_quantity__c")));
                    // hql 20251027 新增
                    if (itemMap.get(jsstr.getString("productId.External_matnr__c") + "-" + houseApiMap.get(jsstr.getJSONArray("orderId.wareHouse__c").getString(0))) != null){
                        BigDecimal qty = itemMap.get(jsstr.getString("productId.External_matnr__c") + "-" + houseApiMap.get(jsstr.getJSONArray("orderId.wareHouse__c").getString(0)));
                        itemMap.put(jsstr.getString("productId.External_matnr__c") + "-" + houseApiMap.get(jsstr.getJSONArray("orderId.wareHouse__c").getString(0)), qty.add(BigDecimal.valueOf(jsstr.getDouble("Converted_quantity__c"))));
                    }else {
                        itemMap.put(jsstr.getString("productId.External_matnr__c") + "-" + houseApiMap.get(jsstr.getJSONArray("orderId.wareHouse__c").getString(0)), BigDecimal.valueOf(jsstr.getDouble("Converted_quantity__c")));
                    }
                    if (itemOrdQtyMap.get(jsstr.getString("productId.External_matnr__c") + "-" + houseApiMap.get(jsstr.getJSONArray("orderId.wareHouse__c").getString(0))) != null){
                        BigDecimal ordQty = itemOrdQtyMap.get(jsstr.getString("productId.External_matnr__c") + "-" + houseApiMap.get(jsstr.getJSONArray("orderId.wareHouse__c").getString(0)));
                        itemOrdQtyMap.put(jsstr.getString("productId.External_matnr__c") + "-" + houseApiMap.get(jsstr.getJSONArray("orderId.wareHouse__c").getString(0)), ordQty.add(BigDecimal.valueOf(jsstr.getDouble("quantity"))));
                    } else {
                        itemOrdQtyMap.put(jsstr.getString("productId.External_matnr__c") + "-" + houseApiMap.get(jsstr.getJSONArray("orderId.wareHouse__c").getString(0)), BigDecimal.valueOf(jsstr.getDouble("quantity")));
                    }
                    itemOrdBradMap.put(jsstr.getString("productId.External_matnr__c") + "-" + houseApiMap.get(jsstr.getJSONArray("orderId.wareHouse__c").getString(0)), jsstr.getString("productId.Trade_Name__c"));
                    itemQtyRateMap.put(jsstr.getString("productId.External_matnr__c") + "-" + houseApiMap.get(jsstr.getJSONArray("orderId.wareHouse__c").getString(0)), BigDecimal.valueOf(jsstr.getDouble("Converted_quantity__c")).divide(new BigDecimal(jsstr.getDouble("quantity")),2,roundingMode));
                    ordItemMap.put(jsstr.getString("productId.External_matnr__c") + "-" + houseApiMap.get(jsstr.getJSONArray("orderId.wareHouse__c").getString(0)), jsstr.getLong("id"));
                }
            } else {
                throw new Exception("No warehouse");
            }
            wlhList.add(jsstr.getString("productId.External_matnr__c"));
            if (jsstr.getString("productId.goods") != null && !jsstr.getString("productId.goods").isEmpty()) {
                goodsIds.add(jsstr.getString("productId.goods"));
            }
            logger.info("bankinfo: " +itemsList.size());
            logger.info("itemMap===" + itemMap);
            // logger.info("bankinfo: " +itemsList.get(0));
        }
        if (!wlhList.isEmpty()){
            wlhNumMap = checkNum(wlhList);
            // 如果有进行中的库存
            /* if (wlhNumMap.size() > 0){
                // 20241224   zyh   库存比较   start
                logger.info("itemMap===" + itemMap);
                logger.info("wlhNumMap===" + wlhNumMap);
                for (String key : itemMap.keySet()){
                    if (wlhNumMap.get(key) != null){
                        BigDecimal num = wlhNumMap.get(key).add(itemMap.get(key));
                        // wlhNumMap.put(key,num);
                        logger.error("添加审批中后数量：" + itemMap);*/
            // 如果库存小于0的话记录str
                        /*if (num.compareTo(new BigDecimal(0)) < 0){
                            numStr += key + "Order need " + itemMap.get(key) + "，Warehouse have " + num + "；";
                        }*/
                    /*} else {
                        numStr += key + "Order need " + itemMap.get(key) + "，Warehouse is null；";
                    }*/
                /*}
                logger.info("numStr===" + numStr);
                // 20241224   zyh   库存比较   end
            }*/
        }



        /*JSONObject parameter = new JSONObject();
        JSONObject header = new JSONObject();
        header.put("token",tokenStr);
        parameter.put("head",header);
        parameter.put("body",requestBody);*/

        HttpResult result = new HttpResult();
        ReturnResult returnResult = new ReturnResult();
        try{
            // 同步数据7
            result =  crmToSap(itemsList);
            logger.info("ceshi1212:" + result.getResult().toString());
            logger.info("ceshi1212:" + result);
            logger.info("接口返回result: " + JSON.toJSONString(result));
            returnResult.setIsSuccess(true);
            String response = result.getResult();
            JSONObject esbinfo = JSONObject.parseObject(response).getJSONObject("esbinfo");
            JSONArray resultinfo = JSONObject.parseObject(response).getJSONArray("resultinfo");
            JSONObject responseJson = (JSONObject) JSONObject.parse(result.getResult());
         /*   logger.error("测试1212：" + responseJson.toString());
            logger.error("测试1216001：" + esbinfo.toString());
            logger.error("测试1216002：" + resultinfo.toString());*/
            // 获取返回状态值
            String returnStatus = esbinfo.getString("returnstatus");
//            String returnStatus = "S";
//            String partner = responseJson.getJSONArray("resultinfo").getJSONObject(0).getString("partner");
            List<respesbinfo> infoList = new ArrayList<>();
            Order acc = new Order();
//            List<XObject> updateList = new ArrayList<>();
//            List<XObject> updateList2 = new ArrayList<>();
//            int StatusCode = ((JSONObject) JSONObject.parse(result.getResult())).getString("");
//            // TODO 成功方式判断待定
//            isSuccess = StatusCode == 500;
            logger.error("isSuccess+++" + isSuccess);
            logger.error("测试1216002：" + JSONObject.toJSONString(resultinfo));
            if (isSuccess){
                logger.error("isSuccess+++");
                logger.error("resultinfo.size===" + resultinfo.size());
                if (responseJson.getJSONArray("resultinfo").size() > 0){
                    // 20241224   zyh   计算毛利率   start
                    Map<String,BigDecimal> cbMap = new HashMap<>(); // 成本Map，存储物料号/单价
                    Map<String,BigDecimal> cbUSMap = new HashMap<>(); // 成本Map，存储物料号/单价
                    Map<String,BigDecimal> kcMap = new HashMap<>(); // 库存Map，存储物料号/单价
                    for (int i = 0;i < resultinfo.size();i++){
                        String matnr = resultinfo.getJSONObject(i).getString("matnr");
                        String mapKey = resultinfo.getJSONObject(i).getString("matnr") + "-" + resultinfo.getJSONObject(i).getString("werks") + "_" + resultinfo.getJSONObject(i).getString("lgort");
                        if ("7190 AGROTERRUM GHANA".equals(orderItemInfo.getJSONObject(0).getJSONArray("orderId.Sales_Org__c").getString(0)) ||
                                "7030 Rainbow Agro(Ghana)".equals(orderItemInfo.getJSONObject(0).getJSONArray("orderId.Sales_Org__c").getString(0))){
                            mapKey = resultinfo.getJSONObject(i).getString("matnr") + "-" + resultinfo.getJSONObject(i).getString("werks") + "_" + resultinfo.getJSONObject(i).getString("lgort") + "-" + resultinfo.getJSONObject(i).getString("atwrt");
                        }
                        logger.error("clabs===" + resultinfo.getJSONObject(i).getString("clabs"));
                        BigDecimal mapValue = BigDecimal.valueOf(Double.valueOf(resultinfo.getJSONObject(i).getString("clabs").trim()));
                        if (!"".equals(resultinfo.getJSONObject(i).getString("omeng"))){
                            if (itemQtyRateMap.containsKey(mapKey)){
                                logger.error("20250627==1==" + mapValue);
                                logger.error("20250627==2==" + resultinfo.getJSONObject(i).getString("omeng"));
                                mapValue = mapValue.subtract(BigDecimal.valueOf(Double.valueOf(resultinfo.getJSONObject(i).getString("omeng"))));
                                logger.error("20250627==3==" + mapValue);
                                logger.error("20250627==4==" + itemQtyRateMap.get(mapKey));
                            }
                        }
                        if (kcMap.containsKey(mapKey)){
                            BigDecimal tempNum = kcMap.get(mapKey).add(mapValue);
                            kcMap.put(mapKey,tempNum);
                        } else {
                            kcMap.put(mapKey,mapValue);
                        }
                        logger.error("kcMap===" + kcMap);
                        logger.error("matnr+++" + matnr);
                        logger.error("resultinfo+++" + resultinfo.getJSONObject(i));
                        // TODO 成本价改成vmkum
                        BigDecimal verpr = new BigDecimal(0);
                        if (numStr != null && !"".equals(resultinfo.getJSONObject(i).getString("verpr"))) {
                            verpr = new BigDecimal(resultinfo.getJSONObject(i).getString("verpr").trim());
                        }
                        logger.error("verpr+++" + verpr);
                        logger.error("verpr+1++" + resultinfo.getJSONObject(i).getString("verpr"));
                        cbMap.put(matnr,verpr);
                        // TODO 成本价改成verpr
                        BigDecimal vmkum = new BigDecimal(0);
                        if (numStr != null && !"".equals(resultinfo.getJSONObject(i).getString("vmkum"))) {
                            vmkum = new BigDecimal(resultinfo.getJSONObject(i).getString("vmkum").trim());
                        }
                        logger.error("vmkum+++" + vmkum);
                            logger.error("vmkum+1++" + resultinfo.getJSONObject(i).getString("vmkum"));
                        cbUSMap.put(matnr,vmkum);

                    }
                    // if (wlhNumMap.size() > 0){
                    if (itemMap.size() > 0){
                        List<OrderProduct> opList = new ArrayList<>();
                        for (String str : itemMap.keySet()){ // wlhNumMap===>itemMap↓↓↓↓↓↓↓↓↓
                            OrderProduct op = new OrderProduct();
                            op.setId(ordItemMap.get(str));
                            logger.error("库存对比：" + str);
                            logger.error("库存对比：" + str + kcMap.containsKey(str));
                            BigDecimal appingNum = new BigDecimal(0);
                            BigDecimal excAppingNum = new BigDecimal(0);
                            if (kcMap.containsKey(str)){
                                logger.error("库存对比：" + str + kcMap.containsKey(str) + kcMap.get(str));
                                logger.error("库存对比：" + wlhNumMap);
                                if (wlhNumMap.containsKey(str)) {
                                    logger.error("库存对比：" + kcMap.get(str).subtract(wlhNumMap.get(str)).compareTo(new BigDecimal(0)));
                                    logger.error("库存对比：" + kcMap.get(str).subtract(wlhNumMap.get(str)));
                                    logger.error("库存对比：" + kcMap.get(str) + "===" + wlhNumMap.get(str));
                                    if (kcMap.get(str).subtract(wlhNumMap.get(str).add(itemMap.get(str))).compareTo(new BigDecimal(0)) < 0) {
                                        if ("7190 AGROTERRUM GHANA".equals(orderItemInfo.getJSONObject(0).getJSONArray("orderId.Sales_Org__c").getString(0)) ||
                                                "7030 Rainbow Agro(Ghana)".equals(orderItemInfo.getJSONObject(0).getJSONArray("orderId.Sales_Org__c").getString(0))) {
                                            String[] tradName = str.split("-");
                                            // numStr += str + " Order need " + itemOrdQtyMap.get(str) + " PC " + tradName[tradName.length - 1] + "，there are " + wlhNumMap.get(str) + " PC in progress，Warehouse have " + kcMap.get(str).divide(itemQtyRateMap.get(str), 1, roundingMode) + " PC；";
                                            numStr += str + " Order need " + itemOrdQtyMap.get(str) + " PC " + tradName[tradName.length - 1] + "，there are " + wlhNumMap.get(str).divide(itemQtyRateMap.get(str),1,roundingMode) + " PC in progress，Warehouse have " + kcMap.get(str).divide(itemQtyRateMap.get(str), 1, roundingMode) + " PC；";
                                        } else {
                                            // numStr += str + "-" + itemOrdBradMap.get(str) + " Order need " + itemOrdQtyMap.get(str) + " PC，there are " + wlhNumMap.get(str) + " PC in progress，Warehouse have " + kcMap.get(str).divide(itemQtyRateMap.get(str), 1, roundingMode) + " PC；";
                                            numStr += str + "-" + itemOrdBradMap.get(str) + " Order need " + itemOrdQtyMap.get(str) + " PC，there are " + wlhNumMap.get(str).divide(itemQtyRateMap.get(str),1,roundingMode) + " PC in progress，Warehouse have " + kcMap.get(str).divide(itemQtyRateMap.get(str), 1, roundingMode) + " PC；";
                                        }
                                    }
                                    appingNum = wlhNumMap.get(str).divide(itemQtyRateMap.get(str),1,roundingMode);
                                    excAppingNum = (kcMap.get(str).subtract(wlhNumMap.get(str))).divide(itemQtyRateMap.get(str),1,roundingMode);
                                } else {
                                    if (kcMap.get(str).subtract(itemMap.get(str)).compareTo(new BigDecimal(0)) < 0) {
                                        if ("7190 AGROTERRUM GHANA".equals(orderItemInfo.getJSONObject(0).getJSONArray("orderId.Sales_Org__c").getString(0)) ||
                                                "7030 Rainbow Agro(Ghana)".equals(orderItemInfo.getJSONObject(0).getJSONArray("orderId.Sales_Org__c").getString(0))) {
                                            String[] tradName = str.split("-");
                                            numStr += str + " Order need " + itemOrdQtyMap.get(str) + " PC " + tradName[tradName.length - 1] + "，there are 0.0 PC in progress，Warehouse have " + kcMap.get(str).divide(itemQtyRateMap.get(str), 1, roundingMode) + " PC；";
                                        } else {
                                            numStr += str + "-" + itemOrdBradMap.get(str) + " Order need " + itemOrdQtyMap.get(str) + " PC，there are 0.0 PC in progress，Warehouse have " + kcMap.get(str).divide(itemQtyRateMap.get(str), 1, roundingMode) + " PC；";
                                        }
                                    }
                                    excAppingNum = kcMap.get(str).divide(itemQtyRateMap.get(str),1,roundingMode);
                                }
                            } else {
                                logger.error("20250217==" + numStr);
                                if ("7190 AGROTERRUM GHANA".equals(orderItemInfo.getJSONObject(0).getJSONArray("orderId.Sales_Org__c").getString(0)) ||
                                        "7030 Rainbow Agro(Ghana)".equals(orderItemInfo.getJSONObject(0).getJSONArray("orderId.Sales_Org__c").getString(0))) {
                                    String[] tradName = str.split("-");
                                    numStr += str + " Order need " + itemOrdQtyMap.get(str) + " PC " + tradName[tradName.length - 1] + "，there are 0.0 PC in progress，Warehouse is null ；";
                                } else {
                                    numStr += str + "-" + itemOrdBradMap.get(str) + " Order need " + itemOrdQtyMap.get(str) + " PC，there are 0.0 PC in progress，Warehouse is null ；";
                                }
                            }
                            op.setApprovalingQty__c(appingNum.doubleValue());
                            op.setExceedAppQty__c(excAppingNum.doubleValue());
                            logger.error("opppppppp===" + op);
                            opList.add(op);
                        }
                        logger.error("numStr==" + numStr);
                        logger.error("opList===" + opList);
                        if (opList.size() > 0){
                            BatchOperateResult update = XObjectService.instance().update(opList,true,true);
                            if (!update.getSuccess()){
                                logger.error("更新失败======"+update.getErrorMessage());
                            }
                        }
                    }
                    logger.error("cbMap+++" + cbMap);
                    List<OrderProduct> upOpList = new ArrayList<>();
                    for (Integer i = 0 ;i<orderItemInfo.size(); i++){
                        JSONObject jsstr= orderItemInfo.getJSONObject(i);
                        logger.error("jsstr+++" + jsstr);
                        logger.error("jsstr+++" + !"".equals(jsstr.getString("orderId.Rate__c")));
                        if (/*jsstr.getDouble("orderId.Rate__c") != null &&
                                jsstr.getString("orderId.Rate__c") != null &&
                                !"".equals(jsstr.getString("orderId.Rate__c")) &&
                                jsstr.getDouble("orderId.Rate__c") != 0 &&*/
                                jsstr.getDouble("unitPrice") != null &&
                                        jsstr.getDouble("unitPrice") != 0 &&
                                        jsstr.getDouble("quantity") != null){
                            BigDecimal unitPrice = BigDecimal.valueOf(jsstr.getDouble("unitPrice"));//销售单价
                            BigDecimal quantity = BigDecimal.valueOf(jsstr.getDouble("quantity"));//数量
//                            BigDecimal rate = BigDecimal.valueOf(jsstr.getDouble("orderId.Rate__c"));//汇率
                            BigDecimal con_quantity = BigDecimal.valueOf(jsstr.getDouble("Converted_quantity__c"));//换算后数量
                            logger.info("销售单价："+unitPrice + ";数量：" + quantity + ";换算后数量：" + con_quantity);
                            //needMoney = needMoney.add((unitPrice.multiply(quantity)).divide(rate,2)); // 需要的金额等于销售单价*数量/汇率
                            needMoney = needMoney.add((unitPrice.multiply(quantity))); // 需要的金额等于销售单价*数量/汇率===改成用本位币
                            //needMap.put(jsstr.getString("productId.External_matnr__c"),needMoney); //暂时不用
                            cbMoney = cbMoney.add(cbMap.get(jsstr.getString("productId.External_matnr__c")).multiply(con_quantity)); // 成本=成本单价*换算后数量
                            cbUSMoney = cbUSMoney.add(cbUSMap.get(jsstr.getString("productId.External_matnr__c")).multiply(con_quantity)); // 成本=成本单价*换算后数量
                            logger.info("换算后数量：" + con_quantity + "；成本：" + cbMoney);
                            logger.info("换算后数量：" + con_quantity + "；成本：" + cbUSMoney);
                            logger.info("需要金额：" + needMoney + "；成本：" + cbMoney);
                            logger.info("cbMap取值：" + cbMap.get(jsstr.getString("productId.External_matnr__c")) + "；成本：" + cbMap.get(jsstr.getString("productId.External_matnr__c")).multiply(con_quantity));
                        }else {
                            BigDecimal con_quantity = BigDecimal.valueOf(jsstr.getDouble("Converted_quantity__c"));//换算后数量
                            if (cbMap.get(jsstr.getString("productId.External_matnr__c")) != null) {
                                cbMoney = cbMoney.add(cbMap.get(jsstr.getString("productId.External_matnr__c")).multiply(con_quantity)); // 成本=成本单价*换算后数量
                                cbUSMoney = cbUSMoney.add(cbUSMap.get(jsstr.getString("productId.External_matnr__c")).multiply(con_quantity)); // 成本=成本单价*换算后数量
                            }
                            logger.info("换算后数量：" + con_quantity + "；成本：" + cbMoney);
                            logger.info("换算后数量：" + con_quantity + "；成本：" + cbUSMoney);
                        }
						if(!"".equals(cbMap.get(jsstr.getString("productId.External_matnr__c"))) && jsstr.getDouble("Converted_quantity__c") != null){
	                        OrderProduct op = new OrderProduct();
	                        op.setId(jsstr.getLong("id"));
	                        op.setProduct_Cost__c(Double.valueOf(cbMap.get(jsstr.getString("productId.External_matnr__c")).multiply(BigDecimal.valueOf(jsstr.getDouble("Converted_quantity__c"))).toString()));
	                        upOpList.add(op);
						}
                    }
                    logger.error("upOpList=" + upOpList);
                    if (upOpList.size() > 0){
                        BatchOperateResult opUpdate = XObjectService.instance().update(upOpList,true,true);
                         opUpdate.getOperateResults();
                        for ( OperateResult item : opUpdate.getOperateResults()){
                          if (!item.getSuccess()){
                              logger.error("opUpdate1=" + item.getDataId());
                              logger.error("opUpdate2=" + item.getErrorMessage());
                          }
                        }

                        if (!opUpdate.getSuccess()){
                            throw new RuntimeException(opUpdate.getErrorMessage());
                        }
                    }
                    // 20241224   zyh   计算毛利率   end
//                    returnResult.setMessage(responseJson.getJSONObject("esbinfo").getString("returnmsg"));
                    if("S".equals(returnStatus)){
                        // 毛利润计算
                        /*if (cbMoney.compareTo(new BigDecimal(0)) > 0){
                            // cbMoney.subtract(ordResult.getRecords().get(0).getAmount() * ordResult.getRecords().get(0).getHuilv__c());
                        }*/

                        Order o = new Order();
                        o.setId(longID);
                        //if (numStr != null){
                        o.setKcCheck__c(numStr);
                        // 20241224   zyh   计算毛利率   start
                        logger.error("20250214==0=" + needMoney.compareTo(new BigDecimal(0)));
                        logger.error("20250214==1=" + cbMoney.compareTo(new BigDecimal(0)));
                        logger.error("20250214==1=" + cbUSMoney);
                        logger.error("20250214==2=" + "Free Order".equals(NeoCrmRkhdService.v2QueryRecordInfoByIdXobject(rkhdclient, "order", longID).getString("entityType-label")));
                        if (needMoney.compareTo(new BigDecimal(0)) != 0 && cbMoney.compareTo(new BigDecimal(0)) != 0 && !"Free Order".equals(NeoCrmRkhdService.v2QueryRecordInfoByIdXobject(rkhdclient,"order",longID).getString("entityType-label"))) {
//                                o.setGross_margin__c(((needMoney.subtract(cbMoney)).divide(cbMoney,2)).divide(BigDecimal.valueOf(ordResult.getRecords().get(0).getRate__c())).doubleValue());
                            logger.error("毛利率=成本==" + cbMoney + "==售价==" + needMoney + "==毛利单号ID==" + longID);
                            o.setGross_margin__c(((needMoney.subtract(cbMoney)).divide(needMoney,2,roundingMode)).doubleValue());
                        } else {
                            o.setGross_margin__c(0.0);
                        }
                        // 20241224   zyh   计算毛利率   end

                        //}
                        logger.error("20250320=====" + ordResult.getRecords().get(0).getCurrencyUnit());
                        if (ordResult.getRecords().get(0).getCurrencyUnit() == 1){
                            o.setCost_price__c(cbUSMoney.doubleValue());
                        } else {
                            o.setCost_price__c(cbMoney.doubleValue());
                        }
                        logger.error("更新后订单：：" + o);
                        OperateResult update = XObjectService.instance().update(o,true);
                        logger.error("update1：：" + update.getSuccess());
                        logger.error("update2：：" + update.getErrorMessage());
                        if(!update.getSuccess()) {
                            throw new Exception("Order update error:" + update.getErrorMessage());
                        }
                    }

                    if ("E".equals(returnStatus)){
                        returnResult.setMessage(esbinfo.getString("returnmsg"));
                        returnResult.setIsSuccess(false);
                        isSuccess = false;
                        errorMessage =responseJson.toJSONString();
                    }
                    if ("W".equals(returnStatus)){
                        returnResult.setMessage(esbinfo.getString("returnmsg"));
                        returnResult.setIsSuccess(false);
                        isSuccess = false;
                        errorMessage =responseJson.toJSONString();
                    }
//                    updateList.add(acc);
//                    BatchOperateResult batchResult = XObjectService.instance().update(updateList);
                }else {
//                    returnResult.setMessage(responseJson.toJSONString());
                    returnResult.setMessage(esbinfo.getString("returnmsg"));
                    returnResult.setIsSuccess(false);
                    isSuccess = false;
                    errorMessage =responseJson.toJSONString();
//                    // Todo 错误邮件发送未写Utility.notificationUser(accList[0].OwnerId,Id);
                }
            }else {
                returnResult.setMessage(esbinfo.getString("returnmsg"));
                returnResult.setIsSuccess(false);
                isSuccess = false;
                errorMessage =responseJson.toJSONString();
//                // Todo 错误邮件发送未写Utility.notificationUser(accList[0].OwnerId,Id);
            }


        }catch (Exception e){
            logger.error("错误进来catch了：" + e.getMessage() + "行号：" + e.getStackTrace()[0].getLineNumber() + "类命：" + e.getStackTrace()[0].getClassName());
            logger.debug("错误进来catch了：" + e.getMessage() + "行号：" + e.getStackTrace()[0].getLineNumber() + "类命：" + e.getStackTrace()[0].getClassName());
            isSuccess = false;
            errorMessage = e.getMessage();
            returnResult.setMessage(e.getMessage());
            returnResult.setIsSuccess(false);
        }


        /*List<String> loglist = new ArrayList<>();
        loglist.add(Id);
        ObjectOptionValueRetrieval.insertInterfaceLog("ProductSalesInfoSyncToSAP", "CRM",
                "SAP", END_POINT, JSON.toJSONString(itemsList), isSuccess, false,
                result.getResult(), errorMessage, "POST",loglist,
                "查询产品库存信息", "CRM查询SAP库存信息");*/
        return returnResult;
    }

    //Crm发送数据致SAP
    public static HttpResult crmToSap(List<items> itemsList) throws XsyHttpException, ApiEntityServiceException, IOException, ScriptBusinessException, InterruptedException, CustomConfigException {
        String errorMessage = "";
        Boolean isSuccess = true;
        Long proInTypeID =  MetadataService.instance().getBusiType("Product_Inventory__c", "defaultBusiType").getId();
        logger.error("我不信：" + proInTypeID);
//        CommonHttpClient commonHttpClient= CommonHttpClient.instance();
        RequestBody requestBody = new RequestBody();
        reqesbinfo reqesbinfo = new reqesbinfo();
        //接口编号
        reqesbinfo.instid = "ProductSalesInfoSyncToSAP";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime toDay = LocalDateTime.now();
        String toDayString = toDay.format(formatter);
        //请求时间
        reqesbinfo.requesttime = toDayString;

//        reqresultinfo.setBanks(itemsList);
//        reqresultinfoList.add(reqresultinfo);
//        requestBody.setResultinfo(reqresultinfoList);
//        reqresultinfo.setResultinfo(itemsList);
        requestBody.setResultinfo(itemsList);
        requestBody.setEsbinfo(reqesbinfo);
        logger.info("requestBody: " + JSON.toJSONString(requestBody));
        String url=URL/*+"/rest/action/customer/get_crm_customer_list"*/;
        // 使用 Base64 编码
//        String encoded = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        // String encoded = "eHN5LWNybTAxOkFiY2QxMjM0";
        String encoded = "";
        CustomConfigService customConfigService = CustomConfigService.instance();
        Map<String, String> configProperties = null;
        Map<String, String> special_plant = null;
        special_plant = customConfigService.getConfigSet("special_plant");
        logger.info("special_plant=="+special_plant);

        configProperties = customConfigService.getConfigSet("sap_properties");
        logger.info("configProperties=="+configProperties);

        /*url = "http://120.224.116.35:8901/sap/inventoryinfo/sync";
        encoded = "Basic eHN5LWNybTAxOkFiY2QxMjM0";*/
        if(configProperties != null) {
            Base64.Encoder encoder = Base64.getEncoder();
            // encoded = encoder.encodeToString((configProperties.get("username") + ":" + configProperties.get("password")).getBytes());
            encoded = configProperties.get("Authorization");
            // url = configProperties.get("endpoint") + "/sap/inventoryinfo/sync";
            url = configProperties.get("ProductSalesInfoSyncToSAP");
            logger.info("url==="+url);
        }/* else {
            url = "http://esb.rainbowagro.com:8901/sap/inventoryinfo/sync";
            encoded = "Basic eHN5LWNybTAxOlJhaW5ib3d4c3ljcm0jMTIzNA==";
        }*/

        if("".equals(url)){
            throw new RuntimeException("Get SAPurl is null");
        }
        // 创建 "Basic " 开头的字符串
        String hed = encoded;
        CommonHttpClient commonHttpClient= CommonHttpClient.instance();
        CommonData commonData=new CommonData();
        commonData.addHeader("Content-Type","application/json");
        commonData.setCallString(url);
        commonData.setCall_type("POST");
        commonData.addHeader("Authorization",hed);
        logger.info("Authorization="+hed);

        commonData.setBody(JSON.toJSONString(requestBody));
        logger.info("commonData.setBody: " + commonData.getBody());
        HttpResult result = commonHttpClient.execute(commonData);
        String response = result.getResult();
        JSONObject esbinfo = JSONObject.parseObject(response).getJSONObject("esbinfo");
        JSONArray resultinfo = JSONObject.parseObject(response).getJSONArray("resultinfo");
        logger.info("esbinfo接收：" + esbinfo);
        logger.info("resultinfo接收：" + resultinfo);
        List<respesbinfo> infoList = new ArrayList<>();
        Map<String,respesbinfo> MATNRMap = new HashMap<>();
        List<Product_Inventory__c> proInInsert = new ArrayList<>();
        List<Product_Inventory__c> proInUpdate = new ArrayList<>();
        if (esbinfo != null){
            // 获取返回状态值
            String returnStatus = esbinfo.getString("returnstatus");
            List<String> exIdList = new ArrayList<>();
            List<String> exIdGodList = new ArrayList<>();
            Map<String,HttpResult> returnMap = new HashMap<>();
            if ("S".equals(returnStatus)){
                isSuccess = true;
                if (resultinfo != null){
                    RkhdHttpClient rkhdclient = RkhdHttpClient.instance();
                    Map<String,Integer> curMap = getGlobalPicks("Currency__c",rkhdclient);
                    Map<String,Integer> plantMap = getGlobalPicks("DeliveryPlant__c",rkhdclient);
                    Map<String,Integer> whMap = getGlobalPicks("warehouse__c",rkhdclient);
                    for (int i = 0;i < resultinfo.size();i++){
                        respesbinfo respesbinfo = new respesbinfo();
                        respesbinfo.setAtwrt(resultinfo.getJSONObject(i).getString("atwrt"));
                        respesbinfo.setKtopl(resultinfo.getJSONObject(i).getString("ktopl"));
                        respesbinfo.setVerpr(resultinfo.getJSONObject(i).getString("verpr"));
                        respesbinfo.setVmkum(resultinfo.getJSONObject(i).getString("vmkum"));
                        respesbinfo.setMeins(resultinfo.getJSONObject(i).getString("meins"));
                        respesbinfo.setLgort(resultinfo.getJSONObject(i).getString("lgort"));
                        respesbinfo.setMatnr(resultinfo.getJSONObject(i).getString("matnr"));
                        respesbinfo.setCharg(resultinfo.getJSONObject(i).getString("charg"));
                        respesbinfo.setWerks(resultinfo.getJSONObject(i).getString("werks"));
                        respesbinfo.setClabs(resultinfo.getJSONObject(i).getString("clabs"));
                        respesbinfo.setCspem(resultinfo.getJSONObject(i).getString("cspem"));
                        respesbinfo.setCinsm(resultinfo.getJSONObject(i).getString("cinsm"));
                        respesbinfo.setCretm(resultinfo.getJSONObject(i).getString("cretm"));
                        respesbinfo.setWaers(resultinfo.getJSONObject(i).getString("waers"));
                        respesbinfo.setMaktx(resultinfo.getJSONObject(i).getString("maktx"));
                        respesbinfo.setMatnr(resultinfo.getJSONObject(i).getString("matnr"));
                        respesbinfo.setWerks(resultinfo.getJSONObject(i).getString("werks"));
                        respesbinfo.setAtwrt(resultinfo.getJSONObject(i).getString("atwrt"));
                        respesbinfo.setKwmeng(resultinfo.getJSONObject(i).getDouble("kwmeng"));
                        respesbinfo.setLwedt(resultinfo.getJSONObject(i).getString("lwedt"));
                        respesbinfo.setKunnr(resultinfo.getJSONObject(i).getString("kunnr"));
                        respesbinfo.setOmeng(resultinfo.getJSONObject(i).getString("omeng"));
                        logger.info("返回值：" + resultinfo.getJSONObject(i));
                        infoList.add(respesbinfo);
                        exIdList.add(resultinfo.getJSONObject(i).getString("lgort")+
                                resultinfo.getJSONObject(i).getString("matnr")+
                                resultinfo.getJSONObject(i).getString("charg")+
                                resultinfo.getJSONObject(i).getString("werks")+
                                resultinfo.getJSONObject(i).getString("kunnr"));
                        exIdGodList.add(resultinfo.getJSONObject(i).getString("matnr") + "[" +special_plant.get(resultinfo.getJSONObject(i).getString("werks")) + "]" );
                        // MATNRMap.put(resultinfo.getJSONObject(i).getString("matnr") + "-" + resultinfo.getJSONObject(i).getString("lgort"),respesbinfo);
                    }
                    if (exIdList.size() > 0) {
                        String exId = SqlFormatUtils.joinInSql(exIdList);
                        String exProId = SqlFormatUtils.joinInSql(exIdGodList);
                        // 查询产品数据
                        String proSql = "select id,goods,External_Id__c from product where External_Id__c IN(" + exProId + ")";
                        QueryResult<JSONObject> proQuery = XoqlService.instance().query(proSql,true);
                        JSONArray proInfo = JSONArray.parseArray(proQuery.getRecords().toString());
                        Map<String,JSONObject> proMap = new HashMap<>();
                        if (proInfo != null) {
                            for (int i = 0;i<proInfo.size();i++) {
                                proMap.put(proInfo.getJSONObject(i).getString("External_Id__c"), proInfo.getJSONObject(i));
                            }
                        }
                        // 获取商品库存表
                        String proInSql = "select id,Goods__c,product__c,External_Id__c from Product_Inventory__c where External_Id__c IN(" + exId + ")";
                        // JSONArray goodsInfo = NeoCrmRkhdService.xoql(rkhdclient, goodsSql);
                        QueryResult<JSONObject> proInQuery = XoqlService.instance().query(proInSql,true);
                        JSONArray goodsInfo = JSONArray.parseArray(proInQuery.getRecords().toString());
                        /*String sqlPro = "select id,Product_Name_Text__c,Goods__c,Currency__c,Goods__c.External_matnr__c,name,Storage_Location__c from Product_Inventory__c where Goods__c IN("+str + ")";
                        JSONArray proInfo = NeoCrmRkhdService.xoql(rkhdclient,sqlPro);*/
                        Map<String,JSONObject> gIdMap = new HashMap<>();
                        Map<String,JSONObject> pIdMap = new HashMap<>(); // 20250109   zyh   增加产品
                        if (goodsInfo != null) {
                            for (int i = 0;i<goodsInfo.size();i++){
                                if (!"".equals(goodsInfo.getJSONObject(i).getString("External_Id__c"))) {
                                    gIdMap.put(goodsInfo.getJSONObject(i).getString("External_Id__c"), goodsInfo.getJSONObject(i));
                                    pIdMap.put(goodsInfo.getJSONObject(i).getString("External_Id__c"), goodsInfo.getJSONObject(i)); // 20250109   zyh   增加产品
                                }
                            }
                        }
                        logger.info("gIdMap:" + gIdMap);
                        logger.debug("proMap===" + proMap);
                        for (int i = 0; i < infoList.size(); i++) {
                            Product_Inventory__c nObj = new Product_Inventory__c();
                            respesbinfo info = infoList.get(i);
                            String key =info.lgort +
                                    info.matnr+
                                    info.charg+
                                    info.werks+
                                    info.kunnr;
//                                        nObj.setCurrency__c(MATNRMap.get(str1).ATWRT);
                            // 20241231   zyh   赋值物料号   start
                            if (!"".equals(info.matnr)){
                                nObj.setMaterial_code__c(info.matnr);
                            }
                            // 默认单位
                            if (!"".equals(info.meins)){
                                nObj.setDefault_Unit__c(info.meins);
                            }
                            // 品牌名
                            if (!"".equals(info.maktx)){
                                // nObj.setBrandName__c(info.maktx); // 20250312   update↓   zyh
                                nObj.setProduct_Name_Text__c(info.maktx);
                            }
                            // 工厂
                            if (!"".equals(info.werks)){
                                if (special_plant != null) {
                                    logger.info("info.werks==" + special_plant.get(info.werks) + "==" + info.werks);
                                    logger.info("info.werks==" + plantMap.get(special_plant.get(info.werks)));
                                    logger.info("info.plantMap==" + plantMap);

                                    //nObj.setPlant__c(plantMap.get(special_plant.get(info.werks)));//info.werks
                                    nObj.setPlant__c(plantMap.get(info.werks));//info.werks
                                }
                            }
                            //币种Currency__c
                            if (!"".equals(info.waers)){
                                logger.debug("币种赋值：" + info.waers);
                                nObj.setCurrency__c(curMap.get(info.waers));//info.werks
                                logger.info("币种赋值：" + curMap.get(info.waers));
                            }//Product_Name_Text__c
                            //商品名称
                            if (!"".equals(info.atwrt)){
                                // nObj.setProduct_Name_Text__c(info.atwrt); // 20250312   update   zyh
                                nObj.setBrandName__c(info.atwrt);
                            }
                            //销售单位
                            if (!"".equals(info.vrkme)){
                                nObj.setSales_Unit__c(info.vrkme);
                            }
                            //销售单位库存数
                            if (!"".equals(info.kwmeng)){
                                nObj.setSales_Unit_Inventory__c(info.kwmeng);
                            }
                            // 20241231   zyh   赋值物料号   end
                            if (info.verpr != null && !"".equals(info.verpr)) {
                                nObj.setUnit_Cost__c(Double.valueOf(info.verpr));
                            } else {
                                nObj.setUnit_Cost__c(0.0);
                            }
                            if (info.vmkum != null &&  !"".equals(info.vmkum)) {
                                nObj.setUSD_Cost__c(Double.valueOf(info.vmkum));
                            } else {
                                nObj.setUSD_Cost__c(0.0);
                            }
//                                        nObj.setDefault_Unit__c(MATNRMap.get(str1).MEINS);
                            if (!"".equals(info.lgort)) {
                                nObj.setStorage_Location__c(whMap.get(info.werks + "_" + info.lgort));
                            }
//                                        nObj.setProduct__c(MATNRMap.get(str1).MATNR);
                            nObj.setBatch_Number__c(info.charg);
//                                        nObj.setPlant__c(MATNRMap.get(str1).WERKS);
                            if (info.clabs != null && !"".equals(info.clabs)) {
                                nObj.setInventory_Quantity__c(Double.valueOf(info.clabs));
                            } else {
                                nObj.setInventory_Quantity__c(0.0);
                            }
                            if (info.cspem != null && !"".equals(info.cspem)) {
                                nObj.setFrozen__c(Double.valueOf(info.cspem));
                            } else {
                                nObj.setFrozen__c(0.0);
                            }
                            if (info.cinsm != null && !"".equals(info.cinsm)) {
                                nObj.setChecking__c(Double.valueOf(info.cinsm));
                            } else {
                                nObj.setChecking__c(0.0);
                            }
                            if (info.cretm != null && !"".equals(info.cretm)) {
                                nObj.setReturn__c(Double.valueOf(info.cretm));
                            } else {
                                nObj.setReturn__c(0.0);
                            }
                            if (info.lwedt != null && !"".equals(info.lwedt)) {
                                nObj.setInventory_Age__c(Double.valueOf(info.lwedt));
                            } else {
                                nObj.setInventory_Age__c(0.0);
                            }
                            if (info.kunnr != null && !"".equals(info.kunnr)) {
                                nObj.setCustomer__c(Long.valueOf(info.kunnr));
                            }
                            if (info.kulab != null && !"".equals(info.kulab)) {
                                logger.info("我不信：：：" + info.kulab);
                                nObj.setConsign_Inventory_Quantity__c(Double.valueOf(info.kulab));
                            } else {
                                nObj.setConsign_Inventory_Quantity__c(0.0);
                            }
                            if (!"".equals(info.omeng)) {
                                nObj.setPlannedDNQty__c(Double.valueOf(info.omeng));
                            } else {
                                nObj.setPlannedDNQty__c(0.0);
                            }
//                            nObj.setName("测试用");
                            nObj.setEntityType(proInTypeID);
                            logger.debug("gIdMap-key:" + key);
                            logger.debug("gIdMap-key:" + gIdMap.containsKey(key));
                            if ("2150".equals(info.werks) || "2151".equals(info.werks)){
                                logger.debug("proMap-key:" + (info.matnr + "[" + special_plant.get(info.werks) + "]") + proMap.containsKey(info.matnr + "[" + special_plant.get(info.werks) + "]"));
                                logger.debug("proMap-key:" + (info.matnr + "[" + info.werks + "]") + proMap.containsKey(info.matnr + "[" + info.werks + "]"));
                            }
                            if (gIdMap.containsKey(key)){
                                nObj.setGoods__c(gIdMap.get(key).getLong("Goods__c"));
                                nObj.setProduct__c(pIdMap.get(key).getLong("Product__c")); // 20250109   zyh   增加产品
                                if ("".equals(gIdMap.get(key).getLong("Goods__c"))){
                                    if (proMap.containsKey(info.matnr + "[" + info.werks + "]")) {
                                        nObj.setGoods__c(proMap.get(info.matnr + "[" + info.werks + "]").getLong("goods"));
                                        nObj.setProduct__c(proMap.get(info.matnr + "[" + info.werks + "]").getLong("id")); // 20250109   zyh   增加产品
                                    }
                                    if (proMap.containsKey(info.matnr + "[" + special_plant.get(info.werks) + "]")) {
                                        nObj.setGoods__c(proMap.get(info.matnr + "[" + special_plant.get(info.werks) + "]").getLong("goods"));
                                        nObj.setProduct__c(proMap.get(info.matnr + "[" + special_plant.get(info.werks) + "]").getLong("id")); // 20250109   zyh   增加产品
                                    }
                                }
                                nObj.setId(gIdMap.get(key).getLong("id"));
                                proInUpdate.add(nObj);
                            } else {
                                // 20241224   zyh   商品赋值   start
                                //nObj.setName("测试1223");
                                if (proMap.containsKey(info.matnr + "[" + info.werks + "]")) {
                                    nObj.setGoods__c(proMap.get(info.matnr + "[" + info.werks + "]").getLong("goods"));
                                    nObj.setProduct__c(proMap.get(info.matnr + "[" + info.werks + "]").getLong("id")); // 20250109   zyh   增加产品
                                } else if (proMap.containsKey(info.matnr + "[" + special_plant.get(info.werks) + "]")){
                                    nObj.setGoods__c(proMap.get(info.matnr + "[" + special_plant.get(info.werks) + "]").getLong("goods"));
                                    nObj.setProduct__c(proMap.get(info.matnr + "[" + special_plant.get(info.werks) + "]").getLong("id")); // 20250109   zyh   增加产品
                                }else {
                                    nObj.setProduct__c(proIdMap.get(info.matnr)); // 20250109   zyh   增加产品
                                }
                                if (goodIdMap.get(info.matnr) != null){
                                    nObj.setGoods__c(goodIdMap.get(info.matnr));
                                }
                                nObj.setExternal_Id__c(key);
                                // 20241224   zyh   商品赋值   end
                                proInInsert.add(nObj);
                            }
                        }
                        if (proInInsert.size() > 0){
                            if (proInInsert.size() <= 1000) {
                                BatchOperateResult insert = XObjectService.instance().insert(proInInsert, false, true);
                                if (insert.getSuccess()) {
                                    logger.info("插入成功");
                                } else {
                                    logger.error("插入失败：" + insert.getErrorMessage());
                                    errorMessage += insert.getErrorMessage();
                                }
                            } else {
                                int batchSize = 1000;
                                int total = proInInsert.size();
                                for (int i = 0; i < total; i += batchSize){
                                    int end = Math.min(i+batchSize,total);
                                    List<Product_Inventory__c> batchList = proInInsert.subList(i, end);
                                    try{
                                        logger.error("执行数量" + batchList.size());
                                        BatchOperateResult insert = XObjectService.instance().insert(batchList, false, true);
                                        if (insert.getSuccess()){
                                            logger.info("批次"+ (i/batchSize + 1) + "~" +end +"插入成功，共" + batchList.size() + "条。");
                                            logger.info("插入成功" + batchList);
                                        } else {
                                            logger.error("批次"+ (i/batchSize + 1) + "~" +end +"插入失败，共" + batchList.size() + "条。失败原因：" + insert.getErrorMessage());
                                            logger.error("插入失败" + batchList);
                                        }
                                    } catch (ApiEntityServiceException e) {
                                        logger.error("批次"+ (i/batchSize + 1) + "~" +end +"插入失败，共" + batchList.size() + "条。失败原因：" + e.getMessage());
                                        logger.error("插入失败" + batchList);
                                        throw new RuntimeException(e.getMessage());
                                    }
                                }
                            }
                            // TODO
                            /*for (Product_Inventory__c nObj : proInInsert){
                                OperateResult insert = XObjectService.instance().insert(nObj,true);
                                if (insert.getSuccess()){
                                    logger.info("插入成功");
                                }else {
                                    logger.error("插入失败：" + insert.getErrorMessage());
                                    errorMessage += insert.getErrorMessage();
                                }
                            }*/
                        }
                        if (proInUpdate.size() > 0){
                            if(proInUpdate.size() <= 1000) {
                                BatchOperateResult update = XObjectService.instance().update(proInUpdate, false, true);
                                if (update.getSuccess()) {
                                    logger.info("更新成功");
                                    logger.info("更新成功" + proInUpdate);
                                } else {
                                    logger.error("更新失败：" + update.getErrorMessage());
                                    errorMessage += update.getErrorMessage();
                                }
                            } else {
                                int batchSize = 1000;
                                int total = proInUpdate.size();
                                for (int i = 0; i < total; i += batchSize){
                                    int end = Math.min(i+batchSize,total);
                                    List<Product_Inventory__c> batchList = proInUpdate.subList(i, end);
                                    try{
                                        logger.error("执行数量" + batchList.size());
                                        BatchOperateResult update = XObjectService.instance().update(batchList, false, true);
                                        if (update.getSuccess()){
                                            logger.info("批次"+ (i/batchSize + 1) + "~" +end +"更新成功，共" + batchList.size() + "条。");
                                            logger.info("更新成功" + batchList);
                                        } else {
                                            logger.error("批次"+ (i/batchSize + 1) + "~" +end +"更新失败，共" + batchList.size() + "条。失败原因：" + update.getErrorMessage());
                                            logger.error("更新失败" + batchList);
                                        }
                                    } catch (ApiEntityServiceException e) {
                                        logger.error("批次"+ (i/batchSize + 1) + "~" +end +"更新失败，共" + batchList.size() + "条。失败原因：" + e.getMessage());
                                        logger.error("更新失败" + batchList);
                                        throw new RuntimeException(e.getMessage());
                                    }
                                }
                            }
                            /*for (Product_Inventory__c nObj : proInUpdate){
                                OperateResult update = XObjectService.instance().update(nObj,true);
                                if (update.getSuccess()){
                                    logger.info("更新成功");
                                }else {
                                    logger.error("更新失败：" + update.getErrorMessage());
                                    errorMessage += update.getErrorMessage();
                                }
                            }*/
                        }
                    }
                }
            }
            if ("E".equals(returnStatus)){
                isSuccess = false;
                errorMessage += esbinfo.getString("returnmsg");
            }
        }
        List<String> loglist = new ArrayList<>();
        loglist.add(String.valueOf(tempId));
        ObjectOptionValueRetrieval.insertInterfaceLog("ProductSalesInfoSyncToSAP", "CRM",
                "SAP", END_POINT, commonData.getBody(), isSuccess, false,
                result.getResult(), errorMessage, "POST",loglist,
                "查询产品库存信息", "CRM查询SAP库存信息");
        return result;
    }
    // 校验进行中的库存
    public static Map<String,BigDecimal> checkNum(List<String> wlhList){
        Map<String,BigDecimal> numMap = new HashMap<>();
        Map<String,String> errSQL = new HashMap<>();
        try{
            String wlhCode = SqlFormatUtils.joinInSql(wlhList);
            RkhdHttpClient rkhdclient = RkhdHttpClient.instance();
			// 20250910 ljh 排除退货 Order_type__c != 3  
            String sql = "select id,quantity,Converted_quantity__c,orderId,orderId.po,orderId.Sales_Org__c,orderId.Status__c,orderId.wareHouse__c,productId.External_matnr__c,productId.Trade_Name__c from orderProduct where orderId.Status__c = 2 and orderId.Order_type__c != 3 and productId.External_matnr__c IN( " + wlhCode + ") and entityType <> " + MetadataService.instance().getBusiType("Order", "orderReturn__c").getId() + " and orderId <> " + tempId;
//            JSONArray proWlhInfo = NeoCrmRkhdService.xoql(rkhdclient,sql);
            QueryResult proWlhQuery = XoqlService.instance().query(sql,true);
            JSONArray proWlhInfo = JSONArray.parseArray(proWlhQuery.getRecords().toString());
            // 20241224   zyh   退货单不查询   start
            logger.error("退货单不查" + proWlhInfo);
            // List<Long> orderIdList = new ArrayList<>(); // 存储订单ID
            // List<Long> returnOList = new ArrayList<>(); // 存储退单ID
            if (proWlhInfo != null && proWlhInfo.size() > 0){
                // 定义接口，查询记录类型label时用
                RkhdHttpClient instance = RkhdHttpClient.instance();
                // 获取所有订单ID
                /*for (int i = 0; i < proWlhInfo.size() ; i++){
                    returnOList.add(proWlhInfo.getJSONObject(i).getLong("orderId"));
                }*/
                // 判断订单是否是退单类型
                /*if (orderIdList.size() > 0) {
                    for (int i = 0; i < orderIdList.size(); i++) {
                        logger.error("记录类型：" + NeoCrmRkhdService.v2QueryRecordInfoByIdXobject(instance,"order",orderIdList.get(i)));
                        logger.error("记录类型：" + NeoCrmRkhdService.v2QueryRecordInfoByIdXobject(instance,"order",orderIdList.get(i)).getString("entityType-label"));
                        if ("Return order".equals(NeoCrmRkhdService.v2QueryRecordInfoByIdXobject(instance,"order",orderIdList.get(i)).getString("entityType-label"))){
                            returnOList.add(orderIdList.get(i));
                        }
                    }
                }*/
                // 20241224   zyh   退货单不查询   end
                for (int i = 0; i < proWlhInfo.size() ; i++){
                    // 加纳拼接brandName
                    if (proWlhInfo.getJSONObject(i).getJSONArray("orderId.Sales_Org__c").get(0).equals("7030 Rainbow Agro(Ghana)") ||
                            proWlhInfo.getJSONObject(i).getJSONArray("orderId.Sales_Org__c").get(0).equals("7190 AGROTERRUM GHANA")){
                        if (numMap.containsKey(proWlhInfo.getJSONObject(i).getString("productId.External_matnr__c") + "-" + houseApiMap.get(proWlhInfo.getJSONObject(i).getJSONArray("orderId.wareHouse__c").getString(0)) + "-" + proWlhInfo.getJSONObject(i).getString("productId.Trade_Name__c"))) {
                            BigDecimal num = numMap.get(proWlhInfo.getJSONObject(i).getString("productId.External_matnr__c") + "-" + houseApiMap.get(proWlhInfo.getJSONObject(i).getJSONArray("orderId.wareHouse__c").getString(0)) + "-" + proWlhInfo.getJSONObject(i).getString("productId.Trade_Name__c")).add(BigDecimal.valueOf(proWlhInfo.getJSONObject(i).getDouble("Converted_quantity__c")));
                            // BigDecimal num = numMap.get(proWlhInfo.getJSONObject(i).getString("productId.External_matnr__c") + "-" + houseApiMap.get(proWlhInfo.getJSONObject(i).getJSONArray("orderId.wareHouse__c").getString(0)) + "-" + proWlhInfo.getJSONObject(i).getString("productId.Trade_Name__c")).add(BigDecimal.valueOf(proWlhInfo.getJSONObject(i).getDouble("quantity")));
                            if (proWlhInfo.getJSONObject(i).getJSONArray("orderId.wareHouse__c") != null) {
                                numMap.put(proWlhInfo.getJSONObject(i).getString("productId.External_matnr__c") + "-" + houseApiMap.get(proWlhInfo.getJSONObject(i).getJSONArray("orderId.wareHouse__c").getString(0)) + "-" + proWlhInfo.getJSONObject(i).getString("productId.Trade_Name__c"), num);
                                // errSQL.put(proWlhInfo.getJSONObject(i).getString("orderId.po"),proWlhInfo.getJSONObject(i).getString("Converted_quantity__c"));
                                errSQL.put(proWlhInfo.getJSONObject(i).getString("orderId.po"),proWlhInfo.getJSONObject(i).getString("quantity"));
                            } else {
//                                throw new RuntimeException("用户仓库为空");
                                throw new RuntimeException("The user's warehouse is empty");
                            }
                        } else {
                            if (proWlhInfo.getJSONObject(i).getJSONArray("orderId.wareHouse__c") != null) {
                                logger.error("==================666==" + proWlhInfo.getJSONObject(i).getString("productId.External_matnr__c") + "-" + houseApiMap.get(proWlhInfo.getJSONObject(0).getJSONArray("orderId.wareHouse__c").getString(0)));
                                logger.error("==================666==" + proWlhInfo.getJSONObject(i).getDouble("Converted_quantity__c"));
                                numMap.put(proWlhInfo.getJSONObject(i).getString("productId.External_matnr__c") + "-" + houseApiMap.get(proWlhInfo.getJSONObject(i).getJSONArray("orderId.wareHouse__c").getString(0)) + "-" + proWlhInfo.getJSONObject(i).getString("productId.Trade_Name__c"), BigDecimal.valueOf(proWlhInfo.getJSONObject(i).getDouble("Converted_quantity__c")));
                                // numMap.put(proWlhInfo.getJSONObject(i).getString("productId.External_matnr__c") + "-" + houseApiMap.get(proWlhInfo.getJSONObject(i).getJSONArray("orderId.wareHouse__c").getString(0)) + "-" + proWlhInfo.getJSONObject(i).getString("productId.Trade_Name__c"), BigDecimal.valueOf(proWlhInfo.getJSONObject(i).getDouble("quantity")));
                                // errSQL.put(proWlhInfo.getJSONObject(i).getString("orderId.po"),proWlhInfo.getJSONObject(i).getString("Converted_quantity__c"));
                                errSQL.put(proWlhInfo.getJSONObject(i).getString("orderId.po"),proWlhInfo.getJSONObject(i).getString("quantity"));
                            } else {
//                                throw new RuntimeException("用户仓库为空");
                                throw new RuntimeException("The user's warehouse is empty");
                            }
                        }
                    } else {
                        // 添加判断，returnOList包含该订单的话不走下面逻辑
                        if (proWlhInfo.getJSONObject(i) != null) {
                            if (numMap.containsKey(proWlhInfo.getJSONObject(i).getString("productId.External_matnr__c") + "-" + houseApiMap.get(proWlhInfo.getJSONObject(i).getJSONArray("orderId.wareHouse__c").getString(0)))) {
                                logger.error("====================" + proWlhInfo.getJSONObject(i).getDouble("Converted_quantity__c"));
                                logger.error("====================" + proWlhInfo.getJSONObject(i).getString("productId.External_matnr__c") + "-" + houseApiMap.get(proWlhInfo.getJSONObject(0).getJSONArray("orderId.wareHouse__c").getString(0)));
                                logger.error("====================" + numMap);
                                logger.error("==========222==========" + numMap.get(proWlhInfo.getJSONObject(i).getString("productId.External_matnr__c") + "-" + houseApiMap.get(proWlhInfo.getJSONObject(0).getJSONArray("orderId.wareHouse__c").getString(0))));
                                BigDecimal num = numMap.get(proWlhInfo.getJSONObject(i).getString("productId.External_matnr__c") + "-" + houseApiMap.get(proWlhInfo.getJSONObject(i).getJSONArray("orderId.wareHouse__c").getString(0))).add(BigDecimal.valueOf(proWlhInfo.getJSONObject(i).getDouble("Converted_quantity__c")));
                                // BigDecimal num = numMap.get(proWlhInfo.getJSONObject(i).getString("productId.External_matnr__c") + "-" + houseApiMap.get(proWlhInfo.getJSONObject(i).getJSONArray("orderId.wareHouse__c").getString(0))).add(BigDecimal.valueOf(proWlhInfo.getJSONObject(i).getDouble("quantity")));
                                if (proWlhInfo.getJSONObject(i).getJSONArray("orderId.wareHouse__c") != null) {
                                    numMap.put(proWlhInfo.getJSONObject(i).getString("productId.External_matnr__c") + "-" + houseApiMap.get(proWlhInfo.getJSONObject(i).getJSONArray("orderId.wareHouse__c").getString(0)), num);
                                    // errSQL.put(proWlhInfo.getJSONObject(i).getString("orderId.po"),proWlhInfo.getJSONObject(i).getString("Converted_quantity__c"));
                                    errSQL.put(proWlhInfo.getJSONObject(i).getString("orderId.po"),proWlhInfo.getJSONObject(i).getString("quantity"));
                                } else {
//                                throw new RuntimeException("用户仓库为空");
                                    throw new RuntimeException("The user's warehouse is empty");
                                }
                            } else {
                                if (proWlhInfo.getJSONObject(i).getJSONArray("orderId.wareHouse__c") != null) {
                                    logger.error("==================666==" + proWlhInfo.getJSONObject(i).getString("productId.External_matnr__c") + "-" + houseApiMap.get(proWlhInfo.getJSONObject(0).getJSONArray("orderId.wareHouse__c").getString(0)));
                                    logger.error("==================666==" + proWlhInfo.getJSONObject(i).getDouble("quantity"));
                                    numMap.put(proWlhInfo.getJSONObject(i).getString("productId.External_matnr__c") + "-" + houseApiMap.get(proWlhInfo.getJSONObject(i).getJSONArray("orderId.wareHouse__c").getString(0)), BigDecimal.valueOf(proWlhInfo.getJSONObject(i).getDouble("Converted_quantity__c")));
                                    // numMap.put(proWlhInfo.getJSONObject(i).getString("productId.External_matnr__c") + "-" + houseApiMap.get(proWlhInfo.getJSONObject(i).getJSONArray("orderId.wareHouse__c").getString(0)), BigDecimal.valueOf(proWlhInfo.getJSONObject(i).getDouble("quantity")));
                                    // errSQL.put(proWlhInfo.getJSONObject(i).getString("orderId.po"),proWlhInfo.getJSONObject(i).getString("Converted_quantity__c"));
                                    errSQL.put(proWlhInfo.getJSONObject(i).getString("orderId.po"),proWlhInfo.getJSONObject(i).getString("quantity"));
                                } else {
//                                throw new RuntimeException("用户仓库为空");
                                    throw new RuntimeException("The user's warehouse is empty");
                                }
                            }
                        }
                    }
                }
            }
            logger.error("11111======"+numMap);
            logger.error("库存查询错误信息======"+errSQL);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage() + e.getStackTrace()[0].getLineNumber() + e.getStackTrace()[0].getClassName());
        }
        return numMap;
    }
    // 获取选项列表的API
    public static Map<String,String> getApiKey(String obj,String apiKey) throws IOException, ScriptBusinessException {
        RkhdHttpClient instance = RkhdHttpClient.instance();
        Map<String, Map<String, String>> kvMap = new HashMap<>();
        Map<String, Map<Integer, String>> vkMap = new HashMap<>();
        Map<String,Map<String,String>> pkMap = NeoCrmRkhdService.getPicklistValueKey(instance,obj,apiKey,kvMap,vkMap);
        logger.error("apikeyMap打印：" + pkMap);
        return pkMap.get(apiKey);
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
                        map.put(pickOption.getString("optionApiKey"),pickOption.getInteger("optionCode"));
                    }
                }
            }
        } catch (Exception e) {
            logger.error(" 报错信息：" + e);
        }
        return map;
    }

    public static void main(String[] args) throws CustomConfigException, ScriptBusinessException, IOException, InterruptedException, XsyHttpException, ApiEntityServiceException {
//        ApprovalEventRequest request = new ApprovalEventRequest();
//        request.setDataId(Long.valueOf("3738039591731312"));
//        ProductSalesInfoSyncToSAP ps = new ProductSalesInfoSyncToSAP();
//        ps.execute(request);
        String exProId = "000000200101000285[2150]";
        //String exProId = "000000200101001389[7020]";
        String proSql = "select id,goods,External_Id__c from product where External_Id__c IN('" + exProId + "')";
        QueryResult<JSONObject> proQuery = XoqlService.instance().query(proSql,true);
        System.out.println(proQuery.getRecords().size());
        /*List<items> iList = new ArrayList<>();
        items i = new items();
        i.setMatnr("000000200301001782");
        ProductSalesInfoSyncToSAP.crmToSap(iList);*/
       /* String str = "{\"esbinfo\":{\"instid\":\"ProductSalesInfoSyncToSAP\",\"requesttime\":\"2025-01-02 15:59:33\"},\"resultinfo\":[{\"matnr\":\"000000200302000138\",\"werks\":\"7060\"}]}";
        items i = new items();
        i.setWerks("7060");
        i.setMatnr("000000200302000138");
        List<items> iList = new ArrayList<>();
        ProductSalesInfoSyncToSAP.crmToSap(iList);*/
        /*RkhdHttpClient rkhdclient = RkhdHttpClient.instance();
        String acc = "select id from account limit 1 ";
        JSONArray accInfo = NeoCrmRkhdService.xoql(rkhdclient,acc);
        Long proInTypeID =  MetadataService.instance().getBusiType("order", "defaultOrderBusiType").getId();
        Order o = new Order();
        o.setPayment_Term__c(188);
        o.setEntityType(proInTypeID);
        o.setAccountId(accInfo.getJSONObject(0).getLong("id"));
        OperateResult insert = XObjectService.instance().insert(o,true);
        if (insert.getSuccess()){
            String sqlOrderItem = "select id,payment_Term__c from _order where orderId = "+insert.getDataId();
            String sqlOrderItem1 = "select id,po,payment_Term__c from _order where payment_Term__c = "+188;
            JSONArray orderItemInfo = NeoCrmRkhdService.xoql(rkhdclient,sqlOrderItem);
            JSONArray orderItemInfo1 = NeoCrmRkhdService.xoql(rkhdclient,sqlOrderItem1);
            logger.error("ooo===" + o);
            logger.error("ooo===" + orderItemInfo.size());
            logger.error("ooo===" + orderItemInfo1);
            logger.error("ooo===" + orderItemInfo1.size());
        } else {
            logger.error("ooo===err:" + insert.getErrorMessage());

        }*/
    }
}
