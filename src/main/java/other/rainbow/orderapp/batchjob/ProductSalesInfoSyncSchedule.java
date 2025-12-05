package other.rainbow.orderapp.batchjob;

import com.rkhd.platform.sdk.ScheduleJob;
import com.rkhd.platform.sdk.exception.BatchJobException;
import com.rkhd.platform.sdk.param.ScheduleJobParam;
import com.rkhd.platform.sdk.service.BatchJobProService;
import com.rkhd.platform.sdk.task.param.PrepareParam;

/**
 * @author 武于伦
 * @version 1.0
 * @date 2024/12/16 13:08
 * @e-mail 2717718875@qq.com
 * @description:
 */
public class ProductSalesInfoSyncSchedule  implements ScheduleJob {
    @Override
    public void execute(ScheduleJobParam scheduleJobParam) {
        try {
            BatchJobProService.instance().addBatchJob(ProductSalesInfoSyncBatchJob.class, 1, null);
        } catch (BatchJobException e) {
            throw new RuntimeException(e);
        }
    }

}
