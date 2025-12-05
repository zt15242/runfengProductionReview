package other.rainbow.orderapp.cstss;

import java.io.Serializable;


public class ReqESInfo implements Serializable {
    private String instid;//接口编号

    private String requesttime;//请求时间

    private String attr1;//预留字段

    private String attr2;//预留字段

    private String attr3;//预留字段

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

    @Override
    public String toString() {
        return "ReqESInfo{" +
                "instid='" + instid + '\'' +
                ", requesttime='" + requesttime + '\'' +
                ", attr1='" + attr1 + '\'' +
                ", attr2='" + attr2 + '\'' +
                ", attr3='" + attr3 + '\'' +
                '}';
    }
}
