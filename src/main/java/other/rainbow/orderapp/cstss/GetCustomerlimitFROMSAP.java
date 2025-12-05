package other.rainbow.orderapp.cstss;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rkhd.platform.sdk.data.model.Account;
import com.rkhd.platform.sdk.data.model.Customer_Credit_Info__c;
import com.rkhd.platform.sdk.exception.ApiEntityServiceException;
import com.rkhd.platform.sdk.exception.CustomConfigException;
import com.rkhd.platform.sdk.exception.XsyHttpException;
import com.rkhd.platform.sdk.http.CommonResponse;
import com.rkhd.platform.sdk.log.Logger;
import com.rkhd.platform.sdk.log.LoggerFactory;
import com.rkhd.platform.sdk.model.BatchOperateResult;
import com.rkhd.platform.sdk.model.QueryResult;
import com.rkhd.platform.sdk.service.XObjectService;
import com.rkhd.platform.sdk.service.XoqlService;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class GetCustomerlimitFROMSAP {
    private static final Logger LOG = LoggerFactory.getLogger();

    public static final String END_POINT = "/sap/sales/avacrelimit/query";


    public static RespCustomerInfo submitToSAP(List<String> customerCodeList, String WAERS) {
        List<String> recordIdList = new ArrayList<String>();

        ResponseBody syncReturnData = null;
        Boolean isSuccess = true;
        RequestBody request = null;
        String errorMessage = "";
        try{

            request = new RequestBody();
            ReqESInfo reqESInfo= new ReqESInfo();
            LocalDateTime currentDateTime = LocalDateTime.now();
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            reqESInfo.setRequesttime(currentDateTime.format(dateTimeFormatter));
            reqESInfo.setInstid("GSD004");
            reqESInfo.setAttr1("NEOCRM");
            reqESInfo.setAttr2("SAP");
            request.setEsbinfo(reqESInfo);
            List<ReqCustomerInfo> resultinfoList = new ArrayList<ReqCustomerInfo>();
            // TODO 获取所有id
            String joinWhereCustomer = SqlFormatUtils.joinInSql(customerCodeList);

            String selectSqlCustomer = "SELECT id,MDG_Customer_Code__c FROM account WHERE MDG_Customer_Code__c IN (" + joinWhereCustomer + ")";
            QueryResult<JSONObject> queryAccount = XoqlService.instance().query(selectSqlCustomer,true,true);
            if(!queryAccount.getSuccess()) {
                throw new CustomException("获取客户信息失败！" + queryAccount.getErrorMessage());
            }
            if(queryAccount.getTotalCount() == 0) {
                throw new CustomException("获取客户信息为空！" + queryAccount.getErrorMessage());
            }
            String joinWhere = queryAccount.getRecords().stream().map(jsonObject -> jsonObject.toJavaObject(Account.class)).map(Account::getId).map(String::valueOf).collect(Collectors.joining(","));
            String selectSql = "SELECT id,name,currencyUnit_F__c,Rules__c,Risk_Class__c,Check_Rule__c,Credit_Segment__c,Valid_Until__c,Customer__c,Sinosure_Credit_Limit__c,Local_Credit_Limit__c,Actual_Credit_limit__c,Sync_Status__c,Remaining_Credit__c,Customer__c FROM Customer_Credit_Info__c WHERE Customer__c IN (" + joinWhere + ")";
            //获取客户
            QueryResult<JSONObject> query = XoqlService.instance().query(selectSql,true,true);
            if(!query.getSuccess()) {
                throw new CustomException("获取客户额度失败！" + query.getErrorMessage());
            }
            if(query.getTotalCount() == 0) {
                throw new CustomException("获取客户额度为空！");
            }
            List<PickOption> creditSegment__cList = GlobalPicksReq.instance().getGlobalPicks("Credit_Segment__c");
            Map<String,Long> cciMap = new HashMap<String,Long>();
            for (Customer_Credit_Info__c cci : query.getRecords().stream().map(jsonObject -> jsonObject.toJavaObject(Customer_Credit_Info__c.class)).collect(Collectors.toList())) {
                recordIdList.add(String.valueOf(cci.getId()));
                ReqCustomerInfo rci = new ReqCustomerInfo();
                Account accountGet = queryAccount.getRecords().stream().map(jsonObject -> jsonObject.toJavaObject(Account.class)).filter(account -> account.getId().equals(cci.getCustomer__c())).findAny().orElse(null);

                cciMap.put(accountGet.getMDG_Customer_Code__c(), cci.getId());
               rci.setPartner(accountGet.getMDG_Customer_Code__c());
                rci.setCreditSegment(GlobalPicksReq.instance().getOptionByCode(creditSegment__cList,cci.getCredit_Segment__c()).getOptionApiKey());
                // TODO
//                if (StrUtils.isNotBlank(WAERS)) {
//                    rci.setWAERS(WAERS);
//                }
                rci.setWAERS(cci.getCurrencyUnit_F__c());
                resultinfoList.add(rci);
            }

            request.setResultinfo(resultinfoList);
            LOG.info("请求参数：" + JSON.toJSONString(request));
            CommonResponse<ResponseBody> commonResponse = SapRestReq.instance().send_SAP_POST_Service(JSON.toJSONString(request), END_POINT,(dataString) -> {
                try {
                    LOG.info("请求结果:" + dataString);
                    return JSON.parseObject(dataString, ResponseBody.class);
                } catch (Exception e) {
                    throw new CustomException(e.getMessage() + dataString == null ? "" : ";" + dataString);
                }
            });

            if (commonResponse.getCode() == 200) {
                syncReturnData = commonResponse.getData();
                if (syncReturnData.getEsbinfo() != null ) {
                    if ("S".equals(syncReturnData.getEsbinfo().getReturnstatus())) {
                        List<Customer_Credit_Info__c> updateList = new ArrayList<Customer_Credit_Info__c>();
                        for (RespCustomerInfo rci : syncReturnData.getResultinfo()) {
                            Customer_Credit_Info__c upAcc = new Customer_Credit_Info__c();
                            upAcc.setId(cciMap.get(rci.getPartner()));
                            upAcc.setRemaining_Credit__c(rci.getAmount() == null ? null : rci.getAmount().doubleValue());
                            if (StrUtils.isNotBlank(rci.getZsfyq())) {
                                upAcc.setOverdue__c(true);
                            }else{
                                upAcc.setOverdue__c(false);
                            }
                            upAcc.setCurrencyRate(rci.getExch_rate() == null ? null : new Double(rci.getExch_rate()));
                            updateList.add(upAcc);
                        }
                        // Customer_Credit_Info__c query1 = XObjectService.instance().get(updateList.get(0));
/*                        updateList.forEach(updateDate -> {
                            OperateResult updateResult = null;
                            try {
                                updateResult = XObjectService.instance().update(updateDate, true);
                            } catch (ApiEntityServiceException e) {
                                throw new CustomException("客户额度更新失败！" + e.getMessage() + ";id:" + updateDate.getId());
                            }
                            if(!updateResult.getSuccess()) {
                                throw new CustomException("客户额度更新失败！" + updateResult.getErrorMessage() + ";id:" + updateDate.getId());
                            }
                        });*/

                        try {
                            BatchOperateResult updateResult = XObjectService.instance().update(updateList,false,true);

                            if(!updateResult.getSuccess()) {
                                LOG.error("批量更新失败" + updateResult.getErrorMessage());
                                throw new CustomException("批量更新失败" + updateResult.getErrorMessage());
                            }
                            LOG.info("批量更新成功");
                        } catch (ApiEntityServiceException e) {
                            throw new CustomException(e);
                        }
                    }

                    if("E".equals(syncReturnData.getEsbinfo().getReturnstatus())){
                        isSuccess = false;
                        errorMessage = syncReturnData.getEsbinfo().getReturnmsg();

                    }
                    if("W".equals(syncReturnData.getEsbinfo().getReturnstatus())){
                        isSuccess = false;
                        errorMessage = syncReturnData.getEsbinfo().getReturnmsg();
                    }
                }else{
                    isSuccess = false;
                    errorMessage = JSON.toJSONString(syncReturnData);
                }
            } else {
                isSuccess = false;
                errorMessage = "请求失败code=" + commonResponse.getCode() + ";" + commonResponse.getData() == null ? "" : JSON.toJSONString(commonResponse.getData());
            }
        } catch (XsyHttpException e) {
            isSuccess = false;
            errorMessage = "无法获取sap配置;" + e.getMessage();
        }
        catch (CustomConfigException e) {
            isSuccess = false;
            errorMessage = "sap请求返回错误信息;" + e.getMessage();
        } catch (ApiEntityServiceException e) {

            isSuccess = false;
            errorMessage = "请求实体信息报错;" + e.getMessage();

        }  catch (IOException e) {
            isSuccess = false;
            errorMessage = "IO报错;" + e.getMessage();
        } catch (CustomException e) {
            isSuccess = false;
            errorMessage = e.getMessage();
        } catch (Exception e) {
            // Database.rollback(sp);
            errorMessage = e.getMessage();
            // System.debug('出现错误：' + respBody.message);
            isSuccess = false;
        }

        try {
            InterfaceLogReq.instance().insertInterfaceLog("GetCustomerlimitFROMSAP", "CRM",

                "SAP", END_POINT, JSON.toJSONString(request), isSuccess, false,

                syncReturnData == null ? null : JSON.toJSONString(syncReturnData), errorMessage, "POST", recordIdList,

                "客户额度信息查询", "CRM查询SAP信用额度余额");

        } catch (Exception e) {
            LOG.error(e.getMessage());
        }
        if (syncReturnData!=null && syncReturnData.getResultinfo() !=null) {
            if (syncReturnData.getResultinfo().size()>0) {
                return syncReturnData.getResultinfo().get(0);
            }else{
                RespCustomerInfo r = new RespCustomerInfo();
                r.setMsgty("E");
                r.setMsgtx("No result");
                return r;
            }
        } else {
            RespCustomerInfo r = new RespCustomerInfo();
            r.setMsgty("E");
            r.setMsgtx("No result");
            return r;
        }

    }
}
