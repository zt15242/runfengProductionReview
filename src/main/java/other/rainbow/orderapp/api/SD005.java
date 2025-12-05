package other.rainbow.orderapp.api;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rkhd.platform.sdk.data.model.Order;
import com.rkhd.platform.sdk.data.model.OrderProduct;
import com.rkhd.platform.sdk.exception.ApiEntityServiceException;
import com.rkhd.platform.sdk.exception.CustomConfigException;
import com.rkhd.platform.sdk.exception.ScriptBusinessException;
import com.rkhd.platform.sdk.exception.XsyHttpException;
import com.rkhd.platform.sdk.http.*;
import com.rkhd.platform.sdk.http.handler.ResponseBodyHandler;
import com.rkhd.platform.sdk.log.Logger;
import com.rkhd.platform.sdk.log.LoggerFactory;
import com.rkhd.platform.sdk.model.BatchOperateResult;
import com.rkhd.platform.sdk.model.OperateResult;
import com.rkhd.platform.sdk.model.QueryResult;
import com.rkhd.platform.sdk.model.XObject;
import com.rkhd.platform.sdk.service.CustomConfigService;
import com.rkhd.platform.sdk.service.XObjectService;
import com.rkhd.platform.sdk.service.XoqlService;
import other.rainbow.orderapp.common.NeoCrmRkhdService;
import other.rainbow.orderapp.common.ObjectOptionValueRetrieval;
import other.rainbow.orderapp.cstss.SqlFormatUtils;
import other.rainbow.orderapp.pojo.ReturnResult;
import other.rainbow.orderapp.pojo.items;
import other.rainbow.orderapp.pojo.reqesbinfo;
import other.rainbow.orderapp.pojo.reqresultinfo;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

// 订单发送致SAP
public class SD005 {
    public SD005(String Id,
                 String changeType,
                 Boolean isClose,
                 Boolean isbeforceClose,
                 String reasonText, String s) throws ScriptBusinessException, IOException, InterruptedException, XsyHttpException, ApiEntityServiceException {
        returnResult = productSalesInfo(Id,changeType,isClose,isbeforceClose,reasonText,"");
    }
    public ReturnResult returnResult;
    private static final Logger logger = LoggerFactory.getLogger();
    public static final String END_POINT = "CreateOrder";
    private static final String URL="";
    private static final String TOKEN= "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJkZXZpY2VUeXBlIjoic3lzdGVtIiwibG9naW5OYW1lIjoiY3JtLWRhdGEtYWRtaW5AY29sb3BsYXN0LnN0ZyIsInRlbmFudElkIjoiVDEwMDAwOTExMzg1NDMxMDQ1IiwidXNlcklkIjoiMTc3MTA4OTM2NjA5MzIwOCIsInJlYWxPcGVyYXRvciI6IjE3NzEwODkzNjYwOTMyMDgiLCJ0ZXJyaXRvcnkiOm51bGwsImV4cCI6MzE1Mzc3MzEyOTE0NDgsIm5iZiI6MTczMTI5MTE0OH0.w8VrYW5H-z1tPRY_W5GC9J63Y04T-PpcaWi13O-pJ1g";

