package other.rainbow.orderapp.cstss;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rkhd.platform.sdk.exception.XsyHttpException;
import com.rkhd.platform.sdk.http.RkhdHttpClient;
import com.rkhd.platform.sdk.http.RkhdHttpData;

import java.io.IOException;
import java.util.List;

public class GlobalPicksReq  {
    private static final String RESULT_CODE = "0";

    private static GlobalPicksReq singleton = new GlobalPicksReq();
    public static GlobalPicksReq instance() {
        return singleton;
    }
    public List<PickOption> getGlobalPicks(String globalPickApiKey) throws IOException, XsyHttpException {
        RkhdHttpClient client = RkhdHttpClient.instance();
        RkhdHttpData data = new RkhdHttpData();
        data.setCall_type("GET");
        data.setCallString("/rest/metadata/v2.0/settings/globalPicks/" + globalPickApiKey);
        return client.execute(data, (dataString) -> {
            JSONObject responseObject = JSON.parseObject(dataString);
            if (RESULT_CODE.equals(responseObject.getString("code"))) {
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
}