package other.rainbow.service.pojo;

/**
 * @author 武于伦
 * @version 1.0
 * @date 2024/11/20 10:23
 * @e-mail 2717718875@qq.com
 * @description:
 */
public class ResponseBody {
    public RespESInfo esbinfo;
    public RespCustomerInfo resultinfo;

    public RespESInfo getEsbinfo() {
        return esbinfo;
    }

    public void setEsbinfo(RespESInfo esbinfo) {
        this.esbinfo = esbinfo;
    }

    public RespCustomerInfo getResultinfo() {
        return resultinfo;
    }

    public void setResultinfo(RespCustomerInfo resultinfo) {
        this.resultinfo = resultinfo;
    }

    @Override
    public String toString() {
        return "ResponseBody{" +
                "esbinfo=" + esbinfo +
                ", resultinfo=" + resultinfo +
                '}';
    }
}
