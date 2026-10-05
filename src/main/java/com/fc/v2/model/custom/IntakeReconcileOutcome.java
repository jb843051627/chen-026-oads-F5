package com.fc.v2.model.custom;

import java.util.ArrayList;
import java.util.List;

/**
 * 销账（册账计数）与流水（append-only 裁决流水）对账结果。
 *
 * <p>口径：账实差以流水为准。流水按行逐笔重数与销账对不上时，以流水重数纠销账，
 * 并落一条 READJUST 流水——这一笔归收讫环节自己认，不摊给报送单位，也不留"基本相符"。
 *
 * @author fuce
 * @date 2026-10-05
 */
public class IntakeReconcileOutcome {

    /** 一处对不上的账：销账原值 vs 流水重数值 */
    public static class Diff {
        private String kind;
        private int ledgerValue;
        private int journalValue;
        private String note;

        public Diff() {
        }

        public Diff(String kind, int ledgerValue, int journalValue, String note) {
            this.kind = kind;
            this.ledgerValue = ledgerValue;
            this.journalValue = journalValue;
            this.note = note;
        }

        public String getKind() {
            return kind;
        }

        public void setKind(String kind) {
            this.kind = kind;
        }

        public int getLedgerValue() {
            return ledgerValue;
        }

        public void setLedgerValue(int ledgerValue) {
            this.ledgerValue = ledgerValue;
        }

        public int getJournalValue() {
            return journalValue;
        }

        public void setJournalValue(int journalValue) {
            this.journalValue = journalValue;
        }

        public String getNote() {
            return note;
        }

        public void setNote(String note) {
            this.note = note;
        }
    }

    private String bookNo;
    /** 销账三个数 */
    private int ledgerTotal;
    private int ledgerIn;
    private int ledgerBack;
    /** 流水重数三个数（准） */
    private int journalTotal;
    private int journalIn;
    private int journalBack;
    /** 行账当前结论与流水最近一次 ROUTED 快照对不上的行（逐行点名） */
    private List<IntakeReceipt.RowCheck> rowDrifts = new ArrayList<IntakeReceipt.RowCheck>();
    private List<Diff> diffs = new ArrayList<Diff>();
    private boolean adjusted;
    private String note;

    public String getBookNo() {
        return bookNo;
    }

    public void setBookNo(String bookNo) {
        this.bookNo = bookNo;
    }

    public int getLedgerTotal() {
        return ledgerTotal;
    }

    public void setLedgerTotal(int ledgerTotal) {
        this.ledgerTotal = ledgerTotal;
    }

    public int getLedgerIn() {
        return ledgerIn;
    }

    public void setLedgerIn(int ledgerIn) {
        this.ledgerIn = ledgerIn;
    }

    public int getLedgerBack() {
        return ledgerBack;
    }

    public void setLedgerBack(int ledgerBack) {
        this.ledgerBack = ledgerBack;
    }

    public int getJournalTotal() {
        return journalTotal;
    }

    public void setJournalTotal(int journalTotal) {
        this.journalTotal = journalTotal;
    }

    public int getJournalIn() {
        return journalIn;
    }

    public void setJournalIn(int journalIn) {
        this.journalIn = journalIn;
    }

    public int getJournalBack() {
        return journalBack;
    }

    public void setJournalBack(int journalBack) {
        this.journalBack = journalBack;
    }

    public List<IntakeReceipt.RowCheck> getRowDrifts() {
        return rowDrifts;
    }

    public void setRowDrifts(List<IntakeReceipt.RowCheck> rowDrifts) {
        this.rowDrifts = rowDrifts;
    }

    public List<Diff> getDiffs() {
        return diffs;
    }

    public void setDiffs(List<Diff> diffs) {
        this.diffs = diffs;
    }

    public boolean isAdjusted() {
        return adjusted;
    }

    public void setAdjusted(boolean adjusted) {
        this.adjusted = adjusted;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
