package other.rainbow.cpzerooneone.pojo.CustomerSales;

import java.util.List;

public class RequestBody {
    public ReqESInfo esbinfo;
    public List<ReqCustomerSalesInfo> resultinfo;

    public ReqESInfo getEsbinfo() {
        return esbinfo;
    }

    public void setEsbinfo(ReqESInfo esbinfo) {
        this.esbinfo = esbinfo;
    }

    public List<ReqCustomerSalesInfo> getResultinfo() {
        return resultinfo;
    }

    public void setResultinfo(List<ReqCustomerSalesInfo> resultinfo) {
        this.resultinfo = resultinfo;
    }
}
