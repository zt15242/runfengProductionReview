/**
 * orderApprovalSubAfter
 * Chi Lynne
 * 审批提交后更新订单状态
 */
package other.rainbow.orderapp.approval;
import com.rkhd.platform.sdk.creekflow.approvalevent.ApprovalEvent;
import com.rkhd.platform.sdk.creekflow.approvalevent.ApprovalEventRequest;
import com.rkhd.platform.sdk.creekflow.approvalevent.ApprovalEventResponse;
import com.rkhd.platform.sdk.data.model.Order;
import com.rkhd.platform.sdk.exception.ApiEntityServiceException;
import com.rkhd.platform.sdk.exception.ScriptBusinessException;
import com.rkhd.platform.sdk.log.Logger;
import com.rkhd.platform.sdk.log.LoggerFactory;
import com.rkhd.platform.sdk.model.OperateResult;
import com.rkhd.platform.sdk.model.QueryResult;
import com.rkhd.platform.sdk.service.XObjectService;

import java.util.List;

public class orderApprovalSubAfter implements ApprovalEvent{
    private Logger logger = LoggerFactory.getLogger();

    /**
     * 执行入口
     */
    public ApprovalEventResponse execute(ApprovalEventRequest request)
            throws ScriptBusinessException {
        logger.warn("开始执行提交后事件");

        // 实体apikey
        request.getEntityApiKey();
        //数据Id
        request.getDataId();
        //待处理任务Id
        request.getUsertaskLogId();

        ApprovalEventResponse response = new ApprovalEventResponse();

        try {
            //调用实现
            String status = submitAfterEvent(request);
            if ("success".equals(status)){
                response.setSuccess(true);
                response.setMsg("Event execution succeeded after submission");
            } else {
                response.setSuccess(false);
                response.setMsg("Event execution failed after submission");
            }
        } catch (Exception e) {
            logger.error("Event execution failed after submission:" + e.getMessage());
            response.setSuccess(false);
            response.setMsg("Event execution failed after submission");
        }

        return response;
    }

    /**
     * 提交后事件实现
     * @param request
     */
    private String submitAfterEvent(ApprovalEventRequest request) {
        String sql = "select Id,po,IsClosed__c,Actual_available_Credit_Limit__c,ownerId,Status__c from _order where Id = " + Long.valueOf(request.getDataId());
        String status = "";
        try {
            QueryResult<Order> oResult = XObjectService.instance().query(sql,true);
            if (oResult.getSuccess()){
                List<Order> orderList = oResult.getRecords();
                Order nObj = orderList.get(0);
                nObj.setStatus__c(2);
                OperateResult update = XObjectService.instance().update(nObj,true);
                if (update.getSuccess()){
                    status = "success";
                }else {
                    status = "error";
                }
                logger.warn("提交前校验逻辑===Success");
            } else {
                status = "error";
            }
            logger.warn("执行提交后事件逻辑" + status);
            return status;
        } catch (ApiEntityServiceException e) {
            throw new RuntimeException(e);
        }
    }
}
