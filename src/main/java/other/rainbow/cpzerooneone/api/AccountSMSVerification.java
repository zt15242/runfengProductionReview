package other.rainbow.cpzerooneone.api;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rkhd.platform.sdk.api.annotations.RequestMethod;
import com.rkhd.platform.sdk.api.annotations.RestApi;
import com.rkhd.platform.sdk.api.annotations.RestBeanParam;
import com.rkhd.platform.sdk.api.annotations.RestMapping;
import com.rkhd.platform.sdk.data.model.Account;
import com.rkhd.platform.sdk.data.model.Customer_Info_Update__c;
import com.rkhd.platform.sdk.data.model.Order;
import com.rkhd.platform.sdk.exception.ApiEntityServiceException;
import com.rkhd.platform.sdk.http.*;
import com.rkhd.platform.sdk.log.Logger;
import com.rkhd.platform.sdk.log.LoggerFactory;
import com.rkhd.platform.sdk.model.OperateResult;
import com.rkhd.platform.sdk.model.QueryResult;
import com.rkhd.platform.sdk.service.XObjectService;
import other.rainbow.cpzerooneone.common.NeoCrmRkhdService;
import other.rainbow.cpzerooneone.pojo.RequestResultInfo;
import other.rainbow.cpzerooneone.pojo.ReturnResult;
import other.rainbow.cpzerooneone.pojo.SMSRequest;
import other.rainbow.cpzerooneone.pojo.SMSResponse;

import java.io.IOException;
import java.util.Base64;

import java.util.concurrent.ThreadLocalRandom;

