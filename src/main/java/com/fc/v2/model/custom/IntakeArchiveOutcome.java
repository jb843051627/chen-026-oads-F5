package com.fc.v2.model.custom;

import java.util.ArrayList;
import java.util.List;

/**
 * 老化规矩归档结果：结论长期停在待核、连着满两个年度的行转入历史卷宗层。
 *
 * <p>只对历史册（book_set=HISTORY）生效；现行册一条不碰。存量此前入账的行照常可查、一条不减，
 * 归档只改列示层（archived=1，现行名单不再列出），不删行。
 *
 * @author fuce
 * @date 2026-10-05
 */
public class IntakeArchiveOutcome {

    /** 一条转卷的行 */
    public static class ArchivedRow {
        private String bookNo;
        private int rowNo;
        private String spotCode;
        private String pendingSince;

        public ArchivedRow() {
        }

        public ArchivedRow(String bookNo, int rowNo, String spotCode, String pendingSince) {
            this.bookNo = bookNo;
            this.rowNo = rowNo;
            this.spotCode = spotCode;
            this.pendingSince = pendingSince;
        }

        public String getBookNo() {
            return bookNo;
        }

        public void setBookNo(String bookNo) {
            this.bookNo = bookNo;
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

        public String getPendingSince() {
            return pendingSince;
        }

        public void setPendingSince(String pendingSince) {
            this.pendingSince = pendingSince;
        }
    }

    private String asOf;
    private List<ArchivedRow> archivedRows = new ArrayList<ArchivedRow>();
    /** 满了年限但因在现行册而不归档的行（规矩走不到新册面前，逐行交代） */
    private List<ArchivedRow> skippedCurrent = new ArrayList<ArchivedRow>();
    private String note;

    public String getAsOf() {
        return asOf;
    }

    public void setAsOf(String asOf) {
        this.asOf = asOf;
    }

    public List<ArchivedRow> getArchivedRows() {
        return archivedRows;
    }

    public void setArchivedRows(List<ArchivedRow> archivedRows) {
        this.archivedRows = archivedRows;
    }

    public List<ArchivedRow> getSkippedCurrent() {
        return skippedCurrent;
    }

    public void setSkippedCurrent(List<ArchivedRow> skippedCurrent) {
        this.skippedCurrent = skippedCurrent;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
