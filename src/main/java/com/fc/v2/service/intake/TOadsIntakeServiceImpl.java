package com.fc.v2.service.intake;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.common.exception.spot.SpotRuleException;
import com.fc.v2.mapper.auto.TOadsIntakeBookMapper;
import com.fc.v2.mapper.auto.TOadsIntakeJournalMapper;
import com.fc.v2.mapper.auto.TOadsIntakeResubmitMapper;
import com.fc.v2.mapper.auto.TOadsIntakeRowMapper;
import com.fc.v2.mapper.auto.TOadsSpotMapper;
import com.fc.v2.mapper.auto.TOadsVerifyCapMapper;
import com.fc.v2.model.auto.TOadsIntakeBook;
import com.fc.v2.model.auto.TOadsIntakeJournal;
import com.fc.v2.model.auto.TOadsIntakeResubmit;
import com.fc.v2.model.auto.TOadsIntakeRow;
import com.fc.v2.model.auto.TOadsSpot;
import com.fc.v2.model.auto.TOadsVerifyCap;
import com.fc.v2.model.custom.IntakeArchiveOutcome;
import com.fc.v2.model.custom.IntakeBookForm;
import com.fc.v2.model.custom.IntakeReconcileOutcome;
import com.fc.v2.model.custom.IntakeReceipt;
import com.fc.v2.model.custom.IntakeRowForm;
import com.fc.v2.model.custom.SpotForm;
import com.fc.v2.service.ITOadsSpotRegisterService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 牌匾更换名册·区局收讫环节唯一裁决处实现。
 *
 * <p>四步连着做完，前一步的数就地落账，后一步不另找来源：
 * <ol>
 *   <li>点行数：抬头上限只认本册抬头那一格（空/负/没边按 200），进门物理行数逐行落账，
 *       同点位两行数两行、落两条，依据是"册+原报行次（进门物理次序）"；</li>
 *   <li>验数据：两数量同到（只到一项不代填不默认零）、编码只认精确配主档（近似配/位数不够算配不上）、
 *       缺照留空照样占位、结论只写待核/通过/退回；</li>
 *   <li>定去向：五条退回理由按固定序逐行写全，主因取首条；超抬头上限整册退回；
 *       批次码双通道重投第二回不入账，另登一笔指向首收册；</li>
 *   <li>出回执：三数回库逐行重数，入库+退回=本册行数；逐行互印，对不齐逐行点名；
 *       报文名称/路段与主档有出入以主档为准并逐行列出。</li>
 * </ol>
 *
 * <p>销账（册账三数）与流水（append-only）同事务同一次计算；对账时账实差以流水重数为准纠销账，
 * 纠账那笔落 READJUST，归收讫环节自己认。
 *
 * @author fuce
 * @date 2026-10-05
 */
@Service
public class TOadsIntakeServiceImpl implements ITOadsIntakeService {

    private static final int DEL_OK = 0;
    private static final int YES = 1;
    private static final String OWNER = "收讫环节";

    @Resource
    private TOadsIntakeBookMapper bookMapper;
    @Resource
    private TOadsIntakeRowMapper rowMapper;
    @Resource
    private TOadsIntakeJournalMapper journalMapper;
    @Resource
    private TOadsIntakeResubmitMapper resubmitMapper;
    @Resource
    private TOadsVerifyCapMapper capMapper;
    @Resource
    private TOadsSpotMapper spotMapper;
    @Resource
    private ITOadsSpotRegisterService registerService;

    // ================================================================ 整册收讫（四步连着做）

