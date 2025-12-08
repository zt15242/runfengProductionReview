package other.rainbow.cpzerooneone.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rkhd.platform.sdk.data.model.Customer_Sales_Info__c;
import com.rkhd.platform.sdk.exception.ApiEntityServiceException;
import com.rkhd.platform.sdk.exception.CustomConfigException;
import com.rkhd.platform.sdk.exception.ScriptBusinessException;
import com.rkhd.platform.sdk.exception.XsyHttpException;
import com.rkhd.platform.sdk.http.CommonData;
import com.rkhd.platform.sdk.http.CommonHttpClient;
import com.rkhd.platform.sdk.http.CommonResponse;
import com.rkhd.platform.sdk.http.RkhdHttpClient;
import com.rkhd.platform.sdk.http.handler.ResponseBodyHandler;
import com.rkhd.platform.sdk.log.Logger;
import com.rkhd.platform.sdk.log.LoggerFactory;
import com.rkhd.platform.sdk.model.BatchOperateResult;
import com.rkhd.platform.sdk.model.XObject;
import com.rkhd.platform.sdk.service.CustomConfigService;
import com.rkhd.platform.sdk.service.XObjectService;
import other.rainbow.cpzerooneone.common.NeoCrmRkhdService;
import other.rainbow.cpzerooneone.common.ObjectOptionValueRetrieval;
import other.rainbow.cpzerooneone.pojo.CustomerSales.*;
import other.rainbow.cpzerooneone.pojo.ReturnResult;


