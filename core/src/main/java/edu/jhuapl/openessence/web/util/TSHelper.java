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

import edu.jhuapl.graphs.controller.DefaultGraphData;
import edu.jhuapl.graphs.controller.GraphController;
import edu.jhuapl.graphs.controller.GraphDataInterface;
import edu.jhuapl.graphs.controller.GraphObject;
import edu.jhuapl.openessence.controller.ReportController;
import edu.jhuapl.openessence.datasource.Dimension;
import edu.jhuapl.openessence.datasource.FieldType;
import edu.jhuapl.openessence.datasource.Filter;
import edu.jhuapl.openessence.datasource.OeDataSourceException;
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
import org.springframework.web.context.request.WebRequest;

import java.awt.Color;
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
public class TSHelper {

    private static final Logger log = LoggerFactory.getLogger(TSHelper.class);
    private static final String TIMEZONE_ENABLED = "timezone.enabled";

    // number format for level
    public static final NumberFormat NUM_FORMAT3 = NumberFormat.getNumberInstance();
    // number format for expected count
    public static final NumberFormat NUM_FORMAT1 = NumberFormat.getNumberInstance();

    static {
        NUM_FORMAT3.setMinimumFractionDigits(0);
        NUM_FORMAT3.setMaximumFractionDigits(3);

        NUM_FORMAT1.setMinimumFractionDigits(0);
        NUM_FORMAT1.setMaximumFractionDigits(1);
    }

    /**
     * Extract time resolution for the time series
     * 
     * @param model TimeSeriesModel object
     * @param groupingDim GroupingDimension object
     * @param groupId String group ID
     * @return time resolution as a String
     */
    public static String getResolution(TimeSeriesModel model, GroupingDimension groupingDim, String groupId) {
        String resolution = "";

        if (model.getTimeseriesGroupResolution() != null) {
            String[] parts = model.getTimeseriesGroupResolution().split(":");
            if (parts.length == 2 && !parts[0].trim().isEmpty() && !parts[1].trim().isEmpty()) {
                resolution = parts[1];
            }
        }

        // find resolution handlers as appropriate for groupings
        if (resolution == null || "".equals(groupId)) {
            String[] res = groupingDim.getResolutions().toArray(new String[groupingDim.getResolutions().size()]);
            if (res.length > 0) {
                resolution = res[0];
            }
        }

        return resolution;
    }

    /**
     * Extracts group ID from TimeSeriesModel object
     * 
     * @param model TimeSeriesModel object
     * @return group ID as a String
     */
    public static String getGroupId(TimeSeriesModel model) {
        String groupId = "";

        if (model.getTimeseriesGroupResolution() != null) {
            String[] parts = model.getTimeseriesGroupResolution().split(":");
            if (parts.length == 2 && !parts[0].trim().isEmpty() && !parts[1].trim().isEmpty()) {
                groupId = parts[0];
            }
        }

        if (groupId == null || groupId.isEmpty()) {
            throw new OeDataSourceException("No Grouping Dimension ID specified");
        }
        return groupId;
    }

