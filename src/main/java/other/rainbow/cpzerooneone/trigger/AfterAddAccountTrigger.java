package other.rainbow.cpzerooneone.trigger;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rkhd.platform.sdk.data.model.Account;
import com.rkhd.platform.sdk.data.model.Customer_Payment_Item__c;
import com.rkhd.platform.sdk.data.model.Customer_Tax_Number_Category__c;
import com.rkhd.platform.sdk.exception.ApiEntityServiceException;
import com.rkhd.platform.sdk.exception.ScriptBusinessException;
import com.rkhd.platform.sdk.exception.XsyHttpException;
import com.rkhd.platform.sdk.http.RkhdHttpClient;
import com.rkhd.platform.sdk.log.Logger;
import com.rkhd.platform.sdk.log.LoggerFactory;
import com.rkhd.platform.sdk.model.BatchOperateResult;
import com.rkhd.platform.sdk.model.QueryResult;
import com.rkhd.platform.sdk.model.XObject;
import com.rkhd.platform.sdk.service.XObjectService;
import com.rkhd.platform.sdk.service.XoqlService;
import com.rkhd.platform.sdk.trigger.DataResult;
import com.rkhd.platform.sdk.trigger.Trigger;
import com.rkhd.platform.sdk.trigger.TriggerRequest;
import com.rkhd.platform.sdk.trigger.TriggerResponse;
import other.rainbow.cpzerooneone.common.NeoCrmRkhdService;
import other.rainbow.cpzerooneone.common.ObjectOptionValueRetrieval;

import java.io.IOException;
import java.util.*;

