package other.rainbow.orderapp.pojo;

import java.util.List;

public class RequestBody {
    private Object data;
    private reqesbinfo esbinfo;
    private List<items> resultinfo;

    public RequestBody() {

    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
    public RequestBody(Object data){
        this.data = data;
    }

    public reqesbinfo getEsbinfo() {
        return esbinfo;
    }

    public void setEsbinfo(reqesbinfo esbinfo) {
        this.esbinfo = esbinfo;
    }

    public List<items> getResultinfo() {
        return resultinfo;
    }

    public void setResultinfo(List<items> resultinfo) {
        this.resultinfo = resultinfo;
    }
}
