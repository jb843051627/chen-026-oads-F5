-- oads 牌匾更换名册·区局收讫环节 —— schema（chen-026）
-- 与五检查点那一套完全分开：不改 t_oads_spot / t_oads_check_row 等任何旧表，
-- 收讫四步（点行数→验数据→定去向→出回执）只落在本文件这组新表上。
-- 列名与新实体契约（@TableName/@TableField）逐列对齐，改列必须同步实体。
-- 库：chen_026

-- ================================================================ 收讫册账（销账）
-- 一次报送一册一行。抬头上限只在本册 row_limit_raw 这一格有一条来源：
-- 空着按 200 认，写成负数或大得没边（超百亿）的按没填认，仍按 200，别处不设第二条来源。
-- 进门先数物理行数落到 row_count_physical，事后能翻出当初数了多少；
-- 三个回执数（在库/退回）由行账逐行重数后回写，与流水同一次计算，不采信页面自报。
CREATE TABLE IF NOT EXISTS t_oads_intake_book (
  id bigint NOT NULL COMMENT '主键',
  book_no varchar(64) NOT NULL COMMENT '册号（区局按季度名册合成一册的编号）',
  batch_no varchar(64) NOT NULL COMMENT '批次码（区级报送/市级复核双通道以此认重投）',
  quarter_code varchar(8) NOT NULL COMMENT '上送季度码 如2026Q4（各单位先归并到区里再按季合一册）',
  unit_no varchar(64) DEFAULT NULL COMMENT '归并报送的单位代号',
  channel varchar(16) NOT NULL COMMENT '本册通道 DISTRICT区级报送 CITY市级复核',
  row_limit_raw varchar(32) DEFAULT NULL COMMENT '本册抬头行数上限那格原文（空/负/没边照存，只此一条来源）',
  row_limit int NOT NULL COMMENT '写定上限：抬头空着或填成负数、没边的一律按200',
  row_count_physical int NOT NULL COMMENT '进门数出来的一地：物理行数，同点位两行也数两行',
  in_count int NOT NULL COMMENT '回执数·入库行数（通过+待核，逐行重数回写）',
  back_count int NOT NULL COMMENT '回执数·退回行数（五条理由判退，逐行重数回写）',
  status int NOT NULL COMMENT '册情形 1已收讫 2整册超限退回(留头留数不留行，不入行账)；重投第二回不进本表，另登 t_oads_intake_resubmit',
  -- 去重占键：收讫成功=批次码，第二回双通道重投被 uk_intake_dup_key 挡下；超限册为 NULL 不占码，整改后可用原批次码重报。
  dup_key varchar(64) GENERATED ALWAYS AS (CASE WHEN status = 1 THEN batch_no ELSE NULL END) STORED
    COMMENT '收讫成功才占批次码，第二回双通道重投挡在这',
  book_set varchar(16) NOT NULL DEFAULT 'CURRENT' COMMENT '册层 CURRENT现行册 HISTORY历史册（两年规矩只对历史册生效）',
  archived_count int NOT NULL DEFAULT 0 COMMENT '已转入历史卷宗层的待核行数（只改列示层，行一条不删）',
  recv_at datetime(3) NOT NULL COMMENT '四步连着做完出回执那一刻（毫秒）',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id),
  UNIQUE KEY uk_intake_book_no (book_no),
  -- 只收讫成功的册占批次码：重投整册第二回不写此键；超限册整改后允许用原批次码重报。
  UNIQUE KEY uk_intake_dup_key (dup_key),
  KEY idx_intake_book_batch (batch_no),
  KEY idx_intake_book_set (book_set, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='牌匾名册收讫册账（销账：一册一行）';

-- ================================================================ 收讫行账（一册内物理行各一条）
-- 依据说死：一个物理报送行落一条，按 册+原报行次 全册唯一定位；
-- 同一处点位在一册里写两行的，两行各落各账、不并条、不去重（dup_seq 标出同码第几行）。
-- 行不跨册走：本行永远挂在建账那册上，没有任何口子把它挪到别册。
CREATE TABLE IF NOT EXISTS t_oads_intake_row (
  id bigint NOT NULL COMMENT '主键',
  book_id bigint NOT NULL COMMENT '建账那册（行不跨册）',
  book_no varchar(64) NOT NULL COMMENT '册号冗余',
  row_no int NOT NULL COMMENT '原报行次（物理顺序，同码两行各有各的行次）',
  spot_code varchar(64) DEFAULT NULL COMMENT '报文点位编码（原样，身份只认它精确配主档）',
  board_name varchar(255) DEFAULT NULL COMMENT '报文版面名称（只用来比对，近似配不作数）',
  road_code varchar(64) DEFAULT NULL COMMENT '报文路段代号（只用来比对）',
  qty int DEFAULT NULL COMMENT '应更换处数（NULL=这一项没到，不代填不默认零）',
  chk_num int DEFAULT NULL COMMENT '核验合格处数（NULL=这一项没到，不代填不默认零）',
  qty_raw varchar(32) DEFAULT NULL COMMENT '应更换处数原文（写不成整数照存，按缺项待核）',
  chk_raw varchar(32) DEFAULT NULL COMMENT '核验合格处数原文',
  photo_no varchar(128) DEFAULT NULL COMMENT '现场照片附件编号（缺照片先空着，下轮核看补挂）',
  photo_pending int NOT NULL DEFAULT 0 COMMENT '是否缺照占位 1缺照照样占行 0已挂',
  matched_spot_id bigint DEFAULT NULL COMMENT '精确配上的点位主档行主键（配不上留空）',
  master_board_name varchar(255) DEFAULT NULL COMMENT '主档带出的版面名称（两边有出入以此为准）',
  master_road_code varchar(64) DEFAULT NULL COMMENT '主档带出的路段代号（以此为准）',
  name_mismatch int NOT NULL DEFAULT 0 COMMENT '版面名称报文与主档有出入 1是（回执逐行列）',
  road_mismatch int NOT NULL DEFAULT 0 COMMENT '路段代号报文与主档有出入 1是（回执逐行列）',
  dup_seq int NOT NULL DEFAULT 1 COMMENT '同册同点位编码第几行（不并条的依据）',
  verdict varchar(8) NOT NULL COMMENT '行结论 PENDING待核 PASS通过 BACK退回（第四种写法不予写入）',
  back_reason_code varchar(32) DEFAULT NULL COMMENT '退回主因码（五条定序取首条）',
  back_reason varchar(500) DEFAULT NULL COMMENT '退回理由全文（一条一句，命中几条写几句）',
  pending_note varchar(500) DEFAULT NULL COMMENT '待核交代（配不上/数量只到一项/缺照，逐项写明）',
  pending_since datetime(3) DEFAULT NULL COMMENT '结论首次停在待核的时刻（满两个年度归档用，补挂通过不抹早）',
  archived int NOT NULL DEFAULT 0 COMMENT '1已转入历史卷宗层（现行名单不再列出，行不删照常可查）',
  archived_at datetime(3) DEFAULT NULL COMMENT '转入历史卷宗层时刻',
  channel varchar(16) NOT NULL COMMENT '本行走哪条通道进的（随册）',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_intake_row_line (book_id, row_no),
  KEY idx_intake_row_code (book_id, spot_code),
  KEY idx_intake_row_verdict (book_id, verdict, archived)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='牌匾名册收讫行账（物理行各一条，行不跨册）';

-- ================================================================ 收讫裁决流水（append-only）
-- 四步每步一行/每行一行，只许追加；销账（册账计数）与流水同事务同一次计算写出。
-- 账实差以这张流水为准：销账三个数与流水重数对不上时，以流水重数纠销账，
-- 那一笔由收讫环节自己认（READJUST 事件交代是谁纠的、为什么纠）。
CREATE TABLE IF NOT EXISTS t_oads_intake_journal (
  id bigint NOT NULL COMMENT '主键',
  book_id bigint NOT NULL COMMENT '哪一册',
  book_no varchar(64) NOT NULL COMMENT '册号冗余',
  row_id bigint DEFAULT NULL COMMENT '哪一行（册级事件留空）',
  row_no int DEFAULT NULL COMMENT '原报行次冗余',
  step_no int NOT NULL COMMENT '四步 1点行数 2验数据 3定去向 4出回执',
  event varchar(24) NOT NULL COMMENT '事件 COUNTED/CHECKED/ROUTED/RECEIPTED/RESUBMIT/READJUST/PHOTO_FILL/ARCHIVED',
  verdict varchar(8) DEFAULT NULL COMMENT '本行裁决快照 PENDING/PASS/BACK',
  reason_code varchar(32) DEFAULT NULL COMMENT '退回理由码（逐行可一句句回查）',
  detail varchar(1000) DEFAULT NULL COMMENT '这一步记下的原话（点了多少行、缺哪项、退在哪条）',
  operator varchar(64) DEFAULT NULL COMMENT '经办岗（只记是谁，不由他裁）',
  acted_at datetime(3) NOT NULL COMMENT '落流水时刻（毫秒）',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_journal_book_step (book_id, step_no, event),
  KEY idx_journal_row (row_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='牌匾名册收讫裁决流水（append-only，账实差以此为准）';

-- ================================================================ 双通道重投另登
-- 同一个批次码经区级报送、市级复核各递一回：第一回照收，第二回不入账、不并账期，
-- 在本表另登一笔，指出首收册号/通道/时刻，交得代出。行一行不挪、不重复落。
CREATE TABLE IF NOT EXISTS t_oads_intake_resubmit (
  id bigint NOT NULL COMMENT '主键',
  batch_no varchar(64) NOT NULL COMMENT '重投的批次码',
  first_book_id bigint NOT NULL COMMENT '首收那册主键',
  first_book_no varchar(64) NOT NULL COMMENT '首收册号（回执去翻首收册）',
  first_channel varchar(16) NOT NULL COMMENT '首收通道',
  second_channel varchar(16) NOT NULL COMMENT '第二回通道（与首收不是同一条才登记）',
  second_row_count int NOT NULL COMMENT '第二回自报行数（只登记，不据此动账）',
  note varchar(500) DEFAULT NULL COMMENT '本环节给的交代',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_resubmit_batch (batch_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='收讫双通道重投另登（第二回不入账）';

-- ================================================================ 当地核验上限
-- 退回理由第一条"应更换处数高于当地那条核验上限"的唯一取值处：按辖段给，
-- 不随报文走、页面不能临时改；辖段在此查无上限的，不判超限（流水里注明无此段上限）。
CREATE TABLE IF NOT EXISTS t_oads_verify_cap (
  id bigint NOT NULL COMMENT '主键',
  area_no varchar(64) NOT NULL COMMENT '辖段/路段代号（当地）',
  cap_qty int NOT NULL COMMENT '当地核验上限（应更换处数高于此条判退回）',
  status int DEFAULT 0 COMMENT '情形 0在用 1停用',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id),
  UNIQUE KEY uk_verify_cap_area (area_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='当地核验上限（退回理由第一条唯一取值处）';

INSERT IGNORE INTO t_oads_verify_cap (id, area_no, cap_qty, status, del_flag, create_by, create_time)
VALUES (0, 'DU00', 100, 0, 0, 'seed', NOW()),
       (1, 'DU02', 50, 0, 0, 'seed', NOW());

-- ================================================================ 菜单与权限（幂等）
-- 权限码与 OadsIntakeBookController 上的 @RequiresPermissions 逐字对齐。
INSERT INTO t_sys_permission (id, name, descripion, url, is_blank, pid, perms, type, icon, order_num, visible, create_by, create_time)
SELECT 9110, '牌匾名册收讫', '区局收讫环节：点行数验数据定去向下回执', '/oadsIntakeBook/view', 0, 0, 'oads:oadsIntakeBook:view', 1, 'layui-icon-form', 92, 0, 'seed', NOW()
WHERE NOT EXISTS (SELECT 1 FROM t_sys_permission WHERE id = 9110);
INSERT INTO t_sys_permission (id, name, descripion, url, is_blank, pid, perms, type, icon, order_num, visible, create_by, create_time)
SELECT 9111, '整册收讫', '一册一次交完整，四步连着做完出回执', '/oadsIntakeBook/receive', 0, 9110, 'oads:oadsIntakeBook:receive', 2, 'layui-icon-ok', NULL, 0, 'seed', NOW()
WHERE NOT EXISTS (SELECT 1 FROM t_sys_permission WHERE id = 9111);
INSERT INTO t_sys_permission (id, name, descripion, url, is_blank, pid, perms, type, icon, order_num, visible, create_by, create_time)
SELECT 9112, '回执回查', '三数回库逐行重数，逐行互印点名列差异', '/oadsIntakeBook/receipt', 0, 9110, 'oads:oadsIntakeBook:list', 2, 'fa fa-file-text-o', NULL, 0, 'seed', NOW()
WHERE NOT EXISTS (SELECT 1 FROM t_sys_permission WHERE id = 9112);
INSERT INTO t_sys_permission (id, name, descripion, url, is_blank, pid, perms, type, icon, order_num, visible, create_by, create_time)
SELECT 9113, '缺照补挂', '下一轮核看把照片补挂上，并重算这一行', '/oadsIntakeBook/attachPhoto', 0, 9110, 'oads:oadsIntakeBook:photo', 2, 'fa fa-picture-o', NULL, 0, 'seed', NOW()
WHERE NOT EXISTS (SELECT 1 FROM t_sys_permission WHERE id = 9113);
INSERT INTO t_sys_permission (id, name, descripion, url, is_blank, pid, perms, type, icon, order_num, visible, create_by, create_time)
SELECT 9114, '销账流水对账', '账实差以流水为准，纠销账那笔收讫环节认', '/oadsIntakeBook/reconcile', 0, 9110, 'oads:oadsIntakeBook:reconcile', 2, 'layui-icon-refresh-1', NULL, 0, 'seed', NOW()
WHERE NOT EXISTS (SELECT 1 FROM t_sys_permission WHERE id = 9114);
INSERT INTO t_sys_permission (id, name, descripion, url, is_blank, pid, perms, type, icon, order_num, visible, create_by, create_time)
SELECT 9115, '待核旧行归档', '历史册待核连着满两个年度转卷宗层（只对历史册）', '/oadsIntakeBook/archive', 0, 9110, 'oads:oadsIntakeBook:archive', 2, 'layui-icon-archive', NULL, 0, 'seed', NOW()
WHERE NOT EXISTS (SELECT 1 FROM t_sys_permission WHERE id = 9115);
