package other.rainbow.cpzerooneone.common;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rkhd.platform.sdk.data.model.Interface_Log__c;
import com.rkhd.platform.sdk.exception.ApiEntityServiceException;
import com.rkhd.platform.sdk.exception.ScriptBusinessException;
import com.rkhd.platform.sdk.exception.XsyHttpException;
import com.rkhd.platform.sdk.http.RkhdHttpClient;
import com.rkhd.platform.sdk.http.RkhdHttpData;
import com.rkhd.platform.sdk.http.handler.ResponseBodyHandlers;
import com.rkhd.platform.sdk.log.Logger;
import com.rkhd.platform.sdk.log.LoggerFactory;
import com.rkhd.platform.sdk.model.BatchOperateResult;
import com.rkhd.platform.sdk.model.XObject;
import com.rkhd.platform.sdk.service.XObjectService;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * @author 武于伦
 * @version 1.0
 * @date 2024/11/26 10:11
 * @e-mail 2717718875@qq.com
 * @description:
 */
public class ObjectOptionValueRetrieval {
    private static final Logger logger = LoggerFactory.getLogger();
    private static final Integer RESULT_CODE = 200;
    private static final String RESULT_CODE1 = "0";

    private static ObjectOptionValueRetrieval singleton = new ObjectOptionValueRetrieval();
    public static ObjectOptionValueRetrieval instance() {
        return singleton;
    }
    public List<PickOption> getGlobalPicks(String globalPickApiKey) throws IOException, XsyHttpException {
        RkhdHttpClient client = RkhdHttpClient.instance();
        RkhdHttpData data = new RkhdHttpData();
        data.setCall_type("GET");
        data.setCallString("/rest/metadata/v2.0/settings/globalPicks/" + globalPickApiKey);
        return client.execute(data, (dataString) -> {
            JSONObject responseObject = JSON.parseObject(dataString);
            if (RESULT_CODE1.equals(responseObject.getString("code"))) {
                JSONObject responseObjectdata = (JSONObject) responseObject.get("data");
                JSONObject responseObjectrecords = (JSONObject) responseObjectdata.get("records");
                JSONArray pickOptionJson = (JSONArray) responseObjectrecords.get("pickOption");
                return pickOptionJson.toJavaList(PickOption.class);
            } else {
                throw new CustomException("获取通用项失败：globalPickApiKey:" + globalPickApiKey);
            }
        });
    }

    public PickOption getGlobalPickByOptionLabel(String globalPickApiKey, String optionLabel) throws IOException, XsyHttpException {
        List<PickOption> globalPickList = getGlobalPicks(globalPickApiKey);
        return getOptionByLabel(globalPickList,optionLabel);
    }

    public PickOption getOptionByLabel(List<PickOption> globalPickList, String optionLabel) {
        if(optionLabel == null) {
            return new PickOption();
        }
        if (globalPickList != null && globalPickList.size() > 0) {
            PickOption pickOption = globalPickList.stream().filter(globalPick -> globalPick.getOptionLabel().equals(optionLabel)).findAny().orElse(null);
            if(pickOption != null) {
                return pickOption;
            }
        }
        throw new CustomException("通用项无该optionLabel：optionLabel:" + optionLabel);
    }

    public PickOption getGlobalPickByOptionApiKey(String globalPickApiKey, String optionApiKey) throws IOException, XsyHttpException {
        List<PickOption> globalPickList = getGlobalPicks(globalPickApiKey);
        return getOptionByOptionApiKey(globalPickList,optionApiKey);
    }

    public PickOption getOptionByOptionApiKey(List<PickOption> globalPickList, String optionApiKey) {
        if(optionApiKey == null) {
            return new PickOption();
        }
        if (globalPickList != null && globalPickList.size() > 0) {
            PickOption pickOption = globalPickList.stream().filter(globalPick -> globalPick.getOptionApiKey().equals(optionApiKey)).findAny().orElse(null);
            if(pickOption != null) {
                return pickOption;
            }
        }
        throw new CustomException("通用项无该optionLabel：optionApiKey:" + optionApiKey);
    }

    public PickOption getGlobalPickByOptionCode(String globalPickApiKey, Integer optionCode) throws IOException, XsyHttpException {
        List<PickOption> globalPickList = getGlobalPicks(globalPickApiKey);
        return getOptionByCode(globalPickList,optionCode);
    }