    public ReturnResult productSalesInfo(String Id,//订单ID
                                         String changeType,//创建还是更新
                                         Boolean isClose,//关闭 的时候传
                                         Boolean isbeforceClose,//重新打开 的时候传
                                         String reasonText,//关闭原有,关闭时必填
                                         String orderItemId//订单行ID 用分号 分隔
                                         /*Boolean isRealClose,//是否真正关闭  false
                                         Boolean change*/) throws ScriptBusinessException, InterruptedException, XsyHttpException, IOException, ScriptBusinessException, InterruptedException, ApiEntityServiceException/*, InterruptedException, ApiEntityServiceException */{
        long longID = Long.parseLong(Id);
        logger.error("reasonText====" + reasonText);

//        String url=URL+"/rest/action/customer/get_crm_customer_list";
        String url=URL/*+"/rest/action/customer/get_crm_customer_list"*/;
        // String credentials = "crm01" + ":" + "Abcd1234";
        // 使用 Base64 编码
        // String encoded = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        String encoded = "";
        // 创建 "Basic " 开头的字符串
        // String hed = "Basic " + encoded;
//        String encoded = "eHN5LWNybTAxOkFiY2QxMjM0";
        CustomConfigService customConfigService = CustomConfigService.instance();
        Map<String, String> configProperties = null;

        try {
            configProperties = customConfigService.getConfigSet("sap_properties");
        } catch (CustomConfigException e) {
            throw new RuntimeException(e);
        }
        if(configProperties != null) {
            Base64.Encoder encoder = Base64.getEncoder();
            encoded = configProperties.get("Authorization");
            // url = configProperties.get("endpoint") + "/sap/sales/saleorder/update";
            url = configProperties.get("SD005");
            logger.error("url==="+url);
        } else {
            url = "http://120.224.116.35:8901/sap/sales/saleorder/update";
            encoded = "Basic eHN5LWNybTAxOkFiY2QxMjM0";
        }
        if("".equals(url)){
            throw new RuntimeException("Get SAPurl is null");
        }
        CommonHttpClient commonHttpClient= CommonHttpClient.instance(15000,15000);
        CommonData commonData=new CommonData();
        commonData.addHeader("Content-Type","application/json");
        commonData.setCallString(url);
        commonData.setCall_type("POST");
        commonData.addHeader("Authorization",encoded);
        logger.info("Authorization="+encoded);
//        String tokenStr = TOKEN;
        RkhdHttpClient rkhdclient = RkhdHttpClient.instance();
        HashMap<String,Long> orderTypeMap = new HashMap<>();
//        orderTypeMap = NeoCrmRkhdService.getEntityTypesMap(rkhdclient,"defaultOrderBusiType");
        RequestBody request = new RequestBody();
        ReqESInfo res = new ReqESInfo();
        ResponseBody response = new ResponseBody();
        reqresultinfo reqresultinfo = new reqresultinfo();
        List<items> itemsList = new ArrayList<>();
        reqesbinfo reqesbinfo = new reqesbinfo();
        List<reqresultinfo> reqresultinfoList = new ArrayList<>();
        String errorMessage = "";
        String requestBodyStr = "";
        String responseBodyStr = "";
        Boolean isSuccess = true;
        // 获取全部币种信息
        Map<String,String> curMap =  getCurrencyMap();
        List<ReqOrderInfo> resultinfoList = new ArrayList<>();
        String sqlOrder = "select id,po,Profit_Center__c,Customer_Sales_Info__c,Customer_Sales_Info__c.Sales_Org__c,Sales_Org__c,order_Reason__c," +
                "Customer_Sales_Info__c.Distr_Channel__c,Distr_Channel__c," +
                "Customer_Sales_Info__c.Division__c," +
                "Customer_Sales_Info__c.Terms_Of_Payment__c," +
                "Customer_Sales_Info__c.Payment_method__c," +
                "Customer_Sales_Info__c.Sales_District__c,Sales_District__c," +
                "Customer_Sales_Info__c.Inco_Location1__c," +
                "Customer_Sales_Info__c.Sales_Office__c," +
                "Customer_Sales_Info__c.Sales_Group__c," +
                "accountId.MDG_Customer_Code__c,createdAt,currencyUnit,Business_Model__c," +
                "Order_type__c,createdBy.name,Inco_Location1__c,payment_Method__c,Customer_Shipping_Address__c.Direction__c," +
                "Customer_Sales_Info__c.Price_Group__c," +
                "Customer_Sales_Info__c.Terms_Of_Payment__c," +
                // "accountId.MDG_Customer_Code__c,Terms_Of_Payment__c,Incoterms__c," + // Terms_Of_Payment__c换成payment_Term__c
                "accountId.MDG_Customer_Code__c,payment_Term__c,Incoterms__c," +
                "E_Document_MX__c,SAP_Order_Code__c,Cust_Reference__c,entityType,isWholeshipment__c" +
                " from _order where id = " + longID;
        //JSONArray orderInfo = NeoCrmRkhdService.xoql(rkhdclient, sqlOrder);
        QueryResult<JSONObject> aaa = XoqlService.instance().query(sqlOrder,true);
        if (!aaa.getSuccess()){
            logger.info("0228===" + aaa.getErrorMessage());
        }
        JSONArray orderInfo = JSONArray.parseArray(String.valueOf(aaa.getRecords()));
        logger.error("temp====" + orderInfo);
        String sqlOrderItem = "select id,material_Number__c,Inventory_Location__c,wareHouse__c,productId.goods.factory__c,quantity,unitPrice,orderId.Delivery_Plant__c," +
                "Product_Usage__c,Gross_Weight__c,Net_Weight__c,cancelReason__c," +
                /*"New_Quantity__c," +*/
                "productId.External_matnr__c,productId.goods,productId.unit,productId.productName,productId.Trade_Name__c," +
                // hql 20251027 新增
                "is_free__c," +
                "SAP_ItemNum__c from orderProduct where orderId = " + longID;
        logger.error("zyhTestorderItemDateInfo:" + sqlOrderItem);
        //JSONArray orderItemInfo = NeoCrmRkhdService.xoql(rkhdclient, sqlOrderItem);
        QueryResult<JSONObject> orderItemquery = XoqlService.instance().query(sqlOrderItem,true);
        if (!orderItemquery.getSuccess()){
            logger.error("查询错误：" + orderItemquery.getErrorMessage());
            logger.debug("查询错误：" + orderItemquery.getErrorMessage());
        }
        JSONArray orderItemInfo = JSONArray.parseArray(String.valueOf(orderItemquery.getRecords()));
        //获取订单行最小日期
        String sqlOrderItemDate = "SELECT Delivery_Date__c FROM orderProduct WHERE orderId = " + Id + " ORDER BY Delivery_Date__c ASC Limit 1";
        JSONArray orderItemDateInfo = NeoCrmRkhdService.xoql(rkhdclient, sqlOrderItemDate);
        logger.error("zyhTestorderItemDateInfo:" + orderItemInfo.size());
        logger.error("zyhTestorderItemDateInfo:" + orderItemInfo.toJSONString());
        ReturnResult retRes = new ReturnResult();
        HttpResult result = new HttpResult();
        try {
            ReqOrderInfo roi = new ReqOrderInfo();
            roi.setOperation_type("编辑");
            if ("I".equals(changeType)) {//新增
                roi.setOperation_type("ZSD005_001");
                roi.setOperation_type_t("新增");
                // roi.OPERATION_TYPE = '新建';
            }
            if ("U".equals(changeType)) {//变更
                roi.setOperation_type("ZSD005_002");
                roi.setOperation_type_t("变更");
                //roi.setZczlx("Suspend");
                roi.setVbeln(orderInfo.getJSONObject(0).getString("SAP_Order_Code__c"));
            }
            if ("I".equals(changeType) && orderInfo.getJSONObject(0).getString("SAP_Order_Code__c") != null) {//新增
                roi.setVbeln(orderInfo.getJSONObject(0).getString("SAP_Order_Code__c"));
            }
            if (isClose) {
                roi.setZczlx("Suspend");
            }
            if (isbeforceClose) {
//            if(!change) {
                roi.setZczlx("Release pause");
//            }else{
//                roi.setZCZLX("Release suspension and change");
//            }
            }
            logger.error("订单name：" + orderInfo.getJSONObject(0).getString("po"));
            if (!"".equals(orderInfo.getJSONObject(0).getString("Cust_Reference__c"))) {
                roi.setBstkd(orderInfo.getJSONObject(0).getString("po") + "|" + orderInfo.getJSONObject(0).getString("Cust_Reference__c"));
            } else {
                roi.setBstkd(orderInfo.getJSONObject(0).getString("po"));
            }
            if ("Model_C".equals(orderInfo.getJSONObject(0).getString("entityType"))) {
                roi.setZzcomsamt(Double.valueOf(orderInfo.getJSONObject(0).getString("Commission_Amount__c")));
            }

            // roi.setZzcomsto(orderInfo.getJSONObject(0).getString("Commission_To__r.MDG_Customer_Code__c")); // 20241218接口字典没有这个
            if (orderInfo.getJSONObject(0).getString("Commission_Percentage__c") != null) {
                roi.setZzcomsperct(Double.valueOf(orderInfo.getJSONObject(0).getString("Commission_Percentage__c")) / 100);
            }
            roi.setBstdk(orderItemDateInfo.getJSONObject(0).getString("Delivery_Date__c").replaceAll("-", ""));
            roi.setVdatu(orderItemDateInfo.getJSONObject(0).getString("Delivery_Date__c").replaceAll("-", ""));
            roi.setKunnr(orderInfo.getJSONObject(0).getString("accountId.MDG_Customer_Code__c"));
            logger.error("145行测试："+orderInfo.getJSONObject(0));
            logger.error("145行测试："+orderInfo.getJSONObject(0).getLong("createdAt"));
            logger.error("145行测试："+new SimpleDateFormat("yyyyMMdd").format(orderInfo.getJSONObject(0).getLong("createdAt")));
            logger.error("币种测试："+curMap);
            roi.setAudat(new SimpleDateFormat("yyyyMMdd").format(orderInfo.getJSONObject(0).getLong("createdAt")).replaceAll("-", ""));
            if (orderInfo.getJSONObject(0).getJSONArray("currencyUnit") != null) {
                roi.setWaerk(curMap.get(orderInfo.getJSONObject(0).getJSONArray("currencyUnit").getString(0)));
            } else {
                roi.setWaerk("");
            }
            if (orderInfo.getJSONObject(0).getJSONArray("Order_type__c") != null) {
                roi.setAuart(getApiKey("order", "Order_type__c", orderInfo.getJSONObject(0).getJSONArray("Order_type__c").getString(0)));
            } else {
                roi.setAuart("");
            }
//            roi.setAuart("ZOCR");
            roi.setVsnmr_v(orderInfo.getJSONObject(0).getString("createdBy.name"));
            roi.setInco1(orderInfo.getJSONObject(0).getString("Incoterms__c"));
            roi.setInco2l(orderInfo.getJSONObject(0).getString("Inco_Location1__c"));
            roi.setZzaddress(orderInfo.getJSONObject(0).getString("Customer_Shipping_Address__c.Direction__c"));
            if (orderInfo.getJSONObject(0).getJSONArray("order_Reason__c") != null){
                roi.setAugru(getApiKey("order","order_Reason__c",orderInfo.getJSONObject(0).getJSONArray("order_Reason__c").getString(0)));
            } else {
                roi.setAugru("");
            }
            //销售视图字段赋值
            if (orderInfo.getJSONObject(0).getJSONArray("Sales_Org__c") != null){
                roi.setVkorg(getApiKey("order","Sales_Org__c",orderInfo.getJSONObject(0).getJSONArray("Sales_Org__c").getString(0)));
            } else {
                roi.setVkorg("");
            }
            // 客户组 3字段赋值
            if (orderInfo.getJSONObject(0).getJSONArray("Business_Model__c") != null) {
                roi.setKvgr3(getApiKey("order", "Business_Model__c",
                        orderInfo.getJSONObject(0).getJSONArray("Business_Model__c").getString(0)));
            } else {
                roi.setKvgr3("");
            }
            if (orderInfo.getJSONObject(0).getJSONArray("Distr_Channel__c") != null) {
                roi.setVtweg(getApiKey("order", "Distr_Channel__c", orderInfo.getJSONObject(0).getJSONArray("Distr_Channel__c").getString(0)));
            } else {
                roi.setVtweg("");
            }
            if (orderInfo.getJSONObject(0).getJSONArray("Customer_Sales_Info__c.Division__c") != null) {
                roi.setSpart(getApiKey("Customer_Sales_Info__c", "Division__c", orderInfo.getJSONObject(0).getJSONArray("Customer_Sales_Info__c.Division__c").getString(0)));
            } else {
                roi.setSpart("");
            }
            if (orderInfo.getJSONObject(0).getJSONArray("payment_Term__c") != null) {
                // 20250605 ljh 尼日利亚 Credit 换成S511  start
                // roi.setZterm(getApiKey("order", "payment_Term__c",
                //        orderInfo.getJSONObject(0).getJSONArray("payment_Term__c").get(0).toString()));
                if("Credit".equals(orderInfo.getJSONObject(0).getJSONArray("payment_Term__c").get(0).toString())){
                    roi.setZterm("S511");
                }else{
                    roi.setZterm(getApiKey("order", "payment_Term__c",
                            orderInfo.getJSONObject(0).getJSONArray("payment_Term__c").get(0).toString()));
                }
                // 20250605 ljh 尼日利亚 Credit 换成S511  end
            } else {
                roi.setZterm("");
            }
//            roi.setZterm("T");
//            roi.setBzirk(getApiKey("order","Sales_District__c",orderInfo.getJSONObject(0).getJSONArray("Sales_District__c").getString(0)));
            roi.setVkbur(orderInfo.getJSONObject(0).getString("Customer_Sales_Info__c.Sales_Office__c"));
            roi.setVkgrp(orderInfo.getJSONObject(0).getString("Customer_Sales_Info__c.Sales_Group__c"));
            //roi.KDGRP = orderList[0].Customer__r.Customer_Group__c;
            logger.error("Price_Group__c==null" + orderInfo.getJSONObject(0).getJSONArray("Customer_Sales_Info__c.Price_Group__c"));
//            if (!orderInfo.getJSONObject(0).getJSONArray("Customer_Sales_Info__c.Price_Group__c").getString(0).isEmpty()){
            if (orderInfo.getJSONObject(0).getJSONArray("Customer_Sales_Info__c.Price_Group__c") != null){
                logger.error("Price_Group__c==null" + orderInfo.getJSONObject(0).getJSONArray("Customer_Sales_Info__c.Price_Group__c").getString(0));
                roi.setKonda(getApiKey("Customer_Sales_Info__c", "Price_Group__c", orderInfo.getJSONObject(0).getJSONArray("Customer_Sales_Info__c.Price_Group__c").getString(0)));
            } else {
                logger.error("进来else");
                roi.setKonda("");
            }
            if (orderInfo.getJSONObject(0).getJSONArray("Customer_Sales_Info__c.Payment_method__c") != null){
                roi.setKvgr1(getApiKey("Customer_Sales_Info__c","Payment_method__c",orderInfo.getJSONObject(0).getJSONArray("Customer_Sales_Info__c.Payment_method__c").getString(0)));
            } else {
                roi.setKvgr1("");
            }
            // gzw 注释Kvgr3
            // roi.setKvgr3(orderInfo.getJSONObject(0).getString("Business_Model__c"));
//            logger.error("zyhTest1205=="+orderInfo.getJSONObject(0).getJSONArray("payment_Method__c").get(0).toString());
//            roi.setZlsch(getApiKey("order","payment_Method__c",orderInfo.getJSONObject(0).getJSONArray("payment_Method__c").get(0).toString()));
//            roi.setZlsch("");
//            logger.error("zyhTest1206=="+orderInfo.getJSONObject(0).getJSONArray("payment_Method__c").get(0).toString());
            roi.setEdocument_mx(orderInfo.getJSONObject(0).getString("E_Document_MX__c"));
//        roi.setAUART(orderInfo.getJSONObject(0).getString("Profit_Center__c"));
            //订单行赋值
            List<OrderItemRow> orderIRList = new ArrayList<>();
//        List<String> orderUpdateItemList = Arrays.asList(orderItemId.split(";"));
            if (orderItemInfo.size() > 0) {
                for (int i = 0; i < orderItemInfo.size(); i++) {
                    /*if (orderItemInfo.getJSONObject(i).getString("id") != orderItemId) {
                        continue;
                    }*/
                    OrderItemRow oir = new OrderItemRow();
                    // hql 20251027 新增
                    logger.error("is_free__c="+orderItemInfo.getJSONObject(i).getString("is_free__c"));
                    if (orderItemInfo.getJSONObject(i).getString("is_free__c").equals("true")||orderItemInfo.getJSONObject(i).getString("is_free__c").equals("1")||orderItemInfo.getJSONObject(i).getString("is_free__c").equals("Yes")||orderItemInfo.getJSONObject(i).getString("is_free__c").equals("是")){
                        oir.setPstyv("TANN");
                    } else if (orderItemInfo.getJSONObject(i).getString("is_free__c").equals("false")||orderItemInfo.getJSONObject(i).getString("is_free__c").equals("0")||orderItemInfo.getJSONObject(i).getString("is_free__c").equals("No")||orderItemInfo.getJSONObject(i).getString("is_free__c").equals("否")){
                        oir.setPstyv("");
                    }
                    oir.setMatnr(orderItemInfo.getJSONObject(i).getString("productId.External_matnr__c"));
                    /*if ("Approved".equals(orderInfo.getJSONObject(0).getString("Change_Status__c")) && orderInfo.getJSONObject(0).getBoolean("Order_Change__c")) {
                        if (orderItemInfo.getJSONObject(i).getDouble("New_Quantity__c") != null) {
                            oir.setLFIMG(orderItemInfo.getJSONObject(i).getDouble("New_Quantity__c") + "");
                        } else {*/
                    oir.setLfimg(orderItemInfo.getJSONObject(i).getDouble("quantity") + "");
                       /* }

                    } else {
                        oir.setLFIMG(orderItemInfo.getJSONObject(i).getDouble("Quantity__c") + "");
                    }*/
                    if ("ST".equals(orderItemInfo.getJSONObject(i).getString("productId.unit"))){
                        oir.setVrkme("PC");
                        oir.setZieme("PC");
                    } else {
                        oir.setVrkme(orderItemInfo.getJSONObject(i).getString("productId.unit")); // 暂时不需要
                        // 新增的时候增加销售单位传值
                        // if ("I".equals(changeType)){
                        oir.setZieme(orderItemInfo.getJSONObject(i).getString("productId.unit")); // 新增销售的单位
                        // }
                    }
                    oir.setKpein("1");
                    if (!"".equals(reasonText) /*&& orderItemInfo.getJSONObject(i).getString("id").equals(orderItemId)*/ && isClose) {
                        oir.setAbgru(reasonText);
                    } else if (orderItemInfo.getJSONObject(i).getJSONArray("cancelReason__c") != null) {
                        logger.error("进来cancelReason__c了=====" + orderItemInfo.getJSONObject(i).getJSONArray("cancelReason__c").getString(0));
                        oir.setAbgru(orderItemInfo.getJSONObject(i).getJSONArray("cancelReason__c").getString(0));
                    } else {
                        oir.setAbgru("");
                    }
                    oir.setZzname(orderItemInfo.getJSONObject(i).getString("productId.Trade_Name__c"));
                    // oir.setZzname(orderItemInfo.getJSONObject(i).getString("productId.productName"));
//                if (u.Country__c == 'MX') {
//                    if(orderList[0].Change_Status__c == 'Approved' && orderList[0].Order_Change__c){
//                        if(oi.New_Price__c!=null) {
//                            oir.ZPMX = oi.New_Price__c+'';
//                        }else{
//                            oir.ZPMX = oi.Price__c+'';
//                        }
//
//                    }else{
//                        oir.ZPMX = oi.Price__c+'';
//                    }
//                }
//                if (u.Country__c == 'AR') {
//                    if(orderList[0].Change_Status__c == 'Approved' && orderList[0].Order_Change__c){
//                        if(oi.New_Price__c!=null) {
//                            oir.ZP08 = oi.New_Price__c+'';
//                        }else{
//                            oir.ZP08 = oi.Price__c+'';
//                        }
//
//                    }else{
//                        oir.ZP08 = oi.Price__c+'';
//                    }
//                }
                    oir.setNetwr(orderItemInfo.getJSONObject(i).getDouble("unitPrice").toString());
                    if (orderItemInfo.getJSONObject(i).getJSONArray("orderId.Delivery_Plant__c") != null)
                        oir.setWerks(getApiKey("order", "Delivery_Plant__c", orderItemInfo.getJSONObject(i).getJSONArray("orderId.Delivery_Plant__c").getString(0)));
                    else {
                        oir.setWerks("");
                    }
                    // oir.setLgort(orderItemInfo.getJSONObject(i).getString("Inventory_Location__c"));
                    oir.setLgort(orderItemInfo.getJSONObject(i).getString("wareHouse__c"));
                    /*if ("Approved".equals(orderInfo.getJSONObject(0).getString("Change_Status__c")) && orderInfo.getJSONObject(0).getBoolean("Order_Change__c")) {
                        if (orderItemInfo.getJSONObject(i).getDate("New_Delivery_Date__c") != null) {
                            oir.setETDAT(String.valueOf(orderItemInfo.getJSONObject(i).getDate("New_Delivery_Date__c")).replaceAll("-", ""));
                        } else {
                            oir.setETDAT(String.valueOf(orderItemInfo.getJSONObject(i).getDate("Delivery_Date__c")).replaceAll("-", ""));
                        }

                    } else {
                        oir.setETDAT(String.valueOf(orderItemInfo.getJSONObject(i).getDate("Delivery_Date__c")).replaceAll("-", ""));
                    }*/
                    oir.setBrgew(orderItemInfo.getJSONObject(i).getString("Gross_Weight__c"));
                    oir.setNtgew(orderItemInfo.getJSONObject(i).getString("Net_Weight__c"));
                    oir.setPrctr(orderInfo.getJSONObject(0).getString("Profit_Center__c"));
                    oir.setGewei("KG");
                    oir.setZprous(orderItemInfo.getJSONObject(i).getString("Product_Usage__c"));

                    oir.setPosnr(orderItemInfo.getJSONObject(i).getString("SAP_ItemNum__c"));

                    orderIRList.add(oir);
                }
                logger.error("orderIRList:"+orderIRList.size());
                logger.error("orderIRList:"+orderIRList.get(0));
                roi.setItems(orderIRList);
                if (orderInfo.getJSONObject(0).getBoolean("isWholeshipment__c") != null && orderInfo.getJSONObject(0).getBoolean("isWholeshipment__c")){
                    roi.setAutlf("X");
                } else {
                    roi.setAutlf("");
                }
                resultinfoList.add(roi);
                res.setInstid("GSD005");
                res.setRequesttime("20241206");
                request.setEsbinfo(res);
                request.setResultinfo(resultinfoList);
//            requestBodyStr = JSONtoString(request).replaceAll("null", "");
                commonData.setBody(JSON.toJSONString(request).replaceAll("null", ""));
            /*JSONObject parameter = new JSONObject();
            JSONObject header = new JSONObject();
            header.put("token",tokenStr);
            parameter.put("head",header);
            parameter.put("body",requestBodyStr);
            commonData.setBody(parameter.toString());*/
//                CommonResponse<JSONObject> result = new CommonResponse<>();
                logger.error("comminData:"+commonData.getBody().toString());
                /*result = commonHttpClient.execute(commonData, new ResponseBodyHandler<JSONObject>() {
                    @Override
                    public JSONObject handle(String s) throws IOException {
                        JSONObject jsonobj = JSONObject.parseObject(s);
                        logger.error("result==="+ s);
                        return jsonobj;
                    }
                });*/
                result = commonHttpClient.execute(commonData);
                logger.error("接口返回结果：" + result.getResult());
                logger.error("接口返回结果：" + JSONObject.parseObject(result.getResult()).getString("esbinfo"));
                logger.error("接口返回结果：" + JSONObject.parseObject(JSONObject.parseObject(result.getResult()).getString("esbinfo")).getString("returnstatus"));
                String staResult = JSONObject.parseObject(JSONObject.parseObject(result.getResult()).getString("esbinfo")).getString("returnstatus");
                if("S".equals(staResult)){
                    isSuccess = true;
                    retRes.setMessage(JSONObject
                            .parseObject(JSONObject.parseObject(result.getResult()).getString("esbinfo"))
                            .getString("returnmsg"));
                } else {
                    isSuccess = false;
                    retRes.setMessage(JSONObject
                            .parseObject(JSONObject.parseObject(result.getResult()).getString("esbinfo"))
                            .getString("returnmsg"));
                }

                List<OrderProduct> upOpList = new ArrayList<>();
                Order o = new Order();
                o.setId(longID);
                logger.error("接口返回结果是否成功isSuccess：" + isSuccess);
                if (isSuccess) {
                    logger.error("成功后更新SAPNo：" + JSONObject.parseObject(JSONObject.parseObject(result.getResult()).getJSONArray("resultinfo").getString(0)).getString("vbeln"));
                    if (!"".equals(JSONObject.parseObject(JSONObject.parseObject(result.getResult()).getJSONArray("resultinfo").getString(0)).getString("vbeln"))){
                        o.setSAP_Order_Code__c(JSONObject.parseObject(JSONObject.parseObject(result.getResult()).getJSONArray("resultinfo").getString(0)).getString("vbeln"));
                        o.setSync_Status__c(3);
                        if (!JSONObject.parseObject(JSONObject.parseObject(result.getResult()).getString("esbinfo")).getString("responsetime").isEmpty()) {
                            o.setSAP_Created_Time__c(JSONObject.parseObject(JSONObject.parseObject(result.getResult()).getString("esbinfo")).getString("responsetime"));
                            o.setSAP_Created_Date__c(JSONObject.parseObject(JSONObject.parseObject(result.getResult()).getString("esbinfo")).getString("responsetime").split(" ")[0]);
                        }
                        if (orderItemInfo.size() > 0) {
                            for (int i = 0; i < orderItemInfo.size(); i++) {
                                OrderProduct op = new OrderProduct();
                                op.setId(orderItemInfo.getJSONObject(i).getLong("id"));
                                op.setExternal_Id__c(JSONObject.parseObject(JSONObject.parseObject(result.getResult()).getJSONArray("resultinfo").getString(0)).getString("vbeln") + "_" + orderItemInfo.getJSONObject(i).getLong("SAP_ItemNum__c"));
                                upOpList.add(op);
                            }
                        }
                    }else {
                        o.setSAP_Order_Code__c("未返回SAP号");
                    }
                    o.setTo_SAP_Error__c(false);
                    if(isClose){
                        o.setDelivery_Status__c(4);
                    }
                } else {
                    o.setTo_SAP_Error__c(true);
                    o.setSync_Status__c(2);
                    if (!JSONObject.parseObject(JSONObject.parseObject(result.getResult()).getJSONArray("resultinfo").getString(0)).getString("msgtx").isEmpty()){
                        o.setSync_Failure_Reasons__c(JSONObject.parseObject(JSONObject.parseObject(result.getResult()).getJSONArray("resultinfo").getString(0)).getString("msgtx"));
                    }
                }
                logger.error("更新订单信息：" + o);
                OperateResult update = XObjectService.instance().update(o,true);
                logger.error("更新订单行数量：" + upOpList.size());
                if (upOpList.size() > 0){
                    BatchOperateResult upOp = XObjectService.instance().update(upOpList,false,true);
                    logger.error("更新订单行信息：" + upOp.getSuccess().toString() + upOp.getErrorMessage());
                }
                logger.error("更新订单信息：" + update.getSuccess().toString() + update.getErrorMessage());




            }
        } catch (Exception e){

            try {
                Order o = new Order();
                o.setId(longID);
                o.setTo_SAP_Error__c(true);
                o.setSync_Status__c(2);
                OperateResult update = XObjectService.instance().update(o,true);
            } catch (Exception ex){
                logger.error("jinlaiCatch-Catch:" + ex.getMessage());
                retRes.setMessage(ex.getMessage());
            }
            logger.error("jinlaiCatch:" + e.getMessage());
            logger.error("jinlaiCatch:" + e.getStackTrace()[0].getLineNumber() + "class:" + e.getStackTrace()[0].getClassName());
            e.printStackTrace();
            logger.error("jinlaiCatch:" + e);
        }
        retRes.setIsSuccess(isSuccess);
        List<String> loglist = new ArrayList<>();
        loglist.add(Id);
        ObjectOptionValueRetrieval.insertInterfaceLog("SD005", "CRM",
                "SAP", END_POINT, commonData.getBody().toString(), isSuccess, false,
                result.getResult(), errorMessage, "POST",loglist,
                "订单同步SAP", "CRM同步订单至SAP");
        return retRes;
    }
    //    public class ReturnResult{
////        @InvocableVariable(Label='是否成功' Description='是否成功')
//        public Boolean isSuccess;
////        @InvocableVariable(Label='返回信息结果' Description='返回信息结果')
//        public String message;
////        @InvocableVariable(Label='返回数据' Description='返回数据')
//        public List<Items> item;
//
//        public Boolean getSuccess() {
//            return isSuccess;
//        }
//
//        public void setSuccess(Boolean success) {
//            isSuccess = success;
//        }
//
//        public String getMessage() {
//            return message;
//        }
//
//        public void setMessage(String message) {
//            this.message = message;
//        }
//
//        public List<Items> getItem() {
//            return item;
//        }
//
//        public void setItem(List<Items> item) {
//            this.item = item;
//        }
//    }
    // 获取选项列表的API
    public static String getApiKey(String obj,String apiKey,String label) throws IOException, ScriptBusinessException {
        RkhdHttpClient instance = RkhdHttpClient.instance();
        Map<String, Map<String, String>> kvMap = new HashMap<>();
        Map<String, Map<Integer, String>> vkMap = new HashMap<>();
        Map<String,Map<String,String>> pkMap = NeoCrmRkhdService.getPicklistValueKey(instance,obj,apiKey,kvMap,vkMap);
        logger.error("apikeyMap打印：" + pkMap);
        return pkMap.get(apiKey).get(label);
    }
    public static Map<String,String> getCurrencyMap() throws IOException {
        Map<String,String> curMap = new HashMap<>();
        RkhdHttpClient client = RkhdHttpClient.instance();
        RkhdHttpData data = new RkhdHttpData();
        data.setCallString("/rest/metadata/v2.0/settings/systemSettings/currencies");
        data.setCall_type("GET");
        String responseStr = client.performRequest(data);
        JSONObject jObj = JSONObject.parseObject(responseStr);
        JSONArray curArray = jObj.getJSONObject("data").getJSONArray("records");
        for (int i = 0;i < curArray.size();i++){
            JSONObject curJS = curArray.getJSONObject(i);
            logger.error("货币遍历" + curJS);
            curMap.put(curJS.getString("label"),curJS.getString("currencyCode"));
        }
        return curMap;
    }

