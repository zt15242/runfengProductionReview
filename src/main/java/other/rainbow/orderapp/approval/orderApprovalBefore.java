/**
 * @ClassName orderApprovalBefore
 * @Auther Chi-Lynne
 * @Discription 提交审批前校验信用是否足够
 **/
package other.rainbow.orderapp.approval;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rkhd.platform.sdk.creekflow.approvalevent.ApprovalEvent;
import com.rkhd.platform.sdk.creekflow.approvalevent.ApprovalEventRequest;
import com.rkhd.platform.sdk.creekflow.approvalevent.ApprovalEventResponse;
import com.rkhd.platform.sdk.data.model.*;
import com.rkhd.platform.sdk.exception.ApiEntityServiceException;
import com.rkhd.platform.sdk.exception.ScriptBusinessException;
import com.rkhd.platform.sdk.exception.XsyHttpException;
import com.rkhd.platform.sdk.http.RkhdHttpClient;
import com.rkhd.platform.sdk.http.RkhdHttpData;
import com.rkhd.platform.sdk.http.handler.ResponseBodyHandlers;
import com.rkhd.platform.sdk.log.Logger;
import com.rkhd.platform.sdk.log.LoggerFactory;
import com.rkhd.platform.sdk.model.BatchOperateResult;
import com.rkhd.platform.sdk.model.OperateResult;
import com.rkhd.platform.sdk.model.QueryResult;
import com.rkhd.platform.sdk.model.XObject;
import com.rkhd.platform.sdk.service.XObjectService;
import com.rkhd.platform.sdk.service.XoqlService;
import com.sun.org.apache.xpath.internal.operations.Or;
import javafx.scene.effect.Bloom;
import org.apache.commons.lang.StringUtils;
import other.rainbow.orderapp.api.ProductSalesInfoSyncToSAP;
import other.rainbow.orderapp.common.NeoCrmRkhdService;
import other.rainbow.orderapp.cstss.*;
import other.rainbow.orderapp.pojo.ReturnResult;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

public class orderApprovalBefore implements ApprovalEvent {
    private static Logger logger = LoggerFactory.getLogger();

    /**
     * 执行入口
     * 返回：true：流程正常提交；false：不允许提交，message提示信息
     */
    public ApprovalEventResponse execute(ApprovalEventRequest request)
            throws ScriptBusinessException {
        logger.warn("开始提交前校验");

        // 实体apikey
        request.getEntityApiKey();
        //数据Id
        request.getDataId();
        //待处理任务Id
        request.getUsertaskLogId();

        ApprovalEventResponse response = new ApprovalEventResponse();

        try {
            //调用实现
            resultInfo result = validate(request);
            //boolean result2 = linshi();
            // boolean resultDay = getDueDays(request);
            response.setSuccess(result.getSuccess());
            if (result.getSuccess()){
                response.setMsg("Submitted successfully");
            } else {
                response.setMsg(result.getErrMsg());
            }
        } catch (Exception e) {
            logger.error("提交前校验执行失败:" + e.getMessage() + e.getStackTrace()[0].getLineNumber() + e.getStackTrace()[0].getClassName());
            response.setSuccess(false);
            response.setMsg(e.getMessage());
        }
        logger.error("提交前校验执行结果" + response.getMsg());
        return response;
    }

