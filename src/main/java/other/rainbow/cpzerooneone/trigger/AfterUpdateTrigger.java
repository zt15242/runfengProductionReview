package other.rainbow.cpzerooneone.trigger;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rkhd.platform.sdk.data.model.Account;

import com.rkhd.platform.sdk.exception.*;
import com.rkhd.platform.sdk.http.RkhdHttpClient;
import com.rkhd.platform.sdk.log.Logger;
import com.rkhd.platform.sdk.log.LoggerFactory;
import com.rkhd.platform.sdk.model.BatchOperateResult;
import com.rkhd.platform.sdk.model.XObject;
import com.rkhd.platform.sdk.service.XObjectService;
import com.rkhd.platform.sdk.trigger.*;
import other.rainbow.cpzerooneone.service.GMDG011Controller;


import java.io.IOException;
import java.util.*;

public class AfterUpdateTrigger implements Trigger {
    private static final Logger logger = LoggerFactory.getLogger();
    @Override
    public TriggerResponse execute(TriggerRequest triggerRequest) throws ScriptBusinessException {
        List<DataResult> result = new ArrayList<>();
        String msg ="";
        Boolean success = true;
        //获取beforeUpdate 的list
        TriggerContext triggerContext = triggerRequest.getTriggerContext();
        JSONArray jsonArray = null;
        try {
            Boolean hasChanges = false;
            jsonArray = JSONArray.parseArray(triggerContext.get("oldList"));
            List<Account> oldList = jsonArray.toJavaList(Account.class);
            Map<Long,Account> oldMap = new HashMap<Long,Account>();
            for(Account old :oldList){
                logger.info("old=" + old);
                oldMap.put(old.getId(),old);
            }

            logger.info("jsonArray===="+jsonArray);
            logger.info("oldMap=" + oldMap);


            RkhdHttpClient rkhdclient = RkhdHttpClient.instance();          //请求实例
            List<XObject> dataList = triggerRequest.getDataList();
            Map<Long,Account> newMap= new HashMap<>();
            logger.info("getMDG_Customer_Code__c=" + ((Account)dataList.get(0)).getMDG_Customer_Code__c());
            logger.info("getTax_Number__c=" + ((Account)dataList.get(0)).getTax_Number__c());
            logger.info("getRegistration_number__c=" + ((Account)dataList.get(0)).getRegistration_number__c());
            logger.info("getOwnerIDorPassportnumber__c=" + ((Account)dataList.get(0)).getOwnerIDorPassportnumber__c());
            logger.info("getTelephone__c=" + ((Account)dataList.get(0)).getTelephone__c());
            List<XObject> updateList = new ArrayList<>();
            if (((Account)dataList.get(0)).getMDG_Customer_Code__c()==null || Objects.equals(((Account) dataList.get(0)).getMDG_Customer_Code__c(), "")){
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
                updateList.add(acc);
            }
            if (!updateList.isEmpty()){
                BatchOperateResult batchResult = XObjectService.instance().update(updateList);
                logger.info("batchResult1=" + batchResult.getSuccess());
                logger.info("batchResult2=" + batchResult.getErrorMessage());
            }

            for(XObject xObject : dataList){
//                logger.info("NewAccount" + JSONObject.toJSONString(xObject));
//                logger.info("OldAccount" + JSONObject.toJSONString(oldMap.get(xObject.getId())));
                if ((Account) xObject != oldMap.get(xObject.getId())){
                    hasChanges = true;
                }
                newMap.put(xObject.getId(), (Account) xObject);
                result.add(new DataResult(true, "成功", xObject));
                logger.info("((Account) xObject).getCustomer_Approval_Status__c()=" + ((Account) xObject).getCustomer_Approval_Status__c());
                logger.info("(oldMap.get(((Account) xObject).getId()).getCustomer_Approval_Status__c()=" + oldMap.get(((Account) xObject).getId()).getCustomer_Approval_Status__c());
                // 状态为Approved
                if (((Account) xObject).getCustomer_Approval_Status__c()==4 && !Objects.equals(oldMap.get(((Account) xObject).getId()).getCustomer_Approval_Status__c(), ((Account) xObject).getCustomer_Approval_Status__c())){
                    if (((Account) xObject).getMDG_Customer_Code__c() !=null){

                        // 触发接口
                        GMDG011Controller.sendGMDG011(xObject.getId().toString(),"U");
                    }else{
                        GMDG011Controller.sendGMDG011(xObject.getId().toString(),"I");
                    }
                }
                if (((Account) xObject).getCustomer_Approval_Status__c()==4
                        &&((Account) xObject).getSync_Status__c()!=null
                        && ((Account) xObject).getMDG__c()!=null
                        && Objects.equals(oldMap.get(((Account) xObject).getId()).getCustomer_Approval_Status__c(), ((Account) xObject).getCustomer_Approval_Status__c())
                        && !((Account) xObject).getMDG__c()
                        && Objects.equals(((Account) xObject).getSync_Status__c(), oldMap.get(((Account) xObject).getId()).getSync_Status__c())
                        // Sync_Status__c = Synchronized
                        && ((Account) xObject).getSync_Status__c() == 3
                ){
                    if (((Account) xObject).getMDG_Customer_Code__c() !=null){
                        // 触发接口
                        GMDG011Controller.sendGMDG011(xObject.getId().toString(),"U");
                    }else{
                        GMDG011Controller.sendGMDG011(xObject.getId().toString(),"I");
                    }
                }
            }
            msg = "成功";
        } catch (TriggerContextException | IOException | InterruptedException | XsyHttpException |
                 ApiEntityServiceException e) {
            msg = "失败";
            success = false;
            throw new RuntimeException(e);
        } catch (CustomConfigException e) {
            throw new RuntimeException(e);
        }
        logger.info("result==="+result);
        return new TriggerResponse(success, msg, result);
    }
}