    public class ResponseBody {
        public RespESInfo esbinfo;
        public List<RespOrderInfo> resultinfo;

        public RespESInfo getEsbinfo() {
            return esbinfo;
        }

        public void setEsbinfo(RespESInfo esbinfo) {
            this.esbinfo = esbinfo;
        }

        public List<RespOrderInfo> getResultinfo() {
            return resultinfo;
        }

        public void setResultinfo(List<RespOrderInfo> resultinfo) {
            this.resultinfo = resultinfo;
        }
    }

    public class RespESInfo{
        public String instid;//接口编号
        public String requesttime;//请求时间
        public String responsetime;//返回时间
        public String returnstatus;//接口状态
        public String returncode;//接口状态码
        public String returnmsg;//接口消息
        public String attr1;//预留字段
        public String attr2;//预留字段
        public String attr3;//预留字段

        public String getInstid() {
            return instid;
        }

        public void setInstid(String instid) {
            this.instid = instid;
        }

        public String getRequesttime() {
            return requesttime;
        }

        public void setRequesttime(String requesttime) {
            this.requesttime = requesttime;
        }

        public String getResponsetime() {
            return responsetime;
        }

        public void setResponsetime(String responsetime) {
            this.responsetime = responsetime;
        }

        public String getReturnstatus() {
            return returnstatus;
        }