    /**
     * 校验逻辑实现，允许正常提交情况下，返回true，否则返回false
     * 调用信用接口查询信用
     * @param request
     * @return true:正常提交;false:不允许提交，提示错误
     */
    private resultInfo validate(ApprovalEventRequest request) {
        boolean result = true;
        String errMsg = "";
        // 信用超限
        BigDecimal blvNum = null;
        List<String> customerCodeList = new ArrayList<>();
        String sql = "select id,isClosed__c,po,Overdue__c,payment_Term__c,currencyUnit,Order_type__c," +
                "amount,initAmount,listTotal,totalDiscountAmount," + // 售价、原价、折扣
				"accountId.Customer_Level__c,accountId.Available_Credit_Limit__c,accountId.Overdue_Days_Formula__c , " + // 20250830 ljh
                "Actual_available_Credit_Limit__c,ownerId,Status__c,Customer_Sales_Info__c.Terms_Of_Payment__c,accountId,accountId.accountName,accountId.totalReceivableLocalSum__c,blvNum__c,Customer_Sales_Info__c," +
                "entityType,User_Country__c,accountId.MDG_Customer_Code__c " +
                "from _order where id = " + Long.valueOf(request.getDataId());
        ReturnResult result1 = null;
        try {
            Order o = new Order();

            RespCustomerInfo info = new RespCustomerInfo();
            info.setExch_rate("");
            RkhdHttpClient rkhdclient = RkhdHttpClient.instance();
            // TODO
/*            QueryResult<Order> oResult = XObjectService.instance().query(sql);
            if(!oResult.getSuccess()) {
                throw new Exception("获取订单错误" + oResult.getErrorMessage());
            }
            logger.warn("提交前校验逻辑===Success");
            logger.error("提交前校验逻辑===error:ces" + oResult);*/
            QueryResult orderQuery = XoqlService.instance().query(sql,true);
            JSONArray orderinfo = JSONArray.parseArray(orderQuery.getRecords().toString());
			// 20250916 ljh 调整Id赋值提前
			if(orderinfo.getJSONObject(0) != null) {
                o.setId(orderinfo.getJSONObject(0).getLong("id"));
            }
			// 20250825 ljh 跳过退货类型订单  Overseas Return  ZORE
			logger.error("Order_type__c===:"+orderinfo.getJSONObject(0).getJSONArray("Order_type__c").getString(0));
            if(orderinfo.getJSONObject(0).getJSONArray("User_Country__c") != null &&
            "Uganda".equals(orderinfo.getJSONObject(0).getJSONArray("User_Country__c").getString(0))
			&& !"Overseas Return".equals(orderinfo.getJSONObject(0).getJSONArray("Order_type__c").getString(0))
			){
                result1 = ProductSalesInfoSyncToSAP.productSalesInfo1(request.getDataId().toString(),"ProductSalesInfoSyncToSAP");
                if (result1.getIsSuccess()){
                    logger.error("cjw===success");
                }else {
                    resultInfo info123 = new resultInfo();
                    info123.setSuccess(false);
                    info123.setErrMsg(result1.getMessage());
                    logger.error("cjw===error" + result1.getMessage());
                    return info123;
                }
            }
            logger.error("提交前校验逻辑===error:" + orderinfo);
            // 20241225   zyh   逾期字段修改-取客户信用对象的   start
            //Boolean overdue = false;
            BigDecimal allAmount = new BigDecimal(0);
            if (!"".equals(orderinfo.getJSONObject(0).getLong("accountId"))){
				// 20250916 逾期天数从方法中提取出来 start
				long day = 0L;
				if (orderinfo.getJSONObject(0).getDouble("accountId.Overdue_Days_Formula__c") != null  
				&& orderinfo.getJSONObject(0).getDouble("accountId.Overdue_Days_Formula__c") > 0){
					day = Long.valueOf(Math.round(orderinfo.getJSONObject(0).getDouble("accountId.Overdue_Days_Formula__c")));
				} else {
					day = 0L;
				}
				o.setDueDays__c(day);
				// 20250916 逾期天数从方法中提取出来 end
                String acSql = "select id,Overdue__c,overDueDays__c,Customer__c,Actual_Credit_limit__c from Customer_Credit_Info__c where Customer__c = " + orderinfo.getJSONObject(0).getLong("accountId");
                QueryResult acRes = XObjectService.instance().query(acSql,true);
                if (acRes.getSuccess()){
                    if (acRes.getRecords().size() > 0){
                        JSONArray acInfo = JSONArray.parseArray(acRes.getRecords().toString());
                        logger.error("客户应收查出来的信息" + acInfo);
                        logger.info("123123===" + acInfo);
                        //if (acInfo != null && acInfo.getJSONObject(0).getBoolean("Overdue__c") != overdue){
                        //    overdue = acInfo.getJSONObject(0).getBoolean("Overdue__c");
                        // }
                        if (acInfo.getJSONObject(0).getDouble("Actual_Credit_limit__c") != null) {
                            allAmount = BigDecimal.valueOf(acInfo.getJSONObject(0).getDouble("Actual_Credit_limit__c"));
                        }
						customerCodeList.add(orderinfo.getJSONObject(0).getString("accountId.MDG_Customer_Code__c"));// 20250906 ljh 不判断付款方式了，看是否有信用视图
                    } else {
                        logger.error("客户应收查出来的数量" + acRes.getRecords().size() + "===客户ID===" + orderinfo.getJSONObject(0).getLong("accountId"));
                    }
                }else {
                    logger.info("666===" + acRes.getErrorMessage());
                }
				//孟加拉的逾期超过30天不允许提交审批 20250828 ljh 排除退货
				if (!"Overseas Return".equals(orderinfo.getJSONObject(0).getJSONArray("Order_type__c").getString(0))
					&& orderinfo.getJSONObject(0).getJSONArray("User_Country__c") != null 
					&& ("Bangla".equals(orderinfo.getJSONObject(0).getJSONArray("User_Country__c").getString(0)) 
						|| "Bangladesh".equals(orderinfo.getJSONObject(0).getJSONArray("User_Country__c").getString(0))) 
					&& orderinfo.getJSONObject(0).getDouble("accountId.Overdue_Days_Formula__c") != null 
					&& orderinfo.getJSONObject(0).getDouble("accountId.Overdue_Days_Formula__c") > 30 
					&&("B".equals(orderinfo.getJSONObject(0).getJSONArray("accountId.Customer_Level__c").getString(0))  
						||"C".equals(orderinfo.getJSONObject(0).getJSONArray("accountId.Customer_Level__c").getString(0)))
				){
					// 20250830 ljh 加等级
					throw new RuntimeException("Customer overdue more than 30 days,please contact your supervisor");
				}
            }
            // 20241225   zyh   逾期字段修改-取客户信用对象的   end
            // 20250114   zyh   添加验证码判断功能   start
            /*if (!"".equals(orderinfo.getJSONObject(0).getString("verification_codebackend__c"))
                && !orderinfo.getJSONObject(0).getString("verification_codebackend__c").equals(orderinfo.getJSONObject(0).getString("verification_code__c"))){
                logger.error("生成的四位数验证码为: " + orderinfo.getJSONObject(0).getString("verification_codebackend__c"));
                logger.error("输入的四位数验证码为: " + orderinfo.getJSONObject(0).getString("verification_code__c"));
                resultInfo info1 = new resultInfo();
                info1.setSuccess(false);
                info1.setErrMsg("Verification code error");
                return info1;
            }*/
            // 20250114   zyh   添加验证码功能   start
            // TODO
            // if (orderinfo.getJSONObject(0) != null && orderinfo.getJSONObject(0).getJSONArray("Status__c") != null &&  !"Draft".equals(orderinfo.getJSONObject(0).getJSONArray("Status__c").get(0))){
               /* if (orderinfo.getJSONObject(0) != null && orderinfo.getJSONObject(0).getJSONArray("Status__c") != null &&  "Draft".equals(orderinfo.getJSONObject(0).getJSONArray("Status__c").get(0))){
                result = true;
            } else {
                logger.error("状态不对===error:" + orderinfo.getJSONObject(0).getString("Status__c"));
                errMsg += "The status is not equal to that in the draft";
            }*/
            // TODO
            if (orderinfo.getJSONObject(0) != null && orderinfo.getJSONObject(0).getString("accountId.MDG_Customer_Code__c") != null && !"".equals(orderinfo.getJSONObject(0).getString("accountId.MDG_Customer_Code__c"))){
                // 20250906 ljh 不判断付款方式了，看是否有信用视图 start
				/*if (!"Return order".equals(NeoCrmRkhdService.v2QueryRecordInfoByIdXobject(rkhdclient,"order",Long.valueOf(request.getDataId())).getString("entityType-label")) &&
                        //S502 Payable immediately Due net
                        ((orderinfo.getJSONObject(0).getJSONArray("payment_Term__c") != null &&
							(!"S500 Cash in Advance-BD".equals(orderinfo.getJSONObject(0).getJSONArray("payment_Term__c").get(0)) 
							&& !"S502 Payable immediately Due net".equals(orderinfo.getJSONObject(0).getJSONArray("payment_Term__c").get(0))
							&& !"S498 Cash in Advance-BD".equals(orderinfo.getJSONObject(0).getJSONArray("payment_Term__c").get(0))
							)
						))
					) { // 20241226   zyh   判断销售试图支付方式
					// 20250828 ljh 临时S498不查询信用
                    customerCodeList.add(orderinfo.getJSONObject(0).getString("accountId.MDG_Customer_Code__c"));
                }*/
				// 20250906 ljh 不判断付款方式了，看是否有信用视图 end
				// else if ("7020 Rainbow Agro(Nigeria)".equals(orderinfo.getJSONObject(0).getJSONArray("Sales_Org__c").getString(0))){
//                    // 款清功能↓↓↓↓
//                    // 查询收款单
//                    String payOrdSql = "select id,orderId,amount from paymentApplication where orderId = " + orderinfo.getJSONObject(0).getLong("id");
//                    QueryResult payOrdQuery = XObjectService.instance().query(payOrdSql,true);
//                    if (payOrdQuery.getSuccess()){
//                        if (payOrdQuery.getRecords().size() > 0){
//                             // 存储收款单的金额
//                            BigDecimal payNum = new BigDecimal(0);
//                            JSONArray payOrdInfo = JSONArray.parseArray(payOrdQuery.getRecords().toString());
//                            for (int i = 0; i < payOrdInfo.size(); i++){
//                                JSONObject js = payOrdInfo.getJSONObject(i);
//                                payNum = payNum.add(new BigDecimal(js.getDouble("amount")));
//                            }
//                            // 如果收款单金额小于订单金额
//                            if (payNum.compareTo(new BigDecimal(orderinfo.getJSONObject(0).getDouble("amount"))) < 0){
//                                // 对比客户应收总额
//                                if (new BigDecimal(orderinfo.getJSONObject(0).getDouble("accountId.totalReceivableLocalSum__c").toString()).compareTo(new BigDecimal(0)) > 0) {
//                                    resultInfo resInfo = new resultInfo();
//                                    resInfo.setSuccess(false);
//                                    resInfo.setErrMsg("The customer still has outstanding cash order payments. Please contact the finance department to settle the accounts in a timely manner.");
//                                    return resInfo;
//                                    // throw new RuntimeException("The customer still has outstanding cash order payments. Please contact the finance department to settle the accounts in a timely manner.");
//                                }
//                            }/*
//                            // 如果客户应收
//                            if (new BigDecimal(orderinfo.getJSONObject(0).getDouble("accountId.totalReceivableLocalSum__c").toString()).compareTo(new BigDecimal(0)) < 0){
//                                if (new BigDecimal(orderinfo.getJSONObject(0).getDouble("accountId.totalReceivableLocalSum__c").toString()).)
//                            } else {
//                                // 对比客户应收总额
//                                if (payNum.compareTo(new BigDecimal(orderinfo.getJSONObject(0).getDouble("accountId.totalReceivableLocalSum__c").toString())) < 0) {
//                                    throw new RuntimeException("This order is not settled and cannot be submitted for approval.");
//                                }
//                            }*/
//                        } else {
//                            if (orderinfo.getJSONObject(0).getDouble("accountId.totalReceivableLocalSum__c") != null && new BigDecimal(orderinfo.getJSONObject(0).getDouble("accountId.totalReceivableLocalSum__c").toString()).compareTo(new BigDecimal(0)) > 0) {
//                                resultInfo resInfo = new resultInfo();
//                                resInfo.setSuccess(false);
//                                resInfo.setErrMsg("The customer still has outstanding cash order payments. Please contact the finance department to settle the accounts in a timely manner.");
//                                return resInfo;//throw new RuntimeException("The customer still has outstanding cash order payments. Please contact the finance department to settle the accounts in a timely manner.");
//                            }
//                        }
//                    }
//                }
            }
            if (customerCodeList.size() > 0){
                // 调用信用查询接口
                GetCustomerlimitFROMSAP toSap = new GetCustomerlimitFROMSAP();
                info = toSap.submitToSAP(customerCodeList,"");
                if (orderinfo.getJSONObject(0).getJSONArray("currencyUnit") != null && ("美元".equals(orderinfo.getJSONObject(0).getJSONArray("currencyUnit").get(0)) || "US Dollar".equals(orderinfo.getJSONObject(0).getJSONArray("currencyUnit").get(0)))){
                    info.setExch_rate("1");
                }
                logger.error("测试6666666");
                logger.error("测试6666666" + info.toString());
                if ("S".equals(info.getMsgty()) && info.getAmount() != null ){
					logger.error(String.valueOf("测试6666666" + info.getAmount().doubleValue()));
					logger.error(String.valueOf("测试6666666" + orderinfo.getJSONObject(0).getDouble("amount")));
					logger.error(String.valueOf("测试6666666" + info.getExch_rate()));
					logger.error(String.valueOf("测试6666666====" + new BigDecimal(info.getExch_rate())));
					BigDecimal exchangeRate = new BigDecimal(info.getExch_rate());
					BigDecimal orderAmountDecimal = new BigDecimal(orderinfo.getJSONObject(0).getDouble("amount"));
					int scale = 2; // 假设我们希望结果保留两位小数
					RoundingMode roundingMode = RoundingMode.HALF_UP; // 四舍五入
					logger.error("测试6666666==" + orderAmountDecimal.divide(exchangeRate,scale,roundingMode));
					// 信用金额等于原金额减去订单金额
					blvNum = info.getAmount().subtract(orderAmountDecimal.divide(exchangeRate,scale,roundingMode));
					o.setSAP_Credit_Limit__c(info.getAmount().doubleValue()); // 赋值SAP可用额度 20241228   zyh add
					Double rate = null;
					if (info != null && !"".equals(info.getExch_rate()) && !"null".equals(info.getExch_rate())){
						logger.error("20241229001：：：" + info.getExch_rate());
						rate = Double.valueOf(info.getExch_rate()); // 存储汇率 // 20241224   zyh   汇率更换字段
						logger.error("20241229001：：：" + o);
					}else {
						rate = 0.00;
					}
					Double localVal = info.getAmount().doubleValue() * rate;
					o.setSAP_Credit_Limit_Local__c(localVal);
					// 20250827 ljh 信用订单取的是接口实时 start
					if (StringUtils.isNotBlank(info.getZsfyq())) {
						o.setOverdue__c(true);
					} else {
						o.setOverdue__c(false);
					}
					// 20250827 ljh 信用订单取的是接口实时 end
                } else {
                    // TODO
                    throw new Exception(info.getMsgtx());
                }
            }
            logger.error("测试5656");
			// 20250819 LJH 优化where条件
            String kcSql = "select id,Status__c,amount,accountId from _order where (Status__c = 2 or Sync_Status__c = 2) and Order_type__c = 1 and SAP_Order_Code__c = null and accountId = " + orderinfo.getJSONObject(0).getLong("accountId");
            QueryResult<Order> numRes = XObjectService.instance().query(kcSql,true);
            if(!numRes.getSuccess()) {
                throw new Exception("Order query error:" + numRes.getErrorMessage());
            }
            logger.error("测试5656+" + numRes.getSuccess());
            BigDecimal tempAppingNum = new BigDecimal(0); // 用于存储申请中的价格
            if (numRes.getSuccess()){
                for (Order or : numRes.getRecords()){
                    logger.error("测试5656++" + numRes.getSuccess());
                    if (blvNum != null && or.getAmount() != null && info.getExch_rate() != null){
                        BigDecimal exchangeRate = new BigDecimal(info.getExch_rate());
                        BigDecimal orderAmountDecimal = new BigDecimal(or.getAmount());
                        logger.error("12312313=1=" + exchangeRate);
                        logger.error("12312313=2=" + orderAmountDecimal);
                        int scale = 2; // 假设我们希望结果保留两位小数
                        RoundingMode roundingMode = RoundingMode.HALF_UP; // 四舍五入
                        // 信用金额等于原金额减去审批中的订单金额
                        blvNum = blvNum.subtract(orderAmountDecimal.divide(exchangeRate,scale,roundingMode));
                        tempAppingNum = tempAppingNum.add(orderAmountDecimal.divide(exchangeRate,scale,roundingMode)); // 存储申请中的金额
                    }
                    logger.error("测试5656+6+" + numRes.getSuccess());
                    logger.error("存储申请中的金额" + tempAppingNum);
                }
            }
            if(orderinfo.getJSONObject(0) != null) {
                o.setCRM_Approving_Credit__c(tempAppingNum.doubleValue());
            }
            logger.error("测试6666666+" + o);
            if (blvNum != null && blvNum.compareTo(new BigDecimal(0)) < 0){
                logger.error("测试6666666+++" + blvNum);
                // 如果信用金额小于0---即超限，那么就去返数
                blvNum = blvNum.multiply(new BigDecimal(-1)); // 超限金额
                logger.error("超限金额" + blvNum);
                // 计算超限率
                if (allAmount.compareTo(new BigDecimal(0)) != 0){
                    o.setExceedNum__c(blvNum.divide(allAmount,2,RoundingMode.HALF_UP).doubleValue());/*.multiply(new BigDecimal(0.01))*/
                }
                logger.error("超限率" + blvNum + "=" + allAmount);
                o.setBlvNum__c(blvNum.doubleValue()); // 存储超限金额
                o.setUltra_Credit__c(true);
            } else{
                o.setExceedNum__c(0.0);/*.multiply(new BigDecimal(0.01))*/
                o.setBlvNum__c(0.0); // 存储超限金额
                o.setUltra_Credit__c(false);
            }
			// 20250830 ljh 孟家拉 超额 start
			BigDecimal aclAmount = null;
			if(orderinfo.getJSONObject(0).getDouble("accountId.Available_Credit_Limit__c") != null){
				aclAmount = BigDecimal.valueOf(orderinfo.getJSONObject(0).getDouble("accountId.Available_Credit_Limit__c"));
			}else{
				aclAmount = new BigDecimal(0);
			}
			BigDecimal blvNumOver = null;
			BigDecimal levAmount = null;
			if (blvNum != null && blvNum.compareTo(new BigDecimal(0)) < 0){
                blvNumOver = blvNum.multiply(new BigDecimal(-1)); // 超限金额 
				levAmount= aclAmount.add(blvNumOver);				
            } else{
                levAmount = aclAmount;
            }
			BigDecimal levAmount_S = new BigDecimal(75000);
			if("0001020304".equals(orderinfo.getJSONObject(0).getString("accountId.MDG_Customer_Code__c"))){
				levAmount_S = new BigDecimal(150000);
			}
			if("0002002179".equals(orderinfo.getJSONObject(0).getString("accountId.MDG_Customer_Code__c"))){
				levAmount_S = new BigDecimal(100000);
			}
			BigDecimal levAmount_A = new BigDecimal(25000);
			BigDecimal levAmount_B = new BigDecimal(10000);
			BigDecimal levAmount_C = new BigDecimal(5000);
			if (!"Overseas Return".equals(orderinfo.getJSONObject(0).getJSONArray("Order_type__c").getString(0))
				&& orderinfo.getJSONObject(0).getJSONArray("User_Country__c") != null 
				&& ("Bangla".equals(orderinfo.getJSONObject(0).getJSONArray("User_Country__c").getString(0)) 
					|| "Bangladesh".equals(orderinfo.getJSONObject(0).getJSONArray("User_Country__c").getString(0))
					)
				&&((orderinfo.getJSONObject(0).getJSONArray("payment_Term__c") != null &&
						(!"S500 Cash in Advance-BD".equals(orderinfo.getJSONObject(0).getJSONArray("payment_Term__c").get(0)) 
						&& !"S502 Payable immediately Due net".equals(orderinfo.getJSONObject(0).getJSONArray("payment_Term__c").get(0))
						&& !"S498 Cash in Advance-BD".equals(orderinfo.getJSONObject(0).getJSONArray("payment_Term__c").get(0))
						)
					))
				&&(
					("S".equals(orderinfo.getJSONObject(0).getJSONArray("accountId.Customer_Level__c").getString(0)) 
						&& levAmount.compareTo(levAmount_S) > 0 )
					|| ("A".equals(orderinfo.getJSONObject(0).getJSONArray("accountId.Customer_Level__c").getString(0)) 
						&& levAmount.compareTo(levAmount_A) > 0)
					|| ("B".equals(orderinfo.getJSONObject(0).getJSONArray("accountId.Customer_Level__c").getString(0)) 
						&& levAmount.compareTo(levAmount_B) > 0)
					|| ("C".equals(orderinfo.getJSONObject(0).getJSONArray("accountId.Customer_Level__c").getString(0)) 
						&& levAmount.compareTo(levAmount_C) > 0)	
				)
			){
				// MDG_Customer_Code__c 1020304 和 2002179
				throw new RuntimeException("credit exceed the rule, please contact your supervisor");
			}
			// 20250830 ljh 孟家拉 超额 end
            logger.error("测试6666666++" + o);
            logger.error("20241229001：：：" + info.getExch_rate());
//            logger.error(String.valueOf("20241229001：null：：" + "null".equals(info.getExch_rate().toString())));
//            o.setStatus__c(2);
            // TODO 转换后金额有公式字段，不需要这个了
            /*if (info.getExch_rate() != null && orderinfo.getJSONObject(0) != null){
                BigDecimal exchangeRate = new BigDecimal(info.getExch_rate());
                BigDecimal orderAmountDecimal = new BigDecimal(orderinfo.getJSONObject(0).getDouble("amount"));
                int scale = 2; // 假设我们希望结果保留两位小数
                RoundingMode roundingMode = RoundingMode.HALF_UP; // 四舍五入
                o.setAfterAmount__c(orderAmountDecimal.divide(exchangeRate,scale,roundingMode).doubleValue());
            }*/
            if (info != null && !"".equals(info.getExch_rate()) && !"null".equals(info.getExch_rate())){
                logger.error("20241229001：：：" + info.getExch_rate());
                o.setRate__c(Double.valueOf(info.getExch_rate())); // 存储汇率 // 20241224   zyh   汇率更换字段
                logger.error("20241229001：：：" + o);
            }
            QueryResult<Customer_Credit_Info__c> infoQuery = XObjectService.instance().query("select Id from Customer_Credit_Info__c where Customer__c = " + o.getAccountId() + " order by updatedAt desc limit 1");
            if (infoQuery.getSuccess() && 0 < infoQuery.getTotalCount()){
                o.setActual_Credit_limit_Local__c(infoQuery.getRecords().get(0).getLocal_Credit_Limit__c());
            }
            OperateResult update = XObjectService.instance().update(o,true);
            if(!update.getSuccess()) {
                throw new Exception("Order query error:" + update.getErrorMessage());
            }
            logger.error("更新成功666："+update.getSuccess().toString());
            logger.error("更新成功666："+o);
            // logger.error("状态不对===error:" + orderinfo.getJSONObject(0).getString("Status__c"));
            // logger.error("状态不对===error:" + "Draft".equals(orderinfo.getJSONObject(0).getJSONArray("Status__c").get(0)));
        } catch (Exception e) {
            // logger.error("测试订单明细:::" + e.getStackTrace()[0].getLineNumber() + e.getMessage() + e.getStackTrace()[0].getClassName());
            errMsg += e.getMessage();
            logger.error( "*****" + e.getStackTrace()[0].getLineNumber() + "****" + e.getStackTrace()[0].getClassName());
            //throw new RuntimeException(e + "*****"+e.getStackTrace()[0].getLineNumber() + "****" + e.getStackTrace()[0].getClassName());
            if (e.toString().contains("No result")){
                throw new RuntimeException("Credit information query error");
            } else {
                throw new RuntimeException(e);
            }
        }

        logger.warn("提交前校验逻辑");
        resultInfo info = new resultInfo();
        info.setSuccess(result);
        info.setErrMsg(errMsg);

        return info;
    }
    /*public static Boolean linshi() throws ApiEntityServiceException, IOException, ScriptBusinessException, InterruptedException, XsyHttpException {
        RkhdHttpClient rkhdclient = RkhdHttpClient.instance();
        String tempOr = "select id,SAP_Order_Code__c from _order where SAP_Order_Code__c != ''";
        JSONArray tempOrList = NeoCrmRkhdService.xoql(rkhdclient, tempOr);
        Map<Long,String> oId = new HashMap<>();
        if (tempOrList.size() > 0){
            for (int i = 0; i < tempOrList.size();i++){
                oId.put(tempOrList.getJSONObject(i).getLong("id"),tempOrList.getJSONObject(i).getString("SAP_Order_Code__c"));
            }
        }
        if (oId.keySet().size() > 0){
            List<String> strList = new ArrayList<>();
            for (Long id : oId.keySet()){
                strList.add(id.toString());
            }
            String oidTemp = SqlFormatUtils.joinInSql(strList);
            String tempOp = "select id,orderId,External_Id__c,SAP_ItemNum__c,orderId.SAP_Order_Code__c from orderProduct where orderId IN(" + oidTemp + ")";
            JSONArray tempOpList = NeoCrmRkhdService.xoql(rkhdclient, tempOp);
            List<OrderProduct> tempUpOp = new ArrayList<>();
            if (tempOpList.size() > 0){
                for (int i = 0; i < tempOpList.size();i++){
                    OrderProduct op = new OrderProduct();
                    op.setId(tempOpList.getJSONObject(i).getLong("id"));
                    op.setExternal_Id__c(tempOpList.getJSONObject(i).getString("orderId.SAP_Order_Code__c") + "_" + tempOpList.getJSONObject(i).getString("SAP_ItemNum__c"));
                    tempUpOp.add(op);
                }
                logger.error("临时用更新数据：" + tempUpOp.size());
                logger.error("临时用更新数据：" + tempUpOp);
                BatchOperateResult upOp = XObjectService.instance().update(tempUpOp,false,true);
            }

        }
        return true;
    }*/
    public class resultInfo{
        public Boolean isSuccess = true;
        public String errMsg = "";

