package other.rainbow.autoaccount.pojo;
import java.util.List;
public class RequestBody {
    public reqesbinfo esbinfo;
    public List<reqresultinfo> resultinfo;
    public List<ReqCustomerlimit> resCustomerlimitinfo;
    public String iex01;//预留字段1
    public String iex02;//预留字段2
    public String iex03;//预留字段3
    public String iex04;//预留字段4
    public String iex05;//预留字段5
    public String iex06;//预留字段6

    public List<ReqCustomerlimit> getResCustomerlimitinfo() {
        return resCustomerlimitinfo;
    }

    public void setResCustomerlimitinfo(List<ReqCustomerlimit> resCustomerlimitinfo) {
        this.resCustomerlimitinfo = resCustomerlimitinfo;
    }

    public reqesbinfo getEsbinfo() {
        return esbinfo;
    }

    public void setEsbinfo(reqesbinfo esbinfo) {
        this.esbinfo = esbinfo;
    }

    public List<reqresultinfo> getResultinfo() {
        return resultinfo;
    }

    public void setResultinfo(List<reqresultinfo> resultinfo) {
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


}
