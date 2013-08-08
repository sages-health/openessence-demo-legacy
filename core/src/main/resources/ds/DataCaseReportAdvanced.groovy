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

import edu.jhuapl.openessence.datasource.FieldType
import edu.jhuapl.openessence.datasource.jdbc.DimensionBean
import edu.jhuapl.openessence.datasource.jdbc.ResolutionHandler
import edu.jhuapl.openessence.datasource.jdbc.timeresolution.sql.pgsql.PgSqlDailyHandler
import edu.jhuapl.openessence.datasource.jdbc.timeresolution.sql.pgsql.PgSqlMonthlyHandler
import edu.jhuapl.openessence.datasource.jdbc.timeresolution.sql.pgsql.PgSqlWeeklyHandler
import edu.jhuapl.openessence.datasource.map.GetMapQuery
import edu.jhuapl.openessence.datasource.map.MapMetaData
import edu.jhuapl.openessence.datasource.map.Threshold
import edu.jhuapl.openessence.datasource.map.WMSLayer
import edu.jhuapl.openessence.groovy.GroovyDataSource

class DataCaseReportAdvanced extends GroovyDataSource {

    Set roles = ['ROLE_USER']

    DataCaseReportAdvanced() {
        DimensionBean d

        MapMetaData mapInfo = new MapMetaData()
        // Bounds are used to default the center/zoom for the map
        // enter as a array [left, bottom, right, top]
        mapInfo.bounds = [-87.0, 35.9, -81.0, 40.0] as float[]
        mapInfo.baseLayers = [new WMSLayer(
                name: 'Base Layer',
                layerOptions: [
                        singleTile: true,
                        ratio: 1
                ],
                getMapQuery: new GetMapQuery(
                        layers: ['openessence:district_geometry'],
                        format: 'image/png'
                )
        )]
        mapInfo.overlays = [new WMSLayer(
                name: 'Total Counts',
                dataDSName: 'DistrictDetection_Entry',
                accumulationId: 'TotalCount',
                dataFieldMap: [
                        'Count': 'Cases'
                ],
                thresholds: [
                        new Threshold('aaaaaa', -9999, -9999),
                        new Threshold('ABD9E9', 0, 4),
                        new Threshold('FFFFBF', 5, 9),
                        new Threshold('FDAE61', 10, 19),
                        new Threshold('D7191C', 20, 9999)
                ],
                layerOptions: [
                        singleTile: true,
                        ratio: 1
                ],
                getMapQuery: new GetMapQuery(
                        layers: ['openessence:district_counts'],
                        format: 'image/png',
                        transparent: true
                )
        )]
        mapInfo.style = [
                fillOpacity: '0.7',
                stroke: '444444',
                strokeWidth: '1',
                strokeOpacity: '0.9'
        ]

        setMetaData([
                form: [supportMap: true],
                menuCfg: [[parent: 'report', src: 'OE.report.datasource.main', order: 1]],
                grid: [width: 200, sortcolumn: 'Id', sortorder: 'desc'],
                mapInfo: mapInfo
        ])

        init([id: 'Id', sqlCol: 'distinct(dr.id) as datareport', sqlColAlias: 'datareport', sqlType: FieldType.INTEGER,
                isResult: true, isFilter: false, metaData: [grid: [width: 80]]])

        d = init([id: 'District', sqlCol: 'district', sqlType: FieldType.TEXT, isResult: true, isFilter: true,
                isGrouping: true,
                metaData: [form: [width: 300, xtype: 'superboxselect', sortcolumn: 'Order']]])
        d.possibleValuesDsName = 'Districts'

        init([id: 'PatientId', sqlCol: 'dr.patient_id', sqlType: FieldType.TEXT, isResult: true, isFilter: false,
                metaData: [grid: [width: 120]]])

        d = init([id: 'ReportDate', sqlCol: 'dr.report_date', sqlType: FieldType.DATE, isResult: true, isFilter: true,
                isGrouping: true,
                metaData: [grid: [width: 100]]])
        d.resolutionHandlers = (Map<String, ResolutionHandler>) [
                'daily': new PgSqlDailyHandler(),
                'weekly': new PgSqlWeeklyHandler(0),
                'monthly': new PgSqlMonthlyHandler()
        ]

        d = init([id: 'ReturnVisit', sqlCol: 'dr.return_visit', sqlType: FieldType.BOOLEAN, isResult: true,
                isFilter: true,
                metaData: [form: [width: 300, xtype: 'superboxselect'], grid: [width: 80, renderBooleanAsTernary: true]]])
        d.possibleValuesDsData = [[true, 'Yes'], [false, 'No']]

        d = init([id: 'Sex', sqlCol: 'dr.sex', sqlType: FieldType.TEXT, isResult: true, isEdit: true, isFilter: true,
                isGrouping: true,
                metaData: [form: [width: 300, xtype: 'superboxselect'], grid: [width: 40]]])
        d.possibleValuesDsData = [['F', 'Female'], ['M', 'Male'], ['UNK', 'Unknown']]

        init([id: 'Age', sqlCol: 'dr.age', sqlType: FieldType.INTEGER, isResult: true, isFilter: false,
                metaData: [grid: [width: 80]]])

        d = init([id: 'AgeGroupName', sqlCol: 'ag.description', sqlType: FieldType.TEXT, isResult: true, isFilter: true,
                metaData: [grid: [width: 100], form: [width: 300, xtype: 'superboxselect', sortcolumn: 'Order']]])
        d.possibleValuesDsName = 'AgeGroups'
        d.possibleValuesDsResults = ['AgeGroupId', 'AgeGroup', 'Order']

        init([id: 'Weight', sqlCol: 'dr.weight', sqlType: FieldType.DOUBLE, isResult: true, isFilter: false,
                metaData: [grid: [width: 80]]])

        init([id: 'Bp_Systolic', sqlCol: 'dr.bp_systolic', sqlType: FieldType.DOUBLE, isResult: true, isFilter: false,
                metaData: [grid: [width: 80]]])

        init([id: 'Bp_Diastolic', sqlCol: 'dr.bp_diastolic', sqlType: FieldType.DOUBLE, isResult: true, isFilter: false,
                metaData: [grid: [width: 80]]])

        init([id: 'Pulse', sqlCol: 'dr.pulse', sqlType: FieldType.INTEGER, isResult: true, isFilter: false,
                metaData: [grid: [width: 80]]])

        init([id: 'Temperature', sqlCol: 'dr.temperature', sqlType: FieldType.DOUBLE, isResult: true, isFilter: false,
                metaData: [grid: [width: 100]]])

        d = init([id: 'TemperatureGroupName', sqlCol: 'temp.description', sqlType: FieldType.TEXT, isResult: true, isFilter: true,
                metaData: [grid: [width: 100], form: [width: 300, xtype: 'superboxselect', sortcolumn: 'Order']]])
        d.possibleValuesDsName = 'TemperatureGroups'
        d.possibleValuesDsResults = ['TempGroupId', 'TemperatureGroup', 'Order']

        init([id: 'AllSymptoms', sqlCol: 'allsymptoms', sqlType: FieldType.TEXT, isResult: true, isFilter: true,
                metaData: [form: [xtype: 'hidden'], grid: [width: 200]]])

        init([id: 'AllDiagnoses', sqlCol: 'alldiagnosis', sqlType: FieldType.TEXT, isResult: true, isFilter: true,
                metaData: [form: [xtype: 'hidden'], grid: [width: 200]]])

        init([id: 'Notes', sqlCol: 'dr.notes', sqlType: FieldType.TEXT, isResult: true, isFilter: false,
                metaData: [form: [width: 300]]])

        init([id: 'CreateDate', sqlCol: 'dr.create_date', sqlType: FieldType.DATE, isResult: true,
                metaData: [grid: [width: 100]]])
        init([id: 'ModifiedDate', sqlCol: 'dr.modified_date', sqlType: FieldType.DATE, isResult: true,
                metaData: [grid: [width: 100]]])

        d = init([id: 'timeseriesDetectorClass', sqlCol: '\'<na>\'', sqlType: FieldType.TEXT, isResult: false,
                isFilter: true])
        d.possibleValuesDsData = [
                ['edu.jhuapl.bsp.detector.EarsC1', 'CDC-C1'],
                ['edu.jhuapl.bsp.detector.EarsC2', 'CDC-C2'],
                ['edu.jhuapl.bsp.detector.EarsC3', 'CDC-C3'],
                ['edu.jhuapl.bsp.detector.CusumSagesDetector', 'CUSUM SAGES'],
                ['edu.jhuapl.bsp.detector.EWMASagesDetector', 'EWMA SAGES'],
                ['edu.jhuapl.bsp.detector.GSSages', 'GS SAGES']
        ]

        // Symptoms Filters
        d = init([id: 'Symptom1ID', sqlCol: 'rs1.symptom_id', sqlType: FieldType.INTEGER, isResult: false,
                isFilter: true,
                metaData: [form: [width: 300, xtype: 'superboxselect']]])
        d.possibleValuesDsName = 'Symptoms'

        d = init([id: 'Symptom2ID', sqlCol: 'rs2.symptom_id', sqlType: FieldType.INTEGER, isResult: false,
                isFilter: true,
                metaData: [form: [width: 300, xtype: 'superboxselect']]])
        d.possibleValuesDsName = 'Symptoms'

        // Diagnosis Filters
        d = init([id: 'Diagnosis1ID', sqlCol: 'rd1.diagnosis_id', sqlType: FieldType.INTEGER, isResult: false,
                isFilter: true,
                metaData: [form: [width: 300, xtype: 'superboxselect']]])
        d.possibleValuesDsName = 'Diagnoses'

        d = init([id: 'Diagnosis2ID', sqlCol: 'rd2.diagnosis_id', sqlType: FieldType.INTEGER, isResult: false,
                isFilter: true,
                metaData: [form: [width: 300, xtype: 'superboxselect']]])
        d.possibleValuesDsName = 'Diagnoses'

        // Accumulations
        init([id: 'accumId', sqlCol: '\'<na>\'', sqlType: FieldType.TEXT, isResult: false, isFilter: true,
                metaData: [form: [width: 300, xtype: 'multiselect', height: 25]]])

        // Accumulation result dimensions
        init([id: 'Cases', sqlCol: 'count( distinct(dr.id) ) as count', sqlColAlias: 'count',
                sqlType: FieldType.INTEGER, isResult: true, isAccumulation: true,
                metaData: [grid: [hidden: false]]])

        init([id: 'UserId', sqlCol: 'dr.user_id', sqlType: FieldType.INTEGER, isResult: false, isFilter: false,
                metaData: [form: [xtype: 'hidden', allowBlank: true]]])

        setDimensions(dimensionBeans)
        setBaseDetailsQuery('''
                        data_case_report_plus dr

                        left join report_symptoms rs1     on rs1.report_id = dr.id
                        left join symptoms sym1           on sym1.id       = rs1.symptom_id
                        left join report_symptoms rs2     on rs2.report_id = dr.id
                        left join symptoms sym2           on sym2.id       = rs2.symptom_id

                        left join report_diagnoses rd1    on rd1.report_id = dr.id
                        left join diagnoses d1            on d1.id         = rd1.diagnosis_id
                        left join report_diagnoses rd2    on rd2.report_id = dr.id
                        left join diagnoses d2            on d2.id         = rd2.diagnosis_id

                        left join temperature_groups temp on dr.temperature >= temp.min and dr.temperature<=temp.max
                        left join age_groups ag           on dr.age >= ag.min and dr.age<=ag.max
                  ''')
    }
}
