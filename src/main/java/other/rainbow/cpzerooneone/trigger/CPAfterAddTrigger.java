package other.rainbow.cpzerooneone.trigger;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rkhd.platform.sdk.data.model.Customer_Payment_Item__c;
import com.rkhd.platform.sdk.data.model.Customer_Payment__c;
import com.rkhd.platform.sdk.exception.ScriptBusinessException;
import com.rkhd.platform.sdk.exception.XsyHttpException;
import com.rkhd.platform.sdk.http.RkhdHttpClient;
import com.rkhd.platform.sdk.log.Logger;
import com.rkhd.platform.sdk.log.LoggerFactory;
import com.rkhd.platform.sdk.model.XObject;
import com.rkhd.platform.sdk.trigger.DataResult;
import com.rkhd.platform.sdk.trigger.Trigger;
import com.rkhd.platform.sdk.trigger.TriggerRequest;
import com.rkhd.platform.sdk.trigger.TriggerResponse;
import other.rainbow.cpzerooneone.common.NeoCrmRkhdService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CPAfterAddTrigger implements Trigger {
    private static final Logger logger = LoggerFactory.getLogger();
    @Override
    public TriggerResponse execute(TriggerRequest triggerRequest) {
        List<DataResult> result = new ArrayList<>();
        String msg ="";
        Boolean success = true;
        try {
            List<XObject> updateList = new ArrayList<>();
            RkhdHttpClient rkhdclient = RkhdHttpClient.instance();          //请求实例
            List<XObject> dataList = triggerRequest.getDataList();
            for(XObject xObject : dataList){
                logger.info("NewCP=" + xObject);

                if (((Customer_Payment__c) xObject).getAmountAll__c() > ((Customer_Payment__c) xObject).getPaymentamount__c()){
                    result.add(new DataResult(false, "The payment amount must be greater than the total amount of Customer Payment Items", xObject));
                    return new TriggerResponse(false, "The payment amount must be greater than the total amount of Customer Payment Items", result);
                }
                result.add(new DataResult(true, "成功", xObject));
            }
            msg = "成功";
        } catch (IOException e) {
            msg = "失败";
            success = false;
//            throw new RuntimeException(e);
        }
        logger.info("result==="+result);
        return new TriggerResponse(success, msg, result);
    }
}
