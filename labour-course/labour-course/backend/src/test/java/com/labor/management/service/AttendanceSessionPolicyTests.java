package com.labor.management.service;

import com.labor.management.entity.AttendanceSession;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class AttendanceSessionPolicyTests {

    private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");
    private final AttendanceSessionPolicy policy = new AttendanceSessionPolicy(
            10, Clock.fixed(Instant.parse("2026-09-29T04:00:00Z"), SHANGHAI));

    @Test
    void opensOnSessionDate() {
        AttendanceSession session = session(LocalDate.of(2026, 9, 29), 1);

        assertThat(policy.evaluate(session).editable()).isTrue();
    }

    @Test
    void blocksFutureWeek() {
        AttendanceSession session = session(LocalDate.of(2026, 9, 30), 1);

        assertThat(policy.evaluate(session).editable()).isFalse();
        assertThat(policy.evaluate(session).lockReason()).contains("未到");
    }

    @Test
    void sealsAtConfiguredDeadline() {
        AttendanceSession session = session(LocalDate.of(2026, 9, 19), 1);

        assertThat(policy.evaluate(session).editable()).isFalse();
        assertThat(policy.evaluate(session).sealDate()).isEqualTo(LocalDate.of(2026, 9, 29));
    }

    @Test
    void derivesWindowFromDateInsteadOfLegacyStatus() {
        AttendanceSession session = session(LocalDate.of(2026, 9, 29), 0);

        assertThat(policy.evaluate(session).editable()).isTrue();
        assertThat(policy.evaluate(session).lockReason()).isEqualTo("可编辑");
    }

    @Test
    void supportsConfigurableSealDays() {
        AttendanceSessionPolicy threeDayPolicy = new AttendanceSessionPolicy(
                3, Clock.fixed(Instant.parse("2026-09-29T04:00:00Z"), SHANGHAI));
        AttendanceSession session = session(LocalDate.of(2026, 9, 26), 1);

        assertThat(threeDayPolicy.evaluate(session).editable()).isFalse();
        assertThat(threeDayPolicy.evaluate(session).lockReason()).contains("3天");
    }

    private AttendanceSession session(LocalDate date, int status) {
        AttendanceSession session = new AttendanceSession();
        session.setSessionDate(date);
        session.setStatus(status);
        return session;
    }
}
