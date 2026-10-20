package org.example.onlinepossystem.driver;

import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ApprovedShiftTimeTest {
    private static final OffsetDateTime RECORDED_IN = OffsetDateTime.parse("2025-10-18T09:03:45+02:00");
    private static final OffsetDateTime RECORDED_OUT = OffsetDateTime.parse("2025-10-18T17:02:10+02:00");

    @Test
    void calculatesSameDayAndOvernightMinutesFromSelectedTimes() {
        var day = ApprovedShiftTime.select(RECORDED_IN, RECORDED_OUT,
                LocalTime.of(9, 0), LocalTime.of(17, 30));
        assertThat(day.minutes()).isEqualTo(510);
        assertThat(day.approvedClockInAt()).isEqualTo(OffsetDateTime.parse("2025-10-18T09:00+02:00"));
        assertThat(day.approvedClockOutAt()).isEqualTo(OffsetDateTime.parse("2025-10-18T17:30+02:00"));

        var night = ApprovedShiftTime.select(RECORDED_IN, RECORDED_OUT,
                LocalTime.of(23, 0), LocalTime.of(1, 0));
        assertThat(night.minutes()).isEqualTo(120);
        assertThat(night.approvedClockOutAt().toLocalDate()).isEqualTo(RECORDED_IN.toLocalDate().plusDays(1));
    }

    @Test
    void equalTimesMeanZeroAndDefaultsUseRecordedClockTimes() {
        assertThat(ApprovedShiftTime.select(RECORDED_IN, RECORDED_OUT,
                LocalTime.MIDNIGHT, LocalTime.MIDNIGHT).minutes()).isZero();
        var defaultTimes = ApprovedShiftTime.select(RECORDED_IN, RECORDED_OUT, null, null);
        assertThat(defaultTimes.clockInTime()).isEqualTo(LocalTime.of(9, 3));
        assertThat(defaultTimes.clockOutTime()).isEqualTo(LocalTime.of(17, 2));
        assertThat(defaultTimes.minutes()).isEqualTo(479);
    }

    @Test
    void rejectsIncompleteFutureAndExcessivelyLongSelections() {
        assertThatThrownBy(() -> ApprovedShiftTime.select(RECORDED_IN, RECORDED_OUT,
                LocalTime.NOON, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ApprovedShiftTime.select(RECORDED_IN, RECORDED_OUT,
                LocalTime.of(9, 0, 1), LocalTime.NOON)).isInstanceOf(IllegalArgumentException.class);
        var future = OffsetDateTime.now().plusDays(1);
        assertThatThrownBy(() -> ApprovedShiftTime.select(future, future.plusMinutes(10), null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ApprovedShiftTime.select(RECORDED_IN, RECORDED_IN.plusDays(2), null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
