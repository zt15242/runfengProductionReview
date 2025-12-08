package other.rainbow.cpzerooneone.pojo.CustomerSales;

import java.util.List;

public class salesInfo {
    public String kunnr;//客户编码
    public String vkorg;//销售组织
    public String vtweg;//分销渠道
    public String spart;//产品组
    public String bzirk;//销售地区
    public String kdgrp;//客户组
    public String kvgr1;//客户组1
    public String kvgr3;//客户组3
    public String vkbur;//销售部门
    public String vkgrp;//销售组
    public String waers;//币种
    public String konda;//价格组
    public String kalks;//Cust.Price.过程
    public String vwerk;//交货工厂
    public String vsbed;//装运条件
    public String zterm;//付款条件
    public String ktgrd;//客户科目分配组
    public String inco1;//国际贸易条款
    public String linco2_l;//国贸条款位置 1
    public List<TaxRow> TAXKDS;//税收类别

    public String getKunnr() {
        return kunnr;
    }

    public void setKunnr(String kunnr) {
        this.kunnr = kunnr;
    }

    public String getVkorg() {
        return vkorg;
    }

    public void setVkorg(String vkorg) {
        this.vkorg = vkorg;
    }

    public String getVtweg() {
        return vtweg;
    }

    public void setVtweg(String vtweg) {
        this.vtweg = vtweg;
    }

    public String getSpart() {
        return spart;
    }

    public void setSpart(String spart) {
        this.spart = spart;
    }

    public String getBzirk() {
        return bzirk;
    }

    public void setBzirk(String bzirk) {
        this.bzirk = bzirk;
    }

    public String getKdgrp() {
        return kdgrp;
    }

    public void setKdgrp(String kdgrp) {
        this.kdgrp = kdgrp;
    }

    public String getKvgr1() {
        return kvgr1;
    }

    public void setKvgr1(String kvgr1) {
        this.kvgr1 = kvgr1;
    }

    public String getKvgr3() {
        return kvgr3;
    }

    public void setKvgr3(String kvgr3) {
        this.kvgr3 = kvgr3;
    }

    public String getVkbur() {
        return vkbur;
    }

    public void setVkbur(String vkbur) {
        this.vkbur = vkbur;
    }

    public String getVkgrp() {
        return vkgrp;
    }

    public void setVkgrp(String vkgrp) {
        this.vkgrp = vkgrp;
    }

    public String getWaers() {
        return waers;
    }

    public void setWaers(String waers) {
        this.waers = waers;
    }

    public String getKonda() {
        return konda;
    }

    public void setKonda(String konda) {
        this.konda = konda;
    }

    public String getKalks() {
        return kalks;
    }

    public void setKalks(String kalks) {
        this.kalks = kalks;
    }

    public String getVwerk() {
        return vwerk;
    }

    public void setVwerk(String vwerk) {
        this.vwerk = vwerk;
    }

    public String getVsbed() {
        return vsbed;
    }

    public void setVsbed(String vsbed) {
        this.vsbed = vsbed;
    }

    public String getZterm() {
        return zterm;
    }

    public void setZterm(String zterm) {
        this.zterm = zterm;
    }

    public String getKtgrd() {
        return ktgrd;
    }

    public void setKtgrd(String ktgrd) {
        this.ktgrd = ktgrd;
    }

    public String getInco1() {
        return inco1;
    }

    public void setInco1(String inco1) {
        this.inco1 = inco1;
    }

    public String getLinco2_l() {
        return linco2_l;
    }

    public void setLinco2_l(String linco2_l) {
        this.linco2_l = linco2_l;
    }

    public List<TaxRow> getTAXKDS() {
        return TAXKDS;
    }

    public void setTAXKDS(List<TaxRow> TAXKDS) {
        this.TAXKDS = TAXKDS;
    }
}
