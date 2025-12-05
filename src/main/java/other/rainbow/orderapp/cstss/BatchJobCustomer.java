package other.rainbow.orderapp.cstss;
import com.rkhd.platform.sdk.data.model.Account;
import com.rkhd.platform.sdk.exception.BatchJobException;
import com.rkhd.platform.sdk.log.Logger;
import com.rkhd.platform.sdk.log.LoggerFactory;
import com.rkhd.platform.sdk.model.XObject;
import com.rkhd.platform.sdk.task.BatchJob;
import com.rkhd.platform.sdk.task.BatchJobPrepare;

import java.util.ArrayList;
import java.util.List;

public class BatchJobCustomer  implements BatchJob {
    private Logger log = LoggerFactory.getLogger();

    @Override
    public void execute(List<XObject> accounts, String jobId) {
        log.error("一次执行");
        if(accounts != null) {
            log.error("数据量:" + accounts.size());
            List<String> orderIdSet = new ArrayList<String>();
            for (XObject xobject : accounts) {
                Account account = (Account) xobject;
                orderIdSet.add(account.getMDG_Customer_Code__c());
               try {
                   GetCustomerlimitFROMSAP.submitToSAP(orderIdSet,"USD");
                   log.error("batch执行成功");
                } catch (Exception e) {
                    e.printStackTrace();
                    log.error(e.getMessage());
                }
            }
        } else {
            log.error("无数据");
        }

    }

    @Override
    public void finish(String jobId) {
        log.info("finish");
    }

    @Override
    public BatchJobPrepare prepare() throws BatchJobException {
        //sql进行batchjob
        String query = "SELECT id,MDG_Customer_Code__c FROM account WHERE CreditNumber__c > 0 AND MDG_Customer_Code__c is not null";

        return new BatchJobPrepare.Builder(query).isAdmin(true).build();

        /*
                //通过ids进行batchjob
                List<Long> ids = new ArrayList<>();
                ids = getids();
                log.info("总执行数据量："+ids.size());
                log.info("alldata is :"+ids.toString());
            return new BatchJobPrepare.Builder("testzly__c",ids).isAdmin(true).build();
        */
    }
}