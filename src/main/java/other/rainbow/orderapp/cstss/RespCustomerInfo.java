package other.rainbow.orderapp.cstss;

import java.io.Serializable;
import java.math.BigDecimal;

public class RespCustomerInfo implements Serializable {

    private String partner;  //业务伙伴编码

    private BigDecimal amount;   //剩余额度

    private String WAERS; //币种

    private String zsfyq;//是否逾期

    private String exch_rate;//汇率

    private String msgty;

    private String msgtx;

    public String getPartner() {
        return partner;
    }

    public void setPartner(String partner) {
        this.partner = partner;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getWAERS() {
        return WAERS;
    }

    public void setWAERS(String WAERS) {
        this.WAERS = WAERS;
    }

    public String getZsfyq() {
        return zsfyq;
    }

    public void setZsfyq(String zsfyq) {
        this.zsfyq = zsfyq;
    }

    public String getExch_rate() {
        return exch_rate;
    }

    public void setExch_rate(String exch_rate) {
        this.exch_rate = exch_rate;
    }

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
                "partner='" + partner + '\'' +
                ", amount=" + amount +
                ", WAERS='" + WAERS + '\'' +
                ", zsfyq='" + zsfyq + '\'' +
                ", exch_rate='" + exch_rate + '\'' +
                ", msgty='" + msgty + '\'' +
                ", msgtx='" + msgtx + '\'' +
                '}';
    }
}
