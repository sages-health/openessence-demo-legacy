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
package edu.jhuapl.openessence.web.util;

import edu.jhuapl.bsp.detector.DetectorHelper;
import edu.jhuapl.bsp.detector.TemporalDetectorInterface;
import edu.jhuapl.bsp.detector.TemporalDetectorSimpleDataObject;
import edu.jhuapl.graphs.controller.GraphController;
import edu.jhuapl.graphs.controller.GraphDataInterface;
import edu.jhuapl.graphs.controller.GraphObject;
import edu.jhuapl.openessence.controller.ReportController;
import edu.jhuapl.openessence.datasource.Dimension;
import edu.jhuapl.openessence.datasource.FieldType;
import edu.jhuapl.openessence.datasource.Filter;
import edu.jhuapl.openessence.datasource.dataseries.AccumPoint;
import edu.jhuapl.openessence.datasource.dataseries.DataSeriesSource;
import edu.jhuapl.openessence.datasource.dataseries.GroupingDimension;
import edu.jhuapl.openessence.datasource.jdbc.JdbcOeDataSource;
import edu.jhuapl.openessence.datasource.jdbc.filter.OneArgOpFilter;
import edu.jhuapl.openessence.datasource.jdbc.timeresolution.sql.pgsql.PgSqlDateHelper;
import edu.jhuapl.openessence.i18n.InspectableResourceBundleMessageSource;
import edu.jhuapl.openessence.model.TimeSeriesModel;

import org.apache.commons.codec.EncoderException;
import org.apache.commons.codec.net.URLCodec;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.math3.util.Pair;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonParser.Feature;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.NoSuchMessageException;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.WebRequest;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

/**
 * This class provides helper methods for creating time series (used by ReportController)
 */
@Service
public class TSHelper {

    private static final Logger log = LoggerFactory.getLogger(TSHelper.class);
    private static final String TIMEZONE_ENABLED = "timezone.enabled";

    /**
     * Extract timezone offset from the request and return it as hours minutes string
     * 
     * @param request WebRequest object
     * @param messageSource InspectableResourceBundleMessageSource object
     * @return timezone offset as as hours minutes string
     */
    public String getClientTimezone(WebRequest request, InspectableResourceBundleMessageSource messageSource) {
        String clientTimezone = null;
        String timezoneEnabledString = messageSource.getMessage(TIMEZONE_ENABLED, "false");
        if (timezoneEnabledString.equalsIgnoreCase("true")) {
            clientTimezone = ControllerUtils.getRequestTimezoneAsHourMinuteString(request);
        }
        return clientTimezone;
    }

    /**
     * Builds graph time series URL
     * 
     * @param ds datasource object
     * @param contextPath context path String
     * @param servletPath servlet path String
     * @param messageSource InspectableResourceBundleMessageSource object
     * @return graph time series url as a String
     */
    public String buildTimeSeriesURL(JdbcOeDataSource ds, String contextPath, String servletPath,
            InspectableResourceBundleMessageSource messageSource) {
        String graphTimeSeriesUrl = contextPath + servletPath + "/report/graphTimeSeries";
        graphTimeSeriesUrl = appendGraphFontParam(ds, graphTimeSeriesUrl, messageSource);
        return graphTimeSeriesUrl;
    }

    /**
     * Appends given parameter and value to URL
     * 
     * @param url initial URL
     * @param param parameter to be appended
     * @param value value of the parameter
     * @return new url as a String
     */
    public String appendUrlParameter(String url, String param, String value) {
        StringBuilder sb = new StringBuilder(url);
        URLCodec codec = new URLCodec();

        try {
            sb.append(url.contains("?") ? '&' : '?');
            sb.append(codec.encode(param));
            sb.append('=').append(codec.encode(value));
        } catch (EncoderException e) {
            log.error("Exception encoding URL value " + value, e);
        }

        return sb.toString();
    }

    /**
     * If the graph.font property is specified, append its value as a parameter to the given URL. Otherwise, do nothing.
     * 
     * @param dataSource datasource object
     * @param url current url to which we want to append graph font
     * @param messageSource InspectableResourceBundleMessageSource object
     * @return new URL as a String
     */
    public String appendGraphFontParam(JdbcOeDataSource dataSource, String url,
            InspectableResourceBundleMessageSource messageSource) {
        try {
            String graphFont = messageSource.getDataSourceMessage("graph.font", dataSource);
            return appendUrlParameter(url, "font", graphFont);
        } catch (NoSuchMessageException e) {
            log.debug("Property graph.font not found, using default");
            return url;
        }
    }

