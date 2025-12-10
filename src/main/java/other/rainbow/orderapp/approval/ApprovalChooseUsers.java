/**
 * @ClassName ApprovalChooseUsers
 * @Auther Chi-Lynne
 * @Discription 订单审批动态选人
 **/
package other.rainbow.orderapp.approval;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rkhd.platform.sdk.context.ScriptRuntimeContext;
import com.rkhd.platform.sdk.creekflow.approvalchooser.ApprovalChooser;
import com.rkhd.platform.sdk.creekflow.approvalchooser.ApprovalChooserRequest;
import com.rkhd.platform.sdk.creekflow.approvalchooser.ApprovalChooserResponse;
import com.rkhd.platform.sdk.exception.ApiEntityServiceException;
import com.rkhd.platform.sdk.exception.ScriptBusinessException;
import com.rkhd.platform.sdk.log.Logger;
import com.rkhd.platform.sdk.log.LoggerFactory;
import com.rkhd.platform.sdk.model.QueryResult;
import com.rkhd.platform.sdk.service.XObjectService;

import other.rainbow.orderapp.cstss.SqlFormatUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ApprovalChooseUsers implements ApprovalChooser {
    private static final Logger logger = LoggerFactory.getLogger();
    @Override
    public ApprovalChooserResponse execute(ApprovalChooserRequest approvalChooserRequest) throws ScriptBusinessException {
        ApprovalChooserResponse response = new ApprovalChooserResponse();
        Long recodeId = approvalChooserRequest.getDataId();
        Integer wareHouse = null;
		Integer org = null;
        Long department = null;
        List<Long> appIdList = new ArrayList<>();
        String ordSql = "select id,po,Sales_Org__c,wareHouse__c from _order where id = " + recodeId ;
		try {
			QueryResult ordQuery = XObjectService.instance().query(ordSql,true,true);
            if (ordQuery.getSuccess()){
                logger.error("ordQuery.getRecords().size()===" + ordQuery.getRecords().size());
                if (ordQuery.getRecords().size() > 0) {
                    JSONArray orderInfo = JSONArray.parseArray(ordQuery.getRecords().toString());
                    wareHouse = orderInfo.getJSONObject(0).getInteger("wareHouse__c");
					org = orderInfo.getJSONObject(0).getInteger("Sales_Org__c");
					logger.error("wareHouse===" + wareHouse);
					String approalSql = "select id from user where wareHouse__c =  " + wareHouse;
					QueryResult approalQuery = null;
					approalQuery = XObjectService.instance().query(approalSql,true);
					if (approalQuery.getSuccess()){
						if (approalQuery.getRecords().size() > 0){
							JSONArray approalInfo = JSONArray.parseArray(approalQuery.getRecords().toString());
							for (int i = 0;i < approalInfo.size();i++){
								appIdList.add(approalInfo.getJSONObject(i).getLong("id"));
							}
						}
					}
                }
            }
        } catch (ApiEntityServiceException e) {
            throw new RuntimeException(e);
        }

        if (appIdList.size() > 0){
            response.setData(appIdList);
            response.setSuccess(true);
        } else {
            String depSql = "select id,departName from department";
            QueryResult depQuery = null;
            List<Long> depIdList = new ArrayList<>();
            try {
                depQuery = XObjectService.instance().query(depSql,true);
                if (depQuery.getSuccess()){
                    if (depQuery.getRecords().size() > 0){
                        JSONArray jsonArray = JSONArray.parseArray(depQuery.getRecords().toString());
                        logger.error("部门查询成功===" + jsonArray);
                        for (int i = 0;i < jsonArray.size();i++){
                            JSONObject js = jsonArray.getJSONObject(i);
                            if ("Finance Dept".equals(js.getString("departName"))){
                                depIdList.add(js.getLong("id"));
                            }
                        }
                    }
                }
            } catch (ApiEntityServiceException e) {
                throw new RuntimeException(e);
            }
            if (depIdList.size() > 0) {
                String depIds = SqlFormatUtils.joinLongInSql(depIdList);
                String depSQL = "select id,dimDepart from user where dimDepart in (" + depIds + ") and Sales_Org__c = " + org;
                QueryResult approalQuery = null;
                try {
                    approalQuery = XObjectService.instance().query(depSQL, true);
                } catch (ApiEntityServiceException e) {
                    throw new RuntimeException(e);
                }
                if (approalQuery.getSuccess()) {
                    if (approalQuery.getRecords().size() > 0) {
                        JSONArray approalInfo = JSONArray.parseArray(approalQuery.getRecords().toString());
                        logger.error("用户查询成功===" + approalInfo);
                        for (int i = 0; i < approalInfo.size(); i++) {
                            appIdList.add(approalInfo.getJSONObject(i).getLong("id"));
                        }
                    }
                }
            }
            response.setData(appIdList);
            response.setSuccess(true);
        }
        
        return response;
    }
}
