package other.rainbow.cpzerooneone.trigger.CSI;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rkhd.platform.sdk.data.model.Customer_Sales_Info__c;
import com.rkhd.platform.sdk.data.model.Customer_Tax_Category__c;
import com.rkhd.platform.sdk.data.model.Tax_Category_Master_Data__c;
import com.rkhd.platform.sdk.exception.*;
import com.rkhd.platform.sdk.http.RkhdHttpClient;
import com.rkhd.platform.sdk.log.Logger;
import com.rkhd.platform.sdk.log.LoggerFactory;
import com.rkhd.platform.sdk.model.BatchOperateResult;
import com.rkhd.platform.sdk.model.OperateResult;
import com.rkhd.platform.sdk.model.XObject;
import com.rkhd.platform.sdk.service.XObjectService;
import com.rkhd.platform.sdk.trigger.*;
import other.rainbow.cpzerooneone.common.NeoCrmRkhdService;
import other.rainbow.cpzerooneone.common.ObjectOptionValueRetrieval;
import other.rainbow.cpzerooneone.service.CustomerSalescontroller;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class AfterUpdateCSITrigger implements Trigger {
    private static final Logger logger = LoggerFactory.getLogger();
    @Override
    public TriggerResponse execute(TriggerRequest triggerRequest) throws ScriptBusinessException {
        List<DataResult> result = new ArrayList<>();
        String msg ="";
        Boolean success = true;
        //获取beforeUpdate 的list
        TriggerContext triggerContext = triggerRequest.getTriggerContext();
        JSONArray jsonArray = null;
        try {
            jsonArray = JSONArray.parseArray(triggerContext.get("oldList"));
            List<Customer_Sales_Info__c> oldList = jsonArray.toJavaList(Customer_Sales_Info__c.class);
            Map<Long,Customer_Sales_Info__c> oldMap = new HashMap<Long,Customer_Sales_Info__c>();
            for(Customer_Sales_Info__c old :oldList){
                logger.info("old=" + old);
                oldMap.put(old.getId(),old);
            }

            logger.info("jsonArray===="+ JSON.toJSONString(jsonArray));
            logger.info("oldMap=" + oldMap);
            RkhdHttpClient rkhdclient = RkhdHttpClient.instance();
            String sqlcsi = "SELECT id,isSync__c,Sync_Status__c,Customer__c,Sales_View_Status__c,Customer__c,Customer__c.MDG_Customer_Code__c FROM Customer_Sales_Info__c WHERE id= " + triggerRequest.getDataList().get(0).getId();

            JSONArray csiinfo;
            JSONArray ctcinfo;
            JSONArray tcmdinfo;
            try {
                csiinfo = NeoCrmRkhdService.xoql(rkhdclient, sqlcsi);
            } catch (XsyHttpException | InterruptedException e) {
                throw new RuntimeException(e);
            }
            Boolean MDGnull = true;
            logger.info("MDG_Customer_Code__c=" + csiinfo.getJSONObject(0).getString("Customer__c.MDG_Customer_Code__c"));
            if (!Objects.equals(csiinfo.getJSONObject(0).getString("Customer__c.MDG_Customer_Code__c"), "") &&  csiinfo.getJSONObject(0).getString("Customer__c.MDG_Customer_Code__c")!=null){
                MDGnull =false;
            }
                    //请求实例
            List<XObject> dataList = triggerRequest.getDataList();
            Map<Long,Customer_Sales_Info__c> newMap= new HashMap<>();
            Set<String> csiSet = new HashSet<>();
            Set<String> countrySet = new HashSet<>();

            for(XObject xObject : dataList){
                logger.info("getCountry_Code_Text__c=" + ((Customer_Sales_Info__c)xObject).getCountry_Code_Text__c());
                logger.info("getCountry__c=" + ((Customer_Sales_Info__c) xObject).getCountry__c());
                logger.info("oldMapgetCountry__c=" + oldMap.get(xObject.getId()).getCountry__c());
                countrySet.add(((Customer_Sales_Info__c)xObject).getCountry_Code_Text__c());
                if (Objects.equals(((Customer_Sales_Info__c) xObject).getCountry__c(), oldMap.get(xObject.getId()).getCountry__c())){
                    csiSet.add(xObject.getId().toString());
                }
            }
            String csiSetString = csiSet.stream()
                    .map(s -> "'" + s + "'") // 每个元素加上单引号
                    .collect(Collectors.joining(","));
            String sqlctc = "SELECT id FROM Customer_Tax_Category__c WHERE Sales_View__c in  (" + csiSetString+")";
            try {
                ctcinfo = NeoCrmRkhdService.xoql(rkhdclient, sqlctc);
            } catch (XsyHttpException | InterruptedException e) {
                throw new RuntimeException(e);
            }
            logger.info("ctcinfo=" + JSON.toJSONString(ctcinfo));
            List<Customer_Tax_Category__c> deleteList = new ArrayList<>();
            if (!ctcinfo.isEmpty()){
                for(int i = 0; i<ctcinfo.size(); i++){
                    String ctcid = ctcinfo.getJSONObject(i).getString("id");
                    Customer_Tax_Category__c ctc = new Customer_Tax_Category__c();
                    ctc.setId(Long.parseLong(ctcid));
                    deleteList.add(ctc);
                }
                BatchOperateResult delete = XObjectService.instance().delete(deleteList,true);
                logger.info("delete1=" + delete.getErrorMessage());
                logger.info("delete2=" + delete.getSuccess());
            }
            String countrySetString = countrySet.stream()
                    .map(s -> "'" + s + "'") // 每个元素加上单引号
                    .collect(Collectors.joining(","));
            Map<String,List<Tax_Category_Master_Data__c>> tcmdMap = new HashMap<>();
            String sqltcmd = "SELECT id ,Country_Code__c,name FROM Tax_Category_Master_Data__c WHERE Country_Code__c in  (" + countrySetString+")";
            try {
                tcmdinfo = NeoCrmRkhdService.xoql(rkhdclient, sqltcmd);
            } catch (XsyHttpException | InterruptedException e) {
                throw new RuntimeException(e);
            }
            logger.info("tcmdinfo=" + JSON.toJSONString(tcmdinfo));
            for(int i = 0; i<tcmdinfo.size(); i++){
                Tax_Category_Master_Data__c taxCategoryMasterData__c = new Tax_Category_Master_Data__c();
                taxCategoryMasterData__c.setId(Long.parseLong(tcmdinfo.getJSONObject(i).getString("id")));
                taxCategoryMasterData__c.setName(tcmdinfo.getJSONObject(i).getString("name"));
                if (tcmdMap.containsKey(tcmdinfo.getJSONObject(i).getString("Country_Code__c"))) {
                    tcmdMap.get(tcmdinfo.getJSONObject(i).getString("Country_Code__c")).add(taxCategoryMasterData__c);
                }else{
                    List<Tax_Category_Master_Data__c>  tcmdList = new ArrayList<>();
                    tcmdList.add(taxCategoryMasterData__c);
                    tcmdMap.put(tcmdinfo.getJSONObject(i).getString("Country_Code__c"), tcmdList);
                }
            }
            List<Customer_Tax_Category__c> insertList = new ArrayList<>();

            for(XObject xObject : dataList){

                newMap.put(xObject.getId(), (Customer_Sales_Info__c) xObject);
                result.add(new DataResult(true, "成功", xObject));


                if (tcmdMap.get(((Customer_Sales_Info__c) xObject).getCountry_Code_Text__c())!=null && (  Objects.equals(((Customer_Sales_Info__c) xObject).getCountry__c(), oldMap.get(xObject.getId()).getCountry__c()))) {
                    for (Tax_Category_Master_Data__c tcm : tcmdMap.get(((Customer_Sales_Info__c) xObject).getCountry_Code_Text__c())) {
                        Customer_Tax_Category__c ctc = new Customer_Tax_Category__c();
                        ctc.setSales_View__c(xObject.getId());
                        ctc.setTax_Category_Master_Data__c( tcm.getId());
                        ctc.setTax_Category__c(((Customer_Sales_Info__c) xObject).getTax_Category__c());
                        if (ObjectOptionValueRetrieval.OptionValue2("Customer_Tax_Category__c","Tax_Category_Type__c",tcm.getName())!=999){
                            ctc.setTax_Category_Type__c(ObjectOptionValueRetrieval.OptionValue2("Customer_Tax_Category__c","Tax_Category_Type__c",tcm.getName()));
                        }
                        insertList.add(ctc);
                    }
                }

//                logger.info("((Customer_Sales_Info__c) xObject).getSync_Status__c()=" + ((Customer_Sales_Info__c) xObject).getSync_Status__c());
//                logger.info("((Customer_Sales_Info__c) xObject).getSales_View_Status__c()=" + ((Customer_Sales_Info__c) xObject).getSales_View_Status__c());
//                logger.info("oldMap.getSync_Status__c()=" + oldMap.get(xObject.getId()).getSync_Status__c());
//                logger.info("oldMap.getSales_View_Status__c()=" + oldMap.get(xObject.getId()).getSales_View_Status__c());

                if (!Objects.equals(((Customer_Sales_Info__c) xObject).getSales_View_Status__c(), oldMap.get(xObject.getId()).getSales_View_Status__c())
                        && ((Customer_Sales_Info__c) xObject).getSales_View_Status__c()==4
                        && ((Customer_Sales_Info__c) xObject).getCustomer__c()!=null
                        && !MDGnull
                ){
                    if (!((Customer_Sales_Info__c) xObject).getIsSync__c()) {
                        CustomerSalescontroller.sendCustomerSales(xObject.getId().toString(),"I");
                    }else{
                        CustomerSalescontroller.sendCustomerSales(xObject.getId().toString(),"U");
                    }
                }
                if (Objects.equals(((Customer_Sales_Info__c) xObject).getSales_View_Status__c(), oldMap.get(xObject.getId()).getSales_View_Status__c())

                        && Objects.equals(((Customer_Sales_Info__c) xObject).getSync_Status__c(),3 )
                ){
                    if (!((Customer_Sales_Info__c) xObject).getIsSync__c()) {
                        CustomerSalescontroller.sendCustomerSales(xObject.getId().toString(),"I");
                    }else{
                        CustomerSalescontroller.sendCustomerSales(xObject.getId().toString(),"U");
                    }
                }
            }
            BatchOperateResult insert = XObjectService.instance().insert(insertList, true);
            logger.info("insert1=" + insert.getErrorMessage());
            logger.info("insert2=" + insert.getSuccess());
            msg = "成功";
        } catch (TriggerContextException | IOException | InterruptedException | XsyHttpException |
                 ApiEntityServiceException e) {
            msg = "失败"+e.getMessage();
            success = false;
//            throw new RuntimeException(e);
        } catch (CustomConfigException e) {
            throw new RuntimeException(e);
        }
        logger.info("result==="+result);
        return new TriggerResponse(success, msg, result);
    }
}
