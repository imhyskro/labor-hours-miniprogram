package com.labor.management.service.impl;

import com.labor.management.service.AttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AttendanceServiceImpl implements AttendanceService {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public Map<Long, BigDecimal> sumByStudentIds(List<Long> studentIds) {
        Map<Long, BigDecimal> result = new HashMap<>();
        if (studentIds == null || studentIds.isEmpty()) return result;
        String placeholders = String.join(",", java.util.Collections.nCopies(studentIds.size(), "?"));
        List<Object> args = studentIds.stream().map(id -> (Object) id).toList();
        jdbcTemplate.query("""
                SELECT e.student_id, COALESCE(SUM(sr.score_value), 0) total_score
                FROM student_course_enrollment e
                LEFT JOIN score_record sr ON sr.enrollment_id = e.id
                WHERE e.student_id IN (%s)
                GROUP BY e.student_id
                """.formatted(placeholders), rs -> {
            result.put(rs.getLong("student_id"), rs.getBigDecimal("total_score"));
        }, args.toArray());
        return result;
    }

    @Override
    public int getSessionCount() {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM course_session cs
                JOIN academic_term t ON t.id = cs.term_id
                WHERE t.status = 'ACTIVE' AND cs.status <> 'ARCHIVED'
                """, Integer.class);
        return count == null ? 0 : count;
    }
}
