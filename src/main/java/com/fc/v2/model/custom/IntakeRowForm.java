package com.fc.v2.model.custom;

/**
 * 收讫册内一行报文（一行对应一处点位）。
 *
 * <p>两个数量（应更换处数、核验合格处数）以原文交来，两项须同时到达，只到一项不代填、不默认零；
 * 点位身份只认 {@link #spotCode} 精确配主档，版面名称近似去配的不算配上；
 * 现场照片缺时 {@link #photoNo} 留空，行照样占位，下一轮核看补挂。
 *
 * @author fuce
 * @date 2026-10-05
 */
public class IntakeRowForm {

    /** 原报行次（可不传，缺则按报文物理顺序点行次；一册内不许重行次） */
    private Integer rowNo;

    /** 点位编码：身份只认它，配主档只认全等，位数不够/缺填/超长都按各自规矩处理 */
    private String spotCode;

    /** 版面名称：只用来与主档比对，近似配不作数；两边有出入以主档为准 */
    private String boardName;

    /** 路段代号：只用来与主档比对；两边有出入以主档为准 */
    private String roadCode;

    /** 应更换处数原文（缺/写不成整数照存，按这一项没到处理） */
    private String qtyText;

    /** 核验合格处数原文（与应更换处数两项须同时到达） */
    private String chkText;

    /** 现场照片附件编号；缺照片先空着，下一轮核看时补挂 */
    private String photoNo;

    public Integer getRowNo() {
        return rowNo;
    }

    public void setRowNo(Integer rowNo) {
        this.rowNo = rowNo;
    }

    public String getSpotCode() {
        return spotCode;
    }

    public void setSpotCode(String spotCode) {
        this.spotCode = spotCode;
    }

    public String getBoardName() {
        return boardName;
    }

    public void setBoardName(String boardName) {
        this.boardName = boardName;
    }

    public String getRoadCode() {
        return roadCode;
    }

    public void setRoadCode(String roadCode) {
        this.roadCode = roadCode;
    }

    public String getQtyText() {
        return qtyText;
    }

    public void setQtyText(String qtyText) {
        this.qtyText = qtyText;
    }

    public String getChkText() {
        return chkText;
    }

    public void setChkText(String chkText) {
        this.chkText = chkText;
    }

    public String getPhotoNo() {
        return photoNo;
    }

    public void setPhotoNo(String photoNo) {
        this.photoNo = photoNo;
    }
}
