package org.example.onlinepossystem.driver;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

final class ApprovedShiftTime {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Africa/Johannesburg");

    private ApprovedShiftTime() {}

    static Selection select(OffsetDateTime recordedIn, OffsetDateTime recordedOut,
                            LocalTime chosenIn, LocalTime chosenOut) {
        if (recordedOut == null) throw new IllegalStateException("Clock out before cash-up.");
        if ((chosenIn == null) != (chosenOut == null))
            throw new IllegalArgumentException("Select both clock-in and clock-out times.");

        LocalDate date = recordedIn.atZoneSameInstant(BUSINESS_ZONE).toLocalDate();
        LocalTime in = chosenIn == null
                ? recordedIn.atZoneSameInstant(BUSINESS_ZONE).toLocalTime().truncatedTo(ChronoUnit.MINUTES)
                : minuteTime(chosenIn);
        LocalTime out = chosenOut == null
                ? recordedOut.atZoneSameInstant(BUSINESS_ZONE).toLocalTime().truncatedTo(ChronoUnit.MINUTES)
                : minuteTime(chosenOut);
        LocalDate outDate = out.isBefore(in) ? date.plusDays(1) : date;
        if (chosenIn == null && recordedOut.atZoneSameInstant(BUSINESS_ZONE).toLocalDate().isAfter(date)) {
            outDate = recordedOut.atZoneSameInstant(BUSINESS_ZONE).toLocalDate();
        }
        OffsetDateTime approvedIn = date.atTime(in).atZone(BUSINESS_ZONE).toOffsetDateTime();
        OffsetDateTime approvedOut = outDate.atTime(out).atZone(BUSINESS_ZONE).toOffsetDateTime();
        long minutes = Duration.between(approvedIn, approvedOut).toMinutes();
        if (minutes < 0 || minutes > 1440)
            throw new IllegalArgumentException("Selected shift must be between zero and 24 hours.");
        if (approvedOut.toInstant().isAfter(java.time.Instant.now()))
            throw new IllegalArgumentException("Clock-out time cannot be in the future.");
        return new Selection(in, out, approvedIn, approvedOut, (int) minutes);
    }

    private static LocalTime minuteTime(LocalTime time) {
        if (time.getSecond() != 0 || time.getNano() != 0)
            throw new IllegalArgumentException("Select times to the minute.");
        return time;
    }

    record Selection(LocalTime clockInTime, LocalTime clockOutTime,
                     OffsetDateTime approvedClockInAt, OffsetDateTime approvedClockOutAt, int minutes) {}
}
