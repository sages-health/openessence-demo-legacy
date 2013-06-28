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
import edu.jhuapl.openessence.datasource.OeDataSourceException
import edu.jhuapl.openessence.datasource.QueryManipulationStore
import edu.jhuapl.openessence.datasource.Record
import edu.jhuapl.openessence.datasource.jdbc.DimensionBean
import edu.jhuapl.openessence.datasource.jdbc.DimensionJoiner
import edu.jhuapl.openessence.datasource.jdbc.JdbcOeDataSource
import edu.jhuapl.openessence.datasource.jdbc.filter.sorting.OrderByFilter
import edu.jhuapl.openessence.datasource.jdbc.timeresolution.sql.pgsql.PgSqlDailyHandler
import edu.jhuapl.openessence.datasource.jdbc.timeresolution.sql.pgsql.PgSqlMonthlyHandler
import edu.jhuapl.openessence.datasource.jdbc.timeresolution.sql.pgsql.PgSqlWeeklyHandler
import edu.jhuapl.openessence.groovy.GroovyDataSource
import org.springframework.jdbc.core.RowCallbackHandler

class DataAggregateReport extends GroovyDataSource {

    Set roles = ['ROLE_USER']

    DataAggregateReport() {
        DimensionBean d

        def menuCfg = [[parent: 'report', src: 'OE.report.datasource.main', order: 2]]

        setMetaData([
                form: [supportMap: false, supportPivots: false],
                menuCfg: menuCfg,
                grid: [width: 200, sortcolumn: 'Id', sortorder: 'desc']
        ])

        init([id: 'Id', sqlCol: 'distinct dr.id as datareport', sqlColAlias: 'datareport', sqlType: FieldType.INTEGER, isResult: true, isFilter: false,
                metaData: [grid: [width: 80]]])

        d = init([id: 'ReportDate', sqlCol: 'dr.report_date', sqlType: FieldType.DATE, isResult: true, isFilter: true, isGrouping: true,
                metaData: [grid: [width: 100]]])
        d.resolutionHandlers = [
                'daily': new PgSqlDailyHandler(),
                'weekly': new PgSqlWeeklyHandler(0),
                'monthly': new PgSqlMonthlyHandler()
        ]

        d = init([id: 'District', sqlCol: 'district', sqlType: FieldType.TEXT, isResult: true, isFilter: true, isGrouping: true,
                metaData: [form: [width: 300, xtype: 'superboxselect', sortcolumn: 'Order']]])
        d.possibleValuesDsName = 'Districts'

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

        d = init([id: 'timeseriesDetectorClass', sqlCol: '\'<na>\'', sqlType: FieldType.TEXT, isResult: false, isFilter: true])
        d.possibleValuesDsData = [['edu.jhuapl.bsp.detector.EarsC1', 'CDC-C1'],
                ['edu.jhuapl.bsp.detector.EarsC2', 'CDC-C2'],
                ['edu.jhuapl.bsp.detector.EarsC3', 'CDC-C3'],
                ['edu.jhuapl.bsp.detector.CusumSagesDetector', 'CUSUM SAGES'],
                ['edu.jhuapl.bsp.detector.EWMASagesDetector', 'EWMA SAGES'],
                ['edu.jhuapl.bsp.detector.GSSages', 'GS SAGES']
        ]

        // Symptoms Filters
        d = init([id: 'Symptom1ID', sqlCol: 'rs1.symptom_id', sqlType: FieldType.INTEGER, isResult: false, isFilter: false,
                metaData: [form: [xtype: 'superboxselect', width: 300]]])
        d.possibleValuesDsName = 'Symptoms'

        // Accumulations
        init([id: 'accumId', sqlCol: '\'<na>\'', sqlType: FieldType.TEXT, isResult: false, isFilter: true,
                metaData: [form: [xtype: 'superboxselect', width: 300]]])
        // Accumulation result dimensions
        init([id: 'Cases', sqlCol: 'sum ( rs1.count ) as count', sqlColAlias: 'count', sqlType: FieldType.INTEGER, isResult: true, isAccumulation: true,
                metaData: [grid: [hidden: false]]])

        init([id: 'UserId', sqlCol: 'dr.user_id', sqlType: FieldType.INTEGER, isResult: false, isFilter: false,
                metaData: [form: [xtype: 'hidden', allowBlank: true]]])

        //note to see all phones change the "join phones p" to a "left join phones p"
        setDimensions(dimensionBeans)
        setBaseDetailsQuery('''
                        data_aggregate_report_plus dr

                        left join report_symptoms rs1     on rs1.report_id = dr.id
                        left join symptoms sym1           on sym1.id       = rs1.symptom_id
                  ''')
    }


    @Override
    public void detailsQuery(QueryManipulationStore queryManipStore, RowCallbackHandler rcbh, Integer fzparm) throws OeDataSourceException {
        List<OrderByFilter> filters = queryManipStore.getOrderByFilters();
        //custom filter in datasource due to postgres specific sql
        if (filters.size() > 0) {
            OrderByFilter f = filters.remove(0);
            filters.add(new OrderByFilter(f.getFilterId(), f.getOperator()) {

                @Override
                public String getSqlSnippet(String colName) {
                    return colName + " " + this.getOperator() + "  NULLS LAST ";
                }

            });
            queryManipStore.setOrderByFilters(filters);
        }
        super.detailsQuery(queryManipStore, rcbh, fzparm);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet()
        def symDs = getCtx().getBean('ASymptoms')
        def dj = new DimensionJoiner(this, [(symDs): [symDs.getResultDimension('Id').getDimensionBean()]]) {

            @Override
            protected DimensionBean onDimensionJoin(JdbcOeDataSource ds, DimensionBean otherDimension, Record r) {
                def bean = super.onDimensionJoin(ds, otherDimension, r)

                bean.id = 'symptom' + r.getValue('Id').toString()
                bean.displayName = r.getValue('Name')
                bean.isAccumulation = true

                def value = r.getValue(otherDimension.getId())
                def alias = 'symptom' + value
                bean.setSqlColAlias(alias)
                // ELSE NULL since 0 values are different from no values
                bean.setSqlCol("SUM(CASE WHEN rs1.symptom_id = ${value.toString()} THEN rs1.count ELSE NULL END) as ${alias}")
                bean.setIsChildEdit(true)

                return bean;
            }
        }

        setDimensionJoiner(dj)
    }
}
