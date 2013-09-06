
//*****************************************************************************/
/*                              COPYRIGHT NOTICE                              */
/* Copyright (c) 2010 The Johns Hopkins University/Applied Physics Laboratory */
/*                            All rights reserved.                            */
/*                                                                            */
/* This material may only be used, modified, or reproduced by or for the      */
/* U.S. Government pursuant to the license rights granted under FAR clause    */
/* 52.227-14 or DFARS clauses 252.227-7013/7014.                              */
/*                                                                            */
/* For any other permissions, please contact the Legal Office at JHU/APL.     */
//*****************************************************************************/

import edu.jhuapl.openessence.datasource.FieldType
import edu.jhuapl.openessence.datasource.entry.CompleteRecord
import edu.jhuapl.openessence.datasource.entry.DbKeyValMap
import edu.jhuapl.openessence.datasource.jdbc.DimensionBean
import edu.jhuapl.openessence.datasource.jdbc.entry.TableAwareQueryRecord
import edu.jhuapl.openessence.groovy.GroovyOeDataEntrySource
import edu.jhuapl.openessence.security.OEUser
import java.sql.Time

class DataDistrictVisitsReport_Entry extends GroovyOeDataEntrySource {
    
    Set roles = ['ROLE_USER']
    
    DataDistrictVisitsReport_Entry() {
        DimensionBean d
        
        def menuCfg = [[parent: 'input', src: 'OE.input.datasource.main', order: 3]]
	
        setTableName('district_visits_reports')
        addMasterTable([tableName: 'district_visits_reports', pks: ['Id'] as HashSet] as HashMap)
        addChildTable( [tableName: 'district_visits',
                columns: ['ReportId', 'PatientId','ReturnVisit', 'Sex', 'Age', 'Weight', 'Bp_Systolic', 'Bp_Diastolic', 'Pulse', 'Temperature', 'DiagnosesId'],
                    pks: ['ReportId', 'PatientId', 'DiagnosesId' ] as HashSet,
            fksToParent: ['ReportId': 'Id'],
			possibleValuesDsName: 'DataDistrictVisits_Entry',
			metaData: [ form: [ xtype: 'editorgridfield', uploadEnabled : true,
								uploadConfig : [ fileType : 'csv', rowsToSkip : 1],
								allowBlank: false, sortcolumn:'PatientId', sortorder: 'asc'] ]
        ] as HashMap)

        setMetaData([
			menuCfg: menuCfg,
            displayDatasource: 'DataDistrictVisitsReportEntryGrid',
            form: []
		])
        
		init([id: 'Id', sqlCol: 'id', sqlType: FieldType.INTEGER, isResult: true, isEdit: true, isFilter: true, isAutoGen: true,
                metaData: [form: [xtype: 'hidden', allowBlank: true]]])

        d = init([id: 'District', sqlCol: 'district', sqlType: FieldType.TEXT, isResult: true, isEdit: true, isFilter: true,
                metaData: [form: [allowBlank: false, sortcolumn: 'Order']]])
        d.possibleValuesDsName = 'Districts'

		init([id: 'ReportDate', sqlCol: 'report_date', sqlType: FieldType.DATE, isResult: true, isEdit: true, isFilter: true,
			metaData: [form: [allowBlank: false]]])

        
		init([id: 'Notes', sqlCol: 'notes', sqlType: FieldType.TEXT, isResult: true, isEdit: true, isFilter: true,
                metaData: [form: [xtype: 'textarea', height: 62, width: 200, allowBlank: true]]])

        init([id: 'UserId', sqlCol: 'user_id', sqlType: FieldType.INTEGER, isResult: true, isEdit: true, isFilter: true,
                metaData: [form: [xtype: 'hidden', allowBlank: true]]])
        init([id: 'CreateDate', sqlCol: 'create_date', sqlType: FieldType.DATE, isResult: true, isEdit: true, specialSql: 'now()',
                metaData: [form: [xtype: 'hidden', allowBlank: true], grid: [format: 'm-d-Y g:i:s A']]])
        init([id: 'ModifiedDate', sqlCol: 'modified_date', sqlType: FieldType.DATE, isResult: true, isEdit: true, isFilter: false,
                metaData: [form: [xtype: 'hidden', allowBlank: true]]])
		
		// Grid fields 
		init([id: 'ReportId', sqlCol: 'report_id', sqlType: FieldType.INTEGER,  isChildResult: true, isChildEdit: true, isFilter: true])
		init([id: 'PatientId', sqlCol: 'patient_id', sqlType: FieldType.TEXT,  isChildResult: true, isChildEdit: true, isFilter: true])
		init([id: 'ReturnVisit', sqlCol: 'return_visit', sqlType: FieldType.BOOLEAN,  isChildResult: true, isChildEdit: true, isFilter: true])
		init([id: 'Sex', sqlCol: 'sex', sqlType: FieldType.TEXT,  isChildResult: true, isChildEdit: true, isFilter: true])
		init([id: 'Age', sqlCol: 'age', sqlType: FieldType.INTEGER,  isChildResult: true, isChildEdit: true, isFilter: true])
		init([id: 'Weight', sqlCol: 'weight', sqlType: FieldType.DOUBLE,  isChildResult: true, isChildEdit: true, isFilter: true])
		init([id: 'Bp_Systolic', sqlCol: 'bp_systolic', sqlType: FieldType.DOUBLE,  isChildResult: true, isChildEdit: true, isFilter: true])
		init([id: 'Bp_Diastolic', sqlCol: 'bp_diastolic', sqlType: FieldType.DOUBLE,  isChildResult: true, isChildEdit: true, isFilter: true])
		init([id: 'Pulse', sqlCol: 'pulse', sqlType: FieldType.INTEGER,  isChildResult: true, isChildEdit: true, isFilter: true])
		init([id: 'Temperature', sqlCol: 'temperature', sqlType: FieldType.DOUBLE,  isChildResult: true, isChildEdit: true, isFilter: true])
		init([id: 'DiagnosesId', sqlCol: 'diagnoses_id', sqlType: FieldType.INTEGER,  isChildResult: true, isChildEdit: true, isFilter: true])
		
        setDimensions(dimensionBeans)
        setBaseDetailsQuery('district_visits_reports')
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