    /**
     * Calculate start date for yearly series
     * 
     * @param year year as an int
     * @param resolution time resolution string
     * @param isEpiWeekEnabled if we are using EPI week calculation
     * @return year start date as a Date object
     */
    public Date calculateStartDate(int year, String resolution, boolean isEpiWeekEnabled) {
        Calendar cal = Calendar.getInstance();
        cal.set(year, Calendar.JANUARY, 1, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);

        if (resolution.equals(ReportController.WEEKLY) && isEpiWeekEnabled) {
            // if 1st Jan is last week of previous year
            if (PgSqlDateHelper.getEpiWeek(cal) > 1) {
                do {
                    cal.add(Calendar.DATE, 1);
                } while (PgSqlDateHelper.getEpiWeek(cal) > 1);
                return cal.getTime();
            }
            if (PgSqlDateHelper.getEpiWeek(cal) == 1) {
                // if 1st Jan is last week of previous year
                Calendar res;
                do {
                    res = (Calendar) cal.clone();
                    cal.add(Calendar.DATE, -1);
                } while (PgSqlDateHelper.getEpiWeek(cal) == 1);
                return res.getTime();
            }
        }
        return cal.getTime();
    }

    /**
     * Calculate end date for yearly series
     * 
     * @param year year as an int
     * @param resolution time resolution string
     * @param isEpiWeekEnabled if we are using EPI week calculation
     * @return year end date as a Date object
     */
    public Date calculateEndDate(int year, String resolution, boolean isEpiWeekEnabled) {
        Calendar cal = Calendar.getInstance();
        cal.set(year, Calendar.DECEMBER, 31, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);

        if (resolution.equals(ReportController.WEEKLY) && isEpiWeekEnabled) {
            // if Dec 31st is first week of next year
            if (PgSqlDateHelper.getEpiWeek(cal) == 1) {
                do {
                    cal.add(Calendar.DATE, -1);
                } while (PgSqlDateHelper.getEpiWeek(cal) == 1);
                return cal.getTime();
            }
            if (PgSqlDateHelper.getEpiWeek(cal) > 1) {
                Calendar res;
                do {
                    res = (Calendar) cal.clone();
                    cal.add(Calendar.DATE, 1);
                } while (PgSqlDateHelper.getEpiWeek(cal) > 1);
                return res.getTime();
            }
        }
        return cal.getTime();
    }

    /**
     * When we plot each year as a series, we do not ask user for start and end dates, instead, we calculate dates based
     * on selected years/accumulations.
     * 
     * @param params url params as a map
     * @param accumulation accumulation Dimension that hold year number
     * @param groupId group ID as a String
     * @param resolution time resolution as a String
     * @param isEpiWeekEnabled if EPI week calculation being used
     * @return updated parameter map
     */
    public Map<String, String[]> fixStartEndDatesForYearAsSeries(Map<String, String[]> params, Dimension accumulation,
            String groupId, String resolution, boolean isEpiWeekEnabled) {
        Map<String, String[]> updatedParams = new HashMap<String, String[]>(params);

        try {
            int year = Integer.parseInt(accumulation.getId());
            Date startDate = calculateStartDate(year, resolution, isEpiWeekEnabled);
            Date endDate = calculateEndDate(year, resolution, isEpiWeekEnabled);
            log.info("StartDate: " + startDate);
            log.info("EndDate: " + endDate);
            // replace/add start and end dates
            updatedParams.put(groupId + "_start", new String[] {Long.toString(startDate.getTime())});
            updatedParams.put(groupId + "_end", new String[] {Long.toString(endDate.getTime())});
        } catch (NumberFormatException e) {
            log.error("Exception parsing accumulation/year " + accumulation.getId() + " as an integer ", e);
        }
        return updatedParams;
    }

