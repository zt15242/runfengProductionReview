package other.rainbow.orderapp.trigger;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rkhd.platform.sdk.data.model.Delivery_Note__c;
import com.rkhd.platform.sdk.data.model.Order;
import com.rkhd.platform.sdk.exception.ApiEntityServiceException;
import com.rkhd.platform.sdk.exception.ScriptBusinessException;
import com.rkhd.platform.sdk.log.Logger;
import com.rkhd.platform.sdk.log.LoggerFactory;
import com.rkhd.platform.sdk.model.BatchOperateResult;
import com.rkhd.platform.sdk.model.QueryResult;
import com.rkhd.platform.sdk.model.XObject;
import com.rkhd.platform.sdk.service.XObjectService;
import com.rkhd.platform.sdk.service.XoqlService;
import com.rkhd.platform.sdk.trigger.DataResult;
import com.rkhd.platform.sdk.trigger.Trigger;
import com.rkhd.platform.sdk.trigger.TriggerRequest;
import com.rkhd.platform.sdk.trigger.TriggerResponse;
import other.rainbow.orderapp.cstss.SqlFormatUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DnDeleteAfterTrigger implements Trigger {
    private static final Logger logger = LoggerFactory.getLogger();
    @Override
    public TriggerResponse execute(TriggerRequest triggerRequest) throws ScriptBusinessException {
        logger.error("进来触发器====删除DN");
        List<XObject> dnList = triggerRequest.getDataList();
        List <DataResult> dataResults = new ArrayList< DataResult >();
        logger.error("" + dnList.get(0) + dnList.size());
        List<Long> dnOrdIdList = new ArrayList<>();
        Map<Long,List<Delivery_Note__c>> ordIdDnMap = new HashMap<>();
        if (dnList.size() > 0){
            for (XObject obj : dnList){
                Delivery_Note__c dn = (Delivery_Note__c) obj;
                List<Delivery_Note__c> tempList = new ArrayList<>();
                if (dnOrdIdList.contains(dn.getOrder__c())){
                    tempList = ordIdDnMap.get(dn.getOrder__c());
                } else {
                    dnOrdIdList.add(dn.getOrder__c());
                }
                tempList.add(dn);
                ordIdDnMap.put(dn.getOrder__c(),tempList);
                dataResults.add(new DataResult(true, null, obj));
            }
            if (dnOrdIdList.size() > 0){
                List<Long> ordIdList = new ArrayList<>();
                List<Long> ordDelIdList = new ArrayList<>();
                String ordIds = SqlFormatUtils.joinLongInSql(dnOrdIdList);
                String ordSql = "select id,po,Delivery_Qty__c from _order where id in (" + ordIds + ")";
                QueryResult ordQuery = null;
                try {
                    ordQuery = XoqlService.instance().query(ordSql,true);
                } catch (ApiEntityServiceException e) {
                    throw new RuntimeException(e);
                }
                if (ordQuery.getSuccess() && ordQuery.getRecords().size()  > 0){
                    JSONArray jsonArray = JSONArray.parseArray(ordQuery.getRecords().toString());
                    List<Order> upOrdList = new ArrayList<>();
                    for (int i = 0;i < jsonArray.size();i++){
                        JSONObject js = jsonArray.getJSONObject(i);
                        if (js.getDouble("Delivery_Qty__c") == 0 || js.getDouble("Delivery_Qty__c") == null){
                            Order o = new Order();
                            o.setId(js.getLong("id"));
                            o.setDnHaveC__c(false);
                            upOrdList.add(o);
                            ordDelIdList.add(js.getLong("id"));
                            logger.error("删除DN后更新订单====" + js.getString("po"));
                        } else {
                            logger.error("删除DN后更新订单else====" + js.getString("po"));
                            logger.info("删除DN后更新订单else=ordIdList===" + ordIdList.size());
                            logger.info("删除DN后更新订单else=ordDelIdList===" + ordDelIdList.size());
                            if (!ordIdList.contains(js.getLong("id")) && !ordDelIdList.contains(js.getLong("id"))) {
                                logger.info("删除DN后更新订单else=ordIdList.contains(js.getLong(\"id\")===" + ordIdList.contains(js.getLong("id")));
                                logger.info("删除DN后更新订单else=ordDelIdList.contains(js.getLong(\"id\"))===" + ordDelIdList.contains(js.getLong("id")));
                                ordIdList.add(js.getLong("id"));
                            }
                        }
                    }
                    // 判断要校验的是否有已经清除标识的数据
                    logger.info("判断要校验的是否有已经清除标识的数据=ordIdList===" + ordIdList.size());
                    logger.info("判断要校验的是否有已经清除标识的数据=ordDelIdList===" + ordDelIdList.size());
                    if (ordDelIdList.size() > 0 && ordIdList.size() > 0){
                        for (int i = ordIdList.size() - 1;i >= 0;i--){
                            for (int j = ordDelIdList.size() - 1;j >= 0;j--){
                                logger.error("校验数据：" + ordIdList.get(i) + "===删除的数据：" + ordDelIdList.get(j));
                                if (ordIdList.get(i).equals(ordDelIdList.get(j))){
                                    ordIdList.remove(i);
                                }
                            }
                        }
                    }
                    if (ordIdList.size() > 0) {
                        logger.error("还存在DN的订单====" + ordIdList.size());
                        List<Long> nullOrdList = new ArrayList<>();
                        String ordDnSql = "select id,Order__c from Delivery_Note__c where Order__c in (" + SqlFormatUtils.joinLongInSql(ordIdList) + ")";
                        QueryResult ordDnQuery = null;
                        try {
                            ordDnQuery = XoqlService.instance().query(ordDnSql,true);
                        } catch (ApiEntityServiceException e) {
                            throw new RuntimeException(e);
                        }
                        if (ordDnQuery.getSuccess() && ordDnQuery.getRecords().size() > 0){
                            JSONArray ordDnJson = JSONArray.parseArray(ordDnQuery.getRecords().toString());
                            for (int i = 0;i < ordDnJson.size();i++){
                                JSONObject ordDnJs = ordDnJson.getJSONObject(i);
                                if (!nullOrdList.contains(ordDnJs.getLong("Order__c"))){
                                    // 将有DN的存到List
                                    nullOrdList.add(ordDnJs.getLong("Order__c"));
                                }
                            }
                            logger.info("nullOrdList" + nullOrdList.size());
                            logger.info("ordIdList" + ordIdList.size());
                            if (nullOrdList.size() > 0){
                                for (int i = ordIdList.size() - 1;i >= 0;i--){
                                    logger.info("进来一层" + ordIdList.size());
                                    for (int j = nullOrdList.size() - 1;j >= 0;j--) {
                                        logger.error("原来：" + ordIdList.get(i) + "===现在：" + nullOrdList.get(j));
                                        if (ordIdList.get(i).equals(nullOrdList.get(j))) {
                                            ordIdList.remove(i);
                                        }
                                    }
                                }
                                // 对比完有DN的数据之后剩下的就是没有DN的
                                if (ordIdList.size() > 0){
                                    logger.error("存在漏网之鱼1===" + ordIdList.size());
                                    for (int i = 0;i < ordIdList.size();i++){
                                        logger.error("存在漏网之鱼1===" + i + "==" + ordIdList.get(i));
                                        Order o = new Order();
                                        o.setId(ordIdList.get(i));
                                        o.setDnHaveC__c(false);
                                        upOrdList.add(o);
                                    }
                                }
                            }
                            // 如果没有查出DN信息，那么证明所有订单都没有dn了，都清除标识
                        } else if (ordDnQuery.getSuccess() && ordDnQuery.getRecords().size() == 0){
                            logger.error("存在漏网之鱼2===" + ordIdList.size());
                            for (int i = 0;i < ordIdList.size();i++){
                                if (!nullOrdList.contains(ordIdList.get(i))){
                                    nullOrdList.add(ordIdList.get(i));
                                    logger.error("存在漏网之鱼2===" + i + "==" + ordIdList.get(i));
                                    Order o = new Order();
                                    o.setId(ordIdList.get(i));
                                    o.setDnHaveC__c(false);
                                    upOrdList.add(o);
                                }
                            }
                        }

                    }
                    if (upOrdList.size() > 0) {
                        BatchOperateResult update = null;
                        try {
                            update = XObjectService.instance().update(upOrdList,true,true);
                        } catch (ApiEntityServiceException e) {
                            throw new RuntimeException(e);
                        }
                        if (!update.getSuccess()) {
                            logger.error(update.getErrorMessage());
                        }
                    }
                }
            }
        }
        return new TriggerResponse(true, null, dataResults);
    }
}
