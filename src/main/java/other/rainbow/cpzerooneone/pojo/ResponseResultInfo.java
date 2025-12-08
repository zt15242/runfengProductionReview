package other.rainbow.cpzerooneone.pojo;

import com.alibaba.fastjson.annotation.JSONField;

public class ResponseResultInfo {
    @JSONField(name = "code")
    private int code;

    @JSONField(name = "msg")
    private String msg;

    @JSONField(name = "data")
    private String data;

    // Getters and Setters
    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }
}