    /**
     * Returns start and end date as a Pair from the given filter params
     * 
     * @param grpdim GroupingDimension object that holds date field id
     * @param filters query filters as a list
     * @return start and end dates as a Pair
     */
    public Pair<Date, Date> getStartEndDates(GroupingDimension grpdim, List<Filter> filters) {
        Date startDate = null;
        Date endDate = null;

        if (grpdim != null && (grpdim.getSqlType() == FieldType.DATE || grpdim.getSqlType() == FieldType.DATE_TIME)) {
            for (Filter f : filters) {
                if (f instanceof OneArgOpFilter) {
                    OneArgOpFilter of = (OneArgOpFilter) f;
                    if (of.getFilterId().equalsIgnoreCase(grpdim.getId()) && (of.getSqlSnippet("").contains(">="))) {
                        startDate = (Date) of.getArguments().get(0);
                    } else if (of.getFilterId().equalsIgnoreCase(grpdim.getId())
                            && (of.getSqlSnippet("").contains("<="))) {
                        endDate = (Date) of.getArguments().get(0);
                    }
                }
            }
        }
        return new Pair<Date, Date>(startDate, endDate);
    }

    /**
     * Calculates timezone offset as milliseconds
     * 
     * @param messageSource InspectableResourceBundleMessageSource object
     * @param clientTimezone TimeZone object
     * @return int time zone offset as milliseconds
     */
    public int getTimezoneOffsetMillies(InspectableResourceBundleMessageSource messageSource, TimeZone clientTimezone) {
        int timeOffsetMillies = 0;
        String timezoneEnabledString = messageSource.getMessage(TIMEZONE_ENABLED, "false");
        if (timezoneEnabledString.equalsIgnoreCase("true")) {
            timeOffsetMillies =
                    (clientTimezone.getRawOffset() - clientTimezone.getDSTSavings())
                            - (TimeZone.getDefault().getRawOffset() - TimeZone.getDefault().getDSTSavings());
        }
        return timeOffsetMillies;
    }

    /**
     * Add prepull days to given start date.
     * 
     * @param cal Calendar object
     * @param timeResolution time resolution as a String
     * @param prepull number of days to prepull for daily series and number of weeks to prepull for weekly series
     * @return start date as a Date object
     */
    public Date getQueryStartDate(Calendar cal, String timeResolution, int prepull) {
        Calendar startCal = new GregorianCalendar();
        startCal.setTime(cal.getTime());
        // offset start date to match prepull offset
        if (timeResolution.equals(ReportController.WEEKLY)) {
            startCal.add(Calendar.DATE, (7 * prepull));
        } else if (timeResolution.equals(ReportController.DAILY)) {
            startCal.add(Calendar.DATE, prepull);
        }
        return startCal.getTime();
    }

    /**
     * Builds details javascript URL string for a given date
     * 
     * @param dateFieldName date field name as a String
     * @param timeResolution time resolution as a String
     * @param clientTimezone client time zone
     * @param dt data point date
     * @param startDay week start day for the system
     * @param jsCall Initial javascript StringBuilder object to which we will append date params
     * @return Updated javascript call as String
     */
    public String buildDetailsURL(String dateFieldName, String timeResolution, TimeZone clientTimezone, Date dt,
            int startDay, StringBuilder jsCall) {
        // build the click through url
        StringBuilder tmp = new StringBuilder(jsCall.toString());

        // add the date field with start and end dates from the data point
        if (!ReportController.DAILY.equalsIgnoreCase(timeResolution)) {
            Calendar timeSet = Calendar.getInstance(clientTimezone);
            timeSet.setTime(dt);

            if (ReportController.WEEKLY.equalsIgnoreCase(timeResolution)) {
                timeSet.set(Calendar.DAY_OF_WEEK, startDay + 1);
                tmp.append(",").append(dateFieldName).append("_start:'").append(timeSet.getTimeInMillis()).append("'");
                timeSet.add(Calendar.DAY_OF_YEAR, 6);
                tmp.append(",").append(dateFieldName).append("_end:'").append(timeSet.getTimeInMillis()).append("'");
            } else if (ReportController.MONTHLY.equalsIgnoreCase(timeResolution)) {
                // Compute last day of month
                timeSet.set(Calendar.DAY_OF_MONTH, 1);
                timeSet.add(Calendar.MONTH, 1);
                timeSet.add(Calendar.DAY_OF_YEAR, -1);
                tmp.append(",").append(dateFieldName).append("_end:'").append(timeSet.getTimeInMillis()).append("'");
                // set first day of month
                timeSet.set(Calendar.DAY_OF_MONTH, 1);
                tmp.append(",").append(dateFieldName).append("_start:'").append(timeSet.getTimeInMillis()).append("'");
            } else if (ReportController.YEARLY.equalsIgnoreCase(timeResolution)) {
                // Compute last day of month
                timeSet.set(Calendar.DATE, 31);
                timeSet.add(Calendar.MONTH, Calendar.DECEMBER);
                tmp.append(",").append(dateFieldName).append("_end:'").append(timeSet.getTimeInMillis()).append("'");
                timeSet.set(Calendar.DATE, 1);
                timeSet.add(Calendar.MONTH, Calendar.JANUARY);
                tmp.append(",").append(dateFieldName).append("_start:'").append(timeSet.getTimeInMillis()).append("'");
            }
        } else {
            // compute enddate for individual data points based on the selected
            // resolution
            // detailsPointEndDate = computeEndDate(tdates[i],timeResolution);
            // add the date field with start and end dates from the data point
            tmp.append(",").append(dateFieldName).append("_start:'").append(dt.getTime()).append("'");
            tmp.append(",").append(dateFieldName).append("_end:'").append(dt.getTime()).append("'");
        }
        tmp.append("});");
        return tmp.toString();
    }

