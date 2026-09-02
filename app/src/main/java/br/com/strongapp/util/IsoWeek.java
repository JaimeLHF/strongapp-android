package br.com.strongapp.util;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;

/**
 * Semana ISO-8601 (segunda a domingo, primeira semana com 4 dias ou mais).
 * Usa Calendar em vez de java.time porque o minSdk do projeto é 24.
 */
public final class IsoWeek {

    private final int year;
    private final int week;

    private IsoWeek(int year, int week) {
        this.year = year;
        this.week = week;
    }

    public static IsoWeek current() {
        Calendar calendar = new GregorianCalendar(Locale.getDefault());
        calendar.setFirstDayOfWeek(Calendar.MONDAY);
        calendar.setMinimalDaysInFirstWeek(4);

        int week = calendar.get(Calendar.WEEK_OF_YEAR);
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);

        // Virada de ano: dias de janeiro podem pertencer à última semana do ano anterior
        // e dias de dezembro à primeira semana do ano seguinte.
        if (month == Calendar.JANUARY && week > 50) {
            year--;
        } else if (month == Calendar.DECEMBER && week == 1) {
            year++;
        }
        return new IsoWeek(year, week);
    }

    public int year() {
        return year;
    }

    public int week() {
        return week;
    }
}
