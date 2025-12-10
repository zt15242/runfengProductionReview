package other.rainbow.orderapp.event;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rkhd.platform.sdk.creekflow.ruleevent.RuleEvent;
import com.rkhd.platform.sdk.creekflow.ruleevent.RuleEventRequest;
import com.rkhd.platform.sdk.creekflow.ruleevent.RuleEventResponse;
import com.rkhd.platform.sdk.data.model.Delivery_Note__c;
import com.rkhd.platform.sdk.data.model.Order;
import com.rkhd.platform.sdk.data.model.OrderProduct;
import com.rkhd.platform.sdk.exception.ApiEntityServiceException;
import com.rkhd.platform.sdk.exception.ScriptBusinessException;
import com.rkhd.platform.sdk.exception.XsyHttpException;
import com.rkhd.platform.sdk.log.Logger;
import com.rkhd.platform.sdk.log.LoggerFactory;
import com.rkhd.platform.sdk.model.BatchOperateResult;
import com.rkhd.platform.sdk.model.OperateResult;
import com.rkhd.platform.sdk.model.QueryResult;
import com.rkhd.platform.sdk.service.XObjectService;
import com.rkhd.platform.sdk.service.XoqlService;
import other.rainbow.orderapp.trigger.GlobalPicksReq;
import other.rainbow.orderapp.trigger.PickOption;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
/**
 * 存在DN单，则不允许废弃订单
 * @author Chi-Lynne 张宇恒
 *
 */
public class DNToOrderRuleEvent implements RuleEvent {
    List<PickOption> deliveryPostStatus__cList = GlobalPicksReq.instance().getGlobalPicks("DeliveryPostStatus__c");
    private Logger logger = LoggerFactory.getLogger();

    public DNToOrderRuleEvent() throws IOException, XsyHttpException {
    }

