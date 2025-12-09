package other.rainbow.autoaccount.pojo.CustomerSales;

import java.util.List;

public class ReqCustomerSalesInfo {
    public String ktokk;
    public String partner;
    public List<FinancialRow> flucu00;
    public List<TaxNumberRow> taxnums;
    public List<salesInfo> flucu01;

    public String getKtokk() {
        return ktokk;
    }

    public void setKtokk(String ktokk) {
        this.ktokk = ktokk;
    }

    public String getPartner() {
        return partner;
    }

    public void setPartner(String partner) {
        this.partner = partner;
    }

    public List<FinancialRow> getFlucu00() {
        return flucu00;
    }

    public void setFlucu00(List<FinancialRow> flucu00) {
        this.flucu00 = flucu00;
    }

    public List<TaxNumberRow> getTaxnums() {
        return taxnums;
    }

    public void setTaxnums(List<TaxNumberRow> taxnums) {
        this.taxnums = taxnums;
    }

    public List<salesInfo> getFlucu01() {
        return flucu01;
    }

    public void setFlucu01(List<salesInfo> flucu01) {
        this.flucu01 = flucu01;
    }
}
