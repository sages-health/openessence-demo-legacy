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
import edu.jhuapl.openessence.datasource.jdbc.DimensionBean
import edu.jhuapl.openessence.datasource.jdbc.filter.sorting.OrderByFilter
import edu.jhuapl.openessence.datasource.jdbc.timeresolution.sql.pgsql.PgSqlDailyHandler
import edu.jhuapl.openessence.datasource.jdbc.timeresolution.sql.pgsql.PgSqlMonthlyHandler
import edu.jhuapl.openessence.datasource.jdbc.timeresolution.sql.pgsql.PgSqlWeeklyHandler
import edu.jhuapl.openessence.groovy.GroovyDataSource
import org.springframework.jdbc.core.RowCallbackHandler

class DataLatency extends GroovyDataSource {

    DataLatency() {

        DimensionBean d

        metaData = [
                form: [
                        supportMap: false,
                        supportPivots: false,
                        // savedQueries property is passed to GridPanel constructor
                        savedQueries: [collapsed: true]
                ],
                menuCfg: [[parent: 'report', src: 'OE.report.datasource.main', order: 3, allowMultiple: false]],
                grid: [width: 80, sortcolumn: 'Id', sortorder: 'desc']
        ]

        init([id: 'Id', sqlCol: 'distinct(dr.id) as datareport', sqlColAlias: 'datareport', sqlType: FieldType.INTEGER, isResult: true, isFilter: false,
                metaData: [grid: [width: 80]]])

        d = init([id: 'District', sqlCol: 'district', sqlType: FieldType.TEXT, isResult: true, isFilter: true, isGrouping: true,
                metaData: [form: [width: 300, xtype: 'superboxselect', sortcolumn: 'Order']]])
        d.possibleValuesDsName = 'Districts'

        d = init([id: 'ReportDate', sqlCol: 'dr.report_date', sqlType: FieldType.DATE, isResult: true, isFilter: true, isGrouping: true,
                metaData: [grid: [width: 100]]])
        d.resolutionHandlers = [
                'daily': new PgSqlDailyHandler(),
                'weekly': new PgSqlWeeklyHandler(0),
                'monthly': new PgSqlMonthlyHandler()
        ]

        init([id: 'CreateDate', sqlCol: 'dr.create_date', sqlType: FieldType.DATE, isResult: true,
                metaData: [grid: [width: 100, format: 'm-d-Y g:i:s A']]])

        init([id: 'ModifiedDate', sqlCol: 'dr.modified_date', sqlType: FieldType.DATE, isResult: true,
                metaData: [grid: [width: 100]]])

        init([id: 'UserId', sqlCol: 'dr.user_id', sqlType: FieldType.INTEGER, isResult: false, isFilter: false,
                metaData: [form: [xtype: 'hidden', allowBlank: true]]])

        // Accumulations
        init([id: 'accumId', sqlCol: '\'<na>\'', sqlType: FieldType.TEXT, isResult: false, isFilter: true,
                metaData: [form: [width: 300, xtype: 'multiselect']]])

        // latency in days
        init([id: 'AvgCreateDateLatency',
                sqlCol: 'round(avg(abs(extract(epoch from create_date - report_date)/86400)))::int as createdatelatency',
                sqlColAlias: 'createdatelatency', sqlType: FieldType.INTEGER, isResult: true, isAccumulation: true])

        init([id: 'AvgModifiedDateLatency',
                sqlCol: 'round(avg(abs(extract(epoch from modified_date - report_date)/86400)))::int as modifieddatelatency',
                sqlColAlias: 'modifieddatelatency', sqlType: FieldType.INTEGER, isResult: true, isAccumulation: true])

        init([id: 'MinCreateDateLatency',
                sqlCol: 'min(abs(extract(epoch from create_date - report_date)/86400))::int as mincreatedatelatency',
                sqlColAlias: 'mincreatedatelatency', sqlType: FieldType.INTEGER, isResult: true, isAccumulation: true])

        init([id: 'MinModifiedDateLatency',
                sqlCol: 'min(abs(extract(epoch from modified_date - report_date)/86400))::int as minmodifieddatelatency',
                sqlColAlias: 'minmodifieddatelatency', sqlType: FieldType.INTEGER, isResult: true, isAccumulation: true])

        init([id: 'MaxCreateDateLatency',
                sqlCol: 'max(abs(extract(epoch from create_date - report_date)/86400))::int as maxcreatedatelatency',
                sqlColAlias: 'maxcreatedatelatency', sqlType: FieldType.INTEGER, isResult: true, isAccumulation: true])

        init([id: 'MaxModifiedDateLatency',
                sqlCol: 'max(abs(extract(epoch from modified_date - report_date)/86400))::int as maxmodifieddatelatency',
                sqlColAlias: 'maxmodifieddatelatency', sqlType: FieldType.INTEGER, isResult: true, isAccumulation: true])

        setDimensions(dimensionBeans)
        setBaseDetailsQuery('''data_aggregate_report_plus dr''')

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

}
