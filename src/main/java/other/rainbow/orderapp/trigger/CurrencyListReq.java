package other.rainbow.orderapp.trigger;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rkhd.platform.sdk.exception.XsyHttpException;
import com.rkhd.platform.sdk.http.RkhdHttpClient;
import com.rkhd.platform.sdk.http.RkhdHttpData;
import other.rainbow.orderapp.cstss.CustomException;

import java.io.IOException;
import java.util.List;

public class CurrencyListReq {

    private static final String RESULT_CODE = "0";

    private static CurrencyListReq singleton = new CurrencyListReq();
    public static CurrencyListReq instance() {
        return singleton;
    }
    public List<CurrencyOption> getGlobalPicks() throws IOException, XsyHttpException {
        RkhdHttpClient client = RkhdHttpClient.instance();
        RkhdHttpData data = new RkhdHttpData();
        data.setCall_type("GET");
        data.setCallString("/rest/metadata/v2.0/settings/systemSettings/currencies");
        return client.execute(data, (dataString) -> {
            JSONObject responseObject = JSON.parseObject(dataString);
            if (RESULT_CODE.equals(responseObject.getString("code"))) {
                JSONObject responseObjectdata = (JSONObject) responseObject.get("data");
                JSONArray responseObjectrecords = (JSONArray) responseObjectdata.get("records");
                return responseObjectrecords.toJavaList(CurrencyOption.class);
            } else {
                throw new CustomException("获取币种失败");
            }
        });
    }

    public CurrencyOption getGlobalPickByOptionLabel(String optionLabel) throws IOException, XsyHttpException {
        List<CurrencyOption> globalPickList = getGlobalPicks();
        return getOptionByLabel(globalPickList,optionLabel);
    }

    public CurrencyOption getOptionByLabel(List<CurrencyOption> globalPickList, String optionLabel) {
        if(optionLabel == null) {
            return new CurrencyOption();
        }
        if (globalPickList != null && globalPickList.size() > 0) {
            CurrencyOption pickOption = globalPickList.stream().filter(globalPick -> globalPick.getCurrencyCode().equals(optionLabel)).findAny().orElse(null);
            if(pickOption != null) {
                return pickOption;
            }
        }
        throw new CustomException("币种无该optionLabel：optionLabel:" + optionLabel);
    }


    public CurrencyOption getGlobalPickByOptionCode(Integer optionCode) throws IOException, XsyHttpException {
        List<CurrencyOption> globalPickList = getGlobalPicks();
        return getOptionByCode(globalPickList,optionCode);
    }

    public CurrencyOption getOptionByCode(List<CurrencyOption> globalPickList, Integer optionCode) {
        if(optionCode == null) {
            return new CurrencyOption();
        }
        if (globalPickList != null && globalPickList.size() > 0) {
            CurrencyOption pickOption = globalPickList.stream().filter(globalPick -> globalPick.getCode().equals(optionCode)).findAny().orElse(null);
            if(pickOption != null) {
                return pickOption;
            }
        }
        throw new CustomException("通用项无该code：optionCode:" + optionCode);
    }
}