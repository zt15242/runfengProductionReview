package other.rainbow.cpzerooneone.trigger;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rkhd.platform.sdk.data.model.Account;
import com.rkhd.platform.sdk.exception.ScriptBusinessException;
import com.rkhd.platform.sdk.exception.TriggerContextException;
import com.rkhd.platform.sdk.exception.XsyHttpException;
import com.rkhd.platform.sdk.http.RkhdHttpClient;
import com.rkhd.platform.sdk.log.Logger;
import com.rkhd.platform.sdk.log.LoggerFactory;
import com.rkhd.platform.sdk.model.XObject;
import com.rkhd.platform.sdk.trigger.*;
import other.rainbow.cpzerooneone.common.NeoCrmRkhdService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class BeforeUpdateTrigger implements Trigger {
    private static final Logger logger = LoggerFactory.getLogger();
    @Override
    public TriggerResponse execute(TriggerRequest triggerRequest) throws ScriptBusinessException {
        String msg = "";
        boolean success = true;
        TriggerContext triggerContext = new TriggerContext();
        RkhdHttpClient rkhdclient = null;
        List<DataResult> result;
        try {
            rkhdclient = RkhdHttpClient.instance();
            List<XObject> xObjectList = triggerRequest.getDataList();
            List<XObject> contextList = new ArrayList<>();
            result = new ArrayList<>();
            String sqlaccount = "SELECT id,Customer_Approval_Status__c,Sync_Status__c,ownerId.name,Language__c,Customer_Group__c,Customer_Short_Name__c,accountName,Group_Name__c " +
                    ",Tax_Identification__c,Tax_Number__c,Tax_Number__c,Continent__c,Region__c,Country__c.Code__c," +
                    " Province_State__c.Code__c,City__c,Street__c,Postal_Code__c,SAP_Customer_Type__c,Contact_Name__c," +
                    " Telephone__c,Email__c,fax  FROM account WHERE id= " + xObjectList.get(0).getId();
            JSONArray accountinfo;
            try {
                accountinfo = NeoCrmRkhdService.xoql(rkhdclient, sqlaccount);
            } catch (XsyHttpException | InterruptedException e) {
                throw new RuntimeException(e);
            }
            for (XObject object : xObjectList) {
                Account temp = new Account();
                temp.setId(object.getId());
                temp.setFCity(object.getAttribute("fState"));
                temp.setFCity(object.getAttribute("fCity"));
                temp.setEntityType(object.getAttribute("entityType"));
                temp.setCustomer_Approval_Status__c(((Account) object).getCustomer_Approval_Status__c());
                temp.setSync_Status__c(((Account) object).getSync_Status__c());
                result.add(new DataResult(true, "成功", temp));
            }
            for (Integer i = 0; i < accountinfo.size(); i++) {
                JSONObject jsstr = accountinfo.getJSONObject(i);
                Account temp = new Account();
                temp.setId(xObjectList.get(0).getId());
                if (Objects.equals(jsstr.getJSONArray("Customer_Approval_Status__c").get(0).toString(), "Draft")) {
                    temp.setCustomer_Approval_Status__c((1));
                } else if (Objects.equals(jsstr.getJSONArray("Customer_Approval_Status__c").get(0).toString(), "Approving")) {
                    temp.setCustomer_Approval_Status__c((2));
                } else if (Objects.equals(jsstr.getJSONArray("Customer_Approval_Status__c").get(0).toString(), "Rejected")) {
                    temp.setCustomer_Approval_Status__c((3));
                } else if (Objects.equals(jsstr.getJSONArray("Customer_Approval_Status__c").get(0).toString(), "Approved")) {
                    temp.setCustomer_Approval_Status__c((4));
                } else if (Objects.equals(jsstr.getJSONArray("Customer_Approval_Status__c").get(0).toString(), "SynchronizedMDG")) {
                    temp.setCustomer_Approval_Status__c((5));
                } else if (Objects.equals(jsstr.getJSONArray("Customer_Approval_Status__c").get(0).toString(), "SyncFailed")) {
                    temp.setCustomer_Approval_Status__c((6));
                }
                if (jsstr.getJSONArray("Sync_Status__c") != null) {
                    if (Objects.equals(jsstr.getJSONArray("Sync_Status__c").get(0).toString(), "Unsynchronized")) {
                        temp.setSync_Status__c((1));
                    } else if (Objects.equals(jsstr.getJSONArray("Sync_Status__c").get(0).toString(), "Synchronization Failure")) {
                        temp.setSync_Status__c((2));
                    } else if (Objects.equals(jsstr.getJSONArray("Sync_Status__c").get(0).toString(), "Synchronized")) {
                        temp.setSync_Status__c((3));
                    }
                }
                contextList.add(temp);

            }


            logger.info("accountinfo==" + accountinfo);
            logger.info("result==" + result);
            logger.info("contextList==" + JSON.toJSONString(contextList));
            triggerContext.set("oldList", JSON.toJSONString(contextList));
            msg = "成功";
        } catch (TriggerContextException | IOException e) {
            msg = "失败";
            success = false;
            throw new RuntimeException(e);
        }

        return new TriggerResponse(success, msg, result, triggerContext);
    }
}
