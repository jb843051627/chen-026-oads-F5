package com.fc.v2.mapper.auto;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fc.v2.model.auto.TOadsIntakeBook;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 牌匾名册收讫册账数据层。
 *
 * @author fuce
 * @date 2026-10-05
 */
public interface TOadsIntakeBookMapper extends BaseMapper<TOadsIntakeBook> {

    /** 按册号回查（一次报送一册，册号全库唯一）。 */
    @Select("SELECT * FROM t_oads_intake_book WHERE book_no = #{bookNo} AND del_flag = 0 LIMIT 1")
    TOadsIntakeBook findByBookNo(@Param("bookNo") String bookNo);

    /**
     * 这个批次码是否已有收讫成功的首收册（双通道重投认它）。
     * 整册超限退回不占批次码（dup_key 为 NULL），整改后可用原批次码重报，故只查 status=1。
     */
    @Select("SELECT * FROM t_oads_intake_book WHERE batch_no = #{batchNo} AND status = 1 AND del_flag = 0 LIMIT 1")
    TOadsIntakeBook findReceivedByBatch(@Param("batchNo") String batchNo);
}
