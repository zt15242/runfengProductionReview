package other.rainbow.service.pojo;

/**
 * @author 武于伦
 * @version 1.0
 * @date 2024/11/20 10:27
 * @e-mail 2717718875@qq.com
 * @description:
 */
public class ReqESInfo {
    public String instid;//接口编号
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

    @Override
    public String toString() {
        return "ReqESInfo{" +
                "instid='" + instid + '\'' +
                ", requesttime='" + requesttime + '\'' +
                '}';
    }
}
