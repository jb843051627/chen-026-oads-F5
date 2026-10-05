package com.fc.v2.mapper.auto;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fc.v2.model.auto.TOadsIntakeRow;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 牌匾名册收讫行账数据层（物理行各一条，只追加/就地更新结论，不删行）。
 *
 * @author fuce
 * @date 2026-10-05
 */
public interface TOadsIntakeRowMapper extends BaseMapper<TOadsIntakeRow> {

    /** 一册全部行，按原报行次排好，供回执逐行重数与逐行互印。 */
    @Select("SELECT * FROM t_oads_intake_row WHERE book_id = #{bookId} AND del_flag = 0 ORDER BY row_no, id")
    List<TOadsIntakeRow> listByBook(@Param("bookId") Long bookId);
}
