package com.fc.v2.model.custom;

import java.util.ArrayList;
import java.util.List;

/**
 * 收讫回执。面上只有三个数字：本册行数、入库行数、退回行数，且后两者之和须等于第一个。
 *
 * <p>三个数一律回库（册账 + 行账 + 流水）逐行重数出来，页面那份只是转述，两边出自同一次计算；
 * 任选一行复核，这一行的结论要与三个数互相印证，对不齐的一行一行点名，不说"基本相符"。
 * 报文版面名称/路段代号与主档有出入的行（以主档为准），在 {@link #mismatchRows} 里逐行列。
 *
 * @author fuce
 * @date 2026-10-05
 */
public class IntakeReceipt {

    /** 任选一行复核的一条：结论与三个数是否互印。 */
    public static class RowCheck {
        private Long rowId;
        private int rowNo;
        private String spotCode;
        private String verdict;
        private String verdictName;
        /** 这一行落在哪一摞（入库/退回），与三数口径是否一致 */
        private boolean consistent;
        /** 对不齐时点名说差在哪；对得齐为 null */
        private String mismatchNote;
        private String backReason;
        private String pendingNote;
        private boolean nameMismatch;
        private boolean roadMismatch;
        /** 已转入历史卷宗层（行照常可查；现行名单据此不再列出，一条不减） */
        private boolean archived;
        private String masterBoardName;
        private String masterRoadCode;

        public Long getRowId() {
            return rowId;
        }

        public void setRowId(Long rowId) {
            this.rowId = rowId;
        }

        public int getRowNo() {
            return rowNo;
        }

        public void setRowNo(int rowNo) {
            this.rowNo = rowNo;
        }

        public String getSpotCode() {
            return spotCode;
        }

        public void setSpotCode(String spotCode) {
            this.spotCode = spotCode;
        }

        public String getVerdict() {
            return verdict;
        }

        public void setVerdict(String verdict) {
            this.verdict = verdict;
        }

        public String getVerdictName() {
            return verdictName;
        }

        public void setVerdictName(String verdictName) {
            this.verdictName = verdictName;
        }

        public boolean isConsistent() {
            return consistent;
        }

        public void setConsistent(boolean consistent) {
            this.consistent = consistent;
        }

        public String getMismatchNote() {
            return mismatchNote;
        }

        public void setMismatchNote(String mismatchNote) {
            this.mismatchNote = mismatchNote;
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

        public boolean isNameMismatch() {
            return nameMismatch;
        }

        public void setNameMismatch(boolean nameMismatch) {
            this.nameMismatch = nameMismatch;
        }

        public boolean isRoadMismatch() {
            return roadMismatch;
        }

        public void setRoadMismatch(boolean roadMismatch) {
            this.roadMismatch = roadMismatch;
        }

        public String getMasterBoardName() {
            return masterBoardName;
        }

        public void setMasterBoardName(String masterBoardName) {
            this.masterBoardName = masterBoardName;
        }

        public boolean isArchived() {
            return archived;
        }

        public void setArchived(boolean archived) {
            this.archived = archived;
        }

        public String getMasterRoadCode() {
            return masterRoadCode;
        }

        public void setMasterRoadCode(String masterRoadCode) {
            this.masterRoadCode = masterRoadCode;
        }
    }

    private Long bookId;
    private String bookNo;
    private String batchNo;
    private String quarterCode;
    private String channel;
    private String channelName;

    /** 抬头那格原文与写定上限（空/负/没边按 200） */
    private String rowLimitRaw;
    private int rowLimit;

    /** 回执三数（回库逐行重数） */
    private int totalCount;
    private int storedCount;
    private int backCount;

    /** 三数自身勾稽：入库 + 退回 == 本册行数 */
    private boolean countsBalanced;

    /** 点行数：进门数出来的物理行数（与 totalCount 同一份） */
    private int physicalCount;

    private int bookStatus;
    private String statusText;
    private String note;

    /** 全部行逐行互印；consistent=false 的一行不藏 */
    private List<RowCheck> rowChecks = new ArrayList<RowCheck>();

    /** 版面名称/路段代号报文与主档有出入、以主档为准的行 */
    private List<RowCheck> mismatchRows = new ArrayList<RowCheck>();

    /** 重投第二回时：不入账，回执去首收册看 */
    private boolean resubmit;
    private String firstBookNo;
    private String firstChannel;

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

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getChannelName() {
        return channelName;
    }

    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }

    public String getRowLimitRaw() {
        return rowLimitRaw;
    }

    public void setRowLimitRaw(String rowLimitRaw) {
        this.rowLimitRaw = rowLimitRaw;
    }

    public int getRowLimit() {
        return rowLimit;
    }

    public void setRowLimit(int rowLimit) {
        this.rowLimit = rowLimit;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public int getStoredCount() {
        return storedCount;
    }

    public void setStoredCount(int storedCount) {
        this.storedCount = storedCount;
    }

    public int getBackCount() {
        return backCount;
    }

    public void setBackCount(int backCount) {
        this.backCount = backCount;
    }

    public boolean isCountsBalanced() {
        return countsBalanced;
    }

    public void setCountsBalanced(boolean countsBalanced) {
        this.countsBalanced = countsBalanced;
    }

    public int getPhysicalCount() {
        return physicalCount;
    }

    public void setPhysicalCount(int physicalCount) {
        this.physicalCount = physicalCount;
    }

    public int getBookStatus() {
        return bookStatus;
    }

    public void setBookStatus(int bookStatus) {
        this.bookStatus = bookStatus;
    }

    public String getStatusText() {
        return statusText;
    }

    public void setStatusText(String statusText) {
        this.statusText = statusText;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public List<RowCheck> getRowChecks() {
        return rowChecks;
    }

    public void setRowChecks(List<RowCheck> rowChecks) {
        this.rowChecks = rowChecks;
    }

    public List<RowCheck> getMismatchRows() {
        return mismatchRows;
    }

    public void setMismatchRows(List<RowCheck> mismatchRows) {
        this.mismatchRows = mismatchRows;
    }

    public boolean isResubmit() {
        return resubmit;
    }

    public void setResubmit(boolean resubmit) {
        this.resubmit = resubmit;
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
}
