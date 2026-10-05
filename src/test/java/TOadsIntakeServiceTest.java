import com.fc.v2.common.exception.spot.SpotRuleException;
import com.fc.v2.mapper.auto.TOadsIntakeBookMapper;
import com.fc.v2.mapper.auto.TOadsIntakeJournalMapper;
import com.fc.v2.mapper.auto.TOadsIntakeResubmitMapper;
import com.fc.v2.mapper.auto.TOadsIntakeRowMapper;
import com.fc.v2.mapper.auto.TOadsSpotMapper;
import com.fc.v2.mapper.auto.TOadsVerifyCapMapper;
import com.fc.v2.model.auto.TOadsIntakeBook;
import com.fc.v2.model.auto.TOadsIntakeJournal;
import com.fc.v2.model.auto.TOadsIntakeRow;
import com.fc.v2.model.auto.TOadsSpot;
import com.fc.v2.model.auto.TOadsVerifyCap;
import com.fc.v2.model.custom.IntakeArchiveOutcome;
import com.fc.v2.model.custom.IntakeBookForm;
import com.fc.v2.model.custom.IntakeReceipt;
import com.fc.v2.model.custom.IntakeReconcileOutcome;
import com.fc.v2.model.custom.IntakeRowForm;
import com.fc.v2.service.ITOadsSpotRegisterService;
import com.fc.v2.service.intake.IntakeStep;
import com.fc.v2.service.intake.TOadsIntakeServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 区局收讫环节四步：点行数（抬头上限唯一来源、物理行逐行落账含同码两行）、
 * 验数据（双数量同到、编码精确配主档、缺照占位、结论只三个值）、定去向（五条退回理由逐行、
 * 双通道重投另登）、出回执（三数回库重数、逐行互印）；以及销账/流水对账以流水为准、
 * 两年归档只对历史册。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class TOadsIntakeServiceTest {

    @Mock private TOadsIntakeBookMapper bookMapper;
    @Mock private TOadsIntakeRowMapper rowMapper;
    @Mock private TOadsIntakeJournalMapper journalMapper;
    @Mock private TOadsIntakeResubmitMapper resubmitMapper;
    @Mock private TOadsVerifyCapMapper capMapper;
    @Mock private TOadsSpotMapper spotMapper;
    @Mock private ITOadsSpotRegisterService registerService;

    private TOadsIntakeServiceImpl svc;

    private final List<TOadsIntakeBook> books = new ArrayList<TOadsIntakeBook>();
    private final List<TOadsIntakeRow> rows = new ArrayList<TOadsIntakeRow>();
    private final List<TOadsIntakeJournal> journals = new ArrayList<TOadsIntakeJournal>();
    private long idSeq = 5000;

    @BeforeEach
    public void wire() {
        svc = new TOadsIntakeServiceImpl();
        ReflectionTestUtils.setField(svc, "bookMapper", bookMapper);
        ReflectionTestUtils.setField(svc, "rowMapper", rowMapper);
        ReflectionTestUtils.setField(svc, "journalMapper", journalMapper);
        ReflectionTestUtils.setField(svc, "resubmitMapper", resubmitMapper);
        ReflectionTestUtils.setField(svc, "capMapper", capMapper);
        ReflectionTestUtils.setField(svc, "spotMapper", spotMapper);
        ReflectionTestUtils.setField(svc, "registerService", registerService);

        when(registerService.currentQuarter()).thenReturn("2026Q4");

        doAnswer(inv -> {
            TOadsIntakeBook b = inv.getArgument(0);
            b.setId(++idSeq);
            books.add(b);
            return 1;
        }).when(bookMapper).insert(any(TOadsIntakeBook.class));
        doAnswer(inv -> {
            TOadsIntakeBook b = inv.getArgument(0);
            for (int i = 0; i < books.size(); i++) {
                if (books.get(i).getId().equals(b.getId())) {
                    books.set(i, b);
                }
            }
            return 1;
        }).when(bookMapper).updateById(any(TOadsIntakeBook.class));
        when(bookMapper.findByBookNo(anyString())).thenAnswer(inv -> {
            String no = inv.getArgument(0);
            for (TOadsIntakeBook b : books) {
                if (no.equals(b.getBookNo())) {
                    return b;
                }
            }
            return null;
        });
        when(bookMapper.findReceivedByBatch(anyString())).thenAnswer(inv -> {
            String no = inv.getArgument(0);
            for (TOadsIntakeBook b : books) {
                if (no.equals(b.getBatchNo()) && b.getStatus() == IntakeStep.BOOK_RECEIVED) {
                    return b;
                }
            }
            return null;
        });
        when(bookMapper.selectList(any())).thenAnswer(inv -> new ArrayList<TOadsIntakeBook>(books));

        doAnswer(inv -> {
            TOadsIntakeRow r = inv.getArgument(0);
            r.setId(++idSeq);
            rows.add(r);
            return 1;
        }).when(rowMapper).insert(any(TOadsIntakeRow.class));
        doAnswer(inv -> {
            TOadsIntakeRow r = inv.getArgument(0);
            for (int i = 0; i < rows.size(); i++) {
                if (rows.get(i).getId().equals(r.getId())) {
                    rows.set(i, r);
                }
            }
            return 1;
        }).when(rowMapper).updateById(any(TOadsIntakeRow.class));
        when(rowMapper.listByBook(any())).thenAnswer(inv -> {
            Long bookId = inv.getArgument(0);
            List<TOadsIntakeRow> out = new ArrayList<TOadsIntakeRow>();
            for (TOadsIntakeRow r : rows) {
                if (bookId.equals(r.getBookId())) {
                    out.add(r);
                }
            }
            return out;
        });

        doAnswer(inv -> {
            TOadsIntakeJournal j = inv.getArgument(0);
            j.setId(++idSeq);
            journals.add(j);
            return 1;
        }).when(journalMapper).insert(any(TOadsIntakeJournal.class));
        when(journalMapper.listByBook(any())).thenAnswer(inv -> {
            Long bookId = inv.getArgument(0);
            List<TOadsIntakeJournal> out = new ArrayList<TOadsIntakeJournal>();
            for (TOadsIntakeJournal j : journals) {
                if (bookId.equals(j.getBookId())) {
                    out.add(j);
                }
            }
            return out;
        });

        // 主档：按编码全等配；版面名/路段以主档带出。
        when(spotMapper.selectList(any())).thenAnswer(inv -> {
            com.baomidou.mybatisplus.core.conditions.AbstractWrapper<?, ?, ?> w = inv.getArgument(0);
            w.getSqlSegment(); // 触发占位符绑定，paramNameValuePairs 才会填
            String sql = w.getParamNameValuePairs().values().toString();
            List<TOadsSpot> out = new ArrayList<TOadsSpot>();
            if (sql.contains("SP0000001")) {
                out.add(master(11, "SP0000001", "DU00", "主档·临江路一号牌匾"));
            } else if (sql.contains("SP0000002")) {
                out.add(master(12, "SP0000002", "DU00", "主档·临江路二号牌匾"));
            }
            return out;
        });
        // 当地核验上限：DU00=100。
        when(capMapper.findActiveByArea(anyString())).thenAnswer(inv -> {
            String area = inv.getArgument(0);
            if ("DU00".equals(area)) {
                TOadsVerifyCap c = new TOadsVerifyCap();
                c.setAreaNo("DU00");
                c.setCapQty(100);
                return c;
            }
            return null;
        });
    }

    private TOadsSpot master(long id, String code, String areaNo, String boardName) {
        TOadsSpot s = new TOadsSpot();
        s.setId(id);
        s.setSpotNo(code);
        s.setAreaNo(areaNo);
        s.setContent(boardName);
        s.setQuarterCode("2026Q4");
        s.setDelFlag(0);
        return s;
    }

    private IntakeRowForm row(String code, String qty, String chk, String photo,
                              String boardName, String roadCode) {
        IntakeRowForm r = new IntakeRowForm();
        r.setSpotCode(code);
        r.setQtyText(qty);
        r.setChkText(chk);
        r.setPhotoNo(photo);
        r.setBoardName(boardName);
        r.setRoadCode(roadCode);
        return r;
    }

    private IntakeBookForm book(String bookNo, String batchNo, String channel,
                                String limitRaw, List<IntakeRowForm> rs) {
        IntakeBookForm f = new IntakeBookForm();
        f.setBookNo(bookNo);
        f.setBatchNo(batchNo);
        f.setChannel(channel);
        f.setQuarterCode("2026Q4");
        f.setUnitNo("U-云桥");
        f.setRowLimitText(limitRaw);
        f.setRows(rs);
        return f;
    }

    private TOadsIntakeRow findRow(long bookSeq, int rowNo) {
        Long bookId = books.get((int) bookSeq).getId();
        for (TOadsIntakeRow r : rows) {
            if (bookId.equals(r.getBookId()) && r.getRowNo() == rowNo) {
                return r;
            }
        }
        return null;
    }

    // ---------------------------------------------------------------- 四步连着做：三数勾稽、逐行结论

    @Test
    public void fourStepsProduceBalancedReceiptWithPerRowVerdicts() {
        List<IntakeRowForm> rs = Arrays.asList(
                // 1 通过：编码配上、两数量同到、照片随册到
                row("SP0000001", "10", "8", "P-1", "主档·临江路一号牌匾", "DU00"),
                // 2 同点位再写一行：不并条，两条都落账；缺照占位 → 待核
                row("SP0000001", "5", "5", null, "主档·临江路一号牌匾", "DU00"),
                // 3 应更换高于当地上限100 → 退回（主因第一条）
                row("SP0000002", "150", "1", "P-3", "主档·临江路二号牌匾", "DU00"),
                // 4 应更换为零 → 退回
                row("SP0000002", "0", "0", "P-4", "主档·临江路二号牌匾", "DU00"),
                // 5 应更换为负 → 退回
                row("SP0000002", "-3", "0", "P-5", "主档·临江路二号牌匾", "DU00"),
                // 6 编码缺填 → 退回
                row(null, "5", "5", "P-6", "某牌匾", "DU00"),
                // 7 编码超长（33位）→ 退回
                row("123456789012345678901234567890123", "5", "5", "P-7", "x", "DU00"),
                // 8 编码位数不够(7位)配不上 → 待核（不按版面名近似配）
                row("SP00009", "5", "5", "P-8", "主档·临江路一号牌匾", "DU00"),
                // 9 核验合格这一项没到 → 待核，不代填不默认零
                row("SP0000002", "9", null, "P-9", "主档·临江路二号牌匾", "DU00"));

        IntakeReceipt rc = svc.receive(book("BK-1", "BAT-1", IntakeStep.CHANNEL_DISTRICT, "100", rs), "收讫岗");

        assertEquals(9, rc.getTotalCount());
        assertEquals(4, rc.getStoredCount());   // 行1通过 + 行2/8/9待核
        assertEquals(5, rc.getBackCount());     // 行3-7
        assertTrue(rc.isCountsBalanced());
        assertEquals(IntakeStep.BOOK_RECEIVED, rc.getBookStatus());

        TOadsIntakeBook b = books.get(0);
        assertEquals(9, b.getRowCountPhysical().intValue());
        assertEquals(4, b.getInCount().intValue());
        assertEquals(5, b.getBackCount().intValue());
        assertEquals(100, b.getRowLimit().intValue());

        assertEquals(IntakeStep.VERDICT_PASS, findRow(0, 1).getVerdict());
        TOadsIntakeRow dup1 = findRow(0, 1);
        TOadsIntakeRow dup2 = findRow(0, 2);
        assertEquals(1, dup1.getDupSeq().intValue());
        assertEquals(2, dup2.getDupSeq().intValue());   // 同码两行各落各账的依据
        assertEquals(IntakeStep.VERDICT_PENDING, dup2.getVerdict());
        assertTrue(dup2.getPhotoPending() == 1);
        assertNotNull(dup2.getPendingSince());

        assertEquals(IntakeStep.VERDICT_BACK, findRow(0, 3).getVerdict());
        assertEquals(IntakeStep.R_QTY_OVER_CAP, findRow(0, 3).getBackReasonCode());
        assertTrue(findRow(0, 3).getBackReason().contains("100"));
        assertEquals(IntakeStep.R_QTY_ZERO, findRow(0, 4).getBackReasonCode());
        assertEquals(IntakeStep.R_QTY_NEGATIVE, findRow(0, 5).getBackReasonCode());
        assertEquals(IntakeStep.R_CODE_MISSING, findRow(0, 6).getBackReasonCode());
        assertEquals(IntakeStep.R_CODE_TOO_LONG, findRow(0, 7).getBackReasonCode());

        assertEquals(IntakeStep.VERDICT_PENDING, findRow(0, 8).getVerdict());
        assertTrue(findRow(0, 8).getPendingNote().contains("位数不够"));
        assertEquals(IntakeStep.VERDICT_PENDING, findRow(0, 9).getVerdict());
        assertTrue(findRow(0, 9).getPendingNote().contains("核验合格处数缺项"));
        assertNull(findRow(0, 9).getChkNum());

        // 每行 CHECKED+ROUTED 各一笔，册头 COUNTED+RECEIPTED 各一笔，append-only 全落。
        long routed = journals.stream().filter(j -> IntakeStep.EVT_ROUTED.equals(j.getEvent())
                && j.getRowId() != null).count();
        assertEquals(9, routed);
        assertTrue(journals.stream().anyMatch(j -> IntakeStep.EVT_COUNTED.equals(j.getEvent())));
        assertTrue(journals.stream().anyMatch(j -> IntakeStep.EVT_RECEIPTED.equals(j.getEvent())));

        // 回执逐行互印：九行都在，第四种结论不存在，且每行结论都与三数互印一致。
        assertEquals(9, rc.getRowChecks().size());
        assertTrue(rc.getRowChecks().stream().allMatch(c ->
                Arrays.asList("PENDING", "PASS", "BACK").contains(c.getVerdict())));
        assertTrue(rc.getRowChecks().stream().allMatch(IntakeReceipt.RowCheck::isConsistent),
                "正常册逐行互印应全部一致，没有可点名的对不齐");
    }

    @Test
    public void multipleReasonsAllWrittenWithFirstAsPrimary() {
        // 编码缺填 + 应更换超上限：固定序第一条（超限）为主因，两句都写。
        List<IntakeRowForm> rs = Arrays.asList(
                row("SP0000001", "10", "8", "P-1", "主档·临江路一号牌匾", "DU00"),
                row(null, "999", "1", "P-2", "x", "DU00"));
        IntakeReceipt rc = svc.receive(book("BK-M", "BAT-M", IntakeStep.CHANNEL_DISTRICT, "50", rs), "岗");
        TOadsIntakeRow r2 = findRow(0, 2);
        assertEquals(IntakeStep.R_QTY_OVER_CAP, r2.getBackReasonCode());
        assertTrue(r2.getBackReason().contains("核验上限"));
        assertTrue(r2.getBackReason().contains("点位编码缺填"));
        assertEquals(1, rc.getStoredCount());
        assertEquals(1, rc.getBackCount());
    }

    // ---------------------------------------------------------------- 第1步：抬头上限唯一来源

    @Test
    public void blankOrNonsenseHeaderDefaultsTo200() {
        for (String bad : Arrays.asList(null, "", "  ", "-5", "abc", "99999999999")) {
            books.clear();
            rows.clear();
            journals.clear();
            List<IntakeRowForm> one = Arrays.asList(
                    row("SP0000001", "10", "8", "P", "主档·临江路一号牌匾", "DU00"));
            IntakeReceipt rc = svc.receive(
                    book("BK-D" + books.size() + (bad == null ? "N" : bad.replaceAll("\\W", "")),
                            "BAT-D" + (bad == null ? "N" : bad), IntakeStep.CHANNEL_DISTRICT, bad, one), "岗");
            assertEquals(200, rc.getRowLimit(), "抬头『" + bad + "』应按没填认 200");
        }
    }

    @Test
    public void overHeaderLimitWholeBookRejectedNoRowsStored() {
        List<IntakeRowForm> big = new ArrayList<IntakeRowForm>();
        for (int i = 0; i < 201; i++) {
            big.add(row("SP0000001", "1", "1", "P", "主档·临江路一号牌匾", "DU00"));
        }
        int rowsBefore = rows.size();
        IntakeReceipt rc = svc.receive(book("BK-BIG", "BAT-BIG", IntakeStep.CHANNEL_DISTRICT, "200", big), "岗");
        assertEquals(IntakeStep.BOOK_OVER_LIMIT, rc.getBookStatus());
        assertEquals(201, rc.getTotalCount());
        assertEquals(0, rc.getStoredCount());
        assertEquals(201, rc.getBackCount());
        assertTrue(rc.isCountsBalanced());
        assertEquals(rowsBefore, rows.size(), "整册退回留头留数不留行：行账一条不落");
        // 超限册不占批次码：整改到 200 行以内可用原批次码重报。
        List<IntakeRowForm> fixed = new ArrayList<IntakeRowForm>(big.subList(0, 200));
        IntakeReceipt rc2 = svc.receive(book("BK-BIG2", "BAT-BIG", IntakeStep.CHANNEL_DISTRICT, "200", fixed), "岗");
        assertEquals(IntakeStep.BOOK_RECEIVED, rc2.getBookStatus());
        assertEquals(200, rc2.getStoredCount());
    }

    // ---------------------------------------------------------------- 第3步：双通道重投另登

    @Test
    public void secondChannelSameBatchIsRegisteredNotBooked() {
        List<IntakeRowForm> rs1 = Arrays.asList(
                row("SP0000001", "10", "8", "P-1", "主档·临江路一号牌匾", "DU00"));
        IntakeReceipt first = svc.receive(book("BK-A", "DUP-BAT", IntakeStep.CHANNEL_DISTRICT, null, rs1), "区岗");
        assertFalse(first.isResubmit());
        int booksAfterFirst = books.size();

        List<IntakeRowForm> rs2 = Arrays.asList(
                row("SP0000001", "10", "8", "P-1", "主档·临江路一号牌匾", "DU00"),
                row("SP0000002", "5", "5", "P-2", "主档·临江路二号牌匾", "DU00"));
        IntakeReceipt second = svc.receive(book("BK-B", "DUP-BAT", IntakeStep.CHANNEL_CITY, null, rs2), "市岗");

        assertTrue(second.isResubmit());
        assertEquals("BK-A", second.getFirstBookNo());
        assertEquals(IntakeStep.CHANNEL_DISTRICT, second.getFirstChannel());
        assertEquals(booksAfterFirst, books.size(), "第二回不入账：不新增册");
        assertEquals(1, second.getTotalCount(), "三数回首收册重数，第二回自报 2 行不作数");
        verify(resubmitMapper).insert(any());
        assertTrue(journals.stream().anyMatch(j -> IntakeStep.EVT_RESUBMIT.equals(j.getEvent())));
    }

    // ---------------------------------------------------------------- 第2步：缺照补挂后重裁

    @Test
    public void attachPhotoRejudgesOnlyThatRow() {
        List<IntakeRowForm> rs = Arrays.asList(
                row("SP0000001", "10", "8", null, "主档·临江路一号牌匾", "DU00"));
        IntakeReceipt before = svc.receive(book("BK-P", "BAT-P", IntakeStep.CHANNEL_DISTRICT, null, rs), "岗");
        assertEquals(IntakeStep.VERDICT_PENDING, findRow(0, 1).getVerdict());
        assertEquals(1, before.getStoredCount());

        IntakeReceipt after = svc.attachPhoto("BK-P", 1, "P-LATE", "核看岗");
        assertEquals(IntakeStep.VERDICT_PASS, findRow(0, 1).getVerdict());
        assertEquals(0, findRow(0, 1).getPhotoPending().intValue());
        assertEquals("P-LATE", findRow(0, 1).getPhotoNo());
        assertTrue(after.isCountsBalanced());
        assertTrue(journals.stream().anyMatch(j -> IntakeStep.EVT_PHOTO_FILL.equals(j.getEvent())));

        // 不缺照的行不收补挂；空编号也不收。
        assertThrows(SpotRuleException.class, () -> svc.attachPhoto("BK-P", 1, "P-X2", "岗"));
        assertThrows(SpotRuleException.class, () -> svc.attachPhoto("BK-P", 1, "  ", "岗"));
    }

    // ---------------------------------------------------------------- 第4步：名称/路段以主档为准逐行列

    @Test
    public void boardNameAndRoadDifferencesListedWithMasterWins() {
        List<IntakeRowForm> rs = Arrays.asList(
                row("SP0000001", "10", "8", "P-1", "报文乱写的版面名", "DU99"));
        IntakeReceipt rc = svc.receive(book("BK-N", "BAT-N", IntakeStep.CHANNEL_DISTRICT, null, rs), "岗");
        assertEquals(1, rc.getMismatchRows().size());
        IntakeReceipt.RowCheck c = rc.getMismatchRows().get(0);
        assertTrue(c.isNameMismatch());
        assertTrue(c.isRoadMismatch());
        assertEquals("主档·临江路一号牌匾", c.getMasterBoardName());
        assertEquals("DU00", c.getMasterRoadCode());
        // 报文路段对不上主档，不据此取上限；行仍按编码配上而通过。
        assertEquals(IntakeStep.VERDICT_PASS, c.getVerdict());
    }

    // ---------------------------------------------------------------- 销账/流水：账实差以流水为准

    @Test
    public void reconcileCorrectsLedgerByJournalAndOwnsIt() {
        List<IntakeRowForm> rs = Arrays.asList(
                row("SP0000001", "10", "8", "P-1", "主档·临江路一号牌匾", "DU00"),
                row("SP0000002", "150", "1", "P-3", "主档·临江路二号牌匾", "DU00"));
        svc.receive(book("BK-R", "BAT-R", IntakeStep.CHANNEL_DISTRICT, null, rs), "岗");

        // 人为把销账记错、并把行结论改花，模拟两处对不上。
        TOadsIntakeBook b = books.get(0);
        b.setInCount(0);
        b.setBackCount(1);
        TOadsIntakeRow r1 = findRow(0, 1);
        String truthVerdict = r1.getVerdict();
        r1.setVerdict(IntakeStep.VERDICT_BACK);

        IntakeReconcileOutcome o = svc.reconcile("BK-R", "对账岗");
        assertTrue(o.isAdjusted());
        assertEquals(2, o.getJournalTotal());
        assertEquals(1, o.getJournalIn());
        assertEquals(1, o.getJournalBack());
        assertEquals(truthVerdict, r1.getVerdict(), "行结论按流水最近一次定去向纠回");
        assertEquals(1, b.getInCount().intValue());
        assertEquals(1, b.getBackCount().intValue());
        assertEquals("收讫环节", b.getUpdateBy(), "纠账那笔归收讫环节自己认");
        assertTrue(journals.stream().anyMatch(j -> IntakeStep.EVT_READJUST.equals(j.getEvent())
                && j.getDetail().contains("以流水为准")));
    }

    @Test
    public void reconcileCleanWhenLedgerMatchesJournal() {
        List<IntakeRowForm> rs = Arrays.asList(
                row("SP0000001", "10", "8", "P-1", "主档·临江路一号牌匾", "DU00"));
        svc.receive(book("BK-OK", "BAT-OK", IntakeStep.CHANNEL_DISTRICT, null, rs), "岗");
        IntakeReconcileOutcome o = svc.reconcile("BK-OK", "岗");
        assertFalse(o.isAdjusted());
        assertTrue(o.getNote().contains("一致"));
        verify(journalMapper, never()).insert(org.mockito.ArgumentMatchers.argThat(
                j -> j != null && IntakeStep.EVT_READJUST.equals(j.getEvent())));
    }

    // ---------------------------------------------------------------- 两年归档：只对历史册

    @Test
    public void archiveOnlyHistoricalBooksKeepsEveryRow() {
        List<IntakeRowForm> rs = Arrays.asList(
                // 缺照待核
                row("SP0000001", "10", "8", null, "主档·临江路一号牌匾", "DU00"));
        svc.receive(book("BK-OLD", "BAT-OLD", IntakeStep.CHANNEL_DISTRICT, null, rs), "岗");
        List<IntakeRowForm> rs2 = Arrays.asList(
                row("SP0000001", "10", "8", null, "主档·临江路一号牌匾", "DU00"));
        svc.receive(book("BK-NEW", "BAT-NEW", IntakeStep.CHANNEL_DISTRICT, null, rs2), "岗");
        // 第一册划历史层，第二册仍现行。
        svc.markHistory("BK-OLD", "区局");

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -800);
        // 把两册待核行的待核起始日拨到两年多前。
        findRow(0, 1).setPendingSince(cal.getTime());
        findRow(1, 1).setPendingSince(cal.getTime());

        Date now = new Date();
        IntakeArchiveOutcome o = svc.archive(now, "归档岗");
        assertEquals(1, o.getArchivedRows().size());
        assertEquals("BK-OLD", o.getArchivedRows().get(0).getBookNo());
        assertEquals(1, o.getSkippedCurrent().size());
        assertEquals("BK-NEW", o.getSkippedCurrent().get(0).getBookNo());
        assertEquals(1, findRow(0, 1).getArchived().intValue());
        assertEquals(0, findRow(1, 1).getArchived().intValue(), "现行册走不到两年规矩面前");
        // 存量一条不减：行还在行账上，回执回查照样翻得到，并带着"已转卷宗层"标记。
        IntakeReceipt receipt = svc.receipt("BK-OLD");
        assertEquals(1, receipt.getRowChecks().size());
        assertTrue(receipt.getRowChecks().get(0).isArchived());
        assertTrue(journals.stream().anyMatch(j -> IntakeStep.EVT_ARCHIVED.equals(j.getEvent())));
    }

    // ---------------------------------------------------------------- 进门规矩

    @Test
    public void rejectsZeroRowsMissingHeaderAndPiecemeal() {
        assertThrows(SpotRuleException.class, () -> svc.receive(null, "岗"));
        IntakeBookForm noNo = book(null, "B", IntakeStep.CHANNEL_DISTRICT, null,
                Arrays.asList(row("SP0000001", "1", "1", "P", "m", "DU00")));
        assertThrows(SpotRuleException.class, () -> svc.receive(noNo, "岗"));
        IntakeBookForm noBatch = book("BK-Z", null, IntakeStep.CHANNEL_DISTRICT, null,
                Arrays.asList(row("SP0000001", "1", "1", "P", "m", "DU00")));
        assertThrows(SpotRuleException.class, () -> svc.receive(noBatch, "岗"));
        IntakeBookForm empty = book("BK-Z2", "B-Z2", IntakeStep.CHANNEL_DISTRICT, null,
                new ArrayList<IntakeRowForm>());
        assertThrows(SpotRuleException.class, () -> svc.receive(empty, "岗"));
        assertThrows(SpotRuleException.class, () ->
                svc.receive(book("BK-X", "B-X", "STREET", null,
                        Arrays.asList(row("SP0000001", "1", "1", "P", "m", "DU00"))), "岗"));
    }
}
