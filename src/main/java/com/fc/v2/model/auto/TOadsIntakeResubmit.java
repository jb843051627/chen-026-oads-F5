package com.fc.v2.model.auto;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;

import java.io.Serializable;
import java.util.Date;

/**
 * 收讫双通道重投另登对象 t_oads_intake_resubmit。
 * 同批次码经区级报送与市级复核各递一回：第二回不入账，在本表另登一笔，指向首收册。
 *
 * @author fuce
 * @date 2026-10-05
 */
@TableName("t_oads_intake_resubmit")
@ApiModel(value = "TOadsIntakeResubmit", description = "收讫双通道重投另登")
public class TOadsIntakeResubmit implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @TableField("batch_no")
    private String batchNo;

    @TableField("first_book_id")
    private Long firstBookId;

    @TableField("first_book_no")
    private String firstBookNo;

    @TableField("first_channel")
    private String firstChannel;

    @TableField("second_channel")
    private String secondChannel;

    @TableField("second_row_count")
    private Integer secondRowCount;

    @TableField("note")
    private String note;

    @TableField("del_flag")
    private Integer delFlag;

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

    public String getBatchNo() {
        return batchNo;
    }

    public void setBatchNo(String batchNo) {
        this.batchNo = batchNo;
    }

    public Long getFirstBookId() {
        return firstBookId;
    }

    public void setFirstBookId(Long firstBookId) {
        this.firstBookId = firstBookId;
    }

    public String getFirstBookNo() {
        return firstBookNo;
    }

    public void setFirstBookNo(String firstBookNo) {
        this.firstBookNo = firstBookNo;
    }

    public String getFirstChannel() {
        return firstChannel;
    }

    public void setFirstChannel(String firstChannel) {
        this.firstChannel = firstChannel;
    }

    public String getSecondChannel() {
        return secondChannel;
    }

    public void setSecondChannel(String secondChannel) {
        this.secondChannel = secondChannel;
    }

    public Integer getSecondRowCount() {
        return secondRowCount;
    }

    public void setSecondRowCount(Integer secondRowCount) {
        this.secondRowCount = secondRowCount;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
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
}
