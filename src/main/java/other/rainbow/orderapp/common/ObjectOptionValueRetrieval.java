package other.rainbow.orderapp.common;

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
import com.rkhd.platform.sdk.model.OperateResult;
import com.rkhd.platform.sdk.model.XObject;
import com.rkhd.platform.sdk.service.XObjectService;
import org.apache.commons.lang.StringUtils;
import other.rainbow.orderapp.common.NeoCrmRkhdService;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


/**
 * @author lijinhuan
 * @version 1.0
 * @date 2025/06/09 23:11
 * @e-mail 593356771@qq.com
 * @description:
 */
public class ObjectOptionValueRetrieval {
    private static final Logger logger = LoggerFactory.getLogger();
    private static final Integer RESULT_CODE = 200;
	/***
	{
		"label": "Jibrin Gambo",
		"value": 1,
		"apiKey": "Jibrin Gambo",
		"dependentValue": [2],
		"isActive": true
	}
	Map<对象API,Map<字段API, Map<模式,Map<模式值, List>>>>
	label value apiKey
	模式 label  [0]value,[1]apiKey
	模式 value  [0]apiKey,[1]label
	模式 apiKey [0]label,[1]value
	*/
    private static final Map<String, Map<String, Map<String,Map<String, List<String>>>>> optionsCache = new ConcurrentHashMap<>();
    /**
     * 获取对象所有选项字段的映射
     * @param objectApiKey 对象api 例如：商品：goods
     * @return Map<字段API, Map<模式,Map<模式值, List>>>
     */
    public static Map<String, Map<String,Map<String, List<String>>>> getAllFieldOptions(String objectApiKey) {
        ensureOptionsLoaded(objectApiKey);
        return optionsCache.getOrDefault(objectApiKey, new HashMap<>());
    }

    /**
     * 获取单个字段的选项值映射
     * @param objectApiKey 对象api 例如：商品：goods
     * @param fieldApiKey 字段api
     * @return Map<模式,Map<模式值, List>>
     */
    public static Map<String,Map<String, List<String>>> objectInformation(String objectApiKey, String fieldApiKey) {
        Map<String, Map<String,Map<String, List<String>>>> allOptions = getAllFieldOptions(objectApiKey);
        return allOptions.getOrDefault(fieldApiKey, new HashMap<>());
    }

    private static synchronized void ensureOptionsLoaded(String objectApiKey) {
        if (!optionsCache.containsKey(objectApiKey)) {
            loadOptions(objectApiKey);
        }
    }

    private static void loadOptions(String objectApiKey) {
        Map<String, Map<String,Map<String, List<String>>>> objectMap = new HashMap<>();
        try {
            RkhdHttpClient client = RkhdHttpClient.instance();
            RkhdHttpData data = new RkhdHttpData();
            data.setCall_type("GET");
            data.setCallString("/rest/data/v2.0/xobjects/" + objectApiKey + "/description");
            JSONObject responseObject = client.execute(data, ResponseBodyHandlers.ofJSON());

            if (!RESULT_CODE.equals(responseObject.getIntValue("code"))) {
                logger.error("获取对象描述失败: " + responseObject.getString("message"));
                return;
            }

            String entityStr = responseObject.getString("data");
            if (entityStr == null) {
                logger.error("返回数据为空");
                return;
            }

            JSONObject entity = JSONObject.parseObject(entityStr);
            JSONArray fields = entity.getJSONArray("fields");

            for (int i = 0; i < fields.size(); i++) {
                JSONObject field = fields.getJSONObject(i);
                String fieldApiKey = field.getString("apiKey");
				// Field level map
				Map<String, Map<String, List<String>>> fieldMap = new HashMap<>();
				// Mode maps
				Map<String, List<String>> labelModeMap = new HashMap<>();
				Map<String, List<String>> valueModeMap = new HashMap<>();
				Map<String, List<String>> apiKeyModeMap = new HashMap<>();
                boolean hasOptions = false;
                // 处理selectitem
                JSONArray selectitem = field.getJSONArray("selectitem");
                if (selectitem != null && !selectitem.isEmpty()) {
                    hasOptions = true;
                    for (int j = 0; j < selectitem.size(); j++) {
						/***label value apiKey
						模式 label  [0]value,[1]apiKey
						模式 value  [0]apiKey,[1]label
						模式 apiKey [0]label,[1]value
						*/
                        JSONObject option = selectitem.getJSONObject(j);
                        String label = option.getString("label").toString();
                        String value = option.getInteger("value").toString();
						String apiKey = option.getString("apiKey").toString();
						// Populate label mode
						List<String> labelModeValues = Arrays.asList(value, apiKey);
						labelModeMap.put(label, labelModeValues);
						// Populate value mode
						List<String> valueModeValues = Arrays.asList(apiKey, label);
						valueModeMap.put(value, valueModeValues);
						// Populate apiKey mode
						List<String> apiKeyModeValues = Arrays.asList(label, value);
						apiKeyModeMap.put(apiKey, apiKeyModeValues);
						fieldMap.put("label", labelModeMap);
						fieldMap.put("value", valueModeMap);
						fieldMap.put("apiKey", apiKeyModeMap);
                    }
					objectMap.put(fieldApiKey, fieldMap);
                }
            }
            // 将结果放入缓存
			/***
			Map<对象API,Map<字段API, Map<模式,Map<模式值, List>>>>
			Map<String, Map<String, Map<String,Map<String, List<String>>>>>
			objectMap 是这个类型 Map<String, Map<String,Map<String, List<String>>>> 
			*/
            optionsCache.put(objectApiKey, objectMap);

        } catch (Exception e) {
            logger.error("加载选项值失败: " + e.getMessage(), e);
        }
    }
	
	// 时间格式化
    public static String DateTimeToString(LocalDateTime dateTime) throws IOException {
        try {
            DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            return dateTime.format(format);
        }catch (Exception e){
            return e.getMessage();
        }
    }
	// 日期格式化
    public static String DateToString(Date da) throws IOException {
        try {
            DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDate localDate = da.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            return localDate.format(format);
        }catch (Exception e){
            return e.getMessage();
        }
    }
	// 日志接口记录
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
        if (!Objects.equals(message, "")) {
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
        String sqlog = "select id,entityType from Interface_Log__c limit 1";
        JSONArray loginfo = NeoCrmRkhdService.xoql(rkhdclient,sqlog);
        InterfaceLog.setEntityType(Long.parseLong(loginfo.getJSONObject(0).getString("entityType")));

        logger.info("InterfaceLog="+JSONObject.toJSONString(InterfaceLog));
        List<XObject> updateList = new ArrayList<>();
        updateList.add(InterfaceLog);
        // TODO  测试用
        /*for (XObject xo : updateList){
            OperateResult insert = XObjectService.instance().insert(xo,true);
            logger.info("更新成功batchResult="+insert.getCode());
            logger.info("更新成功batchResult2="+insert.getSuccess());
            logger.info("更新成功batchResult2="+insert.getErrorMessage());
        }*/
        BatchOperateResult batchResult = XObjectService.instance().insert(updateList,false,true);
        logger.info("更新成功batchResult="+batchResult.getCode());
        logger.info("更新成功batchResult2="+batchResult.getSuccess());
        logger.info("更新成功batchResult2="+batchResult.getErrorMessage());
        return InterfaceLog.getId();
    }
}