    public PickOption getOptionByCode(List<PickOption> globalPickList, Integer optionCode) {
        if(optionCode == null) {
            return new PickOption();
        }
        if (globalPickList != null && globalPickList.size() > 0) {
            PickOption pickOption = globalPickList.stream().filter(globalPick -> globalPick.getOptionCode().equals(optionCode)).findAny().orElse(null);
            if(pickOption != null) {
                return pickOption;
            }
        }
        throw new CustomException("通用项无该optionLabel：optionCode:" + optionCode);
    }
    /**
     * @author: 武于伦
     * @date: 2024/11/26 11:03
     * @e-mail 2717718875@qq.com
     * @param:
     * 参数 objectApiKey 对象api 例如：商品：goods
     * 参数 fieldApiKey 字段api
     * @description:
     **/
    public static Map<String,String> objectInformation(String objectApiKey, String fieldApiKey) throws IOException {
        JSONObject entity = null;
        RkhdHttpClient client = RkhdHttpClient.instance();
        RkhdHttpData data = new RkhdHttpData();
        data.setCall_type("GET");
        data.setCallString("/rest/data/v2.0/xobjects/"+objectApiKey+"/description");
        try {
            JSONObject responseObject = client.execute(data, ResponseBodyHandlers.ofJSON());
            Integer responseCode = responseObject.getIntValue("code");
            if (RESULT_CODE.equals(responseCode)) {
                String entityStr = responseObject.getString("data");
                entity = JSONObject.parseObject(entityStr);
            }
        }catch (Exception e){
            logger.error(" 报错信息：" + e);
        }
        Map<String, String> map = new HashMap<>();
        JSONArray fields = entity.getJSONArray("fields");
        for (int i = 0; i < fields.size(); i++) {
            JSONObject field = fields.getJSONObject(i);
            if (fieldApiKey.equals(field.getString("apiKey"))) {
                JSONArray selectitem = field.getJSONArray("selectitem");
                for (int j = 0; j < selectitem.size(); j++) {
                    JSONObject object = selectitem.getJSONObject(j);
                    map.put(object.getString("label"),object.getString("apiKey"));
                }
                JSONArray checkitem = field.getJSONArray("checkitem");
                for (int k = 0; k < checkitem.size(); k++) {
                    JSONObject object = checkitem.getJSONObject(k);
                    map.put(object.getString("label"),object.getString("apiKey"));
                }
            }
        }
        return  map;
    }
    public static Map<String,Integer> objectInformation2(String objectApiKey, String fieldApiKey) throws IOException {
        JSONObject entity = null;
        RkhdHttpClient client = RkhdHttpClient.instance();
        RkhdHttpData data = new RkhdHttpData();
        data.setCall_type("GET");
        data.setCallString("/rest/data/v2.0/xobjects/"+objectApiKey+"/description");
        try {
            JSONObject responseObject = client.execute(data, ResponseBodyHandlers.ofJSON());
            Integer responseCode = responseObject.getIntValue("code");
            if (RESULT_CODE.equals(responseCode)) {
                String entityStr = responseObject.getString("data");
                entity = JSONObject.parseObject(entityStr);
            }
        }catch (Exception e){
            logger.error(" 报错信息：" + e);
        }
        Map<String, Integer> map = new HashMap<>();
        JSONArray fields = entity.getJSONArray("fields");
        for (int i = 0; i < fields.size(); i++) {
            JSONObject field = fields.getJSONObject(i);
            if (fieldApiKey.equals(field.getString("apiKey"))) {
                JSONArray selectitem = field.getJSONArray("selectitem");
                for (int j = 0; j < selectitem.size(); j++) {
                    JSONObject object = selectitem.getJSONObject(j);
                    map.put(object.getString("label"),object.getInteger("value"));
                }
                JSONArray checkitem = field.getJSONArray("checkitem");
                for (int k = 0; k < checkitem.size(); k++) {
                    JSONObject object = checkitem.getJSONObject(k);
                    map.put(object.getString("label"),object.getInteger("value"));
                }
            }
        }
        return  map;
    }
    public static String DateTimeToString(LocalDateTime dateTime) throws IOException {
        try {
            DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            return dateTime.format(format);
        }catch (Exception e){
            return e.getMessage();
        }
    }
    public static String DateToString(Date da) throws IOException {
        try {
            DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDate localDate = da.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            return localDate.format(format);
        }catch (Exception e){
            return e.getMessage();
        }
    }
    public static String OptionValue(String objectApiKey,String fieldApiKey,String fieldvalue) throws IOException {
        String result="未找到对应选项值";
        Map<String, String> stringIntegerMap = ObjectOptionValueRetrieval.objectInformation(objectApiKey,fieldApiKey);
        for (Map.Entry<String, String> entry : stringIntegerMap.entrySet()) {
            if (Objects.equals(entry.getKey(), fieldvalue)){
                return  entry.getValue();
            }
        }
        return result;
    }
    public static Integer OptionValue2(String objectApiKey,String fieldApiKey,String fieldvalue) throws IOException {
        Integer result=999;
        Map<String, Integer> stringIntegerMap = ObjectOptionValueRetrieval.objectInformation2(objectApiKey,fieldApiKey);
        for (Map.Entry<String, Integer> entry : stringIntegerMap.entrySet()) {
            if (Objects.equals(entry.getKey(), fieldvalue)){
                return  entry.getValue();
            }
        }
        return result;
    }
    public static Long insertInterfaceLog(String className,
                                            String requestSystem,
                                            String destinationSystem,
                                            String url,
                                            String interactJSONContent,
                                            Boolean isSuccess,
                                            Boolean isPartSucessful,
                                            String log,
                                            String message,
                                            String callType,
                                            List<String> recordID,
                                            String dmlOption,
                                            String description) throws IOException, ApiEntityServiceException, ScriptBusinessException, InterruptedException, XsyHttpException {
        Interface_Log__c  InterfaceLog= new Interface_Log__c();
        InterfaceLog.setName(LocalDateTime.now().toString());
        InterfaceLog.setApex_Class_Name__c(className);
        InterfaceLog.setRequest_System__c(requestSystem);
        InterfaceLog.setDestination_System__c(destinationSystem);
        InterfaceLog.setInterface_URL__c(url);
        InterfaceLog.setInteract_Time__c(ZonedDateTime.now().toInstant().toEpochMilli());
        InterfaceLog.setIs_Success__c(isSuccess);
        InterfaceLog.setIs_Part_Sucessful__c(isPartSucessful);
        if (interactJSONContent!=null && interactJSONContent.length()>131072) {
            InterfaceLog.setInteract_Content__c(interactJSONContent.substring(0,131072));
            if(interactJSONContent.length()>262144){
                InterfaceLog.setInteract_Content1__c(interactJSONContent.substring(131072,262144));
            }else{
                InterfaceLog.setInteract_Content1__c(interactJSONContent.substring(131072,interactJSONContent.length()));
            }
        }else {
            InterfaceLog.setInteract_Content__c(interactJSONContent);
        }
        if (!Objects.equals(log, "") && log.length()>131072) {
            InterfaceLog.setLog1__c(log.substring(0, 131072));
            if (log.length()>262144){
                InterfaceLog.setLog2__c(log.substring(131072,262144));
            }else{
                InterfaceLog.setLog2__c(log.substring(131072,log.length()));
            }
        }else{
            InterfaceLog.setLog1__c(log);
        }
        if ( message!=null) {
            if (message.length()>131072) {
                InterfaceLog.setException_Message__c(message.substring(0, 131071));
            }else{
                InterfaceLog.setException_Message__c(message);
            }
        }
        InterfaceLog.setCall_Type__c(callType);
        if(!recordID.isEmpty()){
            StringBuilder recordIDList= new StringBuilder();
            for(String iDStrl : recordID){
                recordIDList.append(iDStrl);
                int i = recordID.size() - 1;
                if(recordID.indexOf(iDStrl)!=i){
                    recordIDList.append(',');
                }
            }
            if (recordIDList.length()<= 250&&recordIDList.length()>0) {
                InterfaceLog.setRecordID__c(recordIDList.toString());
            }else if (recordIDList.length()> 250){
                InterfaceLog.setRecordID__c(recordIDList.substring(0,250));
            }
        }
        InterfaceLog.setDml_Option__c(dmlOption);
        InterfaceLog.setDescription__c(description);
        RkhdHttpClient rkhdclient = RkhdHttpClient.instance();
        InterfaceLog.setEntityType(getEntityTypesId(rkhdclient,"Interface_Log__c","defaultBusiType"));

        logger.info("InterfaceLog="+JSONObject.toJSONString(InterfaceLog));
        List<XObject> updateList = new ArrayList<>();
        updateList.add(InterfaceLog);
        BatchOperateResult batchResult = XObjectService.instance().insert(updateList,true,true);
        logger.info("更新成功batchResult="+batchResult.getCode());
        logger.info("更新成功batchResult2="+batchResult.getSuccess());
        logger.info("更新成功batchResult2="+batchResult.getErrorMessage());
        return InterfaceLog.getId();
    }
    public static Long getEntityTypesId(RkhdHttpClient client, String apiKey, String entityApiKey) {
        Long entityTypesId = null;
        try {
            RkhdHttpData data = RkhdHttpData.newBuilder().callType("GET")
                    .callString("/rest/data/v2.0/xobjects/" + apiKey + "/busiType").build();
            JSONObject result = client.execute(data, ResponseBodyHandlers.ofJSON());
            if (200 != result.getInteger("code")) {
                throw new ScriptBusinessException(result.getString("msg"));
            }

            for (Object obj : result.getJSONObject("data").getJSONArray("records")) {
                JSONObject jsonObject = (JSONObject) obj;
                if (entityApiKey.equals(jsonObject.getString("apiKey"))) {
                    entityTypesId = jsonObject.getLong("id");
                }
            }
        } catch (Exception e) {
            // TODO: handle exception
            logger.error("error->" + e.toString());
        }
        return entityTypesId;
    }
}
