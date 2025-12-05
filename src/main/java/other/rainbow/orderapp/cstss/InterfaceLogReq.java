package other.rainbow.orderapp.cstss;

import com.rkhd.platform.sdk.data.model.Interface_Log__c;
import com.rkhd.platform.sdk.exception.ApiEntityServiceException;
import com.rkhd.platform.sdk.model.OperateResult;
import com.rkhd.platform.sdk.service.MetadataService;
import com.rkhd.platform.sdk.service.XObjectService;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

public class InterfaceLogReq {

    private static final Integer RESULT_CODE = 200;

    private static InterfaceLogReq singleton = new InterfaceLogReq();
    public static InterfaceLogReq instance() {
        return singleton;
    }
    public Long insertInterfaceLog(String className,
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
                                   String description) throws ApiEntityServiceException {


        Interface_Log__c InterfaceLog= new Interface_Log__c();
        InterfaceLog.setApex_Class_Name__c(className);
        InterfaceLog.setRequest_System__c(requestSystem);
        InterfaceLog.setDestination_System__c(destinationSystem);
        InterfaceLog.setInterface_URL__c(url);
        InterfaceLog.setInteract_Time__c(LocalDateTime.now().toEpochSecond(ZoneOffset.UTC));
        InterfaceLog.setIs_Success__c(isSuccess);
        InterfaceLog.setIs_Part_Sucessful__c(isPartSucessful);
        if (interactJSONContent!=null && interactJSONContent.length()>131072) {
            InterfaceLog.setInteract_Content__c(interactJSONContent.substring(0,131072));
            if(interactJSONContent!=null&&interactJSONContent.length()>262144){
                InterfaceLog.setInteract_Content1__c(interactJSONContent.substring(131072,262144));
            }else{
                InterfaceLog.setInteract_Content1__c(interactJSONContent.substring(131072,interactJSONContent.length()));
            }

        }else{
            InterfaceLog.setInteract_Content__c(interactJSONContent);
        }
        if (StrUtils.isNotBlank(log) && log.length()>131072) {
            InterfaceLog.setLog1__c(log.substring(0, 131072));
            if (StrUtils.isNotBlank(log) && log.length()>262144){
                InterfaceLog.setLog2__c(log.substring(131072,262144));
            }else{
                InterfaceLog.setLog2__c(log.substring(131072,log.length()));
            }
        }else{
            InterfaceLog.setLog1__c(log);
        }
        if (StrUtils.isNotBlank(message)) {
            if (message.length()>131072) {
                InterfaceLog.setException_Message__c(message.substring(0, 131071));
            }else{
                InterfaceLog.setException_Message__c(message);
            }

        }
        InterfaceLog.setCall_Type__c(callType);

        if(recordID.size()>0){
            String recordIDList="";
            for(String iDStrl : recordID){
                recordIDList +=iDStrl;
                if(recordID.indexOf(iDStrl) != (recordID.size()-1)){
                    recordIDList +=",";
                }
            }
            if (recordIDList !=null && recordIDList.length()<= 250) {
                InterfaceLog.setRecordID__c(recordIDList);
            }else if(recordIDList!=null && recordIDList.length()> 250){
                InterfaceLog.setRecordID__c(recordIDList.substring(0,250));
            }
        }
        InterfaceLog.setDml_Option__c(dmlOption);
        InterfaceLog.setDescription__c(description);
        InterfaceLog.setEntityType(MetadataService.instance().getBusiType("Interface_Log__c", "defaultBusiType").getId());

        OperateResult insertLog = XObjectService.instance().insert(InterfaceLog,true);
        if(!insertLog.getSuccess()) {
            throw new CustomException("日志增加失败！" + insertLog.getErrorMessage());
        }
        return insertLog.getDataId();
    }
}
