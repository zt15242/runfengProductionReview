/**
 * OrderDiscardSyncBatchJob
 * 删除订单应收单，订单作废
 * Chi Lynne
 */
package other.rainbow.orderapp.batchjob;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rkhd.platform.sdk.data.model.Invoice;
import com.rkhd.platform.sdk.data.model.Order;
import com.rkhd.platform.sdk.exception.ApiEntityServiceException;
import com.rkhd.platform.sdk.exception.BatchJobException;
import com.rkhd.platform.sdk.exception.ScriptBusinessException;
import com.rkhd.platform.sdk.exception.XsyHttpException;
import com.rkhd.platform.sdk.http.RkhdHttpClient;
import com.rkhd.platform.sdk.http.RkhdHttpData;
import com.rkhd.platform.sdk.log.Logger;
import com.rkhd.platform.sdk.log.LoggerFactory;
import com.rkhd.platform.sdk.model.BatchOperateResult;
import com.rkhd.platform.sdk.model.OperateResult;
import com.rkhd.platform.sdk.model.QueryResult;
import com.rkhd.platform.sdk.model.XObject;
import com.rkhd.platform.sdk.service.XObjectService;
import com.rkhd.platform.sdk.service.XoqlService;
import com.rkhd.platform.sdk.task.BatchJobPro;
import com.rkhd.platform.sdk.task.param.BatchJobData;
import com.rkhd.platform.sdk.task.param.BatchJobDataBuilder;
import com.rkhd.platform.sdk.task.param.BatchJobParam;
import com.rkhd.platform.sdk.task.param.PrepareParam;
import other.rainbow.orderapp.api.SD005;
import other.rainbow.orderapp.cstss.SqlFormatUtils;
import other.rainbow.orderapp.pojo.ReturnResult;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class OrderDiscardSyncBatchJob implements BatchJobPro<XObject> {
    Logger logger = LoggerFactory.getLogger();
    @Override
    public BatchJobData prepare(PrepareParam prepareParam) throws BatchJobException {
        // 获取当前日期和七天前的日期
        LocalDate currentDate = LocalDate.now();
        LocalDate sevenDaysAgo = currentDate.minus(7, ChronoUnit.DAYS);

        // 将日期转换为Long类型（毫秒）
        long currentDateInMillis = currentDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long sevenDaysAgoInMillis = sevenDaysAgo.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();

        // 打印结果
        System.out.println("当前日期（毫秒）: " + currentDateInMillis);
        System.out.println("七天前的日期（毫秒）: " + sevenDaysAgoInMillis);
        String orderAllSql = "select id,Approve_Date__c from _order where Approve_Date__c < " + sevenDaysAgoInMillis + " and SAP_Order_Code__c != \'\' and poStatus = 2 and User_Country__c = 4";
        List<Long> orderAllIdList = new ArrayList<>();
        List<Long> dnOrderIdList = new ArrayList<>();
        QueryResult orderAllQuery = null;
        QueryResult dnAllQuery = null;
        String sqlOrderIds = "";
        try {
            orderAllQuery = XoqlService.instance().query(orderAllSql,true);
        } catch (ApiEntityServiceException e) {
            throw new RuntimeException(e);
        }
        if (orderAllQuery != null && orderAllQuery.getSuccess() && orderAllQuery.getRecords().size() > 0){
            logger.error("检索总数量===" + orderAllQuery.getRecords().size());
            logger.error("检索数据===" + orderAllQuery.getRecords());
            JSONArray orderJson = JSONArray.parseArray(orderAllQuery.getRecords().toString());
            for (int i = 0;i < orderJson.size();i++){
                orderAllIdList.add(orderJson.getJSONObject(i).getLong("id"));
            }
        }
        if (orderAllIdList.size() > 0){
            String orderAllIds = SqlFormatUtils.joinLongInSql(orderAllIdList);
            String dnAllSql = "select id,Order__c from Delivery_Note__c where Order__c in (" + orderAllIds + ")";
            try {
                dnAllQuery = XoqlService.instance().query(dnAllSql,true);
                logger.error("查询dn===" + dnAllQuery.getSuccess() + dnAllQuery.getErrorMessage());
                logger.error("查询dn数量===" + dnAllQuery.getRecords().size());
                logger.error("查询dn数据===" + dnAllQuery.getRecords());
            } catch (ApiEntityServiceException e) {
                throw new RuntimeException(e);
            }
        }
        if (dnAllQuery != null && dnAllQuery.getSuccess() && dnAllQuery.getRecords().size() > 0){
            JSONArray dnJson = JSONArray.parseArray(dnAllQuery.getRecords().toString());
            for (int i = 0;i < dnJson.size();i++){
                JSONObject dnJs = dnJson.getJSONObject(i);
                if (!dnOrderIdList.contains(dnJs.getLong("Order__c"))) {
                    dnOrderIdList.add(dnJs.getLong("Order__c"));
                }
            }
        }
        if (dnOrderIdList.size() > 0){
            logger.error("dnOrderIdList===" + dnOrderIdList);
            for (int i = orderAllIdList.size()-1; i > 0;i--){
                for (int j = dnOrderIdList.size()-1 ;j > 0;j--){
                    if (orderAllIdList.get(i) == dnOrderIdList.get(j)){
                        orderAllIdList.remove(i);
                    }
                }
            }
        }
        if (orderAllIdList.size() > 0){
            sqlOrderIds = SqlFormatUtils.joinLongInSql(orderAllIdList);
            logger.error("最终检索id===" + sqlOrderIds);
        }
        if ("".equals(sqlOrderIds)){
            return null;
        }
        String exeSql = "select id from _order where id in (" + sqlOrderIds + ")";
        BatchJobDataBuilder batchJobDataBuilder = new BatchJobDataBuilder();
        return batchJobDataBuilder
                .setSql(exeSql)    // 设置sql
                .setAdmin(true) // 设置管理员权限
                .setBatchJobParam("param1", "value1") // 可以单独在这里设置BatchJobParam
                .buildQueryData();
    }

    @Override
    public void execute(List<XObject> list, BatchJobParam batchJobParam) {
        List<Long> ordIdlist = new ArrayList<>();
        List<Long> delInvList = new ArrayList<>();
        for (XObject obj : list){
            Order o = (Order) obj;
            ordIdlist.add(o.getId());
        }
        String ordIds = SqlFormatUtils.joinLongInSql(ordIdlist);
        String invoice = "select id,status from invoice where  orderId in (" + ordIds + ")";
        QueryResult<XObject> invoiceQuery = null;
        try {
            invoiceQuery = XObjectService.instance().query(invoice,true);
        } catch (ApiEntityServiceException e) {
            throw new RuntimeException(e);
        }
        if (invoiceQuery.getSuccess() && invoiceQuery.getRecords().size() > 0){
            try {
                RkhdHttpClient client = RkhdHttpClient.instance();
                JSONArray invJson = JSONArray.parseArray(invoiceQuery.getRecords().toString());
                logger.error("invJson====" + invJson);
                for (int i = 0;i < invJson.size();i++){
                    if (invJson.getJSONObject(i).getInteger("status") == 2) {
                        RkhdHttpData data = new RkhdHttpData();
                        data.setCallString("/rest/data/v2.0/xobjects/invoice/actions/deactivation?dataId=" + invJson.getJSONObject(i).getLong("id"));
                        data.setCall_type("PATCH");
                        String responseStr = client.performRequest(data);
                        JSONObject responseObject = JSONObject.parseObject(responseStr);
                        if ("200".equals(responseObject.getString("code"))){
                            delInvList.add(invJson.getJSONObject(i).getLong("id"));
                            logger.error("应收失效成功===" + invJson.getJSONObject(i).getLong("id"));
                        }
                    }
                }
                BatchOperateResult delete = XObjectService.instance().delete(invoiceQuery.getRecords(),true);
                if (!delete.getSuccess()) {
                    logger.error("应收删除成功===="+delete.getErrorMessage());
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (ApiEntityServiceException e) {
                throw new RuntimeException(e);
            }
        }
        try {
            RkhdHttpClient client = RkhdHttpClient.instance();
            for (Long ordId : ordIdlist){
                RkhdHttpData data = new RkhdHttpData();
                data.setCallString("/rest/data/v2.0/xobjects/order/actions/cancel?recordId=" + ordId + "&cancelResonId=18");
                data.setCall_type("PATCH");
                Order o = new Order();
                o.setIsClosed__c(true);
                o.setId(ordId);
                OperateResult update = XObjectService.instance().update(o,true);
                if (update.getSuccess()) {
                    String responseStr = client.performRequest(data);
                    JSONObject responseObject = JSONObject.parseObject(responseStr);
                    if ("200".equals(responseObject.getString("code"))) {
                        logger.error("订单作废成功===" + ordId);
                        SD005 tosap = new SD005(ordId.toString(),"U",true,false,"Z4","");
                        ReturnResult result = tosap.returnResult;
                        logger.error("ReturnResult===" + result.getIsSuccess());
                    }
                } else {
                    logger.error("更新close失败" + ordId);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ApiEntityServiceException e) {
            throw new RuntimeException(e);
        } catch (ScriptBusinessException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (XsyHttpException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void finish(BatchJobParam batchJobParam) {

    }

    public static void main(String[] args) throws ApiEntityServiceException {
        // 获取当前日期和七天前的日期
        LocalDate currentDate = LocalDate.now();
        LocalDate sevenDaysAgo = currentDate.minus(7, ChronoUnit.DAYS);

        // 将日期转换为Long类型（毫秒）
        long currentDateInMillis = currentDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long sevenDaysAgoInMillis = sevenDaysAgo.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        String orderAllSql = "select id,Approve_Date__c from _order where Approve_Date__c < " + sevenDaysAgoInMillis + " and SAP_Order_Code__c != \'\' and poStatus = 2 limit 2";
        QueryResult orderAllQuery = XoqlService.instance().query(orderAllSql,true);
        if (!orderAllQuery.getSuccess()){
            System.out.println("================"+orderAllQuery.getErrorMessage());
        }
        List<Long> orderAllIdList = new ArrayList<>();
        if (orderAllQuery.getSuccess() && orderAllQuery.getRecords().size() > 0){
            JSONArray orderJson = JSONArray.parseArray(orderAllQuery.getRecords().toString());
            for (int i = 0;i < orderJson.size();i++){
                orderAllIdList.add(orderJson.getJSONObject(i).getLong("id"));
            }
        }
        if (orderAllIdList.size() > 0){
            String orderAllIds = SqlFormatUtils.joinLongInSql(orderAllIdList);
            String dnAllSql = "select id from Delivery_Note__c where Order__c in (" + orderAllIds + ")";
            String invoice = "select id,status from invoice limit 3";
            System.out.println("==DND2========" + dnAllSql);
            try {
                QueryResult dnAllQuery = XoqlService.instance().query(dnAllSql,true);
                QueryResult invAllQuery = XoqlService.instance().query(invoice,true);
                QueryResult<XObject> invAllQuery1 = XObjectService.instance().query(invoice,true);
                if (dnAllQuery.getSuccess()){
                    System.out.println("==DND========" + dnAllQuery.getRecords());
                    System.out.println("==DNDI========" + invAllQuery.getRecords());
                    System.out.println("==DNDI1q========" + invAllQuery1.getRecords());
                }
            } catch (ApiEntityServiceException e) {
                throw new RuntimeException(e);
            }
        }
        QueryResult invoiceQuery = null;
        // 打印结果
        System.out.println("当前日期（毫秒）: " + orderAllQuery.getRecords());
        System.out.println("当前日期（毫秒）: " + currentDate);
        System.out.println("七天前的日期（毫秒）: " + sevenDaysAgo);
        System.out.println("当前日期（毫秒）: " + currentDateInMillis);
        System.out.println("七天前的日期（毫秒）: " + sevenDaysAgoInMillis);
    }
}
