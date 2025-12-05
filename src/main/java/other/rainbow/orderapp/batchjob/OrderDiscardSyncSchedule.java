package other.rainbow.orderapp.batchjob;

import com.rkhd.platform.sdk.ScheduleJob;
import com.rkhd.platform.sdk.exception.BatchJobException;
import com.rkhd.platform.sdk.param.ScheduleJobParam;
import com.rkhd.platform.sdk.service.BatchJobProService;

public class OrderDiscardSyncSchedule implements ScheduleJob {
    @Override
    public void execute(ScheduleJobParam scheduleJobParam) {
        try {
            BatchJobProService.instance().addBatchJob(OrderDiscardSyncBatchJob.class, 1, null);
        } catch (BatchJobException e) {
            throw new RuntimeException("错误信息===" + e + e.getStackTrace()[0].getLineNumber() + e.getStackTrace()[0].getClassName());
        }
    }
}
