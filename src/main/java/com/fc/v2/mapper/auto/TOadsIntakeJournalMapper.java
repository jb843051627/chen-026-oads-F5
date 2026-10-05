package com.fc.v2.mapper.auto;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fc.v2.model.auto.TOadsIntakeJournal;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 牌匾名册收讫裁决流水数据层（append-only：只有 insert，不改不抹）。
 *
 * @author fuce
 * @date 2026-10-05
 */
public interface TOadsIntakeJournalMapper extends BaseMapper<TOadsIntakeJournal> {

    /** 一册全部流水（账实差重数取这一份）。 */
    @Select("SELECT * FROM t_oads_intake_journal WHERE book_id = #{bookId} ORDER BY acted_at, id")
    List<TOadsIntakeJournal> listByBook(@Param("bookId") Long bookId);
}
