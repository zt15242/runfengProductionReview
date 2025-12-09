package other.rainbow.autoaccount.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rkhd.platform.sdk.data.model.Account;
import com.rkhd.platform.sdk.data.model.Customer_Credit_Info__c;
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
import other.rainbow.autoaccount.common.NeoCrmRkhdService;
import other.rainbow.autoaccount.common.NeoCrmRkhdService;
import other.rainbow.autoaccount.pojo.CustomerSales.*;
import other.rainbow.autoaccount.pojo.RequestBody;
import other.rainbow.autoaccount.pojo.ReturnResult;
import other.rainbow.autoaccount.pojo.*;


import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class GMDG011Controller {
    private static final Logger logger = LoggerFactory.getLogger();
    public static final String END_POINT = "CustomerInfo";
    public static final String END_POINT003 = "creditLimit";


    public static ReturnResult sendGMDG011(String Id, String changeType) throws XsyHttpException, IOException, ScriptBusinessException, InterruptedException, ApiEntityServiceException, CustomConfigException {
        ReturnResult returnResult = new ReturnResult();
        long longID = Long.parseLong(Id);
        // 获取对象实例
        CustomConfigService customConfigService = CustomConfigService.instance();
        Map<String, String> configProperties = null;
        String url="";
        String hed = "";
        configProperties = customConfigService.getConfigSet("sap_properties");
        if(configProperties != null) {
            url = configProperties.get("G011");
            hed = configProperties.get("Authorization");
        }else{
            returnResult.setMessage("接口地址为空");
            returnResult.setIsSuccess(false);
        }
//        String credentials = "crm01" + ":" + "Abcd1234";
        String responseBodyStr = "";
        // 使用 Base64 编码
//        String encoded = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        String encoded = "eHN5LWNybTAxOkFiY2QxMjM0";
        // 创建 "Basic " 开头的字符串
        CommonHttpClient commonHttpClient= CommonHttpClient.instance(15000,15000);
        CommonData commonData=new CommonData();
        commonData.addHeader("Content-Type","application/json");
        commonData.setCallString(url);
        commonData.setCall_type("POST");
        commonData.addHeader("Authorization",hed);
        logger.info("Authorization="+hed);

        RkhdHttpClient rkhdclient = RkhdHttpClient.instance();
        RequestBody requestBody = new RequestBody();
        reqresultinfo reqresultinfo = new reqresultinfo();
        List<items> itemsList = new ArrayList<>();
        reqesbinfo reqesbinfo = new reqesbinfo();
        List<reqresultinfo> reqresultinfoList = new ArrayList<>();
        String errorMessage = "";
        Boolean isSuccess = true;
        String sqlaccount = "SELECT id,SAPId__c,Tax_Identification__c,MDG_Customer_Code__c,ownerId.name,Language__c,Customer_Group__c,Customer_Short_Name__c,accountName,Group_Name__c "+
                ",Tax_Identification__c,Tax_Number__c,Tax_Number__c,Continent__c,Region__c,Country__c.Code__c,"+
                " Province_State__c.Code__c,City__c,Street__c,Postal_Code__c,SAP_Customer_Type__c,Contact_Name__c,"+
                " Telephone__c,Email__c,fax  FROM account WHERE id= "+Id;
        JSONArray accountinfo = NeoCrmRkhdService.xoql(rkhdclient,sqlaccount);
        String sqlbankinfo = "SELECT Bank_Name__c,C_R__c,Bank_Key__c,Bank_Acct__c,Account_Holder__c," +
                " Control_Key__c,External_Id__c FROM Bank_Info__c WHERE Customer__c= "+Id;
        JSONArray bankinfo = NeoCrmRkhdService.xoql(rkhdclient,sqlbankinfo);
        logger.info("accountinfo: " +accountinfo);
        logger.info("bankinfo: " +bankinfo);

        for(Integer i = 0 ;i<bankinfo.size(); i++){
            JSONObject jsstr= bankinfo.getJSONObject(i);
            items hos = new items();
            //银行国家
//            hos.setBanks((String) jsstr.getJSONArray("C_R__c").get(0));
            if (jsstr.getJSONArray("C_R__c")!=null){
                hos.setBanks(NeoCrmRkhdService.OptionValue("Bank_Info__c","C_R__c",(String)jsstr.getJSONArray("C_R__c").get(0)));
            }else {
                hos.setBanks("");
            }
            //银行代码
            hos.setBankl(jsstr.getString("Bank_Key__c"));
            //银行名称
            hos.setBanka(jsstr.getString("Bank_Name__c"));
            //银行账号
            hos.setBankn(jsstr.getString("Bank_Acct__c"));
            //账户持有人
            hos.setKoinh(jsstr.getString("Account_Holder__c"));
            //付款类型
//            hos.setAccname((String) jsstr.getJSONArray("Control_Key__c").get(0));
            if (jsstr.getJSONArray("Control_Key__c")!=null){
                hos.setAccname(NeoCrmRkhdService.OptionValue("Bank_Info__c","Control_Key__c",(String)jsstr.getJSONArray("Control_Key__c").get(0)));
            }else {
                hos.setAccname("");
            }
            //外部标识
            hos.setBkext(jsstr.getString("External_Id__c"));
            itemsList.add(hos);
        }
        if (Objects.equals(changeType, "U")){
            //维护标识
            reqresultinfo.setMaintype("BP2P1");
            reqresultinfo.setPartner(accountinfo.getJSONObject(0).getString("MDG_Customer_Code__c"));
        }
        if (Objects.equals(changeType, "I")){
            reqresultinfo.setMaintype("BP1P1");
        }

        //流程说明
        reqresultinfo.setUsmdcreqtext(accountinfo.getJSONObject(0).getString("SAPId__c")+accountinfo.getJSONObject(0).getString("ownerId.name"));
        //业务伙伴语言
        if (accountinfo.getJSONObject(0).getJSONArray("Language__c")!=null){
            reqresultinfo.setLangu(NeoCrmRkhdService.OptionValue("account","Language__c",(String)accountinfo.getJSONObject(0).getJSONArray("Language__c").get(0)));
        }else {
            reqresultinfo.setLangu("");
        }
//        reqresultinfo.setLangu((String)accountinfo.getJSONObject(0).getJSONArray("Language__c").get(0));
        //业务伙伴类别
        reqresultinfo.setButype("2");
        Map<String, String> stringIntegerMap = NeoCrmRkhdService.objectInformation("account","Customer_Group__c");
        for (Map.Entry<String, String> entry : stringIntegerMap.entrySet()) {

            if(accountinfo.getJSONObject(0).getJSONArray("Customer_Group__c") == null){
                reqresultinfo.setBugroup("");
            }
            if (Objects.equals(entry.getKey(), accountinfo.getJSONObject(0).getJSONArray("Customer_Group__c").get(0))){
                //业务伙伴分组
                reqresultinfo.setBugroup(entry.getValue());
            }
        }
        //业务伙伴名称
        reqresultinfo.setNameorg1(accountinfo.getJSONObject(0).getString("accountName"));
        //业务伙伴简称
        reqresultinfo.setBusort1(accountinfo.getJSONObject(0).getString("Customer_Short_Name__c"));
        //业务伙伴所属集团
        reqresultinfo.setNameorg3(accountinfo.getJSONObject(0).getString("Group_Name__c"));
        //唯一性属性类别
//        reqresultinfo.setType((String)accountinfo.getJSONObject(0).getJSONArray("Tax_Identification__c").get(0));
        if (accountinfo.getJSONObject(0).getJSONArray("Tax_Identification__c")!=null){
            reqresultinfo.setType(NeoCrmRkhdService.OptionValue("account","Tax_Identification__c",(String)accountinfo.getJSONObject(0).getJSONArray("Tax_Identification__c").get(0)));
        }else {
            reqresultinfo.setType("");
        }
        //唯一性识别属性
        reqresultinfo.setIdnumber(accountinfo.getJSONObject(0).getString("SAPId__c"));
        Map<String, String> stringIntegerMap2 = NeoCrmRkhdService.objectInformation("account","Continent__c");
        if (accountinfo.getJSONObject(0).getJSONArray("Continent__c") !=null){
            for (Map.Entry<String, String> entry : stringIntegerMap2.entrySet()) {
                if(accountinfo.getJSONObject(0).getJSONArray("Continent__c") == null){

                    reqresultinfo.setZzcontt("");
                }
                if (Objects.equals(entry.getKey(), accountinfo.getJSONObject(0).getJSONArray("Continent__c").get(0))){
                    //业务伙伴所属大洲
                    reqresultinfo.setZzcontt(entry.getValue());
                }
            }
        }

        logger.info("step2" );
        Map<String, String> stringIntegerMap3 = NeoCrmRkhdService.objectInformation("account","Region__c");
        if (accountinfo.getJSONObject(0).getJSONArray("Region__c") !=null){
            for (Map.Entry<String, String> entry : stringIntegerMap3.entrySet()) {
                if(accountinfo.getJSONObject(0).getJSONArray("Region__c")== null){
                    reqresultinfo.setZzregin("");
                }
                if (Objects.equals(entry.getKey(), accountinfo.getJSONObject(0).getJSONArray("Region__c").get(0))){
                    //业务伙伴所属大区
                    reqresultinfo.setZzregin(entry.getValue());
                }
            }
        }

        logger.info("step3" );
        //业务伙伴注册国家
        reqresultinfo.setCountry(accountinfo.getJSONObject(0).getString("Country__c.Code__c"));
        //业务伙伴注册地区
        reqresultinfo.setRegion(accountinfo.getJSONObject(0).getString("Province_State__c.Code__c"));
        //业务伙伴注册城市
        reqresultinfo.setCity1(accountinfo.getJSONObject(0).getString("City__c"));
        //业务伙伴注册详细地址
        reqresultinfo.setStreet(accountinfo.getJSONObject(0).getString("Street__c"));
        //业务伙伴邮编
        reqresultinfo.setPostcode1(accountinfo.getJSONObject(0).getString("Postal_Code__c"));
        //业务伙伴角色
        reqresultinfo.setRltyp("CU");
        logger.info("step4" );
        Map<String, String> stringIntegerMap4 = NeoCrmRkhdService.objectInformation("account","SAP_Customer_Type__c");
        if (accountinfo.getJSONObject(0).getJSONArray("SAP_Customer_Type__c") !=null){
            for (Map.Entry<String, String> entry : stringIntegerMap4.entrySet()) {
                if(accountinfo.getJSONObject(0).getJSONArray("SAP_Customer_Type__c") == null){
                    reqresultinfo.setZzcudis("");
                }
                if (Objects.equals(entry.getKey(), accountinfo.getJSONObject(0).getJSONArray("SAP_Customer_Type__c").get(0))){
                    //客户类型
                    reqresultinfo.setZzcudis(entry.getValue());
                }

            }
        }

        //客户联系人
        reqresultinfo.setNameco(accountinfo.getJSONObject(0).getString("Contact_Name__c"));
        //客户联系人电话
        reqresultinfo.setTelnumber1(accountinfo.getJSONObject(0).getString("Telephone__c"));
        reqresultinfo.setTelnumber2(accountinfo.getJSONObject(0).getString("Telephone__c"));
        //客户联系人邮箱
        reqresultinfo.setSmtpaddr(accountinfo.getJSONObject(0).getString("Email__c"));
        //客户联系人传真
        reqresultinfo.setFaxnumber(accountinfo.getJSONObject(0).getString("fax"));

        //接口编号
        reqesbinfo.setInstid("GMDG011");
        reqesbinfo.setAttr2("SAP");
        reqesbinfo.setAttr1("NEOCRM");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime toDay = LocalDateTime.now();
        //请求时间
        reqesbinfo.setRequesttime(toDay.format(formatter));
        logger.info("step5" );
        reqresultinfo.setBanks(itemsList);
        reqresultinfoList.add(reqresultinfo);
        requestBody.setResultinfo(reqresultinfoList);
        requestBody.setEsbinfo(reqesbinfo);
        logger.info("requestBody: " + JSON.toJSONString(requestBody));



        String jsonString = JSON.toJSONString(requestBody);
        commonData.setBody(jsonString);
        logger.info("接口发送requestBody: " + jsonString);

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
            responseBodyStr  = responseJson.toJSONString();
            String returnStatus = responseJson.getJSONObject("esbinfo").getString("returnstatus");
            String returnmsg = responseJson.getJSONObject("esbinfo").getString("returnmsg");
            logger.info("returnStatus="+responseJson.getJSONObject("esbinfo").getString("returnstatus"));
            String partner = responseJson.getJSONArray("resultinfo").getJSONObject(0).getString("partner");
            respesbinfo respesbinfo = new respesbinfo();
            Account acc = new Account();
            List<XObject> updateList = new ArrayList<>();
            List<XObject> updateList2 = new ArrayList<>();
            int StatusCode = result.getCode();
            // TODO 成功方式判断待定
            isSuccess = StatusCode == 200;
            if (isSuccess){
                if (responseJson.getJSONObject("esbinfo") !=null){
                    returnResult.setMessage(responseJson.getJSONObject("esbinfo").getString("returnmsg"));
                    logger.info("returnmsg="+responseJson.getJSONObject("esbinfo").getString("returnmsg"));
                    acc.setId(longID);
                    if(Objects.equals(returnStatus, "S")){
                        acc.setMDG_Customer_Code__c(partner);
                        //Synchronized
                        acc.setSync_Status__c(3);
                        acc.setSync_Failure_Reasons__c("");
                        acc.setSAP_Customer_Name__c(accountinfo.getJSONObject(0).getString("accountName"));
//                        respesbinfo.setRequesttime(responseJson.getJSONObject("esbinfo").getString("requesttime"));
//                        respesbinfo.setResponsetime(responseJson.getJSONObject("esbinfo").getString("responsetime"));
//                        respesbinfo.setReturnstatus(returnStatus);
//                        respesbinfo.setReturncode(responseJson.getJSONObject("esbinfo").getString("returncode"));
//                        respesbinfo.setReturnmsg(responseJson.getJSONObject("esbinfo").getString("returnmsg"));
//                        respesbinfo.setAttr1(responseJson.getJSONObject("esbinfo").getString("attr1"));
                    }
                    if (Objects.equals(returnStatus, "E")){
                        if (Objects.equals(changeType, "I") && !Objects.equals(partner, "") && partner!=null){
                            acc.setMDG_Customer_Code__c(partner);
                            //Synchronized
                            acc.setSync_Status__c(3);
                            acc.setSync_Failure_Reasons__c("");
                            acc.setMDG__c(true);
                            acc.setSAP_Customer_Name__c(responseJson.getJSONArray("resultinfo").getJSONObject(0).getString("nameorg1"));
                        }else {
                            returnResult.setIsSuccess(false);
                            //Synchronization Failure
                            acc.setSync_Status__c(2);
                            acc.setSync_Failure_Reasons__c(responseJson.getJSONObject("esbinfo").getString("returnmsg"));
                            isSuccess = false;
                            errorMessage = responseJson.getJSONObject("esbinfo").getString("returnmsg");
                            returnResult.setMessage(responseJson.getJSONObject("esbinfo").getString("returnmsg"));
                            // Todo 错误邮件发送未写Utility.notificationUser(accList[0].OwnerId,Id);
                            acc.setSAP_Customer_Name__c(accountinfo.getJSONObject(0).getString("accountName"));
                        }
                    }
                    updateList.add(acc);
                    BatchOperateResult batchResult = XObjectService.instance().update(updateList);

                }else {
                    returnResult.setMessage(responseJson.toJSONString());
                    returnResult.setIsSuccess(false);
                    isSuccess = false;
                    errorMessage =responseJson.toJSONString();
                    // Todo 错误邮件发送未写Utility.notificationUser(accList[0].OwnerId,Id);
                }
            }else {

                returnResult.setMessage(returnmsg);
                returnResult.setIsSuccess(false);
                isSuccess = false;
                errorMessage =responseJson.toJSONString();
                Account acc1 = new Account();
                acc1.setId(longID);
                acc1.setSync_Status__c(2);
                acc1.setSync_Failure_Reasons__c(""+StatusCode);
                acc1.setSAP_Customer_Name__c(accountinfo.getJSONObject(0).getString("accountName"));
                updateList2.add(acc1);
                BatchOperateResult batchResult = XObjectService.instance().update(updateList2);
                // Todo 错误邮件发送未写Utility.notificationUser(accList[0].OwnerId,Id);
            }


        }catch (Exception e){
            if (accountinfo.size()>0){
                // Todo 错误邮件发送未写Utility.notificationUser(accList[0].OwnerId,Id);
            }
            List<XObject> updateList3 = new ArrayList<>();
            Account acc1 = new Account();
            acc1.setId(longID);
            acc1.setSync_Status__c(2);
            acc1.setSync_Failure_Reasons__c(e.getMessage());
            acc1.setSAP_Customer_Name__c(accountinfo.getJSONObject(0).getString("accountName"));
            updateList3.add(acc1);
            BatchOperateResult batchResult = XObjectService.instance().update(updateList3);
            isSuccess = false;
            errorMessage = e.getMessage();
            returnResult.setMessage(e.getMessage());
            returnResult.setIsSuccess(false);
        }
        // Todo 接口日志记录
        List<String> loglist = new ArrayList<>();
        loglist.add(Id);
        NeoCrmRkhdService.insertInterfaceLog("CustomerInfoSyncToSAP", "CRM",
                "SAP", END_POINT, JSON.toJSONString(requestBody), isSuccess, false,
                responseBodyStr, errorMessage, "POST",loglist,
                "客户基本信息同步SAP", "CRM同步客户基本信息至SAP");

        logger.info("最终返回returnResult="+JSONObject.toJSONString(returnResult));
        return returnResult;
    }

    public static ReturnResult sendCustomerCreditInfo(String Id) throws XsyHttpException, IOException, ScriptBusinessException, InterruptedException, ApiEntityServiceException {
        long longID = Long.parseLong(Id);
        String url="http://43.157.186.66:8901/sap/sales/cuscrelimit/update";
        String credentials = "crm01" + ":" + "Abcd1234";
        String responseBodyStr = "";
        // 使用 Base64 编码
        String encoded = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        // 创建 "Basic " 开头的字符串
        String hed = "Basic " + encoded;
        CommonHttpClient commonHttpClient= CommonHttpClient.instance(15000,15000);
        CommonData commonData=new CommonData();
        commonData.addHeader("Content-Type","application/json");
        commonData.setCallString(url);
        commonData.setCall_type("POST");
        commonData.addHeader("Authorization",hed);



        RkhdHttpClient rkhdclient = RkhdHttpClient.instance();
        RequestBody requestBody = new RequestBody();
        ReqCustomerlimit reqresultinfo = new ReqCustomerlimit();
        reqesbinfo reqesbinfo = new reqesbinfo();
        List<ReqCustomerlimit> reqresultinfoList = new ArrayList<>();
        String errorMessage = "";
        boolean isSuccess = true;
        String sqlCustomerCreditInfo = "SELECT id,name,Rules__c,Risk_Class__c,Check_Rule__c,Credit_Segment__c,Valid_Until__c,Customer__c,Sinosure_Credit_Limit__c,Local_Credit_Limit__c,"+
                " Actual_Credit_limit__c,Sync_Status__c,Remaining_Credit__c,Customer__c.MDG_Customer_Code__c,Customer__c.ownerId  FROM Customer_Credit_Info__c  WHERE id= "+Id;
        JSONArray accountinfo = NeoCrmRkhdService.xoql(rkhdclient,sqlCustomerCreditInfo);

        logger.info("accountinfo: " +accountinfo);




        //客户编码
        reqresultinfo.setPartner(accountinfo.getJSONObject(0).getString("Customer__r.MDG_Customer_Code__c"));
        //客户额度编码
        reqresultinfo.setLimitrule((String)accountinfo.getJSONObject(0).getJSONArray("Rules__c").get(0));
        if (accountinfo.getJSONObject(0).getJSONArray("Risk_Class__c")!=null){
            //风险类
            reqresultinfo.setRiskclass(NeoCrmRkhdService.OptionValue("Customer_Credit_Info__c","Risk_Class__c",(String)accountinfo.getJSONObject(0).getJSONArray("Risk_Class__c").get(0)));
        }else {
            reqresultinfo.setRiskclass("");
        }
        //规则
        reqresultinfo.setCheckrule((String)accountinfo.getJSONObject(0).getJSONArray("Check_Rule__c").get(0));
        //信用段
        if (accountinfo.getJSONObject(0).getJSONArray("Credit_Segment__c")!=null){
            reqresultinfo.setCreditsgmnt(NeoCrmRkhdService.OptionValue("Customer_Credit_Info__c","Credit_Segment__c",(String)accountinfo.getJSONObject(0).getJSONArray("Credit_Segment__c").get(0)));
        }else {
            reqresultinfo.setCreditsgmnt("");
        }
        //额度
        reqresultinfo.setCreditlimit(accountinfo.getJSONObject(0).getString("Actual_Credit_limit__c"));
        long millis = Long.parseLong(accountinfo.getJSONObject(0).getString("Valid_Until__c"));
        Date date = new Date(millis);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        // 格式化日期
        String formattedDate = sdf.format(date);
        //有效期
        reqresultinfo.setValid_date(formattedDate);




        //接口编号
        reqesbinfo.instid = "GSD003";

        reqresultinfoList.add(reqresultinfo);
        requestBody.setResCustomerlimitinfo(reqresultinfoList);
        requestBody.setEsbinfo(reqesbinfo);
        logger.info("requestBody: " + JSON.toJSONString(requestBody));

        String jsonString = JSON.toJSONString(requestBody);
        commonData.setBody(jsonString);

        CommonResponse<JSONObject> result = new CommonResponse<>();
        ReturnResult returnResult = new ReturnResult();
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
            Customer_Credit_Info__c acc = new Customer_Credit_Info__c();
            List<XObject> updateList = new ArrayList<>();
            int StatusCode = result.getCode();
            // TODO 成功方式判断待定
            isSuccess = StatusCode == 200;
            if (isSuccess){
                if (responseJson.getJSONObject("esbinfo") !=null){
                    returnResult.setMessage(responseJson.getJSONObject("esbinfo").getString("returnmsg"));
                    acc.setId(longID);
                    if(Objects.equals(returnStatus, "S")){
                        //Synchronized
                        acc.setSync_Status__c(3);
                        acc.setSync_Failure_Reasons__c("");
                    }
                    if (Objects.equals(returnStatus, "E")){
                        returnResult.setIsSuccess(false);
                        //Synchronization Failure
                        acc.setSync_Status__c(2);
                        acc.setSync_Failure_Reasons__c(responseJson.getJSONObject("esbinfo").getString("returnmsg"));
                        isSuccess = false;
                        errorMessage = responseJson.getJSONObject("esbinfo").getString("returnmsg");
                        returnResult.setMessage(responseJson.getJSONObject("esbinfo").getString("returnmsg"));
                        // Todo 错误邮件发送未写Utility.notificationUser(accList[0].OwnerId,Id);
                    }
                    updateList.add(acc);
                    BatchOperateResult batchResult = XObjectService.instance().update(updateList);
                }else {
                    returnResult.setMessage(responseJson.toJSONString());
                    returnResult.setIsSuccess(false);
                    isSuccess = false;
                    errorMessage =responseJson.toJSONString();
                    // Todo 错误邮件发送未写Utility.notificationUser(accList[0].OwnerId,Id);
                }
            }else {
                returnResult.setMessage(responseJson.toJSONString());
                returnResult.setIsSuccess(false);
                errorMessage =responseJson.toJSONString();
                // Todo 错误邮件发送未写Utility.notificationUser(accList[0].OwnerId,Id);
            }


        }catch (Exception e){
            if (!accountinfo.isEmpty()){
                // Todo 错误邮件发送未写Utility.notificationUser(accList[0].OwnerId,Id);
            }
            isSuccess = false;
            errorMessage = e.getMessage();
            returnResult.setMessage(e.getMessage());
            returnResult.setIsSuccess(false);
        }
        // Todo 接口日志记录
        List<String> loglist = new ArrayList<>();
        loglist.add(Id);
        NeoCrmRkhdService.insertInterfaceLog("CustomerlimitSyncToSAP", "CRM",

                "SAP", END_POINT003, JSON.toJSONString(requestBody), isSuccess, false,

                responseBodyStr, errorMessage, "POST", loglist,

                "客户额度信息同步SAP", "CRM同步信用额度至SAP");


        return returnResult;
    }
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
        other.rainbow.autoaccount.pojo.CustomerSales.RequestBody request = new other.rainbow.autoaccount.pojo.CustomerSales.RequestBody();
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
            rcsi.setKtokk(NeoCrmRkhdService.OptionValue("account","Customer_Group__c",(String)csiinfo.getJSONObject(0).getJSONArray("Customer__c.Customer_Group__c").get(0)));
        }else {
            rcsi.setKtokk("");
        }
        salesInfo si = new salesInfo();
        si.setKunnr(csiinfo.getJSONObject(0).getString("Customer__c.MDG_Customer_Code__c"));
        if (csiinfo.getJSONObject(0).getJSONArray("Sales_Org__c")!=null){
            si.setVkorg(NeoCrmRkhdService.OptionValue("Customer_Sales_Info__c","Sales_Org__c",(String)csiinfo.getJSONObject(0).getJSONArray("Sales_Org__c").get(0)));
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
            si.setVkbur(NeoCrmRkhdService.OptionValue("Customer_Sales_Info__c","Sales_Office__c",(String)csiinfo.getJSONObject(0).getJSONArray("Sales_Office__c").get(0)));
        }else {
            si.setVkbur("");
        }
        if (csiinfo.getJSONObject(0).getJSONArray("Sales_Group__c")!=null){
            si.setVkgrp(NeoCrmRkhdService.OptionValue("Customer_Sales_Info__c","Sales_Group__c",(String)csiinfo.getJSONObject(0).getJSONArray("Sales_Group__c").get(0)));
        }else {
            si.setVkgrp("");
        }
        if (csiinfo.getJSONObject(0).getJSONArray("Currency__c")!=null){
            si.setWaers(NeoCrmRkhdService.OptionValue("Customer_Sales_Info__c","Currency__c",(String)csiinfo.getJSONObject(0).getJSONArray("Currency__c").get(0)));
        }else {
            si.setWaers("");
        }
        if (csiinfo.getJSONObject(0).getJSONArray("Price_Group__c") !=null){
            si.setKonda((String)csiinfo.getJSONObject(0).getJSONArray("Price_Group__c").get(0));
        }

        if (csiinfo.getJSONObject(0).getJSONArray("Cust_Pric_Procedure__c")!=null){
            si.setKalks(NeoCrmRkhdService.OptionValue("Customer_Sales_Info__c","Cust_Pric_Procedure__c",(String)csiinfo.getJSONObject(0).getJSONArray("Cust_Pric_Procedure__c").get(0)));
        }else {
            si.setKalks("");
        }
        if (csiinfo.getJSONObject(0).getJSONArray("Delivery_Plant__c")!=null){
            si.setVwerk(NeoCrmRkhdService.OptionValue("Customer_Sales_Info__c","Delivery_Plant__c",(String)csiinfo.getJSONObject(0).getJSONArray("Delivery_Plant__c").get(0)));
        }else {
            si.setVwerk("");
        }
        if (csiinfo.getJSONObject(0).getJSONArray("Shipping_Conditions__c") !=null){
            si.setVsbed((String)csiinfo.getJSONObject(0).getJSONArray("Shipping_Conditions__c").get(0));
        }

        if (csiinfo.getJSONObject(0).getJSONArray("Terms_Of_Payment__c")!=null){
            // 20250605 ljh 尼日利亚 Credit 换成S511  start
            // si.setZterm(NeoCrmRkhdService.OptionValue("Customer_Sales_Info__c","Terms_Of_Payment__c",(String)csiinfo.getJSONObject(0).getJSONArray("Terms_Of_Payment__c").get(0)));
            logger.info("zheli00"+ (String)csiinfo.getJSONObject(0).getJSONArray("Terms_Of_Payment__c").get(0));
            if("Credit".equals((String)csiinfo.getJSONObject(0).getJSONArray("Terms_Of_Payment__c").get(0))){
                logger.info("zheli01");
                si.setZterm("S511");
            }else{
                logger.info("zheli02");
                si.setZterm(NeoCrmRkhdService.OptionValue("Customer_Sales_Info__c","Terms_Of_Payment__c",(String)csiinfo.getJSONObject(0).getJSONArray("Terms_Of_Payment__c").get(0)));
            }
        }else {
            si.setZterm("");
        }
        if (csiinfo.getJSONObject(0).getJSONArray("Acct_Assmt_Grp_Cust__c")!=null){
            si.setKtgrd(NeoCrmRkhdService.OptionValue("Customer_Sales_Info__c","Acct_Assmt_Grp_Cust__c",(String)csiinfo.getJSONObject(0).getJSONArray("Acct_Assmt_Grp_Cust__c").get(0)));
        }else {
            si.setKtgrd("");
        }
        if (csiinfo.getJSONObject(0).getJSONArray("Incoterms__c")!=null){
//            si.setInco1((String)csiinfo.getJSONObject(0).getJSONArray("Incoterms__c").get(0));
            si.setInco1(NeoCrmRkhdService.OptionValue("Customer_Sales_Info__c","Incoterms__c",(String)csiinfo.getJSONObject(0).getJSONArray("Incoterms__c").get(0)));
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
                hos.setTaxkd(NeoCrmRkhdService.OptionValue("Customer_Tax_Category__c","Tax_Category__c",(String)ctcinfo.getJSONObject(0).getJSONArray("Tax_Category__c").get(0)));
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
            fr.setAkont(NeoCrmRkhdService.OptionValue("Customer_Sales_Info__c","Reconciliation_Account__c",(String)csiinfo.getJSONObject(0).getJSONArray("Reconciliation_Account__c").get(0)));
        }else {
            fr.setAkont("");
        }
        fr.setKunnr(csiinfo.getJSONObject(0).getString("Customer__c.MDG_Customer_Code__c"));
        if (csiinfo.getJSONObject(0).getJSONArray("Sales_Org__c")!=null){
            fr.setBukrs(NeoCrmRkhdService.OptionValue("Customer_Sales_Info__c","Sales_Org__c",(String)csiinfo.getJSONObject(0).getJSONArray("Sales_Org__c").get(0)));
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
        NeoCrmRkhdService.insertInterfaceLog("CustomerSalesInfoSyncToSAP", "CRM",
                "SAP", END_POINT, JSON.toJSONString(request), isSuccess, false,
                responseBodyStr, errorMessage, "POST", loglist,
                "客户销售视图同步SAP", "CRM同步客户销售视图至SAP");


        return returnResult;
    }
}