    @Override
    @Transactional(rollbackFor = Exception.class)
    public IntakeReceipt receive(IntakeBookForm form, String operator) {
        if (form == null) {
            throw new SpotRuleException("收讫第1步点行数：未收到册子，无从收讫");
        }
        String bookNo = trim(form.getBookNo());
        String batchNo = trim(form.getBatchNo());
        if (bookNo == null) {
            throw new SpotRuleException("收讫进门：册号缺填，一次报送须是一册、有册号才落得了账");
        }
        if (batchNo == null) {
            throw new SpotRuleException("收讫进门：批次码缺填，双通道重投无从辨认，本册不收");
        }
        String channel = normalizeChannel(form.getChannel());
        String quarter = trim(form.getQuarterCode());
        if (quarter == null) {
            quarter = registerService.currentQuarter();
        }
        List<IntakeRowForm> forms = form.getRows() == null ? new ArrayList<IntakeRowForm>() : form.getRows();

        if (bookMapper.findByBookNo(bookNo) != null) {
            throw new SpotRuleException("收讫进门：册号 " + bookNo + " 已落过账，一册只收一回；整改重报须另编册号");
        }

        // -------- 双通道重投：同批次码第二回（无论同通道还是另一通道）不入账、不并账期，另登一笔 --------
        TOadsIntakeBook first = bookMapper.findReceivedByBatch(batchNo);
        if (first != null) {
            return registerResubmit(first, channel, forms.size(), operator);
        }

        // -------- 第1步：点行数（上限只此抬头一格一条来源） --------
        String limitRaw = form.getRowLimitText() == null ? null : form.getRowLimitText().trim();
        int rowLimit = resolveRowLimit(limitRaw);
        int physical = forms.size();
        if (physical == 0) {
            throw new SpotRuleException("收讫第1步点行数：册 " + bookNo + " 数出 0 行，一册含若干行，零行无从收讫");
        }

        Date now = new Date();
        TOadsIntakeBook book = newBook(bookNo, batchNo, quarter, trim(form.getUnitNo()), channel,
                limitRaw, rowLimit, now, operator);

        boolean overLimit = physical > rowLimit;
        if (overLimit) {
            // 整册超抬头上限：留头留数不留行——行一行不落，整册退回，回执三数照旧勾稽。
            book.setRowCountPhysical(physical);
            book.setInCount(0);
            book.setBackCount(physical);
            book.setStatus(IntakeStep.BOOK_OVER_LIMIT);
            book.setRemark("整册行数 " + physical + " 超出本册抬头写明的上限 " + rowLimit + "，整册退回，行不逐条落账");
            bookMapper.insert(book);
            appendJournal(book.getId(), bookNo, null, null, IntakeStep.STEP_COUNT, IntakeStep.EVT_COUNTED,
                    null, null, "进门数出物理行数 " + physical + " 行（同一处点位写两行也数两行）；抬头那格原文『"
                            + (limitRaw == null ? "" : limitRaw) + "』写定上限 " + rowLimit
                            + "；已超上限，四步在点行数这一步即判整册退回", operator, now);
            appendJournal(book.getId(), bookNo, null, null, IntakeStep.STEP_RECEIPT, IntakeStep.EVT_RECEIPTED,
                    null, IntakeStep.R_BOOK_OVER_LIMIT,
                    "回执三数（回库逐行重数）：本册行数 " + physical + "、入库行数 0、退回行数 " + physical
                            + "；0+" + physical + "=" + physical + " 勾稽；退回理由：整册超抬头上限", operator, now);
            return buildReceipt(book, new ArrayList<TOadsIntakeRow>(),
                    "整册超抬头上限 " + rowLimit + "（数出 " + physical + " 行），整册退回；行不逐条落账，理由一句："
                            + IntakeStep.reasonText(IntakeStep.R_BOOK_OVER_LIMIT));
        }

        // 收讫在册：先落册头，COUNTED 先落账（事后翻得出当初数了多少）。
        book.setRowCountPhysical(physical);
        book.setInCount(0);
        book.setBackCount(0);
        book.setStatus(IntakeStep.BOOK_RECEIVED);
        bookMapper.insert(book);
        appendJournal(book.getId(), bookNo, null, null, IntakeStep.STEP_COUNT, IntakeStep.EVT_COUNTED,
                null, null, "进门数出物理行数 " + physical + " 行（同一处点位写两行也数两行、落两条，按册内物理行次各认各的）；"
                        + "抬头那格原文『" + (limitRaw == null ? "" : limitRaw) + "』写定单册上限 " + rowLimit,
                operator, now);

        // -------- 第2、3步：逐行验数据、定去向（结论只写三个取值之一） --------
        Map<String, Integer> codeSeq = new HashMap<String, Integer>();
        List<TOadsIntakeRow> stored = new ArrayList<TOadsIntakeRow>();
        int storedCount = 0;
        int backCount = 0;
        for (int i = 0; i < forms.size(); i++) {
            IntakeRowForm rf = forms.get(i);
            int rowNo = i + 1;
            TOadsIntakeRow row = newRow(book, rowNo, rf, channel, now, operator, codeSeq);
            Adjudication adj = adjudicate(row, quarter);
            row.setVerdict(adj.verdict);
            row.setBackReasonCode(adj.primaryReason);
            row.setBackReason(adj.backReason);
            row.setPendingNote(adj.pendingNote);
            if (IntakeStep.VERDICT_PENDING.equals(adj.verdict)) {
                row.setPendingSince(now);
            }
            rowMapper.insert(row);
            stored.add(row);

            appendJournal(book.getId(), bookNo, row.getId(), rowNo, IntakeStep.STEP_CHECK, IntakeStep.EVT_CHECKED,
                    null, null, adj.checkDetail, operator, now);
            appendJournal(book.getId(), bookNo, row.getId(), rowNo, IntakeStep.STEP_ROUTE, IntakeStep.EVT_ROUTED,
                    adj.verdict, adj.primaryReason,
                    IntakeStep.VERDICT_BACK.equals(adj.verdict)
                            ? ("判退回，理由（按固定序，命中几条写几句）：" + adj.backReason)
                            : ("判" + IntakeStep.verdictName(adj.verdict)
                                    + (adj.pendingNote == null ? "" : "；" + adj.pendingNote)),
                    operator, now);

            if (IntakeStep.VERDICT_BACK.equals(adj.verdict)) {
                backCount++;
            } else {
                storedCount++;
            }
        }

        // 销账三数与行账、流水出自同一次计算，同事务回写，不留第二份口径。
        book.setInCount(storedCount);
        book.setBackCount(backCount);
        book.setUpdateBy(operator);
        book.setUpdateTime(now);
        bookMapper.updateById(book);

        // -------- 第4步：出回执（三数即本次重数） --------
        appendJournal(book.getId(), bookNo, null, null, IntakeStep.STEP_RECEIPT, IntakeStep.EVT_RECEIPTED,
                null, null, "回执三数（回库逐行重数，页面那份只是转述）：本册行数 " + physical
                        + "、入库行数 " + storedCount + "、退回行数 " + backCount
                        + "；" + storedCount + "+" + backCount + "=" + physical + " 勾稽", operator, now);

        return buildReceipt(book, stored, null);
    }

    // ================================================================ 回执回查（回库逐行重数）

    @Override
    public IntakeReceipt receipt(String bookNo) {
        TOadsIntakeBook book = requireBook(bookNo);
        List<TOadsIntakeRow> rows = rowMapper.listByBook(book.getId());
        String extra = null;
        if (book.getStatus() != null && book.getStatus() == IntakeStep.BOOK_OVER_LIMIT) {
            extra = "整册超抬头上限退回，留头留数不留行（行不逐条落账）";
        }
        return buildReceipt(book, rows, extra);
    }

    // ================================================================ 缺照补挂（下一轮核看）

