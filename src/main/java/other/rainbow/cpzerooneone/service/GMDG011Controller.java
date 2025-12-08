package other.rainbow.cpzerooneone.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rkhd.platform.sdk.data.model.Account;
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
import other.rainbow.cpzerooneone.pojo.ReturnResult;
import other.rainbow.cpzerooneone.pojo.*;


import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class GMDG011Controller {
    private static final Logger logger = LoggerFactory.getLogger();
    public static final String END_POINT = "CustomerInfo";


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
                hos.setBanks(ObjectOptionValueRetrieval.OptionValue("Bank_Info__c","C_R__c",(String)jsstr.getJSONArray("C_R__c").get(0)));
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
                hos.setAccname(ObjectOptionValueRetrieval.OptionValue("Bank_Info__c","Control_Key__c",(String)jsstr.getJSONArray("Control_Key__c").get(0)));
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
            reqresultinfo.setLangu(ObjectOptionValueRetrieval.OptionValue("account","Language__c",(String)accountinfo.getJSONObject(0).getJSONArray("Language__c").get(0)));
        }else {
            reqresultinfo.setLangu("");
        }
//        reqresultinfo.setLangu((String)accountinfo.getJSONObject(0).getJSONArray("Language__c").get(0));
        //业务伙伴类别
        reqresultinfo.setButype("2");
        Map<String, String> stringIntegerMap = ObjectOptionValueRetrieval.objectInformation("account","Customer_Group__c");
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
            reqresultinfo.setType(ObjectOptionValueRetrieval.OptionValue("account","Tax_Identification__c",(String)accountinfo.getJSONObject(0).getJSONArray("Tax_Identification__c").get(0)));
        }else {
            reqresultinfo.setType("");
        }
        //唯一性识别属性
        reqresultinfo.setIdnumber(accountinfo.getJSONObject(0).getString("SAPId__c"));
        Map<String, String> stringIntegerMap2 = ObjectOptionValueRetrieval.objectInformation("account","Continent__c");
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
        Map<String, String> stringIntegerMap3 = ObjectOptionValueRetrieval.objectInformation("account","Region__c");
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
        // 20250408   zyh   孟加拉默认13  start
        if ("BD".equals(accountinfo.getJSONObject(0).getString("Country__c.Code__c"))){
            reqresultinfo.setRegion("13");
        } else {
            reqresultinfo.setRegion(accountinfo.getJSONObject(0).getString("Province_State__c.Code__c"));
        }
        // 20250408   zyh   孟加拉默认13  end
        //业务伙伴注册城市
        reqresultinfo.setCity1(accountinfo.getJSONObject(0).getString("City__c"));
        //业务伙伴注册详细地址
        reqresultinfo.setStreet(accountinfo.getJSONObject(0).getString("Street__c"));
        //业务伙伴邮编
        reqresultinfo.setPostcode1(accountinfo.getJSONObject(0).getString("Postal_Code__c"));
        //业务伙伴角色
        reqresultinfo.setRltyp("CU");
        logger.info("step4" );
        Map<String, String> stringIntegerMap4 = ObjectOptionValueRetrieval.objectInformation("account","SAP_Customer_Type__c");
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
                returnResult.setMessage(responseJson.toJSONString());
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
        ObjectOptionValueRetrieval.insertInterfaceLog("CustomerInfoSyncToSAP", "CRM",
                "SAP", END_POINT, JSON.toJSONString(requestBody), isSuccess, false,
                responseBodyStr, errorMessage, "POST",loglist,
                "客户基本信息同步SAP", "CRM同步客户基本信息至SAP");

        logger.info("最终返回returnResult="+returnResult);
        return returnResult;
    }
}