        public void setReturnstatus(String returnstatus) {
            this.returnstatus = returnstatus;
        }

        public String getReturncode() {
            return returncode;
        }

        public void setReturncode(String returncode) {
            this.returncode = returncode;
        }

        public String getReturnmsg() {
            return returnmsg;
        }

        public void setReturnmsg(String returnmsg) {
            this.returnmsg = returnmsg;
        }

        public String getAttr1() {
            return attr1;
        }

        public void setAttr1(String attr1) {
            this.attr1 = attr1;
        }

        public String getAttr2() {
            return attr2;
        }

        public void setAttr2(String attr2) {
            this.attr2 = attr2;
        }

        public String getAttr3() {
            return attr3;
        }

        public void setAttr3(String attr3) {
            this.attr3 = attr3;
        }
    }

    public class RespOrderInfo{
        public String bstkd;//需货单号
        public String vbeln;//SAP订单号
        public String kunnr;//客户编号
        public String vkorg;//销售组织
        public String vtweg;//分销渠道
        public String spart;//产品组
        public String msgty;//消息类型
        public String msgtx;//消息文本
        public String vbeln2;//寄售订单号
        public List<Items> items;

        public String getBstkd() {
            return bstkd;
        }

        public void setBstkd(String bstkd) {
            this.bstkd = bstkd;
        }