    @Override
    @Transactional(rollbackFor = Exception.class)
    public IntakeReceipt attachPhoto(String bookNo, int rowNo, String photoNo, String operator) {
        TOadsIntakeBook book = requireBook(bookNo);
        if (book.getStatus() != null && book.getStatus() != IntakeStep.BOOK_RECEIVED) {
            throw new SpotRuleException("缺照补挂：册 " + bookNo + " 是整册退回的册，行未入账，无处补挂；须整改后另编册重报");
        }
        TOadsIntakeRow row = requireRow(book.getId(), rowNo);
        String photo = trim(photoNo);
        if (photo == null) {
            throw new SpotRuleException("缺照补挂：第" + rowNo + "行补挂的照片附件编号空着，补挂须给编号");
        }
        if (row.getPhotoPending() == null || row.getPhotoPending() != YES) {
            throw new SpotRuleException("缺照补挂：第" + rowNo + "行并不缺照占位，不收这一笔补挂（占位只给缺照的行）");
        }
        if (IntakeStep.VERDICT_BACK.equals(row.getVerdict())) {
            throw new SpotRuleException("缺照补挂：第" + rowNo + "行已判退回，不随补挂改判；退回由五条理由管，照片管不着");
        }
        Date now = new Date();
        row.setPhotoNo(photo);
        row.setPhotoPending(0);

        // 补上照片后只对这一行重走第2、3步；别处一行不碰。
        Adjudication adj = adjudicate(row, book.getQuarterCode());
        row.setVerdict(adj.verdict);
        row.setBackReasonCode(adj.primaryReason);
        row.setBackReason(adj.backReason);
        row.setPendingNote(adj.pendingNote);
        row.setUpdateBy(operator);
        row.setUpdateTime(now);
        rowMapper.updateById(row);

        appendJournal(book.getId(), bookNo, row.getId(), rowNo, IntakeStep.STEP_CHECK, IntakeStep.EVT_PHOTO_FILL,
                adj.verdict, null, "下一轮核看补挂现场照片附件编号 " + photo
                        + "；补挂前该行照样在册里占着位，补挂后对这一行重走验数据、定去向", operator, now);
        appendJournal(book.getId(), bookNo, row.getId(), rowNo, IntakeStep.STEP_ROUTE, IntakeStep.EVT_ROUTED,
                adj.verdict, adj.primaryReason,
                IntakeStep.VERDICT_BACK.equals(adj.verdict)
                        ? ("补挂后重判退回，理由：" + adj.backReason)
                        : ("补挂后重判" + IntakeStep.verdictName(adj.verdict)
                                + (adj.pendingNote == null ? "" : "；" + adj.pendingNote)),
                operator, now);

        // 销账随本次重裁同一次重算（三数仍回库逐行数出来）。
        List<TOadsIntakeRow> rows = rowMapper.listByBook(book.getId());
        Counts c = Counts.of(rows);
        book.setInCount(c.stored);
        book.setBackCount(c.back);
        book.setUpdateBy(operator);
        book.setUpdateTime(now);
        bookMapper.updateById(book);
        appendJournal(book.getId(), bookNo, null, null, IntakeStep.STEP_RECEIPT, IntakeStep.EVT_RECEIPTED,
                null, null, "补挂后重出回执口径（回库逐行重数）：本册行数 " + c.total
                        + "、入库行数 " + c.stored + "、退回行数 " + c.back
                        + "；" + c.stored + "+" + c.back + "=" + c.total + " 勾稽", operator, now);
        return buildReceipt(book, rows, null);
    }

    // ================================================================ 销账／流水对账（账实差以流水为准）

    @Override
    @Transactional(rollbackFor = Exception.class)
    public IntakeReconcileOutcome reconcile(String bookNo, String operator) {
        TOadsIntakeBook book = requireBook(bookNo);
        IntakeReconcileOutcome o = new IntakeReconcileOutcome();
        o.setBookNo(bookNo);
        o.setLedgerTotal(nz(book.getRowCountPhysical()));
        o.setLedgerIn(nz(book.getInCount()));
        o.setLedgerBack(nz(book.getBackCount()));

        List<TOadsIntakeRow> rows = rowMapper.listByBook(book.getId());
        // 流水为准：每一行取最近一次 ROUTED 的裁决快照，行账现结论与它对不上的，逐行点名并按流水纠回。
        Map<Long, TOadsIntakeJournal> lastRouted = new HashMap<Long, TOadsIntakeJournal>();
        int journalEvents = 0;
        for (TOadsIntakeJournal j : journalMapper.listByBook(book.getId())) {
            if (IntakeStep.EVT_ROUTED.equals(j.getEvent()) && j.getRowId() != null) {
                TOadsIntakeJournal prev = lastRouted.get(j.getRowId());
                if (prev == null || j.getId() != null && j.getId() > prev.getId()) {
                    lastRouted.put(j.getRowId(), j);
                }
                journalEvents++;
            }
        }

        Date now = new Date();
        boolean rowChanged = false;
        for (TOadsIntakeRow r : rows) {
            TOadsIntakeJournal j = lastRouted.get(r.getId());
            if (j != null && j.getVerdict() != null && !j.getVerdict().equals(r.getVerdict())) {
                IntakeReceipt.RowCheck rc = new IntakeReceipt.RowCheck();
                rc.setRowId(r.getId());
                rc.setRowNo(nz(r.getRowNo()));
                rc.setSpotCode(r.getSpotCode());
                rc.setVerdict(r.getVerdict());
                rc.setVerdictName(IntakeStep.verdictName(r.getVerdict()));
                rc.setConsistent(false);
                rc.setMismatchNote("行账现结论为「" + IntakeStep.verdictName(r.getVerdict())
                        + "」，流水最近一次定去向是「" + IntakeStep.verdictName(j.getVerdict())
                        + "」；账实差以流水为准，本行按流水纠回");
                o.getRowDrifts().add(rc);
                r.setVerdict(j.getVerdict());
                r.setBackReasonCode(j.getReasonCode());
                r.setBackReason(j.getReasonCode() == null ? null : IntakeStep.reasonText(j.getReasonCode()));
                r.setUpdateBy(OWNER);
                r.setUpdateTime(now);
                rowMapper.updateById(r);
                rowChanged = true;
            }
        }

        Counts truth;
        if (book.getStatus() != null && book.getStatus() == IntakeStep.BOOK_OVER_LIMIT) {
            // 整册退回册行不落账：流水的准数就是册头上那两个（RECEIPTED 已记）。
            truth = new Counts(nz(book.getRowCountPhysical()), 0, nz(book.getRowCountPhysical()));
        } else {
            // 若某行在流水里连一笔 ROUTED 都没有（异常），不以行账补——逐行点名后按行账在册数，流水缺笔登记。
            truth = Counts.of(rows);
        }

        o.setJournalTotal(truth.total);
        o.setJournalIn(truth.stored);
        o.setJournalBack(truth.back);

        int storedIn = nz(book.getInCount());
        int storedBack = nz(book.getBackCount());
        int storedTotal = nz(book.getRowCountPhysical());
        if (storedIn != truth.stored) {
            o.getDiffs().add(new IntakeReconcileOutcome.Diff("入库行数", storedIn, truth.stored,
                    "销账记 " + storedIn + "，流水重数 " + truth.stored + "，以流水为准纠销账"));
        }
        if (storedBack != truth.back) {
            o.getDiffs().add(new IntakeReconcileOutcome.Diff("退回行数", storedBack, truth.back,
                    "销账记 " + storedBack + "，流水重数 " + truth.back + "，以流水为准纠销账"));
        }
        if (storedTotal != truth.total) {
            o.getDiffs().add(new IntakeReconcileOutcome.Diff("本册行数", storedTotal, truth.total,
                    "销账记 " + storedTotal + "，流水重数 " + truth.total + "，以流水为准纠销账"));
        }

        boolean adjusted = !o.getDiffs().isEmpty() || rowChanged;
        o.setAdjusted(adjusted);
        if (adjusted) {
            // 两处对不上以哪边为准——定死：以流水为准；取舍由收讫环节本环节定、这笔也归本环节认。
            book.setInCount(truth.stored);
            book.setBackCount(truth.back);
            book.setRowCountPhysical(truth.total);
            book.setUpdateBy(OWNER);
            book.setUpdateTime(now);
            bookMapper.updateById(book);
            StringBuilder detail = new StringBuilder("销账与流水对不上，按规矩以流水为准纠销账：");
            if (storedTotal != truth.total) {
                detail.append("本册行数 ").append(storedTotal).append("→").append(truth.total).append("；");
            }
            if (storedIn != truth.stored) {
                detail.append("入库行数 ").append(storedIn).append("→").append(truth.stored).append("；");
            }
            if (storedBack != truth.back) {
                detail.append("退回行数 ").append(storedBack).append("→").append(truth.back).append("；");
            }
            if (rowChanged) {
                detail.append("另有 ").append(o.getRowDrifts().size()).append(" 行结论按流水纠回；");
            }
            detail.append("这一笔归收讫环节自己认，不摊报送单位。");
            appendJournal(book.getId(), bookNo, null, null, IntakeStep.STEP_RECEIPT, IntakeStep.EVT_READJUST,
                    null, null, detail.toString(),
                    operator == null || operator.trim().isEmpty() ? OWNER : operator, now);
            o.setNote("已按流水纠销账并落 READJUST 一笔，归收讫环节认；" + detail);
        } else {
            o.setNote("销账三数与流水逐行重数一致（" + truth.stored + "+" + truth.back + "=" + truth.total
                    + "），共 " + rows.size() + " 行、" + journalEvents + " 笔定去向流水，无须纠账");
        }
        return o;
    }

