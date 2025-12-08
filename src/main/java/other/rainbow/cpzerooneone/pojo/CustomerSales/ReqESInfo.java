package other.rainbow.cpzerooneone.pojo.CustomerSales;

public class ReqESInfo {
    public String instid;//接口编号
    public String attr1;//预留字段
    public String attr2;//预留字段

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

    public String requesttime;//请求时间

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
}