        public String getVbeln() {
            return vbeln;
        }

        public void setVbeln(String vbeln) {
            this.vbeln = vbeln;
        }

        public String getKunnr() {
            return kunnr;
        }

        public void setKunnr(String kunnr) {
            this.kunnr = kunnr;
        }

        public String getVkorg() {
            return vkorg;
        }

        public void setVkorg(String vkorg) {
            this.vkorg = vkorg;
        }

        public String getVtweg() {
            return vtweg;
        }

        public void setVtweg(String vtweg) {
            this.vtweg = vtweg;
        }

        public String getSpart() {
            return spart;
        }

        public void setSpart(String spart) {
            this.spart = spart;
        }

        public String getMsgty() {
            return msgty;
        }

        public void setMsgty(String msgty) {
            this.msgty = msgty;
        }

        public String getMsgtx() {
            return msgtx;
        }

        public void setMsgtx(String msgtx) {
            this.msgtx = msgtx;
        }

        public String getVbeln2() {
            return vbeln2;
        }

        public void setVbeln2(String vbeln2) {
            this.vbeln2 = vbeln2;
        }

        public List<Items> getItems() {
            return items;
        }

        public void setItems(List<Items> items) {
            this.items = items;
        }
    }

    public class Items {
        public String status;
        public String posnr;
        public String taxPrice;  //含税单价
        public String netwr;  //含税总额
        public String ieps; //墨西哥IEPS税
        public String optax;//墨西哥销项税

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getPosnr() {
            return posnr;
        }

