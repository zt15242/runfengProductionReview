package other.rainbow.service.pojo;

/**
 * @author 武于伦
 * @version 1.0
 * @date 2024/11/20 10:22
 * @e-mail 2717718875@qq.com
 * @description:
 */
public class RespCustomerInfo {
    public String msgty;//处理状态
    public String msgtx;//错误信息

    public String getMsgty() {
        return msgty;
    }

    public void setMsgty(String msgty) {
        this.msgty = msgty;
    }

    public String getMsgtx() {
        return msgtx;
    }

    public void setMsgtx(String msgtx) {
        this.msgtx = msgtx;
    }

    @Override
    public String toString() {
        return "RespCustomerInfo{" +
                "msgty='" + msgty + '\'' +
                ", msgtx='" + msgtx + '\'' +
                '}';
    }
}