public class AfterAddAccountTrigger implements Trigger {
    private static final Logger logger = LoggerFactory.getLogger();
    @Override
    public TriggerResponse execute(TriggerRequest triggerRequest) {
        List<DataResult> result = new ArrayList<>();
        String msg ="";
        Boolean success = true;
        List<XObject> updateList = new ArrayList<>();

        List<XObject> dataList = triggerRequest.getDataList();
        logger.info("getMDG_Customer_Code__c=" + ((Account)dataList.get(0)).getMDG_Customer_Code__c());
        logger.info("getTax_Number__c=" + ((Account)dataList.get(0)).getTax_Number__c());
        logger.info("getRegistration_number__c=" + ((Account)dataList.get(0)).getRegistration_number__c());
        logger.info("getOwnerIDorPassportnumber__c=" + ((Account)dataList.get(0)).getOwnerIDorPassportnumber__c());
        logger.info("getTelephone__c=" + ((Account)dataList.get(0)).getTelephone__c());
        List<XObject> updateList2 = new ArrayList<>();
        if (((Account)dataList.get(0)).getMDG_Customer_Code__c()==null || Objects.equals(((Account) dataList.get(0)).getMDG_Customer_Code__c(), "")){
            String accCode = "";
            Long provinceId = null;
            String accSql = "select id,Country__c.Code__c from account where id = " + dataList.get(0).getId();
            QueryResult accQuery = null;
            try {
                accQuery = XoqlService.instance().query(accSql,true);
            } catch (ApiEntityServiceException e) {
                throw new RuntimeException(e);
            }
            if (accQuery.getSuccess() && accQuery.getRecords().size() > 0){
                JSONArray jsonArray = JSONArray.parseArray(accQuery.getRecords().toString());
                accCode = jsonArray.getJSONObject(0).getString("Country__c.Code__c");
                if ("BD".equals(accCode)){
                    String provinceSql = "select id from Country_Province_Master_Data__c where Code__c = 13";
                    QueryResult provinceQuery = null;
                    try {
                        provinceQuery = XObjectService.instance().query(provinceSql,true);
                    } catch (ApiEntityServiceException e) {
                        throw new RuntimeException(e);
                    }
                    if (provinceQuery.getSuccess() && provinceQuery.getRecords().size() > 0){
                        provinceId = JSONArray.parseArray(provinceQuery.getRecords().toString()).getJSONObject(0).getLong("id");
                    }
                }
            }
            Account acc = new Account();
            acc.setId(dataList.get(0).getId());
            if (((Account)dataList.get(0)).getTax_Number__c()!=null){
                acc.setSAPId__c(((Account)dataList.get(0)).getTax_Number__c());
            }else if(((Account)dataList.get(0)).getRegistration_number__c()!=null){
                acc.setSAPId__c(((Account)dataList.get(0)).getRegistration_number__c());
            }else if(((Account)dataList.get(0)).getOwnerIDorPassportnumber__c()!=null){
                acc.setSAPId__c(((Account)dataList.get(0)).getOwnerIDorPassportnumber__c());
            }else if(((Account)dataList.get(0)).getTelephone__c()!=null){
                acc.setSAPId__c(((Account)dataList.get(0)).getTelephone__c());
            }
            if ("BD".equals(accCode) && !"".equals(provinceId)){
                acc.setProvince_State__c(provinceId);
            }
            updateList2.add(acc);
        }
        if (!updateList2.isEmpty()){
            BatchOperateResult batchResult = null;
            try {
                batchResult = XObjectService.instance().update(updateList2,true,true);
            } catch (ApiEntityServiceException e) {
                throw new RuntimeException(e);
            }
            logger.info("batchResult1=" + batchResult.getSuccess());
            logger.info("batchResult2=" + batchResult.getErrorMessage());
        }





        RkhdHttpClient rkhdclient = null;
        try {
            rkhdclient = RkhdHttpClient.instance();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        String sqlaccount = "SELECT  id,name,Code__c FROM Tax_Number_Master_Data__c  ";
        String sqlcountry = "SELECT id,Code__c FROM Country_Province_Master_Data__c WHERE entityType = 3549026366867472 ";
        // sandbox3549026366867472   dev 3617496314134681
        JSONArray accountinfo;
        JSONArray countryinfo;
        Map<String,String> ctncMap = new HashMap<>();
        Map<String,String> countryMap = new HashMap<>();
        try {
            accountinfo = NeoCrmRkhdService.xoql(rkhdclient, sqlaccount);
            countryinfo = NeoCrmRkhdService.xoql(rkhdclient, sqlcountry);
        } catch (XsyHttpException | InterruptedException | ScriptBusinessException e) {
            throw new RuntimeException(e);
        }
        for (int i = 0; i < accountinfo.size(); i++) {
            JSONObject jsstr = accountinfo.getJSONObject(i);
            ctncMap.put(jsstr.getString("Code__c"),jsstr.getString("id"));
        }
        for (int i = 0; i < countryinfo.size(); i++) {
            JSONObject jsstr = countryinfo.getJSONObject(i);
            countryMap.put(jsstr.getString("id"),jsstr.getString("Code__c"));
        }
        logger.info("accountinfo==="+JSONObject.toJSONString(accountinfo));
        logger.info("countryMap==="+JSONObject.toJSONString(countryMap));
        try {
                     //请求实例
            List<Customer_Tax_Number_Category__c> ctncList = new ArrayList<>();
            for(XObject xObject : dataList){
                logger.info("New=" + xObject);
                Account acc = new Account();
                acc.setId(((Account)xObject).getId());
                acc = XObjectService.instance().get(acc,true);
                Customer_Tax_Number_Category__c ctnc = new Customer_Tax_Number_Category__c();
                ctnc.setCustomer__c(xObject.getId());
                ctnc.setLong_Tax_Number__c(acc.getSAPId__c());

                if(Objects.equals(countryMap.get(((Account) xObject).getCountry__c().toString()), "BD")) {
                    if (ctncMap.get("BD1")!=null){
                        ctnc.setTax_Number_Master_Data__c(Long.parseLong(ctncMap.get("BD1"))) ;
                    }
                }
                if(Objects.equals(countryMap.get(((Account) xObject).getCountry__c().toString()), "GH")) {
                    if (ctncMap.get("GH1")!=null){
                        ctnc.setTax_Number_Master_Data__c(Long.parseLong(ctncMap.get("GH1"))) ;
                    }
                }
                if(Objects.equals(countryMap.get(((Account) xObject).getCountry__c().toString()), "NG")) {
                    if (ctncMap.get("NG3")!=null){
                        ctnc.setTax_Number_Master_Data__c(Long.parseLong(ctncMap.get("NG3"))) ;
                    }
                }

                if(Objects.equals(countryMap.get(((Account) xObject).getCountry__c().toString()), "UG")) {
                    if (ctncMap.get("UG1")!=null){
                        ctnc.setTax_Number_Master_Data__c(Long.parseLong(ctncMap.get("UG1"))) ;
                    }
                }

                ctnc.setEntityType(ObjectOptionValueRetrieval.getEntityTypesId(rkhdclient,"Customer_Tax_Number_Category__c","defaultBusiType"));
                ctncList.add(ctnc);
                BatchOperateResult batchResult = XObjectService.instance().insert(ctncList,true,true);

                logger.info("resubatchResultlt==="+batchResult.getErrorMessage());
                logger.info("resubatchResultlt2==="+batchResult.getSuccess());
                result = new ArrayList<>();
                result.add(new DataResult(true, "成功", xObject));
            }

            msg = "成功";
        } catch (ApiEntityServiceException e) {
            msg="错误："+e.getMessage();
//            throw new RuntimeException(e);
        }
        logger.info("result==="+result);
        return new TriggerResponse(success, msg, result);
    }
}