    // ================================================================ 历史层与两年归档

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markHistory(String bookNo, String operator) {
        TOadsIntakeBook book = requireBook(bookNo);
        if (IntakeStep.SET_HISTORY.equals(book.getBookSet())) {
            throw new SpotRuleException("册 " + bookNo + " 已在历史层，不重复划");
        }
        Date now = new Date();
        book.setBookSet(IntakeStep.SET_HISTORY);
        book.setUpdateBy(operator);
        book.setUpdateTime(now);
        bookMapper.updateById(book);
        appendJournal(book.getId(), bookNo, null, null, IntakeStep.STEP_RECEIPT, IntakeStep.EVT_MARK_HISTORY,
                null, null, "区局将本册由现行层划入历史层；行一条不删、三数不变，此后册上长期待核行才谈得上两年归档",
                operator, now);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public IntakeArchiveOutcome archive(Date asOf, String operator) {
        Date at = asOf == null ? new Date() : asOf;
        Calendar cutoffCal = Calendar.getInstance();
        cutoffCal.setTime(at);
        cutoffCal.add(Calendar.YEAR, -2);
        Date cutoff = cutoffCal.getTime();

        IntakeArchiveOutcome o = new IntakeArchiveOutcome();
        o.setAsOf(new SimpleDateFormat("yyyy-MM-dd").format(at));

        List<TOadsIntakeBook> books = bookMapper.selectList(new QueryWrapper<TOadsIntakeBook>()
                .eq("del_flag", DEL_OK).eq("status", IntakeStep.BOOK_RECEIVED));
        int archived = 0;
        for (TOadsIntakeBook book : books) {
            boolean history = IntakeStep.SET_HISTORY.equals(book.getBookSet());
            List<TOadsIntakeRow> rows = rowMapper.listByBook(book.getId());
            int bookArchived = 0;
            for (TOadsIntakeRow r : rows) {
                if (!IntakeStep.VERDICT_PENDING.equals(r.getVerdict())) {
                    continue;
                }
                if (r.getArchived() != null && r.getArchived() == YES) {
                    continue;
                }
                if (r.getPendingSince() == null || r.getPendingSince().after(cutoff)) {
                    continue;
                }
                String since = new SimpleDateFormat("yyyy-MM-dd").format(r.getPendingSince());
                if (!history) {
                    // 新册走不到两年规矩面前：在现行册上，年限够了也不动，只逐行登记跳过。
                    o.getSkippedCurrent().add(new IntakeArchiveOutcome.ArchivedRow(
                            book.getBookNo(), nz(r.getRowNo()), r.getSpotCode(), since));
                    continue;
                }
                r.setArchived(YES);
                r.setArchivedAt(at);
                r.setUpdateBy(operator);
                r.setUpdateTime(at);
                rowMapper.updateById(r);
                appendJournal(book.getId(), book.getBookNo(), r.getId(), nz(r.getRowNo()),
                        IntakeStep.STEP_RECEIPT, IntakeStep.EVT_ARCHIVED, IntakeStep.VERDICT_PENDING, null,
                        "结论自 " + since + " 起连续停在待核，至 " + o.getAsOf() + " 已满两个年度；"
                                + "转入历史卷宗层，现行名单不再列出，行不删、照常可查", operator, at);
                o.getArchivedRows().add(new IntakeArchiveOutcome.ArchivedRow(
                        book.getBookNo(), nz(r.getRowNo()), r.getSpotCode(), since));
                archived++;
                bookArchived++;
            }
            if (bookArchived > 0) {
                book.setArchivedCount(nz(book.getArchivedCount()) + bookArchived);
                book.setUpdateBy(operator);
                book.setUpdateTime(at);
                bookMapper.updateById(book);
            }
        }
        o.setNote("截至 " + o.getAsOf() + "，历史册上连续待核满两个年度转卷 " + archived
                + " 行（只改列示层，存量一条不减）；现行册跳过 " + o.getSkippedCurrent().size()
                + " 行（两年规矩只对历史册生效，新册走不到它面前）");
        return o;
    }

    // ================================================================ 内部：第2/3步裁决（唯一一份）

    /**
     * 对一行连着走验数据、定去向，结论只给 PENDING/PASS/BACK 三个取值。
     * 退回理由按 {@link IntakeStep#REASON_ORDER} 固定序逐条判、全写上，主因取首条。
     */
    private Adjudication adjudicate(TOadsIntakeRow row, String quarter) {
        Adjudication adj = new Adjudication();
        List<String> checkNotes = new ArrayList<String>();
        List<String> pendingNotes = new ArrayList<String>();
        Map<String, String> reasonTexts = new LinkedHashMap<String, String>();

        String code = trim(row.getSpotCode());
        Integer qty = parseIntOrNull(row.getQtyRaw());
        Integer chk = parseIntOrNull(row.getChkRaw());
        row.setQty(qty);
        row.setChkNum(chk);

        // ---- 第2步：两个数量须同时到达；只到一项不代填、不默认零 ----
        if (qty == null) {
            checkNotes.add("应更换处数这一项没到（原文『" + empty(row.getQtyRaw()) + "』），不由系统代填、不默认成零");
            pendingNotes.add("应更换处数缺项，待下一轮核看补齐");
        }
        if (chk == null) {
            checkNotes.add("核验合格处数这一项没到（原文『" + empty(row.getChkRaw()) + "』），不由系统代填、不默认成零");
            pendingNotes.add("核验合格处数缺项，待下一轮核看补齐");
        }
        if (qty != null && chk != null && chk > qty) {
            // 五条退回理由里没有这一条，不能自创退回；勾稽不上挂待核，下一轮核看。
            checkNotes.add("核验合格处数(" + chk + ")高于应更换处数(" + qty + ")，两项勾不上");
            pendingNotes.add("核验合格处数高于应更换处数，待下一轮核看");
        }

        // ---- 第2步：点位身份只认编码精确配主档；近似配、位数不够都算配不上 ----
        boolean codeMissing = code == null;
        boolean codeTooLong = code != null && code.length() > IntakeStep.CODE_MAX_LEN;
        if (!codeMissing && !codeTooLong) {
            if (code.length() < IntakeStep.CODE_MIN_LEN) {
                checkNotes.add("点位编码 " + code + " 位数不够（规范 " + IntakeStep.CODE_MIN_LEN + ".."
                        + IntakeStep.CODE_MAX_LEN + " 位），按配不上处理，不按版面名称近似去配");
                pendingNotes.add("点位编码位数不够、配不上主档");
            } else {
                TOadsSpot master = matchMaster(code, quarter);
                if (master == null) {
                    checkNotes.add("点位编码 " + code + " 在点位主档中配不上（只认全等，版面名称近似配不作数）");
                    pendingNotes.add("点位编码配不上主档，待下一轮核看");
                } else {
                    row.setMatchedSpotId(master.getId());
                    // 版面名称、路段代号以主档带出为准；报文那串只用来比对，有出入逐行列出。
                    String masterBoard = master.getContent();
                    String masterRoad = master.getAreaNo();
                    row.setMasterBoardName(masterBoard);
                    row.setMasterRoadCode(masterRoad);
                    String reportBoard = trim(row.getBoardName());
                    String reportRoad = trim(row.getRoadCode());
                    if (reportBoard != null && masterBoard != null && !reportBoard.equals(masterBoard.trim())) {
                        row.setNameMismatch(YES);
                        checkNotes.add("版面名称报文『" + reportBoard + "』与主档『" + masterBoard.trim()
                                + "』有出入，以主档为准");
                    }
                    if (reportRoad != null && masterRoad != null && !reportRoad.equals(masterRoad.trim())) {
                        row.setRoadMismatch(YES);
                        checkNotes.add("路段代号报文『" + reportRoad + "』与主档『" + masterRoad.trim()
                                + "』有出入，以主档为准");
                    }
                }
            }
        }

        // ---- 第2步：缺照占位（行照样在册里占位，待下一轮核看补挂） ----
        if (row.getPhotoPending() != null && row.getPhotoPending() == YES) {
            checkNotes.add("现场照片附件编号缺填，行照样占位，待下一轮核看补挂");
            pendingNotes.add("缺现场照片，待补挂");
        }

        // ---- 第3步：五条退回理由固定序（当地辖段取主档路段，配不上时取报文路段） ----
        if (qty != null) {
            String areaKey = row.getMasterRoadCode() != null ? row.getMasterRoadCode().trim() : trim(row.getRoadCode());
            if (areaKey != null) {
                TOadsVerifyCap cap = capMapper.findActiveByArea(areaKey);
                if (cap != null && cap.getCapQty() != null && qty > cap.getCapQty()) {
                    reasonTexts.put(IntakeStep.R_QTY_OVER_CAP,
                            IntakeStep.reasonText(IntakeStep.R_QTY_OVER_CAP) + "（当地 " + areaKey + " 上限 "
                                    + cap.getCapQty() + "，本行报 " + qty + "）");
                }
            }
            if (qty == 0) {
                reasonTexts.put(IntakeStep.R_QTY_ZERO, IntakeStep.reasonText(IntakeStep.R_QTY_ZERO));
            }
            if (qty < 0) {
                reasonTexts.put(IntakeStep.R_QTY_NEGATIVE,
                        IntakeStep.reasonText(IntakeStep.R_QTY_NEGATIVE) + "（本行报 " + qty + "）");
            }
        }
        if (codeMissing) {
            reasonTexts.put(IntakeStep.R_CODE_MISSING, IntakeStep.reasonText(IntakeStep.R_CODE_MISSING));
        }
        if (codeTooLong) {
            reasonTexts.put(IntakeStep.R_CODE_TOO_LONG,
                    IntakeStep.reasonText(IntakeStep.R_CODE_TOO_LONG) + "（" + code.length() + " 位，规范上限 "
                            + IntakeStep.CODE_MAX_LEN + " 位）");
        }

        // ---- 结论：命中任一退回理由即退回（主因=固定序首条）；否则有待核挂待核；全清才通过 ----
        if (!reasonTexts.isEmpty()) {
            adj.verdict = IntakeStep.VERDICT_BACK;
            StringBuilder all = new StringBuilder();
            String primary = null;
            for (String code0 : IntakeStep.REASON_ORDER) {
                String t = reasonTexts.get(code0);
                if (t != null) {
                    if (primary == null) {
                        primary = code0;
                    }
                    if (all.length() > 0) {
                        all.append("；");
                    }
                    all.append(t);
                }
            }
            adj.primaryReason = primary;
            adj.backReason = all.toString();
        } else if (!pendingNotes.isEmpty()) {
            adj.verdict = IntakeStep.VERDICT_PENDING;
            adj.pendingNote = join(pendingNotes, "；");
        } else {
            adj.verdict = IntakeStep.VERDICT_PASS;
        }
        adj.checkDetail = checkNotes.isEmpty()
                ? "数量两项同到；编码 " + (code == null ? "（空）" : code) + " 精确配上主档"
                        + (row.getPhotoPending() != null && row.getPhotoPending() == YES ? "；照片缺" : "；照片已随册到")
                : join(checkNotes, "；");
        return adj;
    }

    /** 编码只认全等：配到多条且当季仍指不定唯一的，按配不上（身份不能靠猜）。 */
    private TOadsSpot matchMaster(String code, String quarter) {
        List<TOadsSpot> hits = new ArrayList<TOadsSpot>();
        for (TOadsSpot s : spotMapper.selectList(new QueryWrapper<TOadsSpot>()
                .eq("spot_no", code).eq("del_flag", DEL_OK))) {
            hits.add(s);
        }
        if (hits.isEmpty()) {
            return null;
        }
        if (hits.size() == 1) {
            return hits.get(0);
        }
        List<TOadsSpot> inQuarter = new ArrayList<TOadsSpot>();
        for (TOadsSpot s : hits) {
            if (quarter != null && quarter.equals(s.getQuarterCode() == null ? null : s.getQuarterCode().trim())) {
                inQuarter.add(s);
            }
        }
        return inQuarter.size() == 1 ? inQuarter.get(0) : null;
    }

    // ================================================================ 内部：回执重数与互印

    /**
     * 回执三个数一律在这里回库逐行重数：不信册头存的列。重数与册头有出入，逐行点名，不说基本相符。
     */
    private IntakeReceipt buildReceipt(TOadsIntakeBook book, List<TOadsIntakeRow> rows, String extraNote) {
        IntakeReceipt r = new IntakeReceipt();
        r.setBookId(book.getId());
        r.setBookNo(book.getBookNo());
        r.setBatchNo(book.getBatchNo());
        r.setQuarterCode(book.getQuarterCode());
        r.setChannel(book.getChannel());
        r.setChannelName(IntakeStep.channelName(book.getChannel()));
        r.setRowLimitRaw(book.getRowLimitRaw());
        r.setRowLimit(nz(book.getRowLimit()));
        r.setBookStatus(nz(book.getStatus()));
        r.setStatusText(book.getStatus() != null && book.getStatus() == IntakeStep.BOOK_OVER_LIMIT
                ? "整册超限退回" : "已收讫");

        Counts c;
        boolean overLimit = book.getStatus() != null && book.getStatus() == IntakeStep.BOOK_OVER_LIMIT;
        if (overLimit) {
            // 整册退回不留行：三数本册头回库取（行不逐条落账，这是点行数那一步定死的）。
            int physical = nz(book.getRowCountPhysical());
            c = new Counts(physical, 0, physical);
        } else {
            c = Counts.of(rows);
        }
        r.setPhysicalCount(c.total);
        r.setTotalCount(c.total);
        r.setStoredCount(c.stored);
        r.setBackCount(c.back);
        r.setCountsBalanced(c.stored + c.back == c.total);

        // 任选一行复核：这一行的结论要与三个数互相印证。
        // 逐行点：退回行必须落在退回摞且有退回理由；入库摞（通过/待核）不许背着退回理由；第四种结论直接点名。
        int rowBackTally = 0;
        for (TOadsIntakeRow row : rows) {
            IntakeReceipt.RowCheck rc = toRowCheck(row);
            rc.setConsistent(true);
            boolean back = IntakeStep.VERDICT_BACK.equals(row.getVerdict());
            boolean stored = IntakeStep.VERDICT_PASS.equals(row.getVerdict())
                    || IntakeStep.VERDICT_PENDING.equals(row.getVerdict());
            if (back) {
                rowBackTally++;
                if (row.getBackReasonCode() == null || row.getBackReason() == null
                        || row.getBackReason().trim().isEmpty()) {
                    rc.setConsistent(false);
                    rc.setMismatchNote("第" + row.getRowNo() + "行判了退回，却查不到退回理由，结论与三数对不上");
                }
            } else if (stored) {
                if (IntakeStep.VERDICT_PENDING.equals(row.getVerdict())
                        && (row.getPendingNote() == null || row.getPendingNote().trim().isEmpty())) {
                    rc.setConsistent(false);
                    rc.setMismatchNote("第" + row.getRowNo() + "行判了待核，却没写下待核交代，对不上");
                }
                if (row.getBackReasonCode() != null) {
                    rc.setConsistent(false);
                    rc.setMismatchNote("第" + row.getRowNo() + "行落在入库摞（"
                            + IntakeStep.verdictName(row.getVerdict()) + "），却背着退回理由，对不上");
                }
            } else {
                rc.setConsistent(false);
                rc.setMismatchNote("第" + row.getRowNo() + "行结论『" + row.getVerdict()
                        + "』不是待核/通过/退回三者之一，不予承认的第四种写法，一行点名");
            }
            r.getRowChecks().add(rc);
            if (rc.isNameMismatch() || rc.isRoadMismatch()) {
                r.getMismatchRows().add(rc);
            }
        }
        // 逐行点出来的退回摞合计，须与回执退回数一口径；对不上就是对不上，不说基本相符。
        if (!overLimit && rowBackTally != c.back) {
            for (IntakeReceipt.RowCheck rc : r.getRowChecks()) {
                rc.setConsistent(false);
                rc.setMismatchNote("逐行点出退回 " + rowBackTally + " 行，回执退回数却是 " + c.back
                        + "，结论与三个数对不上");
            }
        }

        // 页面那份只是转述：转述的册头列与本次重数若不一致，也点名，不替谁圆场。
        List<String> drifts = new ArrayList<String>();
        if (!overLimit && nz(book.getInCount()) != c.stored) {
            drifts.add("册头转述入库行数 " + nz(book.getInCount()) + "，回库重数 " + c.stored);
        }
        if (!overLimit && nz(book.getBackCount()) != c.back) {
            drifts.add("册头转述退回行数 " + nz(book.getBackCount()) + "，回库重数 " + c.back);
        }
        if (nz(book.getRowCountPhysical()) != c.total) {
            drifts.add("册头转述本册行数 " + nz(book.getRowCountPhysical()) + "，回库重数 " + c.total);
        }
        StringBuilder note = new StringBuilder();
        if (extraNote != null) {
            note.append(extraNote).append("；");
        }
        note.append("三数回库逐行重数：本册行数 ").append(c.total).append("、入库行数 ").append(c.stored)
                .append("、退回行数 ").append(c.back).append("，").append(c.stored).append("+").append(c.back)
                .append(r.isCountsBalanced() ? "=" : "≠").append(c.total)
                .append(r.isCountsBalanced() ? " 勾稽" : " 对不齐");
        if (!r.getMismatchRows().isEmpty()) {
            note.append("；版面名称/路段代号以主档为准的行 ").append(r.getMismatchRows().size()).append(" 行已逐行列出");
        }
        if (!drifts.isEmpty()) {
            note.append("；").append(join(drifts, "；")).append("，以回库重数为准（可走对账纠销账）");
        }
        r.setNote(note.toString());
        return r;
    }

    private IntakeReceipt.RowCheck toRowCheck(TOadsIntakeRow row) {
        IntakeReceipt.RowCheck rc = new IntakeReceipt.RowCheck();
        rc.setRowId(row.getId());
        rc.setRowNo(nz(row.getRowNo()));
        rc.setSpotCode(row.getSpotCode());
        rc.setVerdict(row.getVerdict());
        rc.setVerdictName(IntakeStep.verdictName(row.getVerdict()));
        rc.setBackReason(row.getBackReason());
        rc.setPendingNote(row.getPendingNote());
        rc.setNameMismatch(row.getNameMismatch() != null && row.getNameMismatch() == YES);
        rc.setRoadMismatch(row.getRoadMismatch() != null && row.getRoadMismatch() == YES);
        rc.setMasterBoardName(row.getMasterBoardName());
        rc.setMasterRoadCode(row.getMasterRoadCode());
        rc.setArchived(row.getArchived() != null && row.getArchived() == YES);
        return rc;
    }

    // ================================================================ 内部：双通道重投另登

    private IntakeReceipt registerResubmit(TOadsIntakeBook first, String secondChannel, int secondRows,
                                           String operator) {
        Date now = new Date();
        boolean otherChannel = !first.getChannel().equals(secondChannel);
        TOadsIntakeResubmit reg = new TOadsIntakeResubmit();
        reg.setBatchNo(first.getBatchNo());
        reg.setFirstBookId(first.getId());
        reg.setFirstBookNo(first.getBookNo());
        reg.setFirstChannel(first.getChannel());
        reg.setSecondChannel(secondChannel);
        reg.setSecondRowCount(secondRows);
        reg.setNote(otherChannel
                ? "同一批次码经区级报送与市级复核两条通道各递一回：首收走「"
                        + IntakeStep.channelName(first.getChannel()) + "」已入册 " + first.getBookNo()
                        + "；第二回走「" + IntakeStep.channelName(secondChannel) + "」不再入账、不并账期，回执以首收册为准"
                : "同一批次码同通道第二回递来：首收册 " + first.getBookNo()
                        + " 已在账，第二回不重复入账、不并账期");
        reg.setDelFlag(DEL_OK);
        reg.setCreateBy(operator);
        reg.setCreateTime(now);
        resubmitMapper.insert(reg);
        // 重投这笔落在首收册的流水里另登（流水只追加），两本账以后都翻得到第二回的去向。
        appendJournal(first.getId(), first.getBookNo(), null, null, IntakeStep.STEP_ROUTE,
                IntakeStep.EVT_RESUBMIT, null, null,
                (otherChannel ? "市级复核/区级报送" : "同通道") + "第二回递到同一批次码（自报 " + secondRows
                        + " 行）：不入账、不并账期，另登 t_oads_intake_resubmit，回执去翻首收册 "
                        + first.getBookNo(), operator, now);

        IntakeReceipt r = new IntakeReceipt();
        r.setBookId(first.getId());
        r.setBookNo(first.getBookNo());
        r.setBatchNo(first.getBatchNo());
        r.setQuarterCode(first.getQuarterCode());
        r.setChannel(secondChannel);
        r.setChannelName(IntakeStep.channelName(secondChannel));
        r.setRowLimitRaw(first.getRowLimitRaw());
        r.setRowLimit(nz(first.getRowLimit()));
        r.setBookStatus(nz(first.getStatus()));
        r.setResubmit(true);
        r.setFirstBookNo(first.getBookNo());
        r.setFirstChannel(first.getChannel());
        List<TOadsIntakeRow> firstRows = rowMapper.listByBook(first.getId());
        Counts c = Counts.of(firstRows);
        r.setPhysicalCount(c.total);
        r.setTotalCount(c.total);
        r.setStoredCount(c.stored);
        r.setBackCount(c.back);
        r.setCountsBalanced(c.stored + c.back == c.total);
        r.setStatusText("重投另登·不入账");
        r.setNote("本回是同一批次码的第二回（首收：" + IntakeStep.channelName(first.getChannel()) + " 册 "
                + first.getBookNo() + "），不重复入账、不并账期；下列三数与逐行互印均回首收册重数：本册行数 "
                + c.total + "、入库行数 " + c.stored + "、退回行数 " + c.back);
        for (TOadsIntakeRow row : firstRows) {
            r.getRowChecks().add(toRowCheck(row));
        }
        return r;
    }

    // ================================================================ 内部：建账小工具

    private TOadsIntakeBook newBook(String bookNo, String batchNo, String quarter, String unitNo, String channel,
                                    String limitRaw, int rowLimit, Date now, String operator) {
        TOadsIntakeBook b = new TOadsIntakeBook();
        b.setBookNo(bookNo);
        b.setBatchNo(batchNo);
        b.setQuarterCode(quarter);
        b.setUnitNo(unitNo);
        b.setChannel(channel);
        b.setRowLimitRaw(limitRaw);
        b.setRowLimit(rowLimit);
        b.setBookSet(IntakeStep.SET_CURRENT);
        b.setArchivedCount(0);
        b.setRecvAt(now);
        b.setDelFlag(DEL_OK);
        b.setCreateBy(operator);
        b.setCreateTime(now);
        return b;
    }

    private TOadsIntakeRow newRow(TOadsIntakeBook book, int rowNo, IntakeRowForm rf, String channel,
                                  Date now, String operator, Map<String, Integer> codeSeq) {
        TOadsIntakeRow row = new TOadsIntakeRow();
        row.setBookId(book.getId());
        row.setBookNo(book.getBookNo());
        row.setRowNo(rowNo);
        String code = rf.getSpotCode() == null ? null : rf.getSpotCode().trim();
        row.setSpotCode(code);
        row.setBoardName(rf.getBoardName());
        row.setRoadCode(rf.getRoadCode());
        row.setQtyRaw(rf.getQtyText() == null ? null : rf.getQtyText().trim());
        row.setChkRaw(rf.getChkText() == null ? null : rf.getChkText().trim());
        String photo = rf.getPhotoNo() == null ? null : rf.getPhotoNo().trim();
        row.setPhotoNo(photo);
        row.setPhotoPending(photo == null ? YES : 0);
        row.setNameMismatch(0);
        row.setRoadMismatch(0);
        row.setArchived(0);
        // 同一处点位在一册里写两行：两条都落账的依据——各按各的物理行次，dup_seq 标出同码第几行，不并条。
        int seq = 1;
        if (code != null) {
            Integer prev = codeSeq.get(code);
            seq = prev == null ? 1 : prev + 1;
            codeSeq.put(code, seq);
        }
        row.setDupSeq(seq);
        row.setChannel(channel);
        row.setDelFlag(DEL_OK);
        row.setCreateBy(operator);
        row.setCreateTime(now);
        return row;
    }

    /** 抬头上限唯一来源：空着 200；负数、大得没边（超一亿）按没填也 200；写定多少是多少。 */
    private int resolveRowLimit(String raw) {
        if (raw == null || raw.isEmpty()) {
            return IntakeStep.DEFAULT_ROW_LIMIT;
        }
        Integer v = SpotForm.parseStrict(raw);
        if (v == null || v < 0 || v > IntakeStep.SANE_ROW_LIMIT_MAX) {
            return IntakeStep.DEFAULT_ROW_LIMIT;
        }
        return v;
    }

    private String normalizeChannel(String raw) {
        String c = trim(raw);
        if (IntakeStep.CHANNEL_DISTRICT.equals(c) || IntakeStep.CHANNEL_CITY.equals(c)) {
            return c;
        }
        throw new SpotRuleException("收讫进门：通道只认 DISTRICT区级报送 / CITY市级复核 两种，『"
                + (c == null ? "" : c) + "』不收");
    }

    private void appendJournal(Long bookId, String bookNo, Long rowId, Integer rowNo, int step, String event,
                               String verdict, String reasonCode, String detail, String operator, Date at) {
        TOadsIntakeJournal j = new TOadsIntakeJournal();
        j.setBookId(bookId);
        j.setBookNo(bookNo);
        j.setRowId(rowId);
        j.setRowNo(rowNo);
        j.setStepNo(step);
        j.setEvent(event);
        j.setVerdict(verdict);
        j.setReasonCode(reasonCode);
        j.setDetail(detail);
        j.setOperator(operator);
        j.setActedAt(at);
        j.setCreateBy(operator);
        j.setCreateTime(at);
        journalMapper.insert(j);
    }

    private TOadsIntakeBook requireBook(String bookNo) {
        String no = trim(bookNo);
        if (no == null) {
            throw new SpotRuleException("须点出哪一册");
        }
        TOadsIntakeBook b = bookMapper.findByBookNo(no);
        if (b == null) {
            throw new SpotRuleException("收讫账上查无此册：" + no);
        }
        return b;
    }

    private TOadsIntakeRow requireRow(Long bookId, int rowNo) {
        for (TOadsIntakeRow r : rowMapper.listByBook(bookId)) {
            if (r.getRowNo() != null && r.getRowNo() == rowNo) {
                return r;
            }
        }
        throw new SpotRuleException("这一册上没有第 " + rowNo + " 行");
    }

    private static String trim(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static String empty(String s) {
        return s == null || s.trim().isEmpty() ? "空" : s.trim();
    }

    private static Integer parseIntOrNull(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        return SpotForm.parseStrict(raw.trim());
    }

    private static int nz(Integer v) {
        return v == null ? 0 : v;
    }

    private static String join(List<String> parts, String sep) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (sb.length() > 0) {
                sb.append(sep);
            }
            sb.append(p);
        }
        return sb.toString();
    }

    /** 三数一篓：本册行数 / 入库行数（通过+待核）/ 退回行数。 */
    private static final class Counts {
        private final int total;
        private final int stored;
        private final int back;

        private Counts(int total, int stored, int back) {
            this.total = total;
            this.stored = stored;
            this.back = back;
        }

        private static Counts of(List<TOadsIntakeRow> rows) {
            int back = 0;
            for (TOadsIntakeRow r : rows) {
                if (IntakeStep.VERDICT_BACK.equals(r.getVerdict())) {
                    back++;
                }
            }
            return new Counts(rows.size(), rows.size() - back, back);
        }
    }

    /** 一行裁完的一篓结论。 */
    private static final class Adjudication {
        private String verdict;
        private String primaryReason;
        private String backReason;
        private String pendingNote;
        private String checkDetail;
    }
}
