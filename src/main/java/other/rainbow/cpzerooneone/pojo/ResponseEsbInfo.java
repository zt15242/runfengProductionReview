package other.rainbow.cpzerooneone.pojo;

import com.alibaba.fastjson.annotation.JSONField;

public class ResponseEsbInfo {
    @JSONField(name = "instid")
    private String instid;

    @JSONField(name = "requesttime")
    private String requesttime;

    @JSONField(name = "responsetime")
    private String responsetime;

    @JSONField(name = "returnstatus")
    private String returnstatus;

    @JSONField(name = "returncode")
    private String returncode;

    @JSONField(name = "returnmsg")
    private String returnmsg;

    @JSONField(name = "attr1")
    private String attr1;

    @JSONField(name = "attr2")
    private String attr2;

    @JSONField(name = "attr3")
    private String attr3;

    // Getters and Setters
    public String getInstid() {
        return instid;
    }

    public void setInstid(String instid) {
        this.instid = instid;
    }

    public String getRequesttime() {
        return requesttime;
    }

    public void setRequesttime(String requesttime) {
        this.requesttime = requesttime;
    }

    public String getResponsetime() {
        return responsetime;
    }

    public void setResponsetime(String responsetime) {
        this.responsetime = responsetime;
    }

    public String getReturnstatus() {
        return returnstatus;
    }

    public void setReturnstatus(String returnstatus) {
        this.returnstatus = returnstatus;
    }

    public String getReturncode() {
        return returncode;
    }

    public void setReturncode(String returncode) {
        this.returncode = returncode;
    }

    public String getReturnmsg() {
        return returnmsg;
    }

    public void setReturnmsg(String returnmsg) {
        this.returnmsg = returnmsg;
    }

    public String getAttr1() {
        return attr1;
    }

    public void setAttr1(String attr1) {
        this.attr1 = attr1;
    }

    public String getAttr2() {
        return attr2;
    }

    public void setAttr2(String attr2) {
        this.attr2 = attr2;
    }

    public String getAttr3() {
        return attr3;
    }

    public void setAttr3(String attr3) {
        this.attr3 = attr3;
    }
}