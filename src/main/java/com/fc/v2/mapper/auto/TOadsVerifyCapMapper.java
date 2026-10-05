package com.fc.v2.mapper.auto;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fc.v2.model.auto.TOadsVerifyCap;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 当地核验上限数据层（退回理由第一条的唯一取值处）。
 *
 * @author fuce
 * @date 2026-10-05
 */
public interface TOadsVerifyCapMapper extends BaseMapper<TOadsVerifyCap> {

    /** 取某辖段在用的核验上限；该辖段没配上限返回 null（不判超限，回执注明无此段上限）。 */
    @Select("SELECT * FROM t_oads_verify_cap WHERE area_no = #{areaNo} AND status = 0 AND del_flag = 0 LIMIT 1")
    TOadsVerifyCap findActiveByArea(@Param("areaNo") String areaNo);
}
