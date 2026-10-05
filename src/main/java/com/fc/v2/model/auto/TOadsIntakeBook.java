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
 * 牌匾名册收讫册账（销账）对象 t_oads_intake_book。
 * 一次报送一册一行；收讫成功的册才占批次码（dup_key 为库内生成列，不由实体写）。
 *
 * @author fuce
 * @date 2026-10-05
 */
@TableName("t_oads_intake_book")
@ApiModel(value = "TOadsIntakeBook", description = "牌匾名册收讫册账")
public class TOadsIntakeBook implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键")
    private Long id;

    @TableField("book_no")
    @ApiModelProperty(value = "册号")
    private String bookNo;

    @TableField("batch_no")
    @ApiModelProperty(value = "批次码")
    private String batchNo;

    @TableField("quarter_code")
    @ApiModelProperty(value = "上送季度码")
    private String quarterCode;

    @TableField("unit_no")
    @ApiModelProperty(value = "归并报送单位代号")
    private String unitNo;

    @TableField("channel")
    @ApiModelProperty(value = "通道 DISTRICT/CITY")
    private String channel;

    @TableField("row_limit_raw")
    @ApiModelProperty(value = "抬头上限那格原文")
    private String rowLimitRaw;

    @TableField("row_limit")
    @ApiModelProperty(value = "写定上限（空/负/没边按200）")
    private Integer rowLimit;

    @TableField("row_count_physical")
    @ApiModelProperty(value = "进门数出来的物理行数")
    private Integer rowCountPhysical;

    @TableField("in_count")
    @ApiModelProperty(value = "回执·入库行数")
    private Integer inCount;

    @TableField("back_count")
    @ApiModelProperty(value = "回执·退回行数")
    private Integer backCount;

    @TableField("status")
    @ApiModelProperty(value = "册情形 1已收讫 2整册超限退回")
    private Integer status;

    @TableField("book_set")
    @ApiModelProperty(value = "册层 CURRENT/HISTORY")
    private String bookSet;

    @TableField("archived_count")
    @ApiModelProperty(value = "已转历史卷宗层的待核行数")
    private Integer archivedCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss.SSS", timezone = "GMT+8")
    @TableField("recv_at")
    @ApiModelProperty(value = "出回执时刻")
    private Date recvAt;

    @TableField("del_flag")
    @ApiModelProperty(value = "删除标记")
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

    @TableField("remark")
    private String remark;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public String getRowLimitRaw() {
        return rowLimitRaw;
    }

    public void setRowLimitRaw(String rowLimitRaw) {
        this.rowLimitRaw = rowLimitRaw;
    }

    public Integer getRowLimit() {
        return rowLimit;
    }

    public void setRowLimit(Integer rowLimit) {
        this.rowLimit = rowLimit;
    }

    public Integer getRowCountPhysical() {
        return rowCountPhysical;
    }

    public void setRowCountPhysical(Integer rowCountPhysical) {
        this.rowCountPhysical = rowCountPhysical;
    }

    public Integer getInCount() {
        return inCount;
    }

    public void setInCount(Integer inCount) {
        this.inCount = inCount;
    }

    public Integer getBackCount() {
        return backCount;
    }

    public void setBackCount(Integer backCount) {
        this.backCount = backCount;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getBookSet() {
        return bookSet;
    }

    public void setBookSet(String bookSet) {
        this.bookSet = bookSet;
    }

    public Integer getArchivedCount() {
        return archivedCount;
    }

    public void setArchivedCount(Integer archivedCount) {
        this.archivedCount = archivedCount;
    }

    public Date getRecvAt() {
        return recvAt;
    }

    public void setRecvAt(Date recvAt) {
        this.recvAt = recvAt;
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

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
