package other.rainbow.cpzerooneone.pojo.CustomerSales;

import java.util.List;

public class ResponseBody {
    public RespESInfo esbinfo;
    public List<RespCustomerInfo> resultinfo;

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
