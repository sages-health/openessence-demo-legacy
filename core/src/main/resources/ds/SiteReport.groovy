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
import edu.jhuapl.openessence.datasource.jdbc.ResolutionHandler
import edu.jhuapl.openessence.datasource.jdbc.filter.sorting.OrderByFilter
import edu.jhuapl.openessence.datasource.jdbc.timeresolution.sql.pgsql.PgSqlDailyHandler
import edu.jhuapl.openessence.datasource.jdbc.timeresolution.sql.pgsql.PgSqlMonthlyHandler
import edu.jhuapl.openessence.datasource.jdbc.timeresolution.sql.pgsql.PgSqlWeeklyHandler
import edu.jhuapl.openessence.groovy.GroovyDataSource
import org.springframework.jdbc.core.RowCallbackHandler

/**
 * Gives number of reports per site per day
 */
public class SiteReport extends GroovyDataSource {

    public SiteReport() {

        DimensionBean d

        def menuCfg = [[parent: 'report', src: 'OE.report.datasource.main', order: 3]]

        metaData = [
                menuCfg: menuCfg,
                form: [supportMap: false, supportPivots: false],
                grid: [sortcolumn: 'Id', sortorder: 'desc']
        ]

        init([id: 'Id', sqlCol: 'dr.id', sqlType: FieldType.INTEGER, isResult: true])

        d = init([id: 'ReportDate', sqlCol: 'report_date', sqlType: FieldType.DATE, isResult: true, isFilter: true, isGrouping: true])
        d.resolutionHandlers = (Map<String, ResolutionHandler>) [
                'daily': new PgSqlDailyHandler(),
                'weekly': new PgSqlWeeklyHandler(0),
                'monthly': new PgSqlMonthlyHandler()
        ]

        d = init([id: 'accumId', sqlCol: '\'<na>\'', sqlType: FieldType.TEXT, isResult: false, isFilter: true,
                metaData: [form: [width: 300, xtype: 'superboxselect', value: 'TotalCount']]])

        setBaseDetailsQuery('''
  			data_aggregate_report_plus dr
  			left join districts on districts.id = dr.district
         ''')

        setDimensions(dimensionBeans)
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
        def otherDs = getCtx().getBean('Districts')
        def dj = new DimensionJoiner(this, [(otherDs): [otherDs.getResultDimension('Id').getDimensionBean()]]) {

            @Override
            protected DimensionBean onDimensionJoin(JdbcOeDataSource ds, DimensionBean otherDimension, Record r) {
                def bean = super.onDimensionJoin(ds, otherDimension, r)
                bean.id = 'district' + r.getValue('Id').toString()
                bean.displayName = r.getValue('Name')

                def value = r.getValue(otherDimension.getId())
                bean.setSqlColAlias('district' + value)

                bean.setSqlCol("SUM(CASE WHEN dr.district = '${value.toString()}' THEN 1 ELSE 0 END) AS ${bean.id}")
                bean.setIsAccumulation(true)

                return bean;
            }
        }

        setDimensionJoiner(dj)
    }

}
