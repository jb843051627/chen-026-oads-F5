package com.fc.v2.model.custom;

import java.util.ArrayList;
import java.util.List;

/**
 * 收讫环节整册报送报文：一次报送一册，一册含若干行，提交形态固定为一册一次交完整，
 * 不接零散逐行报送。四步连着做完才出回执，中间没有人工兜底。
 *
 * <p>单册行数上限只认本册抬头 {@link #rowLimitText} 那一格；批次码 + 通道用来认
 * "区级报送/市级复核各递一回"的重投；行不许游移到别册，行次在一册内固定。
 *
 * @author fuce
 * @date 2026-10-05
 */
public class IntakeBookForm {

    /** 册号（区局按季度名册合成一册的编号） */
    private String bookNo;

    /** 批次码（双通道重投认它） */
    private String batchNo;

    /** 上送季度码 如2026Q4 */
    private String quarterCode;

    /** 归并报送的单位代号（各单位先归并到区里） */
    private String unitNo;

    /** 通道 DISTRICT 区级报送 / CITY 市级复核，由入口点，不收第四种写法 */
    private String channel;

    /**
     * 本册抬头行数上限那格的原文。唯一来源：
     * 空着按 200 认；写成负数或大得没边的按没填认，也按 200；别处不设第二条来源。
     */
    private String rowLimitText;

    /** 册内各行（物理行，一行对应一处点位；同点位写两行就是两行） */
    private List<IntakeRowForm> rows = new ArrayList<IntakeRowForm>();

    public String getBookNo() {
        return bookNo;
    }

    public void setBookNo(String bookNo) {
        this.bookNo = bookNo;
    }

    public String getBatchNo() {
        return batchNo;
    }

    public void setBatchNo(String batchNo) {
        this.batchNo = batchNo;
    }

    public String getQuarterCode() {
        return quarterCode;
    }

    public void setQuarterCode(String quarterCode) {
        this.quarterCode = quarterCode;
    }

    public String getUnitNo() {
        return unitNo;
    }

    public void setUnitNo(String unitNo) {
        this.unitNo = unitNo;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getRowLimitText() {
        return rowLimitText;
    }

    public void setRowLimitText(String rowLimitText) {
        this.rowLimitText = rowLimitText;
    }

    public List<IntakeRowForm> getRows() {
        return rows;
    }

    public void setRows(List<IntakeRowForm> rows) {
        this.rows = rows;
    }
}
