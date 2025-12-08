package other.rainbow.cpzerooneone.pojo;

import com.alibaba.fastjson.annotation.JSONField;

public class SMSRequest {
    @JSONField(name = "resultinfo")
    private RequestResultInfo resultInfo;

    // Getters and Setters
    public RequestResultInfo getResultInfo() {
        return resultInfo;
    }

    public void setResultInfo(RequestResultInfo resultInfo) {
        this.resultInfo = resultInfo;
    }
}