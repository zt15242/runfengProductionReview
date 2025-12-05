package other.rainbow.orderapp.submit;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rkhd.platform.sdk.api.annotations.RequestMethod;
import com.rkhd.platform.sdk.api.annotations.RestApi;
import com.rkhd.platform.sdk.api.annotations.RestMapping;
import com.rkhd.platform.sdk.api.annotations.RestQueryParam;
import com.rkhd.platform.sdk.data.model.Order;
import com.rkhd.platform.sdk.http.RkhdHttpClient;
import com.rkhd.platform.sdk.http.RkhdHttpData;
import com.rkhd.platform.sdk.log.Logger;
import com.rkhd.platform.sdk.log.LoggerFactory;
import com.rkhd.platform.sdk.model.OperateResult;
import com.rkhd.platform.sdk.model.QueryResult;
//import com.rkhd.platform.sdk.log.LoggerLogger;
import com.rkhd.platform.sdk.service.XObjectService;

import other.rainbow.orderapp.pojo.PreProcessor;

import other.rainbow.orderapp.pojo.RequestBody;
import other.rainbow.orderapp.pojo.orderMsg;

import java.util.List;

@RestApi(baseUrl = "/button")
public class orderSubmitApp {
    private static final Logger log = LoggerFactory.getLogger();
    @RestMapping(value = "/Order", method = RequestMethod.GET)
    public static String init(@RestQueryParam(name = "recordId") String recordId){
        String sql = "select Id,po,IsClosed__c,Actual_available_Credit_Limit__c,ownerId,Status__c from _order where Id = " + Long.valueOf(recordId);
        String str = "";
        try {
            QueryResult<Order> result = XObjectService.instance().query(sql);
            List<Order> orderList = result.getRecords();
            log.debug("debug======"+String.valueOf(orderList));
            log.info("=====" + String.valueOf(orderList));
            log.error( "任务更新失败"+ result.getErrorMessage());
            orderMsg om = new orderMsg();
            if (orderList.size() > 0) {
                log.error( "查训条数:"+ orderList.size());
                for (Order o : orderList){
                    if (o.getStatus__c() != 1){
                        str = "状态不是Draft不可提交审批" + o.getStatus__c();
                        continue;
                    }
                    if (o.getIsClosed__c()){
                        str = "订单已取消，不可提交审批";
                        continue;
                    }
                    if (o.getActual_available_Credit_Limit__c() <= 0){
                        str = "信用额度较低，不可提交订单";
                    }
                }
                log.error( "是否有str:"+ str);
                if (str.isEmpty()){
                    Order nObj = orderList.get(0);
                    nObj.setStatus__c(2);
                    OperateResult update = XObjectService.instance().update(nObj);
                    if (update.getSuccess()){
                        log.error( "进来success");
                        RkhdHttpClient client = RkhdHttpClient.instance();
                        RkhdHttpData build = RkhdHttpData.newBuilder().build();
                        PreProcessor pro = new PreProcessor();
                        pro.setAction("submit");
                        pro.setEntityApiKey("Order");
                        pro.setDataId(recordId.toString());
                        build.setCall_type("POST");
                        build.setCallString("/rest/data/v2.0/creekflow/task/actions/preProcessor");
                        build.setBody(JSONObject.toJSONString(new RequestBody(pro)));
                        build.putHeader("Content-Type","application/json");
                        log.error(JSONObject.toJSONString(new RequestBody(pro))); // 执行审批流程
                        String s = client.performRequest(build);
                        log.error("测试+++：" + build);
                        log.error("测试：" + s);
                        JSONObject jsonObject = JSONObject.parseObject(s);
                        log.error(jsonObject.getString("msg"));
                        log.error("Error Info:" + jsonObject.toJSONString());
                        if ("200".equals(jsonObject.getString("code"))){
                            log.error(String.valueOf(jsonObject));
//                            Task task = new Task();
//                            task.setAction("submit");
//                            task.setEntityApiKey("Order");
//                            task.setDataId(recordId.toString());
//                            task.setProcdefId(procdefId);
//                            task.setNextTaskDefKey(nextTaskDefKey);
//                            RkhdHttpClient client = RkhdHttpClient.instance();
//                            RkhdHttpData build = RkhdHttpData.newBuilder().build();
//                            build.setCall_type("POST");
//                            build.setCallString("/rest/data/v2.0/creekflow/task");
//                            build.setBody(JSON.toJSONString(pro));
//
//                            build.putHeader("Content-Type","application/json");
//                            logger.info(JSON.toJSONString(pro));
//                            JSONObject resJson = client.execute(build, ResponseBodyHandlers.ofJSON());
//                            return resJson;
                            om.setMsg("提交成功了");
                            om.setStatus("success");
                        } else {
                            om.setMsg("失败了：" + jsonObject.getString("msg"));
                            om.setStatus("error");
                            nObj.setStatus__c(1);
                            OperateResult oldupdate = XObjectService.instance().update(nObj);
                        }
                    } else {
                        om.setMsg("失败了" + orderList);
                        om.setStatus("error");
                    }
                } else {
                    om.setMsg(str);
                    om.setStatus("error");
                }
                return JSON.toJSONString(om);
            } else {
                log.error("获取状态已发货但没有物流单号的订单信息失败：getOrderInfo. " + result.getErrorMessage());
                om.setMsg("没查到数据" + String.valueOf(result) + "====" + recordId + "-=-" + orderList.size());
                om.setStatus("error");
                return JSON.toJSONString(om);
            }
        } catch (Exception e){
            log.error( "catch+++++"+ e);
            orderMsg om = new orderMsg();
            om.setMsg(e.getMessage() + "catch");
            om.setStatus("error");
            return JSON.toJSONString(om);
        }
    }
}
