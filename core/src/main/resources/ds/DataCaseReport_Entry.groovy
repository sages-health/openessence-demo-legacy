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

class DataCaseReport_Entry extends GroovyOeDataEntrySource {

    Set roles = ['ROLE_USER']

    DataCaseReport_Entry() {
        DimensionBean d

        setMetaData([
                menuCfg: [[parent: 'input', src: 'OE.input.datasource.main', order: 1]],
                displayDatasource: 'DataCaseReportEntryGrid',
                form: []
        ])

        setTableName('data_case_reports')
        addMasterTable([tableName: 'data_case_reports', pks: ['Id'] as HashSet])

        addChildTable([tableName: 'report_symptoms',        //child mapping table 
                columns: ['c1_ReportId', 'c1_SymptomId'],       //dimensionIds of mapping
                pks: ['c1_ReportId', 'c1_SymptomId'] as HashSet,//primary keys (dimensionIds) of mapping
                fksToParent: ['c1_ReportId': 'Id'],             //foreign key to parent Datasource
                possibleValuesDsName: 'Symptoms',               //Datasource to get values
                possibleValuesDsFks: ['c1_SymptomId': 'Id'],    //foreign key to child/value Datasource
                metaData: [form: [width: 400, xtype: 'superboxselect', allowBlank: false]]
        ])

        addChildTable([tableName: 'report_diagnoses',        //child mapping table 
                columns: ['c2_ReportId', 'c2_DiagnosesId'],       //dimensionIds of mapping
                pks: ['c2_ReportId', 'c2_DiagnosesId'] as HashSet,//primary keys (dimensionIds) of mapping
                fksToParent: ['c2_ReportId': 'Id'],             //foreign key to parent Datasource
                possibleValuesDsName: 'Diagnoses',               //Datasource to get values
                possibleValuesDsFks: ['c2_DiagnosesId': 'Id'],    //foreign key to child/value Datasource
                metaData: [form: [width: 400, height: 52, xtype: 'superboxselect', allowBlank: true]]
        ])

        init([id: 'Id', sqlCol: 'id', sqlType: FieldType.INTEGER, isResult: true, isEdit: true, isFilter: true, isAutoGen: true,
                metaData: [form: [xtype: 'hidden', allowBlank: true]]])

        d = init([id: 'District', sqlCol: 'district', sqlType: FieldType.TEXT, isResult: true, isEdit: true, isFilter: true,
                metaData: [form: [allowBlank: false, sortcolumn: 'Order']]])
        d.possibleValuesDsName = 'Districts'

        init([id: 'PatientId', sqlCol: 'patient_id', sqlType: FieldType.TEXT, isResult: true, isEdit: true, isFilter: true,
                metaData: [form: [allowBlank: false, width: 120]]])

        init([id: 'ReportDate', sqlCol: 'report_date', sqlType: FieldType.DATE, isResult: true, isEdit: true, isFilter: true,
                metaData: [form: [allowBlank: false]]])

        d = init([id: 'ReturnVisit', sqlCol: 'return_visit', sqlType: FieldType.BOOLEAN, isResult: true, isEdit: true, isFilter: true,
                metaData: [form: [allowBlank: false]]])
        d.possibleValuesDsData = [[true, 'Yes'], [false, 'No']]

        d = init([id: 'Sex', sqlCol: 'sex', sqlType: FieldType.TEXT, isResult: true, isEdit: true, isFilter: true,
                metaData: [form: [allowBlank: false]]])
        d.possibleValuesDsData = [['F', 'Female'], ['M', 'Male'], ['UNK', 'Unknown']]

        init([id: 'Age', sqlCol: 'age', sqlType: FieldType.INTEGER, isResult: true, isEdit: true, isFilter: true,
                metaData: [form: [allowBlank: true, minValue: 0, maxValue: 999, width: 80]]])
        init([id: 'Weight', sqlCol: 'weight', sqlType: FieldType.DOUBLE, isResult: true, isEdit: true, isFilter: true,
                metaData: [form: [allowBlank: true, minValue: 0, maxValue: 999, allowDecimals: true, width: 80]]])

        init([id: 'Bp_Systolic', sqlCol: 'bp_systolic', sqlType: FieldType.DOUBLE, isResult: true, isEdit: true, isFilter: true,
                metaData: [form: [allowBlank: true, minValue: 0, maxValue: 200, allowDecimals: true, width: 40]]])

        init([id: 'Bp_Diastolic', sqlCol: 'bp_diastolic', sqlType: FieldType.DOUBLE, isResult: true, isEdit: true, isFilter: true,
                metaData: [form: [allowBlank: true, minValue: 0, maxValue: 200, allowDecimals: true, width: 40]]])

        init([id: 'Pulse', sqlCol: 'pulse', sqlType: FieldType.INTEGER, isResult: true, isEdit: true, isFilter: true,
                metaData: [form: [allowBlank: true, minValue: 0, maxValue: 200, allowDecimals: false, width: 80]]])

        init([id: 'Temperature', sqlCol: 'temperature', sqlType: FieldType.DOUBLE, isResult: true, isEdit: true, isFilter: true,
                metaData: [form: [allowBlank: true, minValue: 0, maxValue: 200, allowDecimals: true, width: 80]]])

        // symptoms data
        init([id: 'c1_ReportId', sqlCol: 'report_id', sqlType: FieldType.INTEGER, isChildResult: true, isChildEdit: true, isFilter: true])
        init([id: 'c1_SymptomId', sqlCol: 'symptom_id', sqlType: FieldType.INTEGER, isChildResult: true, isChildEdit: true, isFilter: true])
//        init([id: 'c1_CountId',    sqlCol: 'count',     sqlType: FieldType.INTEGER, isChildResult: true, isChildEdit: true, isFilter: true])

//        init([id: 'OtherSymptoms',      sqlCol: 'other_symptoms',    sqlType: FieldType.TEXT,    isResult: true, isEdit: true, isFilter: true,
//            metaData: [ form: [ allowBlank: true ] ] ])

        // diagnoses data
        init([id: 'c2_ReportId', sqlCol: 'report_id', sqlType: FieldType.INTEGER, isChildResult: true, isChildEdit: true, isFilter: true])
        init([id: 'c2_DiagnosesId', sqlCol: 'diagnosis_id', sqlType: FieldType.INTEGER, isChildResult: true, isChildEdit: true, isFilter: true])
//        init([id: 'c2_CountId',      sqlCol: 'count',   sqlType: FieldType.INTEGER, isChildResult: true, isChildEdit: true, isFilter: true])

        init([id: 'Notes', sqlCol: 'notes', sqlType: FieldType.TEXT, isResult: true, isEdit: true, isFilter: true,
                metaData: [form: [xtype: 'textarea', height: 62, width: 200, allowBlank: true]]])

        init([id: 'UserId', sqlCol: 'user_id', sqlType: FieldType.INTEGER, isResult: true, isEdit: true, isFilter: true,
                metaData: [form: [xtype: 'hidden', allowBlank: true]]])
        init([id: 'CreateDate', sqlCol: 'create_date', sqlType: FieldType.DATE, isResult: true, isEdit: true, specialSql: 'now()',
                metaData: [form: [xtype: 'hidden', allowBlank: true], grid: [format: 'm-d-Y g:i:s A']]])
        init([id: 'ModifiedDate', sqlCol: 'modified_date', sqlType: FieldType.DATE, isResult: true, isEdit: true, isFilter: false,
                metaData: [form: [xtype: 'hidden', allowBlank: true]]])


        setDimensions(dimensionBeans)
        setBaseDetailsQuery('data_case_reports')
    }

    public void updateCompleteRecord(DbKeyValMap recordPks, CompleteRecord replacementRecord) {
        TableAwareQueryRecord pr = replacementRecord.getParentRecord();
        pr.getValues().put("ModifiedDate", new Time(new Date().getTime()));
        super.updateCompleteRecord(recordPks, replacementRecord);
    }

    public Map addCompleteRecord(CompleteRecord completeRecord, boolean ignoreSpecialSql) {
        TableAwareQueryRecord pr = completeRecord.getParentRecord();
        pr.getValues().put("ModifiedDate", new Time(new Date().getTime()));
        OEUser user = (OEUser) getAuthentication().getPrincipal();
        pr.getValues().put("UserId", user.getAttributes().get("Id"));
        return super.addCompleteRecord(completeRecord, ignoreSpecialSql);
    }
}
