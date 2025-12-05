package other.rainbow.orderapp.cstss;

import java.io.Serializable;
import java.util.List;

public class ResponseBody implements Serializable {

    private RespESInfo esbinfo;

    private List<RespCustomerInfo> resultinfo;

    public RespESInfo getEsbinfo() {
        return esbinfo;
    }

    public void setEsbinfo(RespESInfo esbinfo) {
        this.esbinfo = esbinfo;
    }

    public List<RespCustomerInfo> getResultinfo() {
        return resultinfo;
    }

    public void setResultinfo(List<RespCustomerInfo> resultinfo) {
        this.resultinfo = resultinfo;
    }
}