        public void setPosnr(String posnr) {
            this.posnr = posnr;
        }

        public String getTaxPrice() {
            return taxPrice;
        }

        public void setTaxPrice(String taxPrice) {
            this.taxPrice = taxPrice;
        }

        public String getNetwr() {
            return netwr;
        }

        public void setNetwr(String netwr) {
            this.netwr = netwr;
        }

        public String getIeps() {
            return ieps;
        }

        public void setIeps(String ieps) {
            this.ieps = ieps;
        }

        public String getOptax() {
            return optax;
        }

        public void setOptax(String optax) {
            this.optax = optax;
        }
    }

    public class RequestBody {
        public ReqESInfo esbinfo;
        public List<ReqOrderInfo> resultinfo;

        public ReqESInfo getEsbinfo() {
            return esbinfo;
        }

        public void setEsbinfo(ReqESInfo esbinfo) {
            this.esbinfo = esbinfo;
        }

        public List<ReqOrderInfo> getResultinfo() {
            return resultinfo;
        }

        public void setResultinfo(List<ReqOrderInfo> resultinfo) {
            this.resultinfo = resultinfo;
        }
    }

    public class ReqESInfo{
        public String instid;//接口编号
        public String requesttime;//请求时间

        public String getInstid() {
            return instid;
        }