    /**
     * Adds data details table information to the result map
     * 
     * @param result Map that holds results
     * @param gdBuilder GraphDataBuilder object
     */
    public void addDetailsToResult(Map<String, Object> result, GraphDataBuilder gdBuilder) {
        int totalPoints = 0;
        List<HashMap<String, Object>> details = new ArrayList<HashMap<String, Object>>();
        HashMap<String, Object> detail;
        for (int i = 0; i < gdBuilder.getAllCounts().length; i++) {
            if (gdBuilder.getAllCounts()[i] != null) {
                for (int j = 0; j < gdBuilder.getAllCounts()[i].length; j++) {
                    totalPoints++;
                    detail = new HashMap<String, Object>();
                    detail.put("Date", gdBuilder.getxAxisLables()[j]);
                    detail.put("Series", gdBuilder.getLineSetLabels()[i]);
                    detail.put("Level", gdBuilder.getAllLevels()[i][j]);
                    detail.put("Count", gdBuilder.getAllCounts()[i][j]);
                    if (!ArrayUtils.isEmpty(gdBuilder.getAllExpecteds()[i])) {
                        detail.put("Expected", gdBuilder.getAllExpecteds()[i][j]);
                    }
                    if (!ArrayUtils.isEmpty(gdBuilder.getAllSwitchInfo()[i])) {
                        detail.put("Switch", gdBuilder.getAllSwitchInfo()[i][j]);
                    }
                    detail.put("Color", gdBuilder.getAllColors()[i][j]);
                    details.add(detail);
                }
            }
        }
        result.put("detailsTotalRows", totalPoints);
        result.put("details", details);
    }

    public final void addGraphConfigToResult(Map<String, Object> result, GraphController gc,
            GraphDataInterface graphData, String graphTimeSeriesUrl, double[][] allCounts, boolean graphExpected)
            throws JsonParseException, IOException {

        int maxLabels = graphData.getGraphWidth() / 30;
        graphData.setMaxLabeledCategoryTicks(Math.min(maxLabels, allCounts[0].length));

        StringBuffer sb = new StringBuffer();
        GraphObject graph =
                gc.writeTimeSeriesGraph(sb, graphData, true, true, false, graphTimeSeriesUrl, graphExpected);

        result.put("html", sb.toString());

        // added to build method calls from javascript
        Map<String, Object> graphConfig = new HashMap<String, Object>();
        graphConfig.put("address", graphTimeSeriesUrl);
        graphConfig.put("graphDataId", graph.getGraphDataId());
        graphConfig.put("imageMapName", graph.getImageMapName());

        graphConfig.put("graphTitle", graphData.getGraphTitle());
        graphConfig.put("xAxisLabel", graphData.getXAxisLabel());
        graphConfig.put("yAxisLabel", graphData.getYAxisLabel());
        graphConfig.put("xLabels", graphData.getXLabels());
        graphConfig.put("graphWidth", graphData.getGraphWidth());
        graphConfig.put("graphHeight", graphData.getGraphHeight());

        graphConfig.put("yAxisMin", graph.getYAxisMin());
        graphConfig.put("yAxisMax", graph.getYAxisMax());

        // fix invalid JSON coming from GraphController
        String dataSeriesJson = graph.getDataSeriesJSON().replaceFirst("\\{", "")
        // remove trailing "}"
                .substring(0, graph.getDataSeriesJSON().length() - 2);

        // read malformed JSON
        ObjectMapper mapper = new ObjectMapper();
        JsonFactory jsonFactory =
                mapper.getJsonFactory().configure(Feature.ALLOW_UNQUOTED_FIELD_NAMES, true)
                        .configure(Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);
        JsonParser jsonParser = jsonFactory.createJsonParser(dataSeriesJson);

        // array of String -> Object maps
        TypeReference<Map<String, Object>[]> dataSeriesType = new TypeReference<Map<String, Object>[]>() {};

        // write JSON as Map so that it can be serialized properly back to JSON
        Map<String, Object>[] seriesMap = mapper.readValue(jsonParser, dataSeriesType);
        graphConfig.put("dataSeriesJSON", seriesMap);
        result.put("graphConfiguration", graphConfig);
    }

