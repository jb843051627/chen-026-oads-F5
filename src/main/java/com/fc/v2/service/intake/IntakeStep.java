package com.fc.v2.service.intake;

/**
 * 牌匾更换名册·区局收讫环节的唯一取号处。
 *
 * <p>四步固定连着做：{@link #STEP_COUNT}点行数 → {@link #STEP_CHECK}验数据 →
 * {@link #STEP_ROUTE}定去向 → {@link #STEP_RECEIPT}出回执，中间没有人工兜底。
 * 行结论只认三个取值（待核/通过/退回），第四种写法不予写入；
 * 退回理由只认下面五条（整册超限是册级理由），本处不给第六条开口子。
 *
 * @author fuce
 * @date 2026-10-05
 */
public final class IntakeStep {

    private IntakeStep() {
    }

    // ---------------------------------------------------------------- 四步
    /** 第1步 点行数 */
    public static final int STEP_COUNT = 1;
    /** 第2步 验数据 */
    public static final int STEP_CHECK = 2;
    /** 第3步 定去向 */
    public static final int STEP_ROUTE = 3;
    /** 第4步 出回执 */
    public static final int STEP_RECEIPT = 4;

    // ---------------------------------------------------------------- 行结论（只认三个）
    /** 待核（配不上/数量只到一项/缺照，行照样在册里占位） */
    public static final String VERDICT_PENDING = "PENDING";
    /** 通过（入库） */
    public static final String VERDICT_PASS = "PASS";
    /** 退回（不入库，理由逐行留痕） */
    public static final String VERDICT_BACK = "BACK";

    // ---------------------------------------------------------------- 册情形
    /** 已收讫（四步走完，占批次码） */
    public static final int BOOK_RECEIVED = 1;
    /** 整册超行数上限退回（留头留数，行逐行随册退回） */
    public static final int BOOK_OVER_LIMIT = 2;

    // ---------------------------------------------------------------- 册层
    /** 现行册（两年归档规矩走不到它面前） */
    public static final String SET_CURRENT = "CURRENT";
    /** 历史册（只有历史册上的长期待核行才转入卷宗层） */
    public static final String SET_HISTORY = "HISTORY";

    // ---------------------------------------------------------------- 通道
    /** 区级报送 */
    public static final String CHANNEL_DISTRICT = "DISTRICT";
    /** 市级复核 */
    public static final String CHANNEL_CITY = "CITY";

    // ---------------------------------------------------------------- 唯一口径常量
    /** 抬头那格空着（或写成负数、大得没边）时，单册行数上限一律按 200 认；别处不设第二条来源。 */
    public static final int DEFAULT_ROW_LIMIT = 200;
    /** 大得没边的界：超过一百亿行的上限按没填认。 */
    public static final int SANE_ROW_LIMIT_MAX = 100_000_000;
    /** 点位编码规范位数：位数不够（短于此）按配不上；长度超规范（长于 {@link #CODE_MAX_LEN}）判退回。 */
    public static final int CODE_MIN_LEN = 8;
    /** 点位编码规范上限位数：超出即退回理由第五条。 */
    public static final int CODE_MAX_LEN = 32;

    // ---------------------------------------------------------------- 退回理由码（五条定序 + 册级一条）
    /** 应更换处数高于当地那条核验上限 */
    public static final String R_QTY_OVER_CAP = "R_QTY_OVER_CAP";
    /** 应更换处数为零 */
    public static final String R_QTY_ZERO = "R_QTY_ZERO";
    /** 应更换处数为负数 */
    public static final String R_QTY_NEGATIVE = "R_QTY_NEGATIVE";
    /** 点位编码缺填 */
    public static final String R_CODE_MISSING = "R_CODE_MISSING";
    /** 编码长度超出规范 */
    public static final String R_CODE_TOO_LONG = "R_CODE_TOO_LONG";
    /** 册级：整册行数超抬头上限，每行随册退回 */
    public static final String R_BOOK_OVER_LIMIT = "R_BOOK_OVER_LIMIT";

    // ---------------------------------------------------------------- 流水事件（append-only）
    /** 第1步：点行数（册级，物理行数与写定上限各一句） */
    public static final String EVT_COUNTED = "COUNTED";
    /** 第2步：验数据（每行，缺项/配不上/缺照写原话） */
    public static final String EVT_CHECKED = "CHECKED";
    /** 第3步：定去向（每行，verdict+理由码快照） */
    public static final String EVT_ROUTED = "ROUTED";
    /** 第4步：出回执（册级，三数与勾稽） */
    public static final String EVT_RECEIPTED = "RECEIPTED";
    /** 双通道重投：第二回不入账，另登一笔 */
    public static final String EVT_RESUBMIT = "RESUBMIT";
    /** 对账纠销账：账实差以流水为准，这一笔归收讫环节认 */
    public static final String EVT_READJUST = "READJUST";
    /** 缺照补挂后重裁本行 */
    public static final String EVT_PHOTO_FILL = "PHOTO_FILL";
    /** 历史册长期待核行转入卷宗层 */
    public static final String EVT_ARCHIVED = "ARCHIVED";
    /** 区局把一册由现行划入历史层（两年规矩的前置） */
    public static final String EVT_MARK_HISTORY = "MARK_HISTORY";

    /** 五条退回理由的固定次序：一句句回查、取主因都照这个序。 */
    public static final String[] REASON_ORDER = {
            R_QTY_OVER_CAP, R_QTY_ZERO, R_QTY_NEGATIVE, R_CODE_MISSING, R_CODE_TOO_LONG
    };

    public static String reasonText(String code) {
        if (code == null) {
            return "";
        }
        switch (code) {
            case R_QTY_OVER_CAP:
                return "应更换处数高于当地那条核验上限";
            case R_QTY_ZERO:
                return "应更换处数为零";
            case R_QTY_NEGATIVE:
                return "应更换处数为负数";
            case R_CODE_MISSING:
                return "点位编码缺填";
            case R_CODE_TOO_LONG:
                return "编码长度超出规范";
            case R_BOOK_OVER_LIMIT:
                return "整册行数超出本册抬头写明的上限，整册退回";
            default:
                return code;
        }
    }

    public static String verdictName(String verdict) {
        if (VERDICT_PENDING.equals(verdict)) {
            return "待核";
        }
        if (VERDICT_PASS.equals(verdict)) {
            return "通过";
        }
        if (VERDICT_BACK.equals(verdict)) {
            return "退回";
        }
        return "未知";
    }

    public static String channelName(String channel) {
        if (CHANNEL_DISTRICT.equals(channel)) {
            return "区级报送";
        }
        if (CHANNEL_CITY.equals(channel)) {
            return "市级复核";
        }
        return channel;
    }
}
