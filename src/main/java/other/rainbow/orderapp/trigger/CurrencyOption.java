package other.rainbow.orderapp.trigger;

import java.io.Serializable;

public class CurrencyOption implements Serializable {

    private  Integer code;

    private String currencyCode;

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    @Override
    public String toString() {
        return "CurrencyOption{" +
                "code=" + code +
                ", currencyCode='" + currencyCode + '\'' +
                '}';
    }
}