        public void setInstid(String instid) {
            this.instid = instid;
        }

        public String getRequesttime() {
            return requesttime;
        }

        public void setRequesttime(String requesttime) {
            this.requesttime = requesttime;
        }
    }

    public class ReqOrderInfo{
        public String operation_type;//接口操作类型 SD005_001：新增，SD005_002：变更，SD005_003：再次交付承诺
        public String operation_type_t;//接口操作类型描述
        public String vbeln;//SAP订单号
        public String bstkd;//客户参考
        public String bstdk;//客户参考日期
        public String kunnr;//客户编号
        public String vkorg;//销售组织
        public String vtweg;//分销渠道
        public String spart;//产品组
        public String audat;//凭证日期
        public String waerk;//SD 凭证货币
        public String auart;//销售凭证类型
        public String zterm;//付款条件代码
        public String bzirk;//销售地区
        public String inco1;//国际贸易条款（第 1 部分）
        public String inco2l;//国际贸易条款位置 2
        public String vkbur;//销售办事处
        public String vkgrp;//销售组
        public String kvgr1;//客户组 1
        public String kvgr3;//客户组 3
        public String vsnmr_v;//销售单据版本编号
        public String kdgrp;//客户组
        public String konda;//客户价格组
        public String zczlx;//对销售订单的操作类型
        public String zzaddress;//地址备注
        public String augru;//订单原因
        public String zlsch;//付款方式
        public String edocument_mx;//电票备注
        public Include include;//备用字段
        public String vdatu;//交货日期
        public Double zzcomsamt;//  佣金
        public String zzcomsto;//  佣金对象
        public Double zzcomsperct;//  佣金比例
        public List<OrderItemRow> items;//销售订单行项目表
        public String autlf;//  是否整单

        public String getAutlf() {
            return autlf;
        }

        public void setAutlf(String autlf) {
            this.autlf = autlf;
        }

        public String getOperation_type() {
            return operation_type;
        }

        public void setOperation_type(String operation_type) {
            this.operation_type = operation_type;
        }

        public String getOperation_type_t() {
            return operation_type_t;
        }

        public void setOperation_type_t(String operation_type_t) {
            this.operation_type_t = operation_type_t;
        }

        public String getVbeln() {
            return vbeln;
        }

        public void setVbeln(String vbeln) {
            this.vbeln = vbeln;
        }

        public String getBstkd() {
            return bstkd;
        }

        public void setBstkd(String bstkd) {
            this.bstkd = bstkd;
        }

        public String getBstdk() {
            return bstdk;
        }

        public void setBstdk(String bstdk) {
            this.bstdk = bstdk;
        }

        public String getKunnr() {
            return kunnr;
        }

        public void setKunnr(String kunnr) {
            this.kunnr = kunnr;
        }

        public String getVkorg() {
            return vkorg;
        }

        public void setVkorg(String vkorg) {
            this.vkorg = vkorg;
        }

        public String getVtweg() {
            return vtweg;
        }

        public void setVtweg(String vtweg) {
            this.vtweg = vtweg;
        }

        public String getSpart() {
            return spart;
        }

        public void setSpart(String spart) {
            this.spart = spart;
        }

        public String getAudat() {
            return audat;
        }

        public void setAudat(String audat) {
            this.audat = audat;
        }

        public String getWaerk() {
            return waerk;
        }

        public void setWaerk(String waerk) {
            this.waerk = waerk;
        }

        public String getAuart() {
            return auart;
        }

        public void setAuart(String auart) {
            this.auart = auart;
        }

        public String getZterm() {
            return zterm;
        }

        public void setZterm(String zterm) {
            this.zterm = zterm;
        }

        public String getBzirk() {
            return bzirk;
        }

        public void setBzirk(String bzirk) {
            this.bzirk = bzirk;
        }

        public String getInco1() {
            return inco1;
        }

        public void setInco1(String inco1) {
            this.inco1 = inco1;
        }

        public String getInco2l() {
            return inco2l;
        }

        public void setInco2l(String inco2l) {
            this.inco2l = inco2l;
        }

        public String getVkbur() {
            return vkbur;
        }

        public void setVkbur(String vkbur) {
            this.vkbur = vkbur;
        }

        public String getVkgrp() {
            return vkgrp;
        }

        public void setVkgrp(String vkgrp) {
            this.vkgrp = vkgrp;
        }

        public String getKvgr1() {
            return kvgr1;
        }

        public void setKvgr1(String kvgr1) {
            this.kvgr1 = kvgr1;
        }

        public String getKvgr3() {
            return kvgr3;
        }

        public void setKvgr3(String kvgr3) {
            this.kvgr3 = kvgr3;
        }

        public String getVsnmr_v() {
            return vsnmr_v;
        }

        public void setVsnmr_v(String vsnmr_v) {
            this.vsnmr_v = vsnmr_v;
        }

        public String getKdgrp() {
            return kdgrp;
        }

        public void setKdgrp(String kdgrp) {
            this.kdgrp = kdgrp;
        }

        public String getKonda() {
            return konda;
        }

        public void setKonda(String konda) {
            this.konda = konda;
        }

        public String getZczlx() {
            return zczlx;
        }

        public void setZczlx(String zczlx) {
            this.zczlx = zczlx;
        }

        public String getZzaddress() {
            return zzaddress;
        }

        public void setZzaddress(String zzaddress) {
            this.zzaddress = zzaddress;
        }

        public String getAugru() {
            return augru;
        }

        public void setAugru(String augru) {
            this.augru = augru;
        }

        public String getZlsch() {
            return zlsch;
        }

