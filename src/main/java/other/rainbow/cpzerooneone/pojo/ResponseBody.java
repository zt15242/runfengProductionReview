package other.rainbow.cpzerooneone.pojo;
import java.util.List;
public class ResponseBody {
    public respesbinfo esbinfo;
    public List<respresultinfo> resultinfo;

    public respesbinfo getEsbinfo() {
        return esbinfo;
    }

    public void setEsbinfo(respesbinfo esbinfo) {
        this.esbinfo = esbinfo;
    }

    public List<respresultinfo> getResultinfo() {
        return resultinfo;
    }

    public void setResultinfo(List<respresultinfo> resultinfo) {
        this.resultinfo = resultinfo;
    }
}
