package other.rainbow.orderapp.button;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSON;
import com.rkhd.platform.sdk.api.annotations.RequestMethod;
import com.rkhd.platform.sdk.api.annotations.RestApi;
import com.rkhd.platform.sdk.api.annotations.RestBeanParam;
import com.rkhd.platform.sdk.api.annotations.RestMapping;
import com.rkhd.platform.sdk.context.ScriptRuntimeContext;
import com.rkhd.platform.sdk.data.model.Order;
import com.rkhd.platform.sdk.exception.ApiEntityServiceException;
import com.rkhd.platform.sdk.exception.ScriptBusinessException;
import com.rkhd.platform.sdk.exception.XsyHttpException;
import com.rkhd.platform.sdk.http.RkhdHttpClient;
import com.rkhd.platform.sdk.log.Logger;
import com.rkhd.platform.sdk.log.LoggerFactory;
import com.rkhd.platform.sdk.model.BatchOperateResult;
import com.rkhd.platform.sdk.model.QueryResult;
import com.rkhd.platform.sdk.model.XObject;
import com.rkhd.platform.sdk.service.XoqlService;
import com.rkhd.platform.sdk.data.model.Order;
import other.rainbow.orderapp.api.SD005;
import other.rainbow.orderapp.common.NeoCrmRkhdService;
import other.rainbow.orderapp.pojo.ReturnResult;


import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;


@RestApi(baseUrl = "/button")
public class CancelToSap {
    private static final Logger logger = LoggerFactory.getLogger();
    @RestMapping(value = "/ToSAP", method = RequestMethod.POST)
    public static String toSap(@RestBeanParam(name = "data") String param) throws ScriptBusinessException, IOException, InterruptedException, XsyHttpException, ApiEntityServiceException {
        logger.info("param="+param);
        logger.error("进来调接口param===" + param);
        JSONObject jsonObject = JSONObject.parseObject(param);
        logger.info("jsonObject="+jsonObject.toJSONString());
        String  id = jsonObject.getString("id");
		// 20230918 ljh 关单加 start
		String  cancelReason = "";
		if(jsonObject.getString("cancelReason") != null){
			cancelReason = jsonObject.getString("cancelReason"); 
		}
		// 20230918 ljh 关单加 end
        logger.info("id="+id);
        ReturnResult result = new ReturnResult();
        String type = "";
        try {
            String sqlaccount = "SELECT id,Shipping_Status__c,poStatus,cancelReason,isClosed__c,SAP_Order_Code__c FROM _order WHERE id= " + id;
			QueryResult<JSONObject> query = XoqlService.instance().query(sqlaccount, true,true);
			if (query.getTotalCount()> 0 ){
				Order order = JSON.parseObject(query.getRecords().get(0).toJSONString(), Order.class);
				if (order.getIsClosed__c() 
				|| (!"".equals(order.getSAP_Order_Code__c()) && order.getSAP_Order_Code__c() != null)
				){
					type = "U";
				} else {
					type = "I";
				}
				logger.error("orderList===" + order);
				if(order.getCancelReason() != null ){
					cancelReason = String.valueOf(order.getCancelReason());
				}
				logger.error("cancelReason===" + cancelReason);
				if(cancelReason != null){
					switch (cancelReason) {
						case "22": cancelReason = "Z1";break;
						case "23": cancelReason = "Z2";break;
						case "24": cancelReason = "ZY";break;
						default:cancelReason = "ZY";
					}
				}
				SD005 tosap = new SD005(id,type,order.getIsClosed__c(),false,cancelReason,"");
				result = tosap.returnResult;
			}else{
				logger.info("没有查询订单数据");
			}
        } catch (ApiEntityServiceException e) {
            result.setIsSuccess(false);
            result.setMessage(e.getMessage());
            throw new RuntimeException(e);
        } catch (ScriptBusinessException | InterruptedException | IOException | XsyHttpException e) {
            throw new RuntimeException(e);
        }
        logger.info("最终返回result="+JSONObject.toJSONString(result));
//        SD005 tosap = new SD005(id.toString(),"I",true,false,"","");
        return JSONObject.toJSONString(result);
    }

    public static void main(String[] args) throws ScriptBusinessException, IOException, InterruptedException, XsyHttpException, ApiEntityServiceException {
        String str = "{\"id\" : \"3734058244442210\"}";
        CancelToSap.toSap(str);
    }
}