    /**
     * Takes a List of SeriesPoints and generates a double array that holds the value of each SeriesPoint
     * 
     * @param seriespoints - List<AccumPoint> whose values need to be extracted into a double[] for detectors
     * @param dimId - The dimension id to pull from each AccumPoint
     * @param divisors
     * @param multiplier
     * @return pointarray - double[] that holds all the values from the passed in list of SeriesPoint
     */
    public double[] generateSeriesValues(List<AccumPoint> seriespoints, String dimId, double[] divisors,
            double multiplier) {
        // List<Number> pointlist = new ArrayList<Number>();
        double[] pointarray = new double[seriespoints.size()];
        int ix = 0;
        for (AccumPoint point : seriespoints) {
            // pointlist.add(point.getValue());
            if (point != null && point.getValue(dimId) != null) {
                pointarray[ix] = point.getValue(dimId).doubleValue();
            } else {
                pointarray[ix] = Double.NaN;
            }
            ix++;
        }

        // run divisor before detection
        for (int i = 0; i < pointarray.length; i++) {
            double div = divisors[i];
            if (div == 0) {
                pointarray[i] = 0.0;
            } else {
                pointarray[i] = (pointarray[i] / div) * multiplier;
            }
        }
        return pointarray;
    }

    public TemporalDetectorSimpleDataObject runDetection(Calendar startDayCal, Pair<Date, Date> startEndDatePair,
            double[] seriesDoubleArray, TimeSeriesModel model, String timeResolution) {
        // for yearly series, prepull is 0. Thus, queryStartDate is same as start date
        Date queryStartDate = getQueryStartDate(startDayCal, timeResolution, model.getPrepull());

        TemporalDetectorInterface TDI =
                (TemporalDetectorInterface) DetectorHelper.createObject(model.getTimeseriesDetectorClass());
        TemporalDetectorSimpleDataObject TDDO = new TemporalDetectorSimpleDataObject();

        // run detection
        TDDO.setCounts(seriesDoubleArray);
        TDDO.setStartDate(startEndDatePair.getFirst());
        TDDO.setTimeResolution(timeResolution);

        TDI.runDetector(TDDO);

        TDDO.cropStartup(model.getPrepull());

        if (!ReportController.DAILY.equalsIgnoreCase(timeResolution)) {
            // toggle between start date and end date
            // TDDO.setDates(getOurDates(startDate, endDate, tddoLength, timeResolution));
            TDDO.setDates(getOurDates(queryStartDate, startEndDatePair.getSecond(), TDDO.getCounts().length,
                    timeResolution, model.isDisplayIntervalEndDate()));
        }
        return TDDO;
    }

    // Original code taken from TemporalDetectorSimpleDataObserver.setupDates and reworked to better handle months
    private Date[] getOurDates(Date queryStartDate, Date endDate, int size, String timeResolution,
            boolean displayIntervalEndDate) {
        Date startDate = queryStartDate;
        if (displayIntervalEndDate) {
            startDate = computeResolutionBasedEndDate(queryStartDate, timeResolution, endDate);
        }

        Date[] dates = new Date[size];

        String tr = timeResolution;
        if (tr == null) {
            tr = ReportController.DAILY;
        }
        int zeroFillInterval =
                ReportController.intervalMap.keySet().contains(timeResolution) ? ReportController.intervalMap
                        .get(timeResolution) : -1;
        if (startDate != null && size >= 0) {
            Calendar cal = new GregorianCalendar();
            // forward point allows us to place the accumulated data at the front
            int i = 0;
            for (i = 0; i < size; i++) {
                // reset date to avoid unexpected date changes
                cal.setTime(startDate);
                cal.add(zeroFillInterval, 1 * i);
                if (endDate != null && cal.getTime().after(endDate)) {
                    cal.setTime(endDate);
                }
                // store date after interval addition
                dates[i] = cal.getTime();
            }
        }
        return dates;
    }