        public void setZlsch(String zlsch) {
            this.zlsch = zlsch;
        }

        public String getEdocument_mx() {
            return edocument_mx;
        }

        public void setEdocument_mx(String edocument_mx) {
            this.edocument_mx = edocument_mx;
        }

        public Include getInclude() {
            return include;
        }

        public void setInclude(Include include) {
            this.include = include;
        }

        public String getVdatu() {
            return vdatu;
        }

        public void setVdatu(String vdatu) {
            this.vdatu = vdatu;
        }

        public Double getZzcomsamt() {
            return zzcomsamt;
        }

        public void setZzcomsamt(Double zzcomsamt) {
            this.zzcomsamt = zzcomsamt;
        }

        public String getZzcomsto() {
            return zzcomsto;
        }

        public void setZzcomsto(String zzcomsto) {
            this.zzcomsto = zzcomsto;
        }

        public Double getZzcomsperct() {
            return zzcomsperct;
        }

        public void setZzcomsperct(Double zzcomsperct) {
            this.zzcomsperct = zzcomsperct;
        }

        public List<OrderItemRow> getItems() {
            return items;
        }

        public void setItems(List<OrderItemRow> items) {
            this.items = items;
        }
    }

    public class Include{
        public String hex01;//备用字段
        public String hex02;//备用字段
        public String hex03;//备用字段
        public String hex04;//备用字段
        public String hex05;//备用字段
        public String hex06;//备用字段

        public String getHex01() {
            return hex01;
        }

        public void setHex01(String hex01) {
            this.hex01 = hex01;
        }

        public String getHex02() {
            return hex02;
        }

        public void setHex02(String hex02) {
            this.hex02 = hex02;
        }

        public String getHex03() {
            return hex03;
        }

        public void setHex03(String hex03) {
            this.hex03 = hex03;
        }

        public String getHex04() {
            return hex04;
        }

        public void setHex04(String hex04) {
            this.hex04 = hex04;
        }

        public String getHex05() {
            return hex05;
        }

        public void setHex05(String hex05) {
            this.hex05 = hex05;
        }

        public String getHex06() {
            return hex06;
        }

        public void setHex06(String hex06) {
            this.hex06 = hex06;
        }
    }

    public class OrderItemRow{
        public String matnr;//物料编号
        public String lfimg;//实际已交货量（按销售单位）
        public String vrkme;//销售单位
        public String zpmx;//不含税单价(墨西哥字段)
        public String zp08;//不含税单价(阿根廷字段)



        public String netwr;//不含税单价(阿根廷字段)

        public String kpein;//价格单位
        public String werks;//交货工厂
        public String lgort;//库位
        public String etdat;//交货日期
        public String brgew;//毛重
        public String ntgew;//净重
        public String prctr;//利润中心
        public String gewei;//重量单位
        public String zzname;//产品名称
        public String posnr;//行编码
        // hql 20251027 新增
        public String pstyv;//备用字段
        public String iex02;//备用字段
        public String iex03;//备用字段
        public String iex04;//备用字段
        public String iex05;//备用字段
        public String iex06;//备用字段
        public String abgru;
        public String zprous;

        public String zieme; // 新增销售单位
        // hql 20251027 新增
        public String getPstyv() {
            return pstyv;
        }

        public void setPstyv(String pstyv) {
            this.pstyv = pstyv;
        }

        public String getZieme() {
            return zieme;
        }

        public void setZieme(String zieme) {
            this.zieme = zieme;
        }


        public String getMatnr() {
            return matnr;
        }

        public void setMatnr(String matnr) {
            this.matnr = matnr;
        }

        public String getLfimg() {
            return lfimg;
        }

        public void setLfimg(String lfimg) {
            this.lfimg = lfimg;
        }

        public String getVrkme() {
            return vrkme;
        }

        public void setVrkme(String vrkme) {
            this.vrkme = vrkme;
        }

        public String getZpmx() {
            return zpmx;
        }

        public void setZpmx(String zpmx) {
            this.zpmx = zpmx;
        }

        public String getZp08() {
            return zp08;
        }

        public void setZp08(String zp08) {
            this.zp08 = zp08;
        }

        public String getKpein() {
            return kpein;
        }

        public void setKpein(String kpein) {
            this.kpein = kpein;
        }

        public String getWerks() {
            return werks;
        }

        public void setWerks(String werks) {
            this.werks = werks;
        }

        public String getLgort() {
            return lgort;
        }

        public void setLgort(String lgort) {
            this.lgort = lgort;
        }

        public String getEtdat() {
            return etdat;
        }

        public void setEtdat(String etdat) {
            this.etdat = etdat;
        }

        public String getBrgew() {
            return brgew;
        }

        public void setBrgew(String brgew) {
            this.brgew = brgew;
        }

        public String getNtgew() {
            return ntgew;
        }

        public void setNtgew(String ntgew) {
            this.ntgew = ntgew;
        }

        public String getPrctr() {
            return prctr;
        }

        public void setPrctr(String prctr) {
            this.prctr = prctr;
        }

        public String getGewei() {
            return gewei;
        }

        public void setGewei(String gewei) {
            this.gewei = gewei;
        }

        public String getZzname() {
            return zzname;
        }

        public void setZzname(String zzname) {
            this.zzname = zzname;
        }

        public String getPosnr() {
            return posnr;
        }

        public void setPosnr(String posnr) {
            this.posnr = posnr;
        }

        public String getIex02() {
            return iex02;
        }

        public void setIex02(String iex02) {
            this.iex02 = iex02;
        }

        public String getIex03() {
            return iex03;
        }

        public void setIex03(String iex03) {
            this.iex03 = iex03;
        }

        public String getIex04() {
            return iex04;
        }

        public void setIex04(String iex04) {
            this.iex04 = iex04;
        }

        public String getIex05() {
            return iex05;
        }

        public void setIex05(String iex05) {
            this.iex05 = iex05;
        }

        public String getIex06() {
            return iex06;
        }

        public void setIex06(String iex06) {
            this.iex06 = iex06;
        }

        public String getAbgru() {
            return abgru;
        }

        public void setAbgru(String abgru) {
            this.abgru = abgru;
        }

        public String getZprous() {
            return zprous;
        }

        public void setZprous(String zprous) {
            this.zprous = zprous;
        }
        public String getNetwr() {
            return netwr;
        }

        public void setNetwr(String netwr) {
            this.netwr = netwr;
        }
    }

    public static void main(String[] args) {
        CommonHttpClient cc = CommonHttpClient.instance();
        System.out.println(cc);
    }
}