        public Boolean getSuccess() {
            return isSuccess;
        }

        public void setSuccess(Boolean success) {
            isSuccess = success;
        }

        public String getErrMsg() {
            return errMsg;
        }

        public void setErrMsg(String errMsg) {
            this.errMsg = errMsg;
        }
    }
    // 获取选项列表值code
    public static Map<String,Integer> getGlobalPicks(String globalPickApiKey, RkhdHttpClient client) {
        Map<String,Integer> map = new HashMap<String,Integer>();
        try {
            if (client == null) {
                client = RkhdHttpClient.instance();
            }
            RkhdHttpData data = new RkhdHttpData();
            data.setCall_type("GET");
            data.setCallString("/rest/metadata/v2.0/settings/globalPicks/" + globalPickApiKey);
            String responseStr = client.execute(data, ResponseBodyHandlers.ofString());

            if (StringUtils.isNotBlank(responseStr)) {
                logger.info("查询列表结果：" + responseStr);
                JSONObject responseObject = JSONObject.parseObject(responseStr);
                Integer responseCode = responseObject.getIntValue("code");
                if (responseCode.equals(0)) {
                    String resultStr = responseObject.getString("data");
                    JSONObject dataJson = JSONObject.parseObject(resultStr);
                    String records = dataJson.getString("records");
                    JSONArray pickOptions = JSONObject.parseObject(records).getJSONArray("pickOption");
                    for (int i = 0; i < pickOptions.size(); i++) {
                        JSONObject pickOption = pickOptions.getJSONObject(i);
                        map.put(pickOption.getString("optionApiKey"),pickOption.getInteger("optionCode"));
                    }
                }
            }
        } catch (Exception e) {
            logger.error(" 报错信息：" + e);
        }
        return map;
    }
    public static void main(String[] args) throws Exception {
//        RkhdHttpClient client = RkhdHttpClient.instance();
//        RkhdHttpData data = new RkhdHttpData();
//        data.setCallString("/rest/metadata/v2.0/settings/systemSettings/currencies/currencyUnit6");
//        data.setCall_type("PATCH");
//        data.setBody("{\"rate\": 0.061}");
//        String responseStr = client.performRequest(data);
//        JSONObject js = JSONObject.parseObject(responseStr);
//        System.out.println(js);
        String sql = "select id,isClosed__c,po,Overdue__c,payment_Term__c,currencyUnit," +
                "amount,initAmount,listTotal,totalDiscountAmount," + // 售价、原价、折扣
                "Actual_available_Credit_Limit__c,ownerId,Status__c,Customer_Sales_Info__c.Terms_Of_Payment__c,accountId,accountId.accountName,accountId.totalReceivableLocalSum__c,blvNum__c,Customer_Sales_Info__c," +
                "entityType,User_Country__c,accountId.MDG_Customer_Code__c " +
                "from _order where id in(" + 3696410049431620L + ")";
        QueryResult aa = XoqlService.instance().query(sql,true);
        RkhdHttpClient rkhdclient = RkhdHttpClient.instance();
        JSONArray orderinfo = NeoCrmRkhdService.xoql(rkhdclient,sql);
        System.out.println(aa.getRecords());
        System.out.println(orderinfo);
        System.out.println(JSONArray.parseArray(aa.getRecords().toString()));
    }
}
