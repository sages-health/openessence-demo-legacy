/*
 * Copyright (c) 2013 The Johns Hopkins University/Applied Physics Laboratory
 *                             All rights reserved.
 *
 * This material may be used, modified, or reproduced by or for the U.S.
 * Government pursuant to the rights granted under the clauses at
 * DFARS 252.227-7013/7014 or FAR 52.227-14.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * NO WARRANTY.   THIS MATERIAL IS PROVIDED "AS IS."  JHU/APL DISCLAIMS ALL
 * WARRANTIES IN THE MATERIAL, WHETHER EXPRESS OR IMPLIED, INCLUDING (BUT NOT
 * LIMITED TO) ANY AND ALL IMPLIED WARRANTIES OF PERFORMANCE,
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, AND NON-INFRINGEMENT OF
 * INTELLECTUAL PROPERTY RIGHTS. ANY USER OF THE MATERIAL ASSUMES THE ENTIRE
 * RISK AND LIABILITY FOR USING THE MATERIAL.  IN NO EVENT SHALL JHU/APL BE
 * LIABLE TO ANY USER OF THE MATERIAL FOR ANY ACTUAL, INDIRECT,
 * CONSEQUENTIAL, SPECIAL OR OTHER DAMAGES ARISING FROM THE USE OF, OR
 * INABILITY TO USE, THE MATERIAL, INCLUDING, BUT NOT LIMITED TO, ANY DAMAGES
 * FOR LOST PROFITS.
 */

package edu.jhuapl.openessence.datasource.jdbc.timeresolution.sql.pgsql;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

public class PgSqlDateHelper {

    private static final int ms1d = 86400000; // milliseconds in a day
    private static final int ms7d = 7 * ms1d; // milliseconds in a week


    /**
     * Calculates week number for given date This function is derived from JavaScript implementation used in OE
     *
     * @param startDay Week start day (0 sunday -> 6 saturday)
     * @param dt       Date
     * @return Week number
     */
    public static int getWeekOfYear(int startDay, Calendar dt) {
        // adapted from http://www.merlyn.demon.co.uk/weekcalc.htm
        // Used to compute offset based on epi week start day (0 sunday -> 6
        // saturday), defaults to 1
        int offset = (((Math.abs(startDay - 6)) + 5) % 7); // epi week start day
        // offset

        Calendar d3 = new GregorianCalendar(dt.get(Calendar.YEAR),
                                            dt.get(Calendar.MONTH), dt.get(Calendar.DATE), 0, 0, 0);

        d3.set(Calendar.MILLISECOND, 0);
        d3.add(Calendar.DAY_OF_YEAR, offset);

        double DC3 = d3.getTimeInMillis() / ms1d; // an Absolute Day Number
        double AWN = Math.floor(DC3 / 7); // an Absolute Week Number

        Calendar yCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        yCal.clear();
        yCal.setTimeInMillis((long) AWN * ms7d);//
        int Wyr = yCal.get(Calendar.YEAR);

        Calendar firstCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        firstCal.clear();
        firstCal = new GregorianCalendar(Wyr, 0, 7, 0, 0, 0);
        firstCal.set(Calendar.MILLISECOND, 0);
        int t = (int) (AWN
                       - (int) Math.floor(firstCal.getTimeInMillis() / ms7d) + 1);

        return t;
    }

    /**
     * Calculates year number for given date This function is derived from JavaScript implementation used in OE
     *
     * @param startDay Week start day (0 sunday -> 6 saturday)
     * @param dt       Date
     * @return Year number
     */
    public static int getYear(int startDay, Calendar dt) {
        // adapted from http://www.merlyn.demon.co.uk/weekcalc.htm
        // Used to compute offset based on epi week start day (0 sunday -> 6
        // saturday), defaults to 1
        int offset = (((Math.abs(startDay - 6)) + 5) % 7); // epi week start day offset

        Calendar d3 = new GregorianCalendar(dt.get(Calendar.YEAR),
                                            dt.get(Calendar.MONTH), dt.get(Calendar.DATE), 0, 0, 0);

        d3.set(Calendar.MILLISECOND, 0);
        d3.add(Calendar.DAY_OF_YEAR, offset);
        double DC3 = d3.getTimeInMillis() / ms1d; // an Absolute Day Number
        double AWN = Math.floor(DC3 / 7); // an Absolute Week Number
        Calendar yCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        yCal.clear();
        yCal.setTimeInMillis((long) AWN * ms7d);//
        return yCal.get(Calendar.YEAR);
    }
    
    /**
     * Calculate EPI week using CDC EPI week definition. The first epi week of
     * the year ends, by definition, on the first Saturday of January, as long
     * as it falls at least four days into the month. Each epi week begins on a
     * Sunday and ends on a Saturday.
     * 
     * @param date 
     * @return EPI week number
     */
    public static int getEpiWeek(Calendar date) {
        Calendar dt = Calendar.getInstance();
        dt.setTime(date.getTime());
        dt.setFirstDayOfWeek(Calendar.SUNDAY);
        dt.setMinimalDaysInFirstWeek(4);
        
        Calendar d4 = Calendar.getInstance();
        d4.setFirstDayOfWeek(Calendar.SUNDAY);
        d4.setMinimalDaysInFirstWeek(4);
        d4.set(dt.get(Calendar.YEAR), 0, 4, 0, 0, 0);
        d4.set(Calendar.MILLISECOND, 0);

        int result = dt.get(Calendar.WEEK_OF_YEAR) + 1 - d4.get(Calendar.WEEK_OF_YEAR);

        if (result == 0) {
            Calendar cal = Calendar.getInstance();
            cal.set(dt.get((Calendar.YEAR) - 1), 11, (24 + dt.get(Calendar.DATE)), 0, 0, 0);
            result = getEpiWeek(cal) + 1;
        }

        if (dt.get(Calendar.MONTH) == 11 && ((dt.get(Calendar.DATE) - (dt.get(Calendar.DAY_OF_WEEK) + 1)) >= 28)) {
            result = 1;
        }

        return result;
    }
    
    /**
     * Calculate EPI year using CDC EPI week definition. The first epi week of
     * the year ends, by definition, on the first Saturday of January, as long
     * as it falls at least four days into the month. Each epi week begins on a
     * Sunday and ends on a Saturday.
     * 
     * @param date 
     * @return EPI year number
     */
    public static int getEpiYear(Calendar date) {
        Calendar dt = Calendar.getInstance();
        dt.setTime(date.getTime());
        dt.setFirstDayOfWeek(Calendar.SUNDAY);

        int result = dt.get(Calendar.YEAR);
        int epiWeek = getEpiWeek(dt);
        if(epiWeek == 1 && dt.get(Calendar.MONTH) == 11){
            result = result +1;
        }
        if(epiWeek > 50 && dt.get(Calendar.MONTH) == 0){
            result = result -1;
        }
        return result;
    }
}
