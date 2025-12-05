package other.rainbow.service.pojo;

import java.util.List;

/**
 * @author 武于伦
 * @version 1.0
 * @date 2024/11/20 10:34
 * @e-mail 2717718875@qq.com
 * @description:
 */
public class RequestParameters {
    public ReqESInfo esbinfo;
    public List<ReqProductInfo> resultinfo;

    public ReqESInfo getEsbinfo() {
        return esbinfo;
    }

    public void setEsbinfo(ReqESInfo esbinfo) {
        this.esbinfo = esbinfo;
    }

    public List<ReqProductInfo> getResultinfo() {
        return resultinfo;
    }

    public void setResultinfo(List<ReqProductInfo> resultinfo) {
        this.resultinfo = resultinfo;
    }
}
