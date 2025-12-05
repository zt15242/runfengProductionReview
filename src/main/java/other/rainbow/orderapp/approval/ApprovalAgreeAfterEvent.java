package other.rainbow.orderapp.approval;

import com.rkhd.platform.sdk.creekflow.approvalevent.ApprovalEvent;
import com.rkhd.platform.sdk.creekflow.approvalevent.ApprovalEventRequest;
import com.rkhd.platform.sdk.creekflow.approvalevent.ApprovalEventResponse;
import com.rkhd.platform.sdk.exception.ApiEntityServiceException;
import com.rkhd.platform.sdk.exception.ScriptBusinessException;
import com.rkhd.platform.sdk.exception.XsyHttpException;
import com.rkhd.platform.sdk.log.Logger;
import com.rkhd.platform.sdk.log.LoggerFactory;
import other.rainbow.orderapp.api.SD005;

import java.io.IOException;


/**
 * 审批流/工作流，节点级审批通过后代码脚本事件:审批同意后，同步/异步执行
 * @author rkhd
 *
 */
public class ApprovalAgreeAfterEvent implements ApprovalEvent {
    private Logger logger = LoggerFactory.getLogger();

    /**
     * 执行入口
     */
    public ApprovalEventResponse execute(ApprovalEventRequest request)
            throws ScriptBusinessException {
        logger.warn("开始执行通过后事件");
        // 实体apikey
        request.getEntityApiKey();
        //数据Id
        request.getDataId();
        //待处理任务Id
        request.getUsertaskLogId();

        ApprovalEventResponse response = new ApprovalEventResponse();

        try {
            //提交后事件实现
            agreeAfter(request);

            response.setSuccess(true);
            response.setMsg("Order approval passed");
        } catch (Exception e) {
            logger.error("Order approval failed:" + e.getMessage());
            response.setSuccess(false);
            response.setMsg("Order approval failed" + e.getMessage());
        }

        return response;
    }

    /**
     * 通过后事件实现
     * @param request
     */
    private void agreeAfter(ApprovalEventRequest request) throws ScriptBusinessException, IOException, InterruptedException, XsyHttpException, ApiEntityServiceException {
        logger.warn("通过后逻辑");
        try {
            SD005 sd = new SD005(request.getDataId().toString(),"I",false,false,"","");
        }catch (Exception e){
            logger.error("进来catch：" + e.getMessage());
        }


    }
}
