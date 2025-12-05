package other.rainbow.orderapp.cstss;

import com.rkhd.platform.sdk.exception.BatchJobException;
import com.rkhd.platform.sdk.log.Logger;
import com.rkhd.platform.sdk.log.LoggerFactory;
import com.rkhd.platform.sdk.ScheduleJob;
import com.rkhd.platform.sdk.param.ScheduleJobParam;
import com.rkhd.platform.sdk.service.BatchJobService;

public class GetCustomerlimitFROMSAPSchedule implements ScheduleJob {
    private static final Logger log = LoggerFactory.getLogger();
    @Override
    public void execute(ScheduleJobParam arg0) {

        try {
            // BatchJobProService.instance().addBatchJob(GetCustomerLimitBatch.class,50,null);
            BatchJobService.instance().addBatchJob(BatchJobCustomer.class, 100);
        } catch (BatchJobException e) {
            log.info(e.getMessage());
        }
    }
}