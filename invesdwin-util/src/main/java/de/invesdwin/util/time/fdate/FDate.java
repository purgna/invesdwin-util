package de.invesdwin.util.time.fdate;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import javax.annotation.concurrent.ThreadSafe;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;
import org.joda.time.MutableDateTime;
import org.joda.time.ReadableDateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.google.common.primitives.Ints;

import de.invesdwin.norva.marker.IDate;
import de.invesdwin.util.lang.ADelegateComparator;
import de.invesdwin.util.time.Duration;
import de.invesdwin.util.time.TimeZones;

/**
 * FDate stands for an immutable Fast Date implementation by utilizing heavy caching.
 */
@ThreadSafe
public final class FDate implements IDate, Serializable, Cloneable, Comparable<Object> {

    /**
     * Somehow leveldb-jni does not like going higher than this year...
     */
    public static final int MAX_YEAR = 5555;
    public static final int MIN_YEAR = 1;

    public static final FDate MIN_DATE = FDateBuilder.newDate(MIN_YEAR);
    public static final FDate MAX_DATE = FDateBuilder.newDate(MAX_YEAR);

    public static final int COUNT_NANOSECONDS_IN_MILLISECOND = Ints.checkedCast(TimeUnit.MILLISECONDS.toNanos(1));
    public static final int COUNT_NANOSECONDS_IN_MICROSECOND = Ints.checkedCast(TimeUnit.MICROSECONDS.toNanos(1));
    public static final int COUNT_WEEKDAYS_IN_WEEK = Duration.DAYS_IN_WEEK;
    public static final int COUNT_WEEKDAYS_IN_MONTH = COUNT_WEEKDAYS_IN_WEEK * Duration.WEEKS_IN_MONTH;
    public static final int COUNT_WEEKDAYS_IN_YEAR = COUNT_WEEKDAYS_IN_MONTH * Duration.MONTHS_IN_YEAR;
    public static final int COUNT_WORKDAYS_IN_WEEK = 5;
    public static final int COUNT_WORKDAYS_IN_MONTH = COUNT_WORKDAYS_IN_WEEK * Duration.WEEKS_IN_MONTH;
    public static final int COUNT_WORKDAYS_IN_YEAR = COUNT_WORKDAYS_IN_MONTH * Duration.MONTHS_IN_YEAR;

    /*
     * ISO 8601 date-time format, example: "2003-04-01T13:01:02"
     */
    public static final String FORMAT_ISO_DATE = "yyyy-MM-dd";
    public static final String FORMAT_ISO_TIME = "HH:mm:ss";
    public static final String FORMAT_ISO_TIME_MS = FORMAT_ISO_TIME + ".SSS";
    public static final String FORMAT_ISO_DATE_TIME = FORMAT_ISO_DATE + "'T'" + FORMAT_ISO_TIME;
    public static final String FORMAT_ISO_DATE_TIME_SPACE = FORMAT_ISO_DATE + " " + FORMAT_ISO_TIME;
    public static final String FORMAT_ISO_DATE_TIME_MS = FORMAT_ISO_DATE + "'T'" + FORMAT_ISO_TIME_MS;
    public static final String FORMAT_TIMESTAMP_NUMBER = "yyyyMMddHHmmssSSS";
    public static final String FORMAT_TIMESTAMP_UNDERSCORE = "yyyy_MM_dd_HH_mm_ss_SSS";

    public static final ADelegateComparator<FDate> DATE_COMPARATOR = new ADelegateComparator<FDate>() {
        @Override
        protected Comparable<?> getCompareCriteria(final FDate e) {
            return e;
        }
    };

    //CHECKSTYLE:OFF
    private static final Calendar TEMPLATE_CALENDAR = Calendar.getInstance();
    //CHECKSTYLE:ON

    private final long millis;
    private final int hashCode;

    static {
        TEMPLATE_CALENDAR.clear();
        TEMPLATE_CALENDAR.setTimeZone(TimeZones.UTC);
    }

    public FDate() {
        this(System.currentTimeMillis());
    }

    public FDate(final long millis) {
        this.millis = millis;
        this.hashCode = Long.hashCode(millis);
    }

    public FDate(final ReadableDateTime jodaTime) {
        this(jodaTime.getMillis());
    }

    public FDate(final LocalDateTime jodaTime) {
        this(jodaTime.toDateTime().getMillis());
    }

    public FDate(final Calendar calendar) {
        this(calendar.getTime());
    }

    public FDate(final Date date) {
        this(date.getTime());
    }

    public int getYear() {
        return get(FDateField.Year);
    }

    public int getMonth() {
        //no conversion needed since joda time has same index
        return get(FDateField.Month);
    }

    public FMonth getFMonth() {
        return FMonth.valueOfIndex(getMonth());
    }