    /**
     * Extract timezone offset from the request and return it as hours minutes string
     * 
     * @param request WebRequest object
     * @param messageSource InspectableResourceBundleMessageSource object
     * @return timezone offset as as hours minutes string
     */
    public static String getClientTimezone(WebRequest request, InspectableResourceBundleMessageSource messageSource) {
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
    public static String buildTimeSeriesURL(JdbcOeDataSource ds, String contextPath, String servletPath,
            InspectableResourceBundleMessageSource messageSource) {
        String graphTimeSeriesUrl = contextPath + servletPath + "/report/graphTimeSeries";
        graphTimeSeriesUrl = TSHelper.appendGraphFontParam(ds, graphTimeSeriesUrl, messageSource);
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
    public static String appendUrlParameter(String url, String param, String value) {
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
    public static String appendGraphFontParam(JdbcOeDataSource dataSource, String url,
            InspectableResourceBundleMessageSource messageSource) {
        try {
            String graphFont = messageSource.getDataSourceMessage("graph.font", dataSource);
            return TSHelper.appendUrlParameter(url, "font", graphFont);
        } catch (NoSuchMessageException e) {
            log.debug("Property graph.font not found, using default");
            return url;
        }
    }

    /**
     * Builds graph data object using given time series model information
     * 
     * @param model TimeSeriesModel object
     * @return DefaultGraphData object
     */
    public static DefaultGraphData buildGraphData(TimeSeriesModel model) {

        // create graph data and set known configuration
        DefaultGraphData graphData = new DefaultGraphData();
        graphData.setShowSingleSeverityLegends(false);
        graphData.setGraphTitle(model.getTimeseriesTitle());
        graphData.setGraphWidth(model.getWidth());
        graphData.setGraphHeight(model.getHeight());
        graphData.setShowLegend(true);
        graphData.setBackgroundColor(new Color(255, 255, 255, 0));

        // only set an array if they provided one
        if (model.getGraphBaseColors() != null && model.getGraphBaseColors().length > 0) {
            // TODO leverage Spring to convert colors
            graphData.setGraphBaseColors(ControllerUtils.getColorsFromHex(Color.BLACK, model.getGraphBaseColors()));
        }
        return graphData;
    }

    /**
     * Calculate start date for yearly series
     * 
     * @param year year as an int
     * @param resolution time resolution string
     * @param isEpiWeekEnabled if we are using EPI week calculation
     * @return year start date as a Date object
     */
    public static Date calculateStartDate(int year, String resolution, boolean isEpiWeekEnabled) {
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
    public static Date calculateEndDate(int year, String resolution, boolean isEpiWeekEnabled) {
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
    public static Map<String, String[]> fixStartEndDatesForYearAsSeries(Map<String, String[]> params,
            Dimension accumulation, String groupId, String resolution, boolean isEpiWeekEnabled) {
        Map<String, String[]> updatedParams = new HashMap<String, String[]>(params);

        try {
            int year = Integer.parseInt(accumulation.getId());
            Date startDate = TSHelper.calculateStartDate(year, resolution, isEpiWeekEnabled);
            Date endDate = TSHelper.calculateEndDate(year, resolution, isEpiWeekEnabled);
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
    public static Pair<Date, Date> getStartEndDates(GroupingDimension grpdim, List<Filter> filters) {
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
    public static int getTimezoneOffsetMillies(InspectableResourceBundleMessageSource messageSource,
            TimeZone clientTimezone) {
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
    public static Date getQueryStartDate(Calendar cal, String timeResolution, int prepull) {
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
    public static String buildDetailsURL(String dateFieldName, String timeResolution, TimeZone clientTimezone, Date dt,
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
     * @param allCounts counts as double 2D array
     * @param dates dates as String array
     * @param lineSetLabels lineset labels as String array
     * @param allLevels levels as 2D double array
     * @param allExpecteds expected values as 2D double array
     * @param allSwitchInfo switch info as 2D String array
     * @param allColors colors as 2D int array
     */
    public static void addDetailsToResult(Map<String, Object> result, double[][] allCounts, String[] dates,
            String[] lineSetLabels, double[][] allLevels, double[][] allExpecteds, String[][] allSwitchInfo,
            int[][] allColors) {
        int totalPoints = 0;
        List<HashMap<String, Object>> details = new ArrayList<HashMap<String, Object>>();
        HashMap<String, Object> detail;
        for (int i = 0; i < allCounts.length; i++) {
            if (allCounts[i] != null) {
                for (int j = 0; j < allCounts[i].length; j++) {
                    totalPoints++;
                    detail = new HashMap<String, Object>();
                    detail.put("Date", dates[j]);
                    detail.put("Series", lineSetLabels[i]);
                    detail.put("Level", allLevels[i][j]);
                    detail.put("Count", allCounts[i][j]);
                    if (!ArrayUtils.isEmpty(allExpecteds[i])) {
                        detail.put("Expected", allExpecteds[i][j]);
                    }
                    if (!ArrayUtils.isEmpty(allSwitchInfo[i])) {
                        detail.put("Switch", allSwitchInfo[i][j]);
                    }
                    detail.put("Color", allColors[i][j]);
                    details.add(detail);
                }
            }
        }
        result.put("detailsTotalRows", totalPoints);
        result.put("details", details);
    }

    public static final void addGraphConfigToResult(Map<String, Object> result, GraphController gc,
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
     * Provided series counts two dimension array, find out number of points in the longest series
     * 
     * @param allCounts counts for the time series
     * @return number of data elements in the longest series
     */
    public static int findMaxDataLength(double[][] allCounts) {
        int maxLen = 0;
        for (double[] count : allCounts) {
            maxLen = (maxLen < count.length) ? count.length : maxLen;
        }
        return maxLen;
    }

    /**
     * Appends default value if one or more series is shorter than given length
     * 
     * @param dataArray two dimensional double array
     * @param length integer length
     * @param defaultVal default value to be used for elements if series is shorter than given length
     * @return updated data array
     */
    public static double[][] updateDoubleArray(double[][] dataArray, int length, double defaultVal) {
        for (int i = 0; i < dataArray.length; i++) {
            if (dataArray[i].length == 0) {
                dataArray[i] = new double[length];
                Arrays.fill(dataArray[i], 0, length - 1, defaultVal);
            } else if (dataArray[i].length < length) {
                double[] tmp = dataArray[i];
                dataArray[i] = new double[length];
                System.arraycopy(tmp, 0, dataArray[i], 0, tmp.length);
                Arrays.fill(dataArray[i], tmp.length - 1, length - 1, defaultVal);
            }
        }
        return dataArray;
    }

    /**
     * Appends default value if one or more series is shorter than given length
     * 
     * @param dataArray two dimensional int array
     * @param length integer length
     * @param defaultVal default value to be used for elements if series is shorter than given length
     * @return updated data array
     */
    public static int[][] updateIntArray(int[][] dataArray, int length, int defaultVal) {
        for (int i = 0; i < dataArray.length; i++) {
            if (dataArray[i].length == 0) {
                dataArray[i] = new int[length];
                Arrays.fill(dataArray[i], 0, length - 1, defaultVal);
            } else if (dataArray[i].length < length) {
                int[] tmp = dataArray[i];
                dataArray[i] = new int[length];
                System.arraycopy(tmp, 0, dataArray[i], 0, tmp.length);
                Arrays.fill(dataArray[i], tmp.length - 1, length - 1, defaultVal);
            }
        }
        return dataArray;
    }

    /**
     * Appends default value if one or more series is shorter than given length
     * 
     * @param dataArray two dimensional String array
     * @param length integer length
     * @param defaultVal default value to be used for elements if series is shorter than given length
     * @return updated data array
     */
    public static String[][] updateStringArray(String[][] dataArray, int length, String defaultVal) {
        for (int i = 0; i < dataArray.length; i++) {
            if (dataArray[i].length == 0) {
                dataArray[i] = new String[length];
                Arrays.fill(dataArray[i], 0, length - 1, defaultVal);
            } else if (dataArray[i].length < length) {
                String[] tmp = dataArray[i];
                dataArray[i] = new String[length];
                System.arraycopy(tmp, 0, dataArray[i], 0, tmp.length);
                Arrays.fill(dataArray[i], tmp.length - 1, length - 1, defaultVal);
            }
        }
        return dataArray;
    }

    /**
     * Appends default label value if the label array is not long enough
     * 
     * @param xAxisLabels labels for the X axis
     * @param length expected length for x axis labels array
     * @param defaultVal default value for the data points we are appending to the label array
     * @return updated x axis array
     */
    public static String[] updateXAxisLabels(String[] xAxisLabels, int length, String defaultVal) {
        if (xAxisLabels.length == 0) {
            xAxisLabels = new String[length];
            Arrays.fill(xAxisLabels, 0, length - 1, defaultVal);
        } else if (xAxisLabels.length < length) {
            String[] tmp = xAxisLabels;
            xAxisLabels = new String[length];
            System.arraycopy(tmp, 0, xAxisLabels, 0, tmp.length);
            Arrays.fill(xAxisLabels, tmp.length - 1, length - 1, defaultVal);
        }
        return xAxisLabels;
    }

    /**
     * For time series X axis labels, pick the one having max data lenght
     * 
     * @param labels label arrays
     * @param maxDataLength number of data points on the longest series
     * @return x axis labels as a String array
     */
    public static String[] getXAxisLabels(String[][] labels, int maxDataLength) {
        for (String[] labelsArray : labels) {
            if (labelsArray.length == maxDataLength) {
                return labelsArray;
            }
        }
        return new String[0];
    }

}
