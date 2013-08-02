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
import edu.jhuapl.openessence.datasource.entry.CompleteRecord
import edu.jhuapl.openessence.datasource.entry.DbKeyValMap
import edu.jhuapl.openessence.datasource.jdbc.DimensionBean
import edu.jhuapl.openessence.datasource.jdbc.entry.TableAwareQueryRecord
import edu.jhuapl.openessence.groovy.GroovyOeDataEntrySource
import edu.jhuapl.openessence.security.OEUser

import java.sql.Time

class DataDistrictVisits_Entry extends GroovyOeDataEntrySource {

    Set roles = ['ROLE_USER']

    DataDistrictVisits_Entry() {
        DimensionBean d

        setMetaData([ grid: [ sortcolumn: 'VisitId', sortorder: 'asc' ] ] as HashMap)

        setTableName('district_visits')
        addMasterTable([tableName: 'district_visits', pks: ['PatientId', 'DiagnosesId'] as HashSet])

		init([id: 'ReportId', sqlCol: 'report_id', sqlType: FieldType.INTEGER, isResult: true, isEdit: true, isFilter: true, isAutoGen: true,
                metaData: [form: [xtype: 'hidden', allowBlank: true]]])

		init([id: 'PatientId', sqlCol: 'patient_id', sqlType: FieldType.TEXT, isResult: true, isEdit: true, isFilter: true,
                metaData: [form: [allowBlank: false, width: 120]]])

        
        d = init([id: 'ReturnVisit', sqlCol: 'return_visit', sqlType: FieldType.BOOLEAN, isResult: true, isEdit: true, isFilter: true,
                metaData: [form: [allowBlank: true, width: 100]]])
//        d.possibleValuesDsData = [[true, 'Yes'], [false, 'No']]

        d = init([id: 'Sex', sqlCol: 'sex', sqlType: FieldType.TEXT, isResult: true, isEdit: true, isFilter: true,
                metaData: [form: [allowBlank: false, width: 100]]])
        d.possibleValuesDsData = [['F', 'Female'], ['M', 'Male'], ['UNK', 'Unknown']]

        init([id: 'Age', sqlCol: 'age', sqlType: FieldType.INTEGER, isResult: true, isEdit: true, isFilter: true,
                metaData: [form: [allowBlank: true, minValue: 0, maxValue: 999, width: 80]]])
        init([id: 'Weight', sqlCol: 'weight', sqlType: FieldType.DOUBLE, isResult: true, isEdit: true, isFilter: true,
                metaData: [form: [allowBlank: true, minValue: 0, maxValue: 999, allowDecimals: true, width: 100]]])

        init([id: 'Bp_Systolic', sqlCol: 'bp_systolic', sqlType: FieldType.DOUBLE, isResult: true, isEdit: true, isFilter: true,
                metaData: [form: [allowBlank: true, minValue: 0, maxValue: 200, allowDecimals: true, width: 100]]])

        init([id: 'Bp_Diastolic', sqlCol: 'bp_diastolic', sqlType: FieldType.DOUBLE, isResult: true, isEdit: true, isFilter: true,
                metaData: [form: [allowBlank: true, minValue: 0, maxValue: 200, allowDecimals: true, width: 100]]])

        init([id: 'Pulse', sqlCol: 'pulse', sqlType: FieldType.INTEGER, isResult: true, isEdit: true, isFilter: true,
                metaData: [form: [allowBlank: true, minValue: 0, maxValue: 200, allowDecimals: false, width: 100]]])

        init([id: 'Temperature', sqlCol: 'temperature', sqlType: FieldType.DOUBLE, isResult: true, isEdit: true, isFilter: true,
                metaData: [form: [allowBlank: true, minValue: 0, maxValue: 200, allowDecimals: true, width: 120]]])

		d=init([id: 'DiagnosesId',     sqlCol: 'diagnoses_id',     sqlType: FieldType.INTEGER, isEdit: true, isResult: true, isFilter: true,
			metaData: [ form: [ width: 200 ] ] ] as HashMap)
		d.possibleValuesDsName = 'Diagnoses'


        setDimensions(dimensionBeans)
        setBaseDetailsQuery('district_visits')
    }

}