    public int getDay() {
        return get(FDateField.Day);
    }

    public int getWeekday() {
        //no conversion needed since joda time has same index
        return get(FDateField.Weekday);
    }

    public FWeekday getFWeekday() {
        return FWeekday.valueOfIndex(getWeekday());
    }

    public int getHour() {
        return get(FDateField.Hour);
    }

    public int getMinute() {
        return get(FDateField.Minute);
    }

    public int getSecond() {
        return get(FDateField.Second);
    }

    public int getMillisecond() {
        return get(FDateField.Millisecond);
    }

    public TimeZone getTimeZone() {
        return TimeZone.getDefault();
    }

    public FDate setYear(final int year) {
        return set(FDateField.Year, year);
    }

    public FDate setMonth(final int month) {
        return set(FDateField.Month, month);
    }

    public FDate setDay(final int day) {
        return set(FDateField.Day, day);
    }

    public FDate setWeekday(final int weekday) {
        return set(FDateField.Weekday, weekday);
    }

    public FDate setHour(final int hour) {
        return set(FDateField.Hour, hour);
    }

    public FDate setMinute(final int minute) {
        return set(FDateField.Minute, minute);
    }

    public FDate setSecond(final int second) {
        return set(FDateField.Second, second);
    }

    public FDate setMillisecond(final int millisecond) {
        return set(FDateField.Millisecond, millisecond);
    }

    public FDate addYears(final int years) {
        return add(FTimeUnit.Years, years);
    }

    public FDate addMonths(final int months) {
        return add(FTimeUnit.Months, months);
    }

    public FDate addDays(final int days) {
        return add(FTimeUnit.Days, days);
    }

    public FDate addWeeks(final int weeks) {
        return addDays(weeks * COUNT_WEEKDAYS_IN_WEEK);
    }

    public FDate addHours(final int hours) {
        return add(FTimeUnit.Hours, hours);
    }

    public FDate addMinutes(final int minutes) {
        return add(FTimeUnit.Minutes, minutes);
    }

    public FDate addSeconds(final int seconds) {
        return add(FTimeUnit.Seconds, seconds);
    }

    public FDate addMilliseconds(final long milliseconds) {
        return new FDate(millis + milliseconds);
    }

    public int get(final FDateField field) {
        final MutableDateTime delegate = new MutableDateTime(millis);
        return delegate.get(field.jodaTimeValue());
    }

    public FDate set(final FDateField field, final int value) {
        final MutableDateTime delegate = new MutableDateTime(millis);
        delegate.set(field.jodaTimeValue(), value);
        return new FDate(delegate);
    }

    public FDate add(final FTimeUnit field, final int amount) {
        final MutableDateTime delegate = new MutableDateTime(millis);
        delegate.add(field.jodaTimeValue(), amount);
        return new FDate(delegate);
    }

    public FDate add(final Duration duration) {
        return add(FTimeUnit.Milliseconds, duration.intValue(TimeUnit.MILLISECONDS));
    }

    public FDate truncate(final FDateField field) {
        final MutableDateTime delegate = new MutableDateTime(millis);
        delegate.setRounding(field.jodaTimeValue().getField(delegate.getChronology()));
        final FDate truncated = new FDate(delegate);
        return truncated;
    }

    /**
     * sets hour, minute, second and millisecond each to 0.
     */
    public FDate withoutTime() {
        return truncate(FDateField.Day);
    }

    /**
     * Returns the number of milliseconds since January 1, 1970, 00:00:00 GMT.
     */
    public long millisValue() {
        return millis;
    }

    public Date dateValue() {
        //CHECKSTYLE:OFF
        return new Date(millis);
        //CHECKSTYLE:ON
    }

    public long longValue(final TimeUnit timeUnit) {
        return timeUnit.convert(millis, TimeUnit.MILLISECONDS);
    }

    public Calendar calendarValue() {
        final Calendar cal = (Calendar) TEMPLATE_CALENDAR.clone();
        cal.setTimeInMillis(millis);
        return cal;
    }

    public LocalDateTime jodaTimeValue() {
        return new LocalDateTime(millis);
    }

    public static FDate valueOf(final Long millis) {
        if (millis != null) {
            return new FDate(millis);
        } else {
            return null;
        }
    }

    public static FDate valueOf(final Date date) {
        if (date != null) {
            return new FDate(date);
        } else {
            return null;
        }
    }

    public static FDate valueOf(final Calendar calendar) {
        if (calendar != null) {
            return new FDate(calendar);
        } else {
            return null;
        }
    }

    public static FDate valueOf(final ReadableDateTime jodaTime) {
        if (jodaTime != null) {
            return new FDate(jodaTime);
        } else {
            return null;
        }
    }

    public static FDate valueOf(final LocalDateTime jodaTime) {
        if (jodaTime != null) {
            return new FDate(jodaTime);
        } else {
            return null;
        }
    }

