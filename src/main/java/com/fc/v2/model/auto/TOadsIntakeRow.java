package com.fc.v2.model.auto;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.util.Date;

/**
 * 牌匾名册收讫行账对象 t_oads_intake_row。
 * 一个物理报送行落一条（uk：册+原报行次）；同点位写两行各落各账，dup_seq 标出同码第几行。
 *
 * @author fuce
 * @date 2026-10-05
 */
@TableName("t_oads_intake_row")
@ApiModel(value = "TOadsIntakeRow", description = "牌匾名册收讫行账")
public class TOadsIntakeRow implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @TableField("book_id")
    private Long bookId;

    @TableField("book_no")
    private String bookNo;

    @TableField("row_no")
    private Integer rowNo;

    @TableField("spot_code")
    private String spotCode;

    @TableField("board_name")
    private String boardName;

    @TableField("road_code")
    private String roadCode;

    @TableField("qty")
    private Integer qty;

    @TableField("chk_num")
    private Integer chkNum;

    @TableField("qty_raw")
    private String qtyRaw;

    @TableField("chk_raw")
    private String chkRaw;

    @TableField("photo_no")
    private String photoNo;

    @TableField("photo_pending")
    private Integer photoPending;

    @TableField("matched_spot_id")
    private Long matchedSpotId;

    @TableField("master_board_name")
    private String masterBoardName;

    @TableField("master_road_code")
    private String masterRoadCode;

    @TableField("name_mismatch")
    private Integer nameMismatch;

    @TableField("road_mismatch")
    private Integer roadMismatch;

    @TableField("dup_seq")
    private Integer dupSeq;

    /** PENDING 待核 / PASS 通过 / BACK 退回，第四种写法不予写入 */
    @TableField("verdict")
    private String verdict;

    @TableField("back_reason_code")
    private String backReasonCode;

    @TableField("back_reason")
    private String backReason;

    @TableField("pending_note")
    private String pendingNote;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss.SSS", timezone = "GMT+8")
    @TableField("pending_since")
    private Date pendingSince;

    @TableField("archived")
    private Integer archived;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss.SSS", timezone = "GMT+8")
    @TableField("archived_at")
    private Date archivedAt;

    @TableField("channel")
    private String channel;

    @TableField("del_flag")
    private Integer delFlag;

    @TableField(value = "create_by", fill = FieldFill.INSERT)
    private String createBy;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    @TableField(value = "update_by", fill = FieldFill.UPDATE)
    private String updateBy;

    @TableField(value = "update_time", fill = FieldFill.UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getBookId() {
        return bookId;
    }

    public void setBookId(Long bookId) {
        this.bookId = bookId;
    }

    public String getBookNo() {
        return bookNo;
    }

    public void setBookNo(String bookNo) {
        this.bookNo = bookNo;
    }

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

    public Integer getQty() {
        return qty;
    }

    public void setQty(Integer qty) {
        this.qty = qty;
    }

    public Integer getChkNum() {
        return chkNum;
    }

    public void setChkNum(Integer chkNum) {
        this.chkNum = chkNum;
    }

    public String getQtyRaw() {
        return qtyRaw;
    }

    public void setQtyRaw(String qtyRaw) {
        this.qtyRaw = qtyRaw;
    }

    public String getChkRaw() {
        return chkRaw;
    }

    public void setChkRaw(String chkRaw) {
        this.chkRaw = chkRaw;
    }

    public String getPhotoNo() {
        return photoNo;
    }

    public void setPhotoNo(String photoNo) {
        this.photoNo = photoNo;
    }

    public Integer getPhotoPending() {
        return photoPending;
    }

    public void setPhotoPending(Integer photoPending) {
        this.photoPending = photoPending;
    }

    public Long getMatchedSpotId() {
        return matchedSpotId;
    }

    public void setMatchedSpotId(Long matchedSpotId) {
        this.matchedSpotId = matchedSpotId;
    }

    public String getMasterBoardName() {
        return masterBoardName;
    }

    public void setMasterBoardName(String masterBoardName) {
        this.masterBoardName = masterBoardName;
    }

    public String getMasterRoadCode() {
        return masterRoadCode;
    }

    public void setMasterRoadCode(String masterRoadCode) {
        this.masterRoadCode = masterRoadCode;
    }

    public Integer getNameMismatch() {
        return nameMismatch;
    }

    public void setNameMismatch(Integer nameMismatch) {
        this.nameMismatch = nameMismatch;
    }

    public Integer getRoadMismatch() {
        return roadMismatch;
    }

    public void setRoadMismatch(Integer roadMismatch) {
        this.roadMismatch = roadMismatch;
    }

    public Integer getDupSeq() {
        return dupSeq;
    }

    public void setDupSeq(Integer dupSeq) {
        this.dupSeq = dupSeq;
    }

    public String getVerdict() {
        return verdict;
    }

    public void setVerdict(String verdict) {
        this.verdict = verdict;
    }

    public String getBackReasonCode() {
        return backReasonCode;
    }

    public void setBackReasonCode(String backReasonCode) {
        this.backReasonCode = backReasonCode;
    }

    public String getBackReason() {
        return backReason;
    }

    public void setBackReason(String backReason) {
        this.backReason = backReason;
    }

    public String getPendingNote() {
        return pendingNote;
    }

    public void setPendingNote(String pendingNote) {
        this.pendingNote = pendingNote;
    }

    public Date getPendingSince() {
        return pendingSince;
    }

    public void setPendingSince(Date pendingSince) {
        this.pendingSince = pendingSince;
    }

    public Integer getArchived() {
        return archived;
    }

    public void setArchived(Integer archived) {
        this.archived = archived;
    }

    public Date getArchivedAt() {
        return archivedAt;
    }

    public void setArchivedAt(Date archivedAt) {
        this.archivedAt = archivedAt;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public Integer getDelFlag() {
        return delFlag;
    }

    public void setDelFlag(Integer delFlag) {
        this.delFlag = delFlag;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }
}
