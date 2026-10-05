package com.fc.v2.model.auto;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;

import java.io.Serializable;
import java.util.Date;

/**
 * 牌匾名册收讫裁决流水对象 t_oads_intake_journal（append-only，只 insert）。
 * 账实差以本表逐行重数为准；销账对不上时由收讫环节记 READJUST 纠销账并认账。
 *
 * @author fuce
 * @date 2026-10-05
 */
@TableName("t_oads_intake_journal")
@ApiModel(value = "TOadsIntakeJournal", description = "牌匾名册收讫裁决流水")
public class TOadsIntakeJournal implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @TableField("book_id")
    private Long bookId;

    @TableField("book_no")
    private String bookNo;

    @TableField("row_id")
    private Long rowId;

    @TableField("row_no")
    private Integer rowNo;

    @TableField("step_no")
    private Integer stepNo;

    /** COUNTED/CHECKED/ROUTED/RECEIPTED/RESUBMIT/READJUST/PHOTO_FILL/ARCHIVED */
    @TableField("event")
    private String event;

    @TableField("verdict")
    private String verdict;

    @TableField("reason_code")
    private String reasonCode;

    @TableField("detail")
    private String detail;

    @TableField("operator")
    private String operator;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss.SSS", timezone = "GMT+8")
    @TableField("acted_at")
    private Date actedAt;

    @TableField("create_by")
    private String createBy;

    @TableField("create_time")
    private Date createTime;

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

    public Long getRowId() {
        return rowId;
    }

    public void setRowId(Long rowId) {
        this.rowId = rowId;
    }

    public Integer getRowNo() {
        return rowNo;
    }

    public void setRowNo(Integer rowNo) {
        this.rowNo = rowNo;
    }

    public Integer getStepNo() {
        return stepNo;
    }

    public void setStepNo(Integer stepNo) {
        this.stepNo = stepNo;
    }

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public String getVerdict() {
        return verdict;
    }

    public void setVerdict(String verdict) {
        this.verdict = verdict;
    }

    public String getReasonCode() {
        return reasonCode;
    }

    public void setReasonCode(String reasonCode) {
        this.reasonCode = reasonCode;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public Date getActedAt() {
        return actedAt;
    }

    public void setActedAt(Date actedAt) {
        this.actedAt = actedAt;
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
}