    public static FDate valueOf(final String str, final String... parsePatterns) {
        return valueOf(str, null, null, parsePatterns);
    }

    public static FDate valueOf(final String str, final String parsePattern) {
        return valueOf(str, null, null, parsePattern);
    }

    public static FDate valueOf(final String str, final Locale locale, final String... parsePatterns) {
        return valueOf(str, null, locale, parsePatterns);
    }

    public static FDate valueOf(final String str, final TimeZone timeZone, final String... parsePatterns) {
        return valueOf(str, timeZone, null, parsePatterns);
    }

    public static FDate valueOf(final String str, final TimeZone timeZone, final Locale locale,
            final String... parsePatterns) {
        if (parsePatterns == null || parsePatterns.length == 0) {
            throw new IllegalArgumentException("atleast one parsePattern is needed");
        }
        for (final String parsePattern : parsePatterns) {
            try {
                return valueOf(str, timeZone, locale, parsePattern);
            } catch (final IllegalArgumentException e) {
                continue;
            }
        }
        throw new IllegalArgumentException("None of the parsePatterns [" + Arrays.toString(parsePatterns)
                + "] matches the date string [" + str + "]");
    }

    public static FDate valueOf(final String str, final TimeZone timeZone, final String parsePattern) {
        return valueOf(str, timeZone, null, parsePattern);
    }

    public static FDate valueOf(final String str, final Locale locale, final String parsePattern) {
        return valueOf(str, null, locale, parsePattern);
    }

    public static FDate valueOf(final String str, final TimeZone timeZone, final Locale locale,
            final String parsePattern) {
        if (str == null) {
            return null;
        }
        DateTimeFormatter df = DateTimeFormat.forPattern(parsePattern);
        if (timeZone != null) {
            df = df.withZone(DateTimeZone.forTimeZone(timeZone));
        }
        if (locale != null) {
            df = df.withLocale(locale);
        }
        final DateTime date = df.parseDateTime(str);
        return new FDate(date);
    }

    public static FDate now() {
        return new FDate();
    }