@RestApi(baseUrl = "/button")
public class AccountSMSVerification {
    private static final Logger logger = LoggerFactory.getLogger();
    @RestMapping(value = "/AccountSMSVerification", method = RequestMethod.POST)
    public static String AccountSMSVerification(@RestBeanParam(name = "data") String param) throws ApiEntityServiceException, IOException {
        ReturnResult result = new ReturnResult();
        String url = "http://120.224.116.35:8916/send/international/sms";
        String method = "POST";
        Boolean dataCheck = false;
        String messages = "";
        CommonResponse<JSONObject> Responseresult = null;
        logger.info("param="+param);
        logger.error("进来调接口param===" + param);
        JSONObject jsonObject = JSONObject.parseObject(param);
        String  id = jsonObject.getString("id");
        String code = generateRandom6DigitCode();
        logger.info("code="+code);
        Account account = new Account();
        account.setId(Long.valueOf(id));
        account.setSys_verification_code__c(code);
        OperateResult update = XObjectService.instance().update(account,true);
        if (!update.getSuccess()){
            result.setIsSuccess(false);
            result.setMessage(update.getErrorMessage());
            return JSONObject.toJSONString(result);
        }

        String accSql = "select id,Telephone__c,accountName from account where id = " + id;
        QueryResult accQuery = XObjectService.instance().query(accSql,true);
        // 修改：替换为CommonHttpClient
        CommonHttpClient client = CommonHttpClient.instance();
        CommonData request = new CommonData();
        logger.info("accQuery="+JSONObject.toJSONString(accQuery));
        if (accQuery.getSuccess() && accQuery.getRecords().size() > 0) {
            Account obj = (Account) accQuery.getRecords().get(0);
            String telephone = obj.getTelephone__c();
            // 新增：如果手机号以+开头则去除
            if (telephone != null && telephone.startsWith("+")) {
                telephone = telephone.substring(1);
            }
            String accountName = obj.getAccountName();


             messages = "【Rainbow】Dear " + accountName + ".Your verification code is " + code + ". Please forward to relevant Rainbow Personnel only and do not disclose to any other party. Thank you.";
            // 添加：设置Basic Auth鉴权头
            String username = "xsy-crm01";
            String password = "Abcd1234";
            String auth = username + ":" + password;
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
            request.addHeader("Authorization", "Basic " + encodedAuth);
            request.addHeader("Content-Type", "application/json;charset=UTF-8");
            request.setCallString(url);
            request.setCall_type(method);

            RequestResultInfo requestinfo = new RequestResultInfo();
            SMSRequest smsRequest = new SMSRequest();
            requestinfo.setBusinessId("AccountSMS"+System.currentTimeMillis() + code);
            requestinfo.setMobile(telephone);
            requestinfo.setMessage(messages);
            // 构建请求体

            smsRequest.setResultInfo(requestinfo);
            request.setBody(JSONObject.toJSONString(smsRequest));
            logger.info("smsRequest="+JSONObject.toJSONString(smsRequest));
            logger.info("request="+JSONObject.toJSONString(request));


            // 执行请求并处理响应
            try {
                Responseresult = client.execute(request, JSONObject::parseObject);
                String response1 = JSONObject.toJSONString(Responseresult);
                logger.info("response1="+response1);

                // 获取响应数据中的data对象
                JSONObject data = Responseresult.getData();
                if (data != null) {
                    // 从data中获取resultinfo对象
                    JSONObject resultinfo = data.getJSONObject("resultinfo");
                    if (resultinfo != null) {
                        int resultCode = resultinfo.getIntValue("code");
                        String resultMsg = resultinfo.getString("msg");

                        if (resultCode == 0) {
                            result.setIsSuccess(true);
                            result.setMessage("短信发送成功");
                        } else {
                            result.setIsSuccess(false);
                            result.setMessage("短信发送失败:" + resultMsg);
                        }
                    } else {
                        result.setIsSuccess(false);
                        result.setMessage("响应数据中未找到resultinfo");
                    }
                } else {
                    result.setIsSuccess(false);
                    result.setMessage("响应数据中未找到data");
                }
            } catch (Exception e) {
                logger.error("HTTP请求异常", e);
                result.setIsSuccess(false);
                result.setMessage("短信发送异常:" + e.getMessage());
            }
        }
        logger.info("jsonObject="+jsonObject.toJSONString());
        return JSONObject.toJSONString(result);
    }
    @RestMapping(value = "/CustomerInfoUpdateSMSVerification", method = RequestMethod.POST)
    public static String CustomerInfoUpdateSMSVerification(@RestBeanParam(name = "data") String param) throws Exception {
        ReturnResult result = new ReturnResult();
        String url = "http://120.224.116.35:8916/send/international/sms";
        String method = "POST";
        Boolean dataCheck = false;
        String messages = "";
        CommonResponse<JSONObject> Responseresult = null;
        logger.info("param="+param);
        logger.error("进来调接口param===" + param);
        JSONObject jsonObject = JSONObject.parseObject(param);
        String  id = jsonObject.getString("id");
        String code = generateRandom6DigitCode();
        logger.info("code="+code);
        Customer_Info_Update__c account = new Customer_Info_Update__c();
        account.setId(Long.valueOf(id));
        account.setSys_verification_code__c(code);
        OperateResult update = XObjectService.instance().update(account,true);
        if (!update.getSuccess()){
            result.setIsSuccess(false);
            result.setMessage(update.getErrorMessage());
            return JSONObject.toJSONString(result);
        }

        String accSql = "select id,account__c.Telephone__c,account__c.accountName from customer_Info_Update__c where id = " + id;
        JSONArray accQuery = NeoCrmRkhdService.executeSqlQuery(accSql);
        logger.info("accQuery="+JSONObject.toJSONString(accQuery));
        CommonHttpClient client = CommonHttpClient.instance();
        CommonData request = new CommonData();
        if (!accQuery.isEmpty()) {
            JSONObject obj = accQuery.getJSONObject(0);
            String telephone = obj.getString("account__c.Telephone__c");
            String accountName = obj.getString("account__c.accountName");
            messages = "【Rainbow】Dear " + accountName + ".Your verification code is " + code + ". Please forward to relevant Rainbow Personnel only and do not disclose to any other party. Thank you.";

            // 添加：设置Basic Auth鉴权头
            String username = "xsy-crm01";
            String password = "Abcd1234";
            String auth = username + ":" + password;
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
            request.addHeader("Authorization", "Basic " + encodedAuth);
            request.addHeader("Content-Type", "application/json;charset=UTF-8");
            request.setCallString(url);
            request.setCall_type(method);

            RequestResultInfo requestinfo = new RequestResultInfo();
            SMSRequest smsRequest = new SMSRequest();
            requestinfo.setBusinessId("CustomerInfoUpdateSMS"+System.currentTimeMillis() + code);
            requestinfo.setMobile(telephone);
            requestinfo.setMessage(messages);
            // 构建请求体

            smsRequest.setResultInfo(requestinfo);
            request.setBody(JSONObject.toJSONString(smsRequest));
            logger.info("smsRequest="+JSONObject.toJSONString(smsRequest));
            logger.info("request="+JSONObject.toJSONString(request));


            // 执行请求并处理响应
            try {
                Responseresult = client.execute(request, JSONObject::parseObject);
                String response1 = JSONObject.toJSONString(Responseresult);
                logger.info("response1="+response1);

                // 获取响应数据中的data对象
                JSONObject data = Responseresult.getData();
                if (data != null) {
                    // 从data中获取resultinfo对象
                    JSONObject resultinfo = data.getJSONObject("resultinfo");
                    if (resultinfo != null) {
                        int resultCode = resultinfo.getIntValue("code");
                        String resultMsg = resultinfo.getString("msg");

                        if (resultCode == 0) {
                            result.setIsSuccess(true);
                            result.setMessage("短信发送成功");
                        } else {
                            result.setIsSuccess(false);
                            result.setMessage("短信发送失败:" + resultMsg);
                        }
                    } else {
                        result.setIsSuccess(false);
                        result.setMessage("响应数据中未找到resultinfo");
                    }
                } else {
                    result.setIsSuccess(false);
                    result.setMessage("响应数据中未找到data");
                }
            } catch (Exception e) {
                logger.error("HTTP请求异常", e);
                result.setIsSuccess(false);
                result.setMessage("短信发送异常:" + e.getMessage());
            }
        }
        logger.info("jsonObject="+jsonObject.toJSONString());
        return JSONObject.toJSONString(result);
    }
//    @RestMapping(value = "/OrderSMSVerification", method = RequestMethod.POST)
//    public static String SMSVerification(@RestBeanParam(name = "data") String param) throws Exception {
//        ReturnResult result = new ReturnResult();
//        String url = "http://120.224.116.35:8916/send/international/sms";
//        String method = "POST";
//        Boolean dataCheck = false;
//        String messages = "";
//        CommonResponse<JSONObject> Responseresult = null;
//        logger.info("param="+param);
//        logger.error("进来调接口param===" + param);
//        JSONObject jsonObject = JSONObject.parseObject(param);
//        String  id = jsonObject.getString("id");
//        String code = generateRandom6DigitCode();
//        logger.info("code="+code);
//        Order account = new Order();
//        account.setId(Long.valueOf(id));
//        account.setSys_verification_code__c(code);
//        OperateResult update = XObjectService.instance().update(account,true);
//        if (!update.getSuccess()){
//            result.setIsSuccess(false);
//            result.setMessage(update.getErrorMessage());
//            return JSONObject.toJSONString(result);
//        }
//
//        String accSql = "select id,accountId.Telephone__c,accountId.accountName from order where id = " + id;
//        JSONArray accQuery = NeoCrmRkhdService.executeSqlQuery(accSql);
//        logger.info("accQuery="+JSONObject.toJSONString(accQuery));
//        CommonHttpClient client = CommonHttpClient.instance();
//        CommonData request = new CommonData();
//        if (!accQuery.isEmpty()) {
//            JSONObject obj = accQuery.getJSONObject(0);
//            String telephone = obj.getString("accountId.Telephone__c");
//            String accountName = obj.getString("accountId.accountName");
//            if (telephone != null && telephone.startsWith("+")) {
//                telephone = telephone.substring(1);
//            }
//
//            messages = "【Rainbow】Dear " + accountName + ".Your verification code is " + code + ". Please forward to relevant Rainbow Personnel only and do not disclose to any other party. Thank you.";
//            // 添加：设置Basic Auth鉴权头
//            String username = "xsy-crm01";
//            String password = "Abcd1234";
//            String auth = username + ":" + password;
//            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
//            request.addHeader("Authorization", "Basic " + encodedAuth);
//            request.addHeader("Content-Type", "application/json;charset=UTF-8");
//            request.setCallString(url);
//            request.setCall_type(method);
//
//            RequestResultInfo requestinfo = new RequestResultInfo();
//            SMSRequest smsRequest = new SMSRequest();
//            requestinfo.setBusinessId("OrderSMS"+System.currentTimeMillis() + code);
//            requestinfo.setMobile(telephone);
//            requestinfo.setMessage(messages);
//            // 构建请求体
//
//            smsRequest.setResultInfo(requestinfo);
//            request.setBody(JSONObject.toJSONString(smsRequest));
//            logger.info("smsRequest="+JSONObject.toJSONString(smsRequest));
//            logger.info("request="+JSONObject.toJSONString(request));
//
//
//            // 执行请求并处理响应
//            try {
//                Responseresult = client.execute(request, JSONObject::parseObject);
//                String response1 = JSONObject.toJSONString(Responseresult);
//                logger.info("response1="+response1);
//
//                // 获取响应数据中的data对象
//                JSONObject data = Responseresult.getData();
//                if (data != null) {
//                    // 从data中获取resultinfo对象
//                    JSONObject resultinfo = data.getJSONObject("resultinfo");
//                    if (resultinfo != null) {
//                        int resultCode = resultinfo.getIntValue("code");
//                        String resultMsg = resultinfo.getString("msg");
//
//                        if (resultCode == 0) {
//                            result.setIsSuccess(true);
//                            result.setMessage("短信发送成功");
//                        } else {
//                            result.setIsSuccess(false);
//                            result.setMessage("短信发送失败:" + resultMsg);
//                        }
//                    } else {
//                        result.setIsSuccess(false);
//                        result.setMessage("响应数据中未找到resultinfo");
//                    }
//                } else {
//                    result.setIsSuccess(false);
//                    result.setMessage("响应数据中未找到data");
//                }
//            } catch (Exception e) {
//                logger.error("HTTP请求异常", e);
//                result.setIsSuccess(false);
//                result.setMessage("短信发送异常:" + e.getMessage());
//            }
//        }
//        logger.info("jsonObject="+jsonObject.toJSONString());
//        return JSONObject.toJSONString(result);
//    }
    // 新增：生成随机6位数字码的静态方法
    private static String generateRandom6DigitCode() {
        // 生成100000-999999之间的随机整数（包含边界）
        int code = ThreadLocalRandom.current().nextInt(100000, 1000000);
        return String.valueOf(code);
    }
}