    @Override
    public RuleEventResponse execute(RuleEventRequest request) throws ScriptBusinessException {
        logger.warn("开始执行触发规则代码脚本事件");
        //数据id
        request.getDataId();
        //对象apikey
        request.getEntityApiKey();

        RuleEventResponse response = new RuleEventResponse();

        try {
            //调用执行逻辑
            ruleEvent(request);
            //执行结果
            response.setSuccess(true);
            //返回信息
            response.setMsg("触发规则，代码脚本事件执行成功");
        } catch (Exception e) {
            //执行结果
            response.setSuccess(false);
            //返回信息
            response.setMsg("触发规则，代码脚本事件");
        }

        return response;
    }
    /**
     * 触发规则代码脚本事件逻辑
     * @param request
     */
    private void ruleEvent(RuleEventRequest request) throws ApiEntityServiceException {
        Map<Integer,String> dpsMap = new HashMap<>();
        logger.warn("触发规则代码脚本事件逻辑");
        if (deliveryPostStatus__cList.size() > 0){
            for (PickOption o : deliveryPostStatus__cList){
                dpsMap.put(o.getOptionCode(),o.getOptionLabel());
            }
        }
        Long dnId = request.getDataId();
        String sql = "select id,Order__c,Delete_Status__c,Order_Text__c,Delivery_Post_Status__c from Delivery_Note__c where id = " + dnId;
        QueryResult<Delivery_Note__c> queryResult = XObjectService.instance().query(sql,true);
        if (queryResult.getSuccess()){
            if (queryResult.getRecords().size() > 0){
                List<String> orderTextList = new ArrayList<>();
                JSONArray ja = JSONArray.parseArray(queryResult.getRecords().toString());
                logger.error("1111===" + ja.getJSONObject(0).getString("Delivery_Post_Status__c"));
                logger.error("1111===" + ja.getJSONObject(0));
                List<Order> upOList = new ArrayList<>();
                List<Long> upOidList = new ArrayList<>();
                for (int i = 0; i < ja.size() ;i++){
                    JSONObject js = ja.getJSONObject(i);
                    if (js.getBoolean("Delete_Status__c")){
                        orderTextList.add(js.getString("Order_Text__c"));
                    }
                    if (js.getLong("Order__c") != null && !js.getBoolean("Delete_Status__c")){
                        upOidList.add(js.getLong("Order__c"));
                    }/*
                    if (js.getInteger("Delivery_Post_Status__c") != null && js.getLong("Order__c") != null){
                        if ("C".equals(dpsMap.get(js.getInteger("Delivery_Post_Status__c")))){
                            Order o = new Order();
                            o.setId(js.getLong("Order__c"));
                            o.setDnHaveC__c(true);
                            upOList.add(o);
                        }
                    }*/
                }
                String orSql = "";
                logger.error("订单号====" + orderTextList.size());
                if (orderTextList.size() > 0){
                    logger.error("订单号====" + orderTextList.get(0));
                    String ordSql = "select id from _order where po = \'" + orderTextList.get(0) + "\'";
                    QueryResult delQuery = XoqlService.instance().query(ordSql,true);
                    if (delQuery.getSuccess() && delQuery.getRecords().size() > 0){
                        JSONArray delJson = JSONArray.parseArray(delQuery.getRecords().toString());
                        logger.error("订单号==delJson==" + delJson);
                        for (int i = 0;i < delJson.size();i++){
                            upOidList.add(delJson.getJSONObject(i).getLong("id"));
                        }
                    } else {
                        logger.error("budui==" + delQuery.getErrorMessage());
                    }
                }
                if (upOidList.size() > 0){
                    orSql = "select id,Order__c,Delivery_Post_Status__c from Delivery_Note__c where Order__c = " + upOidList.get(0);
                    logger.error("订单号==orSql==" + orSql);
                    QueryResult<Delivery_Note__c> queryOrdResult = XObjectService.instance().query(orSql,true);
                    if (queryOrdResult.getSuccess()){
                        if (queryOrdResult.getRecords().size() > 0){
                            logger.error("0-0-0===" + queryOrdResult.getRecords());
                            logger.error("0-0-0===" + queryOrdResult.getRecords().size());
                            Order o = new Order();
                            o.setId(upOidList.get(0));
                            o.setDnHaveC__c(true);
                            OperateResult up = XObjectService.instance().update(o,true);

                            if (!up.getSuccess()){
                                logger.error("cuowu===" + up.getErrorMessage());
                                throw new RuntimeException(up.getErrorMessage());
                            }
                        } else {
                            Order o = new Order();
                            o.setId(upOidList.get(0));
                            o.setDnHaveC__c(false);
                            OperateResult up = XObjectService.instance().update(o,true);

                            if (!up.getSuccess()){
                                logger.error("cuowu===" + up.getErrorMessage());
                                throw new RuntimeException(up.getErrorMessage());
                            }
                        }
                    } else {
                        logger.error("这里报错了==" + queryOrdResult.getErrorMessage());
                    }

                    // 获取该DN单的订单的明细
                    String diSql = "select id,Order__c,Order_Item__c,Delivery_Note__c,Delivery_Note__c.Delivery_Post_Status__c,Delivery_Quantity__c from Delivery_Note_Item__c where Order__c = " + upOidList.get(0);
                    QueryResult diQuery = XoqlService.instance().query(diSql,true);
                    if (diQuery.getSuccess()){
                        if (diQuery.getRecords().size() > 0) {
                            JSONArray jsonArray = JSONArray.parseArray(diQuery.getRecords().toString());
                            Map<Long, BigDecimal> opMap = new HashMap<>();
                            for (int i = 0;i < jsonArray.size();i++){
                                JSONObject js = jsonArray.getJSONObject(i);
                                logger.error("查看DN明细的信息====" + js);
                                if (js.getJSONArray("Delivery_Note__c.Delivery_Post_Status__c") != null && "A".equals(js.getJSONArray("Delivery_Note__c.Delivery_Post_Status__c").getString(0))){
                                    if (js.getDouble("Delivery_Quantity__c") > 0){
                                        if (opMap.containsKey(js.getLong("Order_Item__c"))){
                                            BigDecimal tempNum = opMap.get(js.getLong("Order_Item__c")).add(new BigDecimal(js.getDouble("Delivery_Quantity__c").toString()));
                                            opMap.put(js.getLong("Order_Item__c"),tempNum);
                                        } else {
                                            opMap.put(js.getLong("Order_Item__c"),new BigDecimal(js.getDouble("Delivery_Quantity__c").toString()));
                                        }
                                    }
                                }
                            }
                            if (!opMap.isEmpty()){
                                List<OrderProduct> opList = new ArrayList<>();
                                for (Long opId : opMap.keySet()){
                                    OrderProduct op = new OrderProduct();
                                    op.setId(opId);
                                    op.setUnshippedNumbers__c(opMap.get(opId).doubleValue());
                                    opList.add(op);
                                }
                                if (opList.size() > 0){
                                    BatchOperateResult update = XObjectService.instance().update(opList,true,true);
                                    if (!update.getSuccess()){
                                        logger.error("更新失败====" + update.getErrorMessage());
                                    }
                                }
                            }
                        } else {
                            logger.error("未查询出来==");
                        }
                    }
                } else {
                    logger.error("它真没数据");
                }
                if (upOList.size() > 0){
                    BatchOperateResult update = XObjectService.instance().update(upOList, false, true);
                    if (!update.getSuccess()){
                        logger.error("cuowu===" + update.getErrorMessage());
                        throw new RuntimeException(update.getErrorMessage());
                    }
                }
            }
        }
    }

    public static void main(String[] args) throws ApiEntityServiceException, IOException, XsyHttpException {
        String ii = "3828055719773227";
        RuleEventRequest req = new RuleEventRequest();
        req.setDataId(Long.valueOf(ii));
        DNToOrderRuleEvent event = new DNToOrderRuleEvent();
        event.ruleEvent(req);
    }
}
