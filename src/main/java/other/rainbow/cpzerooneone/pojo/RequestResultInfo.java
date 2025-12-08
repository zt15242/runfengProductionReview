package other.rainbow.cpzerooneone.pojo;

import com.alibaba.fastjson.annotation.JSONField;

public class RequestResultInfo {
    @JSONField(name = "mobile")
    private String mobile;

    @JSONField(name = "message")
    private String message;

    @JSONField(name = "businessId")
    private String businessId;

    // Getters and Setters
    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getBusinessId() {
        return businessId;
    }

    public void setBusinessId(String businessId) {
        this.businessId = businessId;
    }
}