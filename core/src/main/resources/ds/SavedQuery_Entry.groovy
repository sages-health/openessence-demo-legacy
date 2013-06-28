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
import edu.jhuapl.openessence.datasource.Filter
import edu.jhuapl.openessence.datasource.OeDataSourceException
import edu.jhuapl.openessence.datasource.QueryManipulationStore
import edu.jhuapl.openessence.datasource.entry.CompleteRecord
import edu.jhuapl.openessence.datasource.jdbc.DimensionBean
import edu.jhuapl.openessence.datasource.jdbc.filter.EqFilter
import edu.jhuapl.openessence.groovy.GroovyOeDataEntrySource
import edu.jhuapl.openessence.security.OEUser
import org.springframework.jdbc.core.RowCallbackHandler

class SavedQuery_Entry extends GroovyOeDataEntrySource {

    SavedQuery_Entry() {
        addChildTable([tableName: 'users',
                columns: ['WebUserId'],
                pks: ['WebUserId'] as HashSet,
                fksToParent: ['WebUserId': 'UserId']
        ])

        DimensionBean d

        // FIXME it seems that the PK must be called "Id"
        init([id: 'Id', sqlCol: 'query_id', sqlType: FieldType.INTEGER, isResult: true, isFilter: true, isEdit: true, isAutoGen: true,
                metaData: [grid: [width: 100]]])

        d = init([id: 'QueryName', sqlCol: 'query_name', sqlType: FieldType.TEXT, isResult: true, isFilter: true, isEdit: true, isChildEdit: false,
                metaData: [form: [xtype: 'multiselect']]])

        init([id: 'UserId', sqlCol: 'user_id', sqlType: FieldType.INTEGER, isFilter: true, isEdit: true])
        init([id: 'DataSource', sqlCol: 'data_source', sqlType: FieldType.TEXT, isFilter: true, isEdit: true])
        init([id: 'Parameters', sqlCol: 'parameters', sqlType: FieldType.TEXT, isResult: true, isFilter: true, isEdit: true])

        // child table
        init([id: 'WebUserId', sqlCol: 'id', sqlType: FieldType.INTEGER, isChildResult: true, isChildEdit: true, isFilter: true])

        setDimensions(dimensionBeans)

        setTableName('saved_queries')
        addMasterTable([tableName: 'saved_queries', pks: ['Id'] as HashSet])

        setBaseDetailsWhereClauses(null)
        setBaseDetailsQuery('saved_queries')
    }

    @Override
    public Map addCompleteRecord(CompleteRecord completeRecord, boolean ignoreSpecialSql) {
        // add user ID
        OEUser user = (OEUser) getAuthentication().getPrincipal();
        completeRecord.getParentRecord().getValues().put('UserId', user.getAttributes().get("Id"))
        return super.addCompleteRecord(completeRecord, ignoreSpecialSql);
    }

    @Override
    public void detailsQuery(QueryManipulationStore queryManipStore, RowCallbackHandler rcbh, Integer fzparm) throws OeDataSourceException {
        // Adds a filter for logged in user
        Collection<Filter> filters = queryManipStore.getWhereClauseFilters();
        OEUser user = (OEUser) getAuthentication().getPrincipal();
        filters.add(new EqFilter('UserId', user.getAttributes().get("Id")));
        queryManipStore.setWhereClauseFilters(filters);

        super.detailsQuery(queryManipStore, rcbh, fzparm);
    }
}