import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class CustomerSalescontroller {
    private static final Logger logger = LoggerFactory.getLogger();

    public static ReturnResult sendCustomerSales(String Id, String changeType) throws XsyHttpException, IOException, ScriptBusinessException, InterruptedException, ApiEntityServiceException, CustomConfigException {
        ReturnResult returnResult = new ReturnResult();
        String END_POINT = null;
        String urlnew ="";
        String url ="";
        String urlUpdate ="";
        String hed = "";

        CustomConfigService customConfigService = CustomConfigService.instance();
        Map<String, String> configProperties = null;
        configProperties = customConfigService.getConfigSet("sap_properties");
        if(configProperties != null) {
            urlnew = configProperties.get("CustomerSalesUrlNew");
            urlUpdate = configProperties.get("CustomerSalesUrlUpdate");
            hed = configProperties.get("Authorization");
        }else{
            returnResult.setMessage("接口地址为空");
            returnResult.setIsSuccess(false);
        }

        long longID = Long.parseLong(Id);

        if (Objects.equals(changeType, "I")) {
            END_POINT = "CustomerSalesInfoNEW";
            url = urlnew;
        }
        if(Objects.equals(changeType, "U")) {
            END_POINT = "CustomerSalesInfoUpdate";
            url = urlUpdate;
        }

//        String credentials = "crm01" + ":" + "Abcd1234";
        // 使用 Base64 编码
        String encoded = "eHN5LWNybTAxOkFiY2QxMjM0";
        // 创建 "Basic " 开头的字符串



        CommonHttpClient commonHttpClient= CommonHttpClient.instance(15000,15000);
        CommonData commonData=new CommonData();
        commonData.addHeader("Content-Type","application/json");
        commonData.setCallString(url);
        commonData.setCall_type("POST");
        commonData.addHeader("Authorization",hed);
        String responseBodyStr = "";

        RkhdHttpClient rkhdclient = RkhdHttpClient.instance();
        RequestBody request = new RequestBody();
        ReqESInfo reqESInfo = new ReqESInfo();
        reqESInfo.setAttr2("SAP");
        reqESInfo.setAttr1("NEOCRM");
        request.setEsbinfo(reqESInfo);
        List<ReqCustomerSalesInfo> resultinfoList = new ArrayList<>();
        String errorMessage = "";
        boolean isSuccess = true;
        String sqlcsi = "SELECT id,name,Customer__c.MDG_Customer_Code__c," +
                "Sales_Org__c,Distr_Channel__c,Customer__c," +
                "Division__c,Sales_District__c,District__c.Code__c,Sales_Office__c," +
                "Sales_Group__c,Currency__c,Price_Group__c,Cust_Pric_Procedure__c," +
                "Delivery_Plant__c,Shipping_Conditions__c,Terms_Of_Payment__c," +
                "Acct_Assmt_Grp_Cust__c,Incoterms__c,Inco_Location1__c,Payment_method__c " +
                ",Business_Model__c,Customer__c.Customer_Group__c,Reconciliation_Account__c,ownerId  FROM Customer_Sales_Info__c WHERE id= "+Id;

        //获取客户销售信息
        JSONArray csiinfo = NeoCrmRkhdService.xoql(rkhdclient,sqlcsi);
        String customerId = csiinfo.getJSONObject(0).getString("Customer__c");
        //获取客户税收类别
        String sqlctcinfo = "SELECT id,Country_District__c," +
                "Tax_Category__c,Tax_Category_Name__c,Tax_Category_Master_Data__c.name,Tax_Category_Master_Data__c.Country_Code__c " +
                " FROM Customer_Tax_Category__c  WHERE Sales_View__c= "+Id;
        JSONArray ctcinfo = NeoCrmRkhdService.xoql(rkhdclient,sqlctcinfo);
        //获取客户税号类别
        String sqlctncinfo = "SELECT id,Customer__c.MDG_Customer_Code__c," +
                "Tax_Number_Master_Data__c.Code__c,Long_Tax_Number__c   " +
                "FROM Customer_Tax_Number_Category__c " +
                "WHERE Customer__c = "+customerId;
        JSONArray ctncinfo = NeoCrmRkhdService.xoql(rkhdclient,sqlctncinfo);

        logger.info("csiinfo: " +csiinfo);
        logger.info("ctcinfo: " +ctcinfo);
        logger.info("ctncinfo: " +ctncinfo);
        ReqCustomerSalesInfo rcsi = new ReqCustomerSalesInfo();
        rcsi.setPartner(csiinfo.getJSONObject(0).getString("Customer__c.MDG_Customer_Code__c"));

        logger.info("Customer_Group__c1: " +csiinfo.getJSONObject(0).getJSONArray("Customer__c.Customer_Group__c"));
        logger.info("Customer_Group__c2: " +csiinfo.getJSONObject(0).getString("Customer__c.Customer_Group__c"));
        if (csiinfo.getJSONObject(0).getJSONArray("Customer__c.Customer_Group__c")!=null){
            rcsi.setKtokk(ObjectOptionValueRetrieval.OptionValue("account","Customer_Group__c",(String)csiinfo.getJSONObject(0).getJSONArray("Customer__c.Customer_Group__c").get(0)));
        }else {
            rcsi.setKtokk("");
        }
        salesInfo si = new salesInfo();
        si.setKunnr(csiinfo.getJSONObject(0).getString("Customer__c.MDG_Customer_Code__c"));
        if (csiinfo.getJSONObject(0).getJSONArray("Sales_Org__c")!=null){
            si.setVkorg(ObjectOptionValueRetrieval.OptionValue("Customer_Sales_Info__c","Sales_Org__c",(String)csiinfo.getJSONObject(0).getJSONArray("Sales_Org__c").get(0)));
        }else {
            si.setVkorg("");
        }
        if (csiinfo.getJSONObject(0).getJSONArray("Distr_Channel__c") !=null){
            si.setVtweg((String)csiinfo.getJSONObject(0).getJSONArray("Distr_Channel__c").get(0));
        }
        if (csiinfo.getJSONObject(0).getJSONArray("Division__c") !=null){
            si.setSpart((String)csiinfo.getJSONObject(0).getJSONArray("Division__c").get(0));
        }

        if (csiinfo.getJSONObject(0).getString("District__c.Code__c") !=null){
            si.setBzirk(csiinfo.getJSONObject(0).getString("District__c.Code__c"));
        }
        si.setKdgrp("");
        if (csiinfo.getJSONObject(0).getJSONArray("Payment_method__c") !=null){
            si.setKvgr1((String)csiinfo.getJSONObject(0).getJSONArray("Payment_method__c").get(0));
        }
        if (csiinfo.getJSONObject(0).getJSONArray("Business_Model__c")!=null){
            si.setKvgr3((String)csiinfo.getJSONObject(0).getJSONArray("Business_Model__c").get(0));
        }

        if (csiinfo.getJSONObject(0).getJSONArray("Sales_Office__c")!=null){
            si.setVkbur(ObjectOptionValueRetrieval.OptionValue("Customer_Sales_Info__c","Sales_Office__c",(String)csiinfo.getJSONObject(0).getJSONArray("Sales_Office__c").get(0)));
        }else {
            si.setVkbur("");
        }
        if (csiinfo.getJSONObject(0).getJSONArray("Sales_Group__c")!=null){
            si.setVkgrp(ObjectOptionValueRetrieval.OptionValue("Customer_Sales_Info__c","Sales_Group__c",(String)csiinfo.getJSONObject(0).getJSONArray("Sales_Group__c").get(0)));
        }else {
            si.setVkgrp("");
        }
        if (csiinfo.getJSONObject(0).getJSONArray("Currency__c")!=null){
            si.setWaers(ObjectOptionValueRetrieval.OptionValue("Customer_Sales_Info__c","Currency__c",(String)csiinfo.getJSONObject(0).getJSONArray("Currency__c").get(0)));
        }else {
            si.setWaers("");
        }
        if (csiinfo.getJSONObject(0).getJSONArray("Price_Group__c") !=null){
            si.setKonda((String)csiinfo.getJSONObject(0).getJSONArray("Price_Group__c").get(0));
        }

        if (csiinfo.getJSONObject(0).getJSONArray("Cust_Pric_Procedure__c")!=null){
            si.setKalks(ObjectOptionValueRetrieval.OptionValue("Customer_Sales_Info__c","Cust_Pric_Procedure__c",(String)csiinfo.getJSONObject(0).getJSONArray("Cust_Pric_Procedure__c").get(0)));
        }else {
            si.setKalks("");
        }
        if (csiinfo.getJSONObject(0).getJSONArray("Delivery_Plant__c")!=null){
            si.setVwerk(ObjectOptionValueRetrieval.OptionValue("Customer_Sales_Info__c","Delivery_Plant__c",(String)csiinfo.getJSONObject(0).getJSONArray("Delivery_Plant__c").get(0)));
        }else {
            si.setVwerk("");
        }
        if (csiinfo.getJSONObject(0).getJSONArray("Shipping_Conditions__c") !=null){
            si.setVsbed((String)csiinfo.getJSONObject(0).getJSONArray("Shipping_Conditions__c").get(0));
        }

        if (csiinfo.getJSONObject(0).getJSONArray("Terms_Of_Payment__c")!=null){
			// 20250605 ljh 尼日利亚 Credit 换成S511  start
            // si.setZterm(ObjectOptionValueRetrieval.OptionValue("Customer_Sales_Info__c","Terms_Of_Payment__c",(String)csiinfo.getJSONObject(0).getJSONArray("Terms_Of_Payment__c").get(0)));
			logger.info("zheli"+(String)csiinfo.getJSONObject(0).getJSONArray("Terms_Of_Payment__c").get(0));
			if("Credit".equals((String)csiinfo.getJSONObject(0).getJSONArray("Terms_Of_Payment__c").get(0))){
				si.setZterm("S511");
				logger.info("zheli03");
			}else{
				logger.info("zheli04");
				si.setZterm(ObjectOptionValueRetrieval.OptionValue("Customer_Sales_Info__c","Terms_Of_Payment__c",(String)csiinfo.getJSONObject(0).getJSONArray("Terms_Of_Payment__c").get(0)));
			}
			// 20250605 ljh 尼日利亚 Credit 换成S511  end
        }else {
            si.setZterm("");
        }
        if (csiinfo.getJSONObject(0).getJSONArray("Acct_Assmt_Grp_Cust__c")!=null){
            si.setKtgrd(ObjectOptionValueRetrieval.OptionValue("Customer_Sales_Info__c","Acct_Assmt_Grp_Cust__c",(String)csiinfo.getJSONObject(0).getJSONArray("Acct_Assmt_Grp_Cust__c").get(0)));
        }else {
            si.setKtgrd("");
        }
        if (csiinfo.getJSONObject(0).getJSONArray("Incoterms__c")!=null){
//            si.setInco1((String)csiinfo.getJSONObject(0).getJSONArray("Incoterms__c").get(0));
            si.setInco1(ObjectOptionValueRetrieval.OptionValue("Customer_Sales_Info__c","Incoterms__c",(String)csiinfo.getJSONObject(0).getJSONArray("Incoterms__c").get(0)));
        }else {
            si.setInco1("");
        }
        if (csiinfo.getJSONObject(0).getString("Inco_Location1__c")!=null){
            si.setLinco2_l(csiinfo.getJSONObject(0).getString("Inco_Location1__c"));
        }
        List<TaxRow> taxList = new ArrayList<>();
        for(int i = 0; i<ctcinfo.size(); i++){
            JSONObject jsstr= ctcinfo.getJSONObject(i);
            TaxRow hos = new TaxRow();

            hos.setAland(jsstr.getString("Tax_Category_Master_Data__c.Country_Code__c"));
            hos.setTatyp(jsstr.getString("Tax_Category_Master_Data__c.name"));
            if (jsstr.getJSONArray("Tax_Category__c") !=null){
                hos.setTaxkd(ObjectOptionValueRetrieval.OptionValue("Customer_Tax_Category__c","Tax_Category__c",(String)ctcinfo.getJSONObject(0).getJSONArray("Tax_Category__c").get(0)));
            }else {
                hos.setTaxkd("");
            }

            taxList.add(hos);
        }
        si.setTAXKDS(taxList);
        List<salesInfo> salesInfos = new ArrayList<>();
        salesInfos.add(si);
        logger.info("salesInfos: " + JSON.toJSONString(salesInfos));
        rcsi.setFlucu01(salesInfos);
        rcsi.setFlucu00(new ArrayList<>());
        List<FinancialRow> ficList = new ArrayList<>();
        FinancialRow fr = new FinancialRow();
//        fr.setAkont((String)csiinfo.getJSONObject(0).getJSONArray("Reconciliation_Account__c").get(0));
        if (csiinfo.getJSONObject(0).getJSONArray("Reconciliation_Account__c")!=null){
            fr.setAkont(ObjectOptionValueRetrieval.OptionValue("Customer_Sales_Info__c","Reconciliation_Account__c",(String)csiinfo.getJSONObject(0).getJSONArray("Reconciliation_Account__c").get(0)));
        }else {
            fr.setAkont("");
        }
        fr.setKunnr(csiinfo.getJSONObject(0).getString("Customer__c.MDG_Customer_Code__c"));
        if (csiinfo.getJSONObject(0).getJSONArray("Sales_Org__c")!=null){
            fr.setBukrs(ObjectOptionValueRetrieval.OptionValue("Customer_Sales_Info__c","Sales_Org__c",(String)csiinfo.getJSONObject(0).getJSONArray("Sales_Org__c").get(0)));
        }else {
            fr.setBukrs("");
        }
        ficList.add(fr);
        rcsi.setFlucu00(ficList);
        rcsi.setTaxnums(new ArrayList<>());
        List<TaxNumberRow> tnrList = new ArrayList<>();
        for(int i = 0; i<ctncinfo.size(); i++){
            JSONObject jsstr= ctncinfo.getJSONObject(i);
            TaxNumberRow hos = new TaxNumberRow();
            hos.setPartner(jsstr.getString("Customer__c.MDG_Customer_Code__c"));
            hos.setTaxnumxl(jsstr.getString("Tax_Number_Master_Data__c.Code__c"));
            hos.setTaxtype(jsstr.getString("Long_Tax_Number__c"));
            tnrList.add(hos);
        }
        rcsi.setTaxnums(tnrList);
        resultinfoList.add(rcsi);
        request.setResultinfo(resultinfoList);

        logger.info("request: " + JSON.toJSONString(request));

        String jsonString = JSON.toJSONString(request);
        commonData.setBody(jsonString);

        CommonResponse<JSONObject> result = new CommonResponse<>();
        try{
            // 同步数据
            result =  commonHttpClient.execute(commonData, new ResponseBodyHandler<JSONObject>(){
                @Override
                public JSONObject handle(String s) throws IOException {
                    JSONObject jsonobj = JSONObject.parseObject(s);
                    return jsonobj;
                }
            });
            logger.info("接口返回result: " + JSON.toJSONString(result));
            returnResult.setIsSuccess(true);
            JSONObject responseJson = result.getData();
            String returnStatus = responseJson.getJSONObject("esbinfo").getString("returnstatus");
            responseBodyStr  = responseJson.toJSONString();
            List<XObject> updateList = new ArrayList<>();
            List<XObject> updateList2 = new ArrayList<>();
            int StatusCode = result.getCode();
            // TODO 成功方式判断待定
            isSuccess = StatusCode == 200;
            if (isSuccess){
                if (responseJson.getJSONObject("esbinfo") !=null){
                    returnResult.setMessage(responseJson.getJSONObject("esbinfo").getString("returnmsg"));
                    Customer_Sales_Info__c upcsi = new Customer_Sales_Info__c();
                    upcsi.setId(longID);
                    if(Objects.equals(returnStatus, "S")){

                        //Synchronized
                        upcsi.setSync_Status__c(3);
                        upcsi.setSync_Failure_Reasons__c("");
                        upcsi.setIsSync__c(true);
                    }
                    if (Objects.equals(returnStatus, "E")){

                        returnResult.setIsSuccess(false);
                        //Synchronization Failure
                        upcsi.setSync_Status__c(2);
                        upcsi.setSync_Failure_Reasons__c(responseJson.getJSONArray("resultinfo").getJSONObject(0).getString("msgtx"));
                        isSuccess = false;
                        errorMessage = responseJson.getJSONObject("esbinfo").getString("returnmsg");
                        returnResult.setMessage(responseJson.getJSONObject("esbinfo").getString("returnmsg"));
                        // Todo 错误邮件发送未写Utility.notificationUser(accList[0].OwnerId,Id);
                    }
                    updateList.add(upcsi);
                    BatchOperateResult batchResult = XObjectService.instance().update(updateList);
                }else {
                    returnResult.setMessage(responseJson.toJSONString());
                    returnResult.setIsSuccess(false);
                    isSuccess = false;
                    errorMessage =responseJson.toJSONString();
                    Customer_Sales_Info__c upcsi = new Customer_Sales_Info__c();
                    upcsi.setId(longID);
                    upcsi.setSync_Status__c(2);
                    upcsi.setSync_Failure_Reasons__c("null");
                    updateList2.add(upcsi);
                    BatchOperateResult batchResult = XObjectService.instance().update(updateList2);
                    // Todo 错误邮件发送未写Utility.notificationUser(accList[0].OwnerId,Id);
                }
            }else {
                List<XObject> updateList4 = new ArrayList<>();
                returnResult.setMessage(responseJson.toJSONString());
                returnResult.setIsSuccess(false);
                errorMessage =responseJson.toJSONString();
                Customer_Sales_Info__c upcsi = new Customer_Sales_Info__c();
                upcsi.setId(longID);
                upcsi.setSync_Status__c(2);
                upcsi.setSync_Failure_Reasons__c(""+StatusCode);
                updateList4.add(upcsi);
                BatchOperateResult batchResult = XObjectService.instance().update(updateList4);
                // Todo 错误邮件发送未写Utility.notificationUser(accList[0].OwnerId,Id);
            }


        }catch (Exception e){
            if (!csiinfo.isEmpty()){
                // Todo 错误邮件发送未写Utility.notificationUser(accList[0].OwnerId,Id);
            }
            List<XObject> updateList3 = new ArrayList<>();
            Customer_Sales_Info__c upcsi = new Customer_Sales_Info__c();
            upcsi.setId(longID);
            upcsi.setSync_Status__c(2);
            upcsi.setSync_Failure_Reasons__c(e.getMessage());
            updateList3.add(upcsi);
            BatchOperateResult batchResult = XObjectService.instance().update(updateList3);
            isSuccess = false;
            errorMessage = e.getMessage();
            returnResult.setMessage(e.getMessage());
            returnResult.setIsSuccess(false);
        }
        List<String> loglist = new ArrayList<>();
        loglist.add(Id);
        // Todo 接口日志记录
        ObjectOptionValueRetrieval.insertInterfaceLog("CustomerSalesInfoSyncToSAP", "CRM",
                "SAP", END_POINT, JSON.toJSONString(request), isSuccess, false,
                responseBodyStr, errorMessage, "POST", loglist,
                "客户销售视图同步SAP", "CRM同步客户销售视图至SAP");


        return returnResult;
    }
}