    /**
     * Compute end date based on time resolution. Defaults to the original date unless the resolution is weekly, monthly
     * or yearly in which case it is padded accordingly.
     * 
     * @param maxDate optionally used to keep the computed date below a maxDate (end date for the query for example)
     * @return Date
     */
    private Date computeResolutionBasedEndDate(Date startDate, String timeResolution, Date maxDate) {
        Calendar cal = new GregorianCalendar();
        cal.setTime(startDate);
        if (ReportController.WEEKLY.equalsIgnoreCase(timeResolution)) {
            cal.add(Calendar.WEEK_OF_YEAR, 1);
            cal.add(Calendar.DATE, -1);
        } else if (ReportController.MONTHLY.equalsIgnoreCase(timeResolution)) {
            cal.add(Calendar.MONTH, 1);
            cal.add(Calendar.DATE, -1);
        } else if (ReportController.YEARLY.equalsIgnoreCase(timeResolution)) {
            cal.add(Calendar.YEAR, 1);
            cal.add(Calendar.DATE, -1);
        } else {
            // do nothing for daily currently
        }
        // we want the end date/label to not exceed the query end date
        if (maxDate != null && cal.getTime().after(maxDate)) {
            cal.setTime(maxDate);
        }
        return cal.getTime();
    }

    public Map<String, Object> setDetectionErrorMessage(Exception e) {
        Map<String, Object> result = new HashMap<String, Object>();
        String errorMessage = "Failure to create Timeseries";
        if (e.getMessage() != null) {
            errorMessage = errorMessage + ":<BR>" + e.getMessage();
        }
        result.put("message", errorMessage);
        result.put("success", false);
        return result;
    }

    /**
     * Takes a List of AccumPoints and generates a double array that holds the total of accumulations
     * 
     * @param points - List<AccumPoint> whose values need to be extracted into a double[] for detectors
     * @param dimensions - The list of dimensions to sum from each AccumPoint
     * @return double[] that holds all the values from the passed in list of SeriesPoint
     */
    private double[] totalSeriesValues(List<AccumPoint> points, List<Dimension> dimensions) {
        double[] totalArray = new double[points.size()];
        int i = 0;
        for (AccumPoint point : points) {
            if (point != null) {
                for (Dimension dim : dimensions) {
                    Number value = point.getValue(dim.getId());
                    if (value != null) {
                        totalArray[i] = totalArray[i] + value.doubleValue();
                    }
                }
            } else {
                totalArray[i] = Double.NaN;
            }
            i++;
        }
        return totalArray;
    }

    public double[] getDivisors(List<AccumPoint> points, List<Dimension> timeseriesDenominators) {
        // -- Handles Denominator Types -- //
        double[] divisors = new double[points.size()];

        // if there is a denominator we need to further manipulate the data
        if (timeseriesDenominators != null && !timeseriesDenominators.isEmpty()) {
            // divisor is the sum of timeseriesDenominators
            divisors = totalSeriesValues(points, timeseriesDenominators);
        } else {
            // the query is for total counts
            Arrays.fill(divisors, 1.0);
        }
        return divisors;
    }
    
    public Map<String, Object> buildNoDataResult(DataSeriesSource dss, InspectableResourceBundleMessageSource messageSource){
        Map<String, Object> result = new HashMap<String, Object>();
        StringBuilder sb = new StringBuilder();
        sb.append("<h2>" + messageSource.getDataSourceMessage("graph.nodataline1", dss) + "</h2>");
        sb.append("<p>" + messageSource.getDataSourceMessage("graph.nodataline2", dss) + "</p>");
        result.put("html", sb.toString());
        result.put("success", true);
        return result;
    }

    public NumberFormat getNumberFormat(int maxFractionDigits)
    {
        NumberFormat numFormat = NumberFormat.getNumberInstance();
        numFormat.setMinimumFractionDigits(0);
        numFormat.setMaximumFractionDigits(maxFractionDigits);
        // number format for level (3) and expected values (1)
        return numFormat;
    }
    
}
