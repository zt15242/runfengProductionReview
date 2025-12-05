package other.rainbow.orderapp.cstss;

import java.io.Serializable;
import java.util.List;

public class RequestBody implements Serializable {

    private ReqESInfo esbinfo;

    private List<ReqCustomerInfo> resultinfo;

    private String iex01;//预留字段1

    private String iex02;//预留字段2

    private String iex03;//预留字段3

    private String iex04;//预留字段4

    private String iex05;//预留字段5

    private String iex06;//预留字段6

    public ReqESInfo getEsbinfo() {
        return esbinfo;
    }

    public void setEsbinfo(ReqESInfo esbinfo) {
        this.esbinfo = esbinfo;
    }

    public List<ReqCustomerInfo> getResultinfo() {
        return resultinfo;
    }

    public void setResultinfo(List<ReqCustomerInfo> resultinfo) {
        this.resultinfo = resultinfo;
    }

    public String getIex01() {
        return iex01;
    }

    public void setIex01(String iex01) {
        this.iex01 = iex01;
    }

    public String getIex02() {
        return iex02;
    }

    public void setIex02(String iex02) {
        this.iex02 = iex02;
    }

    public String getIex03() {
        return iex03;
    }

    public void setIex03(String iex03) {
        this.iex03 = iex03;
    }

    public String getIex04() {
        return iex04;
    }

    public void setIex04(String iex04) {
        this.iex04 = iex04;
    }

    public String getIex05() {
        return iex05;
    }

    public void setIex05(String iex05) {
        this.iex05 = iex05;
    }

    public String getIex06() {
        return iex06;
    }

    public void setIex06(String iex06) {
        this.iex06 = iex06;
    }

    @Override
    public String toString() {
        return "RequestBody{" +
                "esbinfo=" + esbinfo +
                ", resultinfo=" + resultinfo +
                ", iex01='" + iex01 + '\'' +
                ", iex02='" + iex02 + '\'' +
                ", iex03='" + iex03 + '\'' +
                ", iex04='" + iex04 + '\'' +
                ", iex05='" + iex05 + '\'' +
                ", iex06='" + iex06 + '\'' +
                '}';
    }
}
