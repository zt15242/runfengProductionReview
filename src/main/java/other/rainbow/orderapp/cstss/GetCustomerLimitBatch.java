package other.rainbow.orderapp.cstss;

import com.rkhd.platform.sdk.exception.BatchJobException;
import com.rkhd.platform.sdk.log.Logger;
import com.rkhd.platform.sdk.log.LoggerFactory;
import com.rkhd.platform.sdk.model.XObject;
import com.rkhd.platform.sdk.task.BatchJobPro;
import com.rkhd.platform.sdk.task.param.BatchJobData;
import com.rkhd.platform.sdk.task.param.BatchJobDataBuilder;
import com.rkhd.platform.sdk.task.param.BatchJobParam;
import com.rkhd.platform.sdk.task.param.PrepareParam;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class GetCustomerLimitBatch implements BatchJobPro<XObject> {

    private static final Logger log = LoggerFactory.getLogger();

    @Override
    public BatchJobData prepare(PrepareParam prepareParam) throws BatchJobException {

        String query = "SELECT Id,MDG_Customer_Code__c FROM account WHERE CreditNumber__c > 0 AND MDG_Customer_Code__c is not null";
        log.error("准备");
        BatchJobDataBuilder batchJobDataBuilder = new BatchJobDataBuilder();
        // prepareParam.getParamMap().forEach(batchJobDataBuilder::setBatchJobParam);
        // startTime = LocalDateTime.now();
        return batchJobDataBuilder
                .setSql(query)    // 设置sql
                .setAdmin(true) // 设置管理员权限
                .setBatchJobParam("param1", "value1")
                .buildQueryData();
    }

    @Override
    public void execute(List<XObject> accList, BatchJobParam batchJobParam) {

        /*try {
            log.error("一次执行");
            if(accList != null) {
                log.error("数据量:" + accList.size());
                List<String> orderIdSet = new ArrayList<String>();

                for (XObject acc : accList) {
                    Account account = (Account) acc;
                    orderIdSet.add(account.getMDG_Customer_Code__c());

                }
              //  GetCustomerlimitFROMSAP.submitToSAP(orderIdSet,"USD");
            } else {
                log.error("无数据");
            }
        } catch (Exception e) {
            flag = false;
            errorMsg = e.getMessage();
            throw e;
        }*/
    }

    @Override
    public void finish(BatchJobParam batchJobParam) {
        log.error("GetCustomerLimitBatch完成");
        // LOG.info("GetCustomerLimitBatch;" + flag.toString() + ";" + startTime.toString() + ";" + errorMsg == null ? "" : errorMsg);
    }
}
