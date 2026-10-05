package com.fc.v2.service.intake;

import com.fc.v2.model.custom.IntakeArchiveOutcome;
import com.fc.v2.model.custom.IntakeBookForm;
import com.fc.v2.model.custom.IntakeReconcileOutcome;
import com.fc.v2.model.custom.IntakeReceipt;

import java.util.Date;

/**
 * 牌匾更换名册·区局收讫环节唯一裁决处。
 *
 * <p>四步固定连着走完（点行数→验数据→定去向→出回执），一册一次交完整，中间没有人工兜底；
 * 原先五检查点那一套一律不动，本接口是另起的"收讫"名目，不碰旧服务本体。
 *
 * <p>销账（册账计数）与流水（append-only）两处口径一致、同事务同一次计算；
 * 账实差以流水为准，对不上的那一笔由收讫环节自己认（READJUST 纠销账）。
 *
 * @author fuce
 * @date 2026-10-05
 */
public interface ITOadsIntakeService {

    /**
     * 整册收讫：四步连着做完，返回回执（三个数回库逐行重数，页面那份只是转述）。
     * 同一批次码经区级报送与市级复核各递一回时，第二回不入账、不并账期，另登一笔并回执指向首收册。
     */
    IntakeReceipt receive(IntakeBookForm form, String operator);

    /** 回执回查：三个数回库（册账/行账/流水）逐行重数，并逐行互印点名。 */
    IntakeReceipt receipt(String bookNo);

    /**
     * 缺照补挂：下一轮核看把现场照片附件编号补上，补挂前这一行照样在册里占着位。
     * 补上后对这一行重走第2、3步：仍不过的按规矩退回/待核，过的转通过；重裁结果再出一次回执口径。
     */
    IntakeReceipt attachPhoto(String bookNo, int rowNo, String photoNo, String operator);

    /**
     * 销账与流水对账：两处对不上以流水重数为准纠销账，差异逐行点名，纠账那笔落 READJUST 归收讫环节认。
     */
    IntakeReconcileOutcome reconcile(String bookNo, String operator);

    /**
     * 区局把一册由现行层划入历史层。只改册层标记，行一条不删、计数不变；
     * 划入历史层后，册上长期待核行才可能被两年规矩归档。
     */
    void markHistory(String bookNo, String operator);

    /**
     * 老化规矩：历史册上结论连着停在待核满两个年度的行，转入历史卷宗层（现行名单不再列出）。
     * 现行册走不到它面前（逐行登记跳过）；存量一条不减，照常可查。asOf 为判定当日。
     */
    IntakeArchiveOutcome archive(Date asOf, String operator);
}
