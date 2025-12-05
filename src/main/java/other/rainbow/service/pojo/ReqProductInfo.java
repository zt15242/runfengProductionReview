package other.rainbow.service.pojo;

import java.util.ArrayList;
import java.util.List;

/**
 * @author 武于伦
 * @version 1.0
 * @date 2024/11/20 10:28
 * @e-mail 2717718875@qq.com
 * @description:
 */
public class ReqProductInfo {
    public String matnr;//物料号
    public String werks;//工厂
    public String mtart;//物料类型
    public String ltext_1;//中文描述
    public String ltext_e;//英文描述
    public String ltext_es;//西语描述
    public String ltext_pt;//葡语描述
    public String meins;//基本计量单位
    public String matkl;//物料组
    public String spart;//产品组
    public String mstae;//跨工厂的物料状态
    public String mstde;//有效起始期
    public String brgew;//毛重
    public String ntgew;//重量单位
    public String gewei;//净重
    public String volum;//业务量
    public String voleh;//体积单位
    public String labor;//实验室/办公室
    public String extwg;//外部物料组
    public String normt;//行业标准描述
    public List<SalesInfoRow> salev= new ArrayList<>();//销售视图
    public List<ProductUnitRow> units= new ArrayList<>();//产品单位

    public String getMatnr() {
        return matnr;
    }

    public void setMatnr(String matnr) {
        this.matnr = matnr;
    }

    public String getWerks() {
        return werks;
    }

    public void setWerks(String werks) {
        this.werks = werks;
    }

    public String getMtart() {
        return mtart;
    }

    public void setMtart(String mtart) {
        this.mtart = mtart;
    }

    public String getLtext_1() {
        return ltext_1;
    }

    public void setLtext_1(String ltext_1) {
        this.ltext_1 = ltext_1;
    }

    public String getLtext_e() {
        return ltext_e;
    }

    public void setLtext_e(String ltext_e) {
        this.ltext_e = ltext_e;
    }

    public String getLtext_es() {
        return ltext_es;
    }

    public void setLtext_es(String ltext_es) {
        this.ltext_es = ltext_es;
    }

    public String getLtext_pt() {
        return ltext_pt;
    }

    public void setLtext_pt(String ltext_pt) {
        this.ltext_pt = ltext_pt;
    }

    public String getMeins() {
        return meins;
    }

    public void setMeins(String meins) {
        this.meins = meins;
    }

    public String getMatkl() {
        return matkl;
    }

    public void setMatkl(String matkl) {
        this.matkl = matkl;
    }

    public String getSpart() {
        return spart;
    }

    public void setSpart(String spart) {
        this.spart = spart;
    }

    public String getMstae() {
        return mstae;
    }

    public void setMstae(String mstae) {
        this.mstae = mstae;
    }

    public String getMstde() {
        return mstde;
    }

    public void setMstde(String mstde) {
        this.mstde = mstde;
    }

    public String getBrgew() {
        return brgew;
    }

    public void setBrgew(String brgew) {
        this.brgew = brgew;
    }

    public String getNtgew() {
        return ntgew;
    }

    public void setNtgew(String ntgew) {
        this.ntgew = ntgew;
    }

    public String getGewei() {
        return gewei;
    }

    public void setGewei(String gewei) {
        this.gewei = gewei;
    }

    public String getVolum() {
        return volum;
    }

    public void setVolum(String volum) {
        this.volum = volum;
    }

    public String getVoleh() {
        return voleh;
    }

    public void setVoleh(String voleh) {
        this.voleh = voleh;
    }

    public String getLabor() {
        return labor;
    }

    public void setLabor(String labor) {
        this.labor = labor;
    }

    public String getExtwg() {
        return extwg;
    }

    public void setExtwg(String extwg) {
        this.extwg = extwg;
    }

    public String getNormt() {
        return normt;
    }

    public void setNormt(String normt) {
        this.normt = normt;
    }

    public List<SalesInfoRow> getSalev() {
        return salev;
    }

    public void setSalev(List<SalesInfoRow> salev) {
        this.salev = salev;
    }

    public List<ProductUnitRow> getUnits() {
        return units;
    }

    public void setUnits(List<ProductUnitRow> units) {
        this.units = units;
    }
}
