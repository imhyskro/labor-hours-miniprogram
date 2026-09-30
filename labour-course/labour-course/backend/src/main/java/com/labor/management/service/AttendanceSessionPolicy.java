package com.labor.management.service;

import com.labor.management.entity.AttendanceSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

/** 根据课次日期和配置的有效期判断小程序是否允许打分。 */
@Component
public class AttendanceSessionPolicy {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");

    private final int sealDays;
    private final Clock clock;

    @Autowired
    public AttendanceSessionPolicy(@Value("${labor.seal-days:10}") int sealDays) {
        this(sealDays, Clock.system(BUSINESS_ZONE));
    }

    AttendanceSessionPolicy(int sealDays, Clock clock) {
        this.sealDays = Math.max(1, sealDays);
        this.clock = clock;
    }

    public SessionWindow evaluate(AttendanceSession session) {
        LocalDate sessionDate = session.getSessionDate();
        if (sessionDate == null) {
            return new SessionWindow(false, "课次日期未设置", null, sealDays);
        }

        LocalDate today = LocalDate.now(clock);
        LocalDate sealDate = sessionDate.plusDays(sealDays);
        if (today.isBefore(sessionDate)) {
            return new SessionWindow(false, "未到本周打分时间", sealDate, sealDays);
        }
        if (!today.isBefore(sealDate)) {
            return new SessionWindow(false, "已超过" + sealDays + "天打分期限", sealDate, sealDays);
        }
        return new SessionWindow(true, "可编辑", sealDate, sealDays);
    }

    public record SessionWindow(boolean editable, String lockReason,
                                LocalDate sealDate, int sealDays) {
    }
}