    public static FDate today() {
        return now().withoutTime();
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public int compareTo(final Object o) {
        if (o instanceof FDate) {
            final FDate cO = (FDate) o;
            return Long.compare(millis, cO.millis);
        } else {
            return 1;
        }
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof FDate) {
            final FDate cObj = (FDate) obj;
            return millis == cObj.millis;
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return toString(FORMAT_ISO_DATE_TIME_MS);
    }

    public String toString(final TimeZone timeZone) {
        return toString(FORMAT_ISO_DATE_TIME_MS, timeZone);
    }

    public String toString(final String format) {
        return toString(format, null);
    }

    public String toString(final String format, final TimeZone timeZone) {
        final MutableDateTime delegate = new MutableDateTime(millis);
        DateTimeFormatter df = DateTimeFormat.forPattern(format);
        if (timeZone != null) {
            df = df.withZone(DateTimeZone.forTimeZone(timeZone));
        } else {
            df = df.withZoneUTC();
        }
        return df.print(delegate);
    }

    public boolean isBefore(final FDate other) {
        return other != null && compareTo(other) < 0;
    }

    public boolean isBeforeOrEqual(final FDate other) {
        return other != null && !isAfter(other);
    }

    public boolean isAfter(final FDate other) {
        return other != null && compareTo(other) > 0;
    }

    public boolean isAfterOrEqual(final FDate other) {
        return other != null && !isBefore(other);
    }

    public static Iterable<FDate> iterable(final FDate start, final FDate end, final FTimeUnit timeUnit,
            final int incrementAmount) {
        return new FDateIterable(start, end, timeUnit, incrementAmount);
    }

    static class FDateIterable implements Iterable<FDate> {
        private final FDate startFinal;
        private final FDate endFinal;
        private final FTimeUnit timeUnit;
        private final int incrementAmount;

        FDateIterable(final FDate startFinal, final FDate endFinal, final FTimeUnit timeUnit, final int incrementAmount) {
            this.startFinal = startFinal;
            this.endFinal = endFinal;
            this.timeUnit = timeUnit;
            this.incrementAmount = incrementAmount;
        }

        @Override
        public Iterator<FDate> iterator() {
            return new Iterator<FDate>() {

                private boolean first = true;
                private FDate spot = startFinal;

                @Override
                public boolean hasNext() {
                    return first || spot.isBefore(endFinal);
                }

                @Override
                public FDate next() {
                    if (first) {
                        first = false;
                        return spot;
                    } else {
                        if (spot.isAfter(endFinal)) {
                            throw new NoSuchElementException();
                        }
                        spot = spot.add(timeUnit, incrementAmount);
                        if (spot.isAfter(endFinal)) {
                            return endFinal;
                        } else {
                            return spot;
                        }
                    }
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }
    }

    public static Object toString(final FDate date) {
        if (date == null) {
            return null;
        } else {
            return date.toString();
        }
    }

    public static String toString(final FDate date, final TimeZone timeZone) {
        if (date == null) {
            return null;
        }
        return date.toString(timeZone);
    }

    public static String toString(final FDate date, final String format) {
        if (date == null) {
            return null;
        }
        return date.toString(format);
    }

    public static String toString(final FDate date, final String format, final TimeZone timeZone) {
        if (date == null) {
            return null;
        }
        return date.toString(format, timeZone);
    }

    public static FDate min(final FDate... dates) {
        FDate minDate = null;
        for (final FDate date : dates) {
            minDate = min(minDate, date);
        }
        return minDate;
    }

    public static FDate min(final Iterable<FDate> dates) {
        FDate minDate = null;
        for (final FDate date : dates) {
            minDate = min(minDate, date);
        }
        return minDate;
    }

    public static FDate min(final FDate date1, final FDate date2) {
        if (date1 == null) {
            return date2;
        } else if (date2 == null) {
            return date1;
        }

        if (date1.isBefore(date2)) {
            return date1;
        } else {
            return date2;
        }
    }

    public static FDate max(final Iterable<FDate> dates) {
        FDate maxDate = null;
        for (final FDate date : dates) {
            maxDate = max(maxDate, date);
        }
        return maxDate;
    }

    public static FDate max(final FDate... dates) {
        FDate maxDate = null;
        for (final FDate date : dates) {
            maxDate = max(maxDate, date);
        }
        return maxDate;
    }

    public static FDate max(final FDate date1, final FDate date2) {
        if (date1 == null) {
            return date2;
        } else if (date2 == null) {
            return date1;
        }

        if (date1.isAfter(date2)) {
            return date1;
        } else {
            return date2;
        }
    }

    public static FDate between(final FDate value, final FDate min, final FDate max) {
        return max(min(value, max), min);
    }

    public static boolean isSameYear(final FDate date1, final FDate date2) {
        return isSameTruncated(date1, date2, FDateField.Year);
    }

    public static boolean isSameMonth(final FDate date1, final FDate date2) {
        return isSameTruncated(date1, date2, FDateField.Month);
    }

    public static boolean isSameDay(final FDate date1, final FDate date2) {
        return isSameTruncated(date1, date2, FDateField.Day);
    }

    public static boolean isSameHour(final FDate date1, final FDate date2) {
        return isSameTruncated(date1, date2, FDateField.Hour);
    }

    public static boolean isSameMinute(final FDate date1, final FDate date2) {
        return isSameTruncated(date1, date2, FDateField.Minute);
    }

    public static boolean isSameSecond(final FDate date1, final FDate date2) {
        return isSameTruncated(date1, date2, FDateField.Second);
    }

    public static boolean isSameMillisecond(final FDate date1, final FDate date2) {
        if (date1 == null || date2 == null) {
            return false;
        } else {
            return date1.millisValue() == date2.millisValue();
        }
    }

    private static boolean isSameTruncated(final FDate date1, final FDate date2, final FDateField field) {
        if (date1 == null || date2 == null) {
            return false;
        }
        return date1.millisValue() == date2.millisValue()
                || date1.truncate(field).millisValue() == date2.truncate(field).millisValue();
    }

    /**
     * Fast but unprecise variation of isSameDay(). Does not count in daylight saving time.
     */
    public static boolean isSameJulianDay(final FDate date1, final FDate date2) {
        if (date1 == null || date2 == null) {
            return false;
        }
        // Strip out the time part of each date.
        final long julianDayNumber1 = date1.millisValue() / org.apache.commons.lang3.time.DateUtils.MILLIS_PER_DAY;
        final long julianDayNumber2 = date2.millisValue() / org.apache.commons.lang3.time.DateUtils.MILLIS_PER_DAY;

        // If they now are equal then it is the same day.
        return julianDayNumber1 == julianDayNumber2;
    }

    public static Date toDate(final FDate date) {
        if (date != null) {
            return date.dateValue();
        } else {
            return null;
        }
    }

    public static List<FDate> valueOf(final Collection<Date> list) {
        final List<FDate> dates = new ArrayList<FDate>(list.size());
        for (final Date e : list) {
            dates.add(valueOf(e));
        }
        return dates;
    }

    public static List<FDate> valueOf(final Date... list) {
        final List<FDate> dates = new ArrayList<FDate>(list.length);
        for (final Date e : list) {
            dates.add(valueOf(e));
        }
        return dates;
    }

    //CHECKSTYLE:OFF
    @Override
    public Object clone() {
        return new FDate(millis);
    }
    //CHECKSTYLE:ON

}
