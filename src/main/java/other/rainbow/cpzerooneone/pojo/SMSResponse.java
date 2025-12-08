package other.rainbow.cpzerooneone.pojo;

import com.alibaba.fastjson.annotation.JSONField;

public class SMSResponse {
    @JSONField(name = "esbinfo")
    private ResponseEsbInfo esbInfo;

    @JSONField(name = "resultinfo")
    private ResponseResultInfo resultInfo;

    // Getters and Setters
    public ResponseEsbInfo getEsbInfo() {
        return esbInfo;
    }

    public void setEsbInfo(ResponseEsbInfo esbInfo) {
        this.esbInfo = esbInfo;
    }

    public ResponseResultInfo getResultInfo() {
        return resultInfo;
    }

    public void setResultInfo(ResponseResultInfo resultInfo) {
        this.resultInfo = resultInfo;
    }
}
