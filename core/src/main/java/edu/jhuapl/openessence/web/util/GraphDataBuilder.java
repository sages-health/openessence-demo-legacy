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

import java.util.Arrays;

public class GraphDataBuilder {

    private double[][] allCounts;
    private int[][] allColors;
    private String[][] allAltTexts;
    private double[][] allExpecteds;
    private double[][] allLevels;
    private String[][] allLineSetURLs;
    private String[][] allSwitchInfo;
    private String[] lineSetLabels;
    private boolean[] displayAlerts;
    private String[] xAxisLables;

    public GraphDataBuilder() {
        this(0);
    }

    public GraphDataBuilder(int numSeries) {
        allCounts = new double[numSeries][];
        allColors = new int[numSeries][];
        allAltTexts = new String[numSeries][];
        allExpecteds = new double[numSeries][];
        allLevels = new double[numSeries][];
        allLineSetURLs = new String[numSeries][];
        allSwitchInfo = new String[numSeries][];
        lineSetLabels = new String[numSeries];
        displayAlerts = new boolean[numSeries];
    }

    public void setDataSeriesInfo(int ix, double[] counts, int[] colors, String[] altTexts, double[] expecteds,
            double[] levels, String[] lineSetURLs, String[] switchInfo, String lineSetLabel, boolean displayAlert) {
        setCountsAt(ix, counts);
        setColorsAt(ix, colors);
        setAltTextsAt(ix, altTexts);
        setExpectedsAt(ix, expecteds);
        setLevelsAt(ix, levels);
        setLineSetURLsAt(ix, lineSetURLs);
        setSwitchInfoAt(ix, switchInfo);
        setLineSetLabelAt(ix, lineSetLabel);
        setDisplayAlert(ix, displayAlert);
    }

    public void setCountsAt(int ix, double[] counts) {
        allCounts[ix] = counts;
    }

    public void setColorsAt(int ix, int[] colors) {
        allColors[ix] = colors;
    }

    public void setAltTextsAt(int ix, String[] altTexts) {
        allAltTexts[ix] = altTexts;
    }

    public void setExpectedsAt(int ix, double[] expecteds) {
        allExpecteds[ix] = expecteds;
    }

    public void setLevelsAt(int ix, double[] levels) {
        allLevels[ix] = levels;
    }

    public void setLineSetURLsAt(int ix, String[] lineSetURLs) {
        allLineSetURLs[ix] = lineSetURLs;
    }

    public void setSwitchInfoAt(int ix, String[] switchInfo) {
        allSwitchInfo[ix] = switchInfo;
    }

    public void setLineSetLabelAt(int ix, String lineSetLabel) {
        lineSetLabels[ix] = lineSetLabel;
    }

    public void setDisplayAlert(int ix, boolean displayAlert) {
        displayAlerts[ix] = displayAlert;
    }

    public void setXAxisLabels(String[] labels) {
        xAxisLables = labels;
    }

    public void updateGraphData(DefaultGraphData graphData) {
        graphData.setCounts(allCounts);
        graphData.setColors(allColors);
        graphData.setAltTexts(allAltTexts);
        graphData.setXLabels(xAxisLables);
        graphData.setExpecteds(allExpecteds);
        graphData.setLevels(allLevels);
        graphData.setLineSetURLs(allLineSetURLs);
        graphData.setLineSetLabels(lineSetLabels);
        graphData.setDisplayAlerts(displayAlerts);
    }

    public double[][] getAllCounts() {
        return allCounts;
    }

    public int[][] getAllColors() {
        return allColors;
    }

    public String[][] getAllAltTexts() {
        return allAltTexts;
    }

    public double[][] getAllExpecteds() {
        return allExpecteds;
    }

    public double[][] getAllLevels() {
        return allLevels;
    }

    public String[][] getAllLineSetURLs() {
        return allLineSetURLs;
    }

    public String[][] getAllSwitchInfo() {
        return allSwitchInfo;
    }

    public String[] getLineSetLabels() {
        return lineSetLabels;
    }

    public boolean[] getDisplayAlerts() {
        return displayAlerts;
    }

    public String[] getxAxisLables() {
        return xAxisLables;
    }

    public void fixDataLengths(String[][] labels) {
        // find max series length
        int maxDataLength = findMaxDataLength(allCounts);
        // fix each series to be same length
        allCounts = updateDoubleArray(allCounts, maxDataLength, 0);
        allColors = updateIntArray(allColors, maxDataLength, 0);
        allAltTexts = updateStringArray(allAltTexts, maxDataLength, "");
        allExpecteds = updateDoubleArray(allExpecteds, maxDataLength, 0);
        allLineSetURLs = updateStringArray(allLineSetURLs, maxDataLength, "");
        allLevels = updateDoubleArray(allLevels, maxDataLength, 0);
        xAxisLables = getXAxisLabels(labels, maxDataLength);
    }

    /**
     * Provided series counts two dimension array, find out number of points in the longest series
     * 
     * @param allCounts counts for the time series
     * @return number of data elements in the longest series
     */
    public int findMaxDataLength(double[][] allCounts) {
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
    public double[][] updateDoubleArray(double[][] dataArray, int length, double defaultVal) {
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
    public int[][] updateIntArray(int[][] dataArray, int length, int defaultVal) {
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
    public String[][] updateStringArray(String[][] dataArray, int length, String defaultVal) {
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
    public String[] updateXAxisLabels(String[] xAxisLabels, int length, String defaultVal) {
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
    public String[] getXAxisLabels(String[][] labels, int maxDataLength) {
        for (String[] labelsArray : labels) {
            if (labelsArray.length == maxDataLength) {
                return labelsArray;
            }
        }
        return new String[0];
    }

}
