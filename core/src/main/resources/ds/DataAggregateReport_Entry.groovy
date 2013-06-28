import edu.jhuapl.openessence.datasource.FieldType
import edu.jhuapl.openessence.datasource.Record
import edu.jhuapl.openessence.datasource.entry.CompleteRecord
import edu.jhuapl.openessence.datasource.entry.DbKeyValMap
import edu.jhuapl.openessence.datasource.jdbc.DimensionBean
import edu.jhuapl.openessence.datasource.jdbc.DimensionJoiner
import edu.jhuapl.openessence.datasource.jdbc.JdbcOeDataSource
import edu.jhuapl.openessence.datasource.jdbc.entry.TableAwareQueryRecord
import edu.jhuapl.openessence.groovy.GroovyOeDataEntrySource
import edu.jhuapl.openessence.security.OEUser

import java.sql.Time

class DataAggregateReport_Entry extends GroovyOeDataEntrySource {

    Set roles = ['ROLE_USER']

    DataAggregateReport_Entry() {
        DimensionBean d

        setMetaData([
                menuCfg: [[parent: 'input', src: 'OE.input.datasource.main', order: 2]],
                displayDatasource: 'DataAggregateReportEntryGrid',
                form: []
        ])

        setTableName('data_aggregate_reports')
        addMasterTable([tableName: 'data_aggregate_reports', pks: ['Id'] as HashSet])

        addChildTable([tableName: 'report_symptoms',        //child mapping table 
                columns: ['c1_ReportId', 'c1_SymptomId', 'c1_SymptomCount'],       //dimensionIds of mapping
                pks: ['c1_ReportId', 'c1_SymptomId'] as HashSet,//primary keys (dimensionIds) of mapping
                fksToParent: ['c1_ReportId': 'Id'],             //foreign key to parent Datasource
//            possibleValuesDsName: 'ASymptoms',               //Datasource to get values
                possibleValuesDsFks: ['c1_SymptomId': 'Id'],    //foreign key to child/value Datasource
                metaData: [form: [width: 400, xtype: 'categorygridfield', allowBlank: true, minValue: 0, allowDecimals: false, allowNegative: false,
                        matchTerm: 'symptom',
                        categoryIdField: 'c1_SymptomId',
                        categoryValueField: 'c1_SymptomCount',
                        reportIdField: 'c1_ReportId'
                ]]
        ])

        addChildTable([tableName: 'report_diagnoses',        //child mapping table 
                columns: ['c2_ReportId', 'c2_DiagnosesId', 'c2_DiagnosesCount'],       //dimensionIds of mapping
                pks: ['c2_ReportId', 'c2_DiagnosesId'] as HashSet,//primary keys (dimensionIds) of mapping
                fksToParent: ['c2_ReportId': 'Id'],             //foreign key to parent Datasource
//            possibleValuesDsName: 'ADiagnoses',               //Datasource to get values
                possibleValuesDsFks: ['c2_DiagnosesId': 'Id'],    //foreign key to child/value Datasource
                metaData: [
                        form: [
                                width: 400,
                                xtype: 'categorygridfield',
                                allowBlank: true,
                                minValue: 0,
                                allowDecimals: false,
                                allowNegative: false,
                                matchTerm: 'diagnosis',
                                categoryIdField: 'c2_DiagnosesId',
                                categoryValueField: 'c2_DiagnosesCount',
                                reportIdField: 'c2_ReportId'
                        ]]
        ])

        init([id: 'Id', sqlCol: 'id', sqlType: FieldType.INTEGER, isResult: true, isEdit: true, isFilter: true, isAutoGen: true,
                metaData: [form: [xtype: 'hidden', allowBlank: true]]])

        init([id: 'ReportDate', sqlCol: 'report_date', sqlType: FieldType.DATE, isResult: true, isEdit: true, isFilter: true,
                metaData: [form: [allowBlank: false]]])

        d = init([id: 'District', sqlCol: 'district', sqlType: FieldType.TEXT, isResult: true, isEdit: true, isFilter: true,
                metaData: [form: [allowBlank: false, sortcolumn: 'Order']]])
        d.possibleValuesDsName = 'Districts'

        // symptoms data
        init([id: 'c1_ReportId', sqlCol: 'report_id', sqlType: FieldType.INTEGER, isChildResult: true, isChildEdit: true, isFilter: true])
        init([id: 'c1_SymptomId', sqlCol: 'symptom_id', sqlType: FieldType.INTEGER, isChildResult: true, isChildEdit: true, isFilter: true])
        init([id: 'c1_SymptomCount', sqlCol: 'count', sqlType: FieldType.INTEGER, isResult: false, isChildEdit: true, isFilter: true])

        // diagnoses data
        init([id: 'c2_ReportId', sqlCol: 'report_id', sqlType: FieldType.INTEGER, isChildResult: true, isChildEdit: true, isFilter: true])
        init([id: 'c2_DiagnosesId', sqlCol: 'diagnosis_id', sqlType: FieldType.INTEGER, isChildResult: true, isChildEdit: true, isFilter: true])
        init([id: 'c2_DiagnosesCount', sqlCol: 'count', sqlType: FieldType.INTEGER, isResult: false, isChildEdit: true, isFilter: true])

        init([id: 'Notes', sqlCol: 'notes', sqlType: FieldType.TEXT, isResult: true, isEdit: true, isFilter: true,
                metaData: [form: [xtype: 'textarea', height: 62, width: 200, allowBlank: true]]])

        init([id: 'UserId', sqlCol: 'user_id', sqlType: FieldType.INTEGER, isResult: true, isEdit: true, isFilter: true,
                metaData: [form: [xtype: 'hidden', allowBlank: true]]])

        init([id: 'CreateDate', sqlCol: 'create_date', sqlType: FieldType.DATE, isResult: true, isEdit: true, specialSql: 'now()',
                metaData: [form: [xtype: 'hidden', allowBlank: true], grid: [format: 'm-d-Y g:i:s A']]])

        init([id: 'ModifiedDate', sqlCol: 'modified_date', sqlType: FieldType.DATE, isResult: true, isEdit: true, isFilter: false,
                metaData: [form: [xtype: 'hidden', allowBlank: true]]])

        setDimensions(dimensionBeans)
        setBaseDetailsQuery('data_aggregate_reports')
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet()
        def symDs = getCtx().getBean('ASymptoms')
        def diagDs = getCtx().getBean('ADiagnoses')
        def dj = new DimensionJoiner(this, [
                (symDs): [symDs.getResultDimension('Id').getDimensionBean()],
                (diagDs): [diagDs.getResultDimension('Id').getDimensionBean()]
        ]) {

            @Override
            protected DimensionBean onDimensionJoin(JdbcOeDataSource ds, DimensionBean otherDimension, Record r) {
                def bean = super.onDimensionJoin(ds, otherDimension, r)
                bean.displayName = r.getValue('Name')
                bean.setIsChildEdit(true)
                def value = r.getValue(otherDimension.getId())

                if (symDs == ds) {
                    bean.id = 'symptom' + r.getValue('Id').toString()
                    def alias = 'symptom' + value
                    bean.setSqlColAlias(alias)
                    // ELSE NULL since 0 values are different from no values
                    bean.setSqlCol("SUM(CASE WHEN symptom_id = ${value.toString()} THEN count ELSE NULL END) as ${alias}")
                } else if (diagDs == ds) {
                    bean.id = 'diagnosis' + r.getValue('Id').toString()
                    def alias = 'diagnosis' + value
                    bean.setSqlColAlias(alias)
                    // ELSE NULL since 0 values are different from no values
                    bean.setSqlCol("SUM(CASE WHEN diagnosis_id = ${value.toString()} THEN count ELSE NULL END) as ${alias}")
                }
                return bean;
            }
        }

        setDimensionJoiner(dj)
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
