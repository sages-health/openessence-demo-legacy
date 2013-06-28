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
import edu.jhuapl.openessence.groovy.GroovyOeDataEntrySource
import edu.jhuapl.openessence.security.SecuredDataSource

class TemperatureGroups_Entry extends GroovyOeDataEntrySource {

    Set roles = ['ROLE_ADMIN']

    TemperatureGroups_Entry() {
        setMetaData([grid: [ sortcolumn: 'TemperatureGroup', sortorder: 'asc' ],
                            menuCfg: [[parent: 'administration', src: 'OE.input.datasource.main', order: 14]],
                            displayDatasource: 'TemperatureGroups' ])

        setTableName('temperature_groups');
        addMasterTable([tableName: 'temperature_groups', pks: ['Id'] as HashSet])

        init([id: 'Id', sqlCol: 'id',          sqlType: FieldType.INTEGER, isResult: true, isFilter: true, isAutoGen: true, isEdit: true,
              metaData: [ form: [ xtype: 'hidden', allowBlank: true ] ] ])

        init([id: 'TemperatureGroup',   sqlCol: 'description', sqlType: FieldType.TEXT, isResult: true, isEdit: true,
            metaData: [ form: [ width: 200 ] ] ])

        init([id: 'Min_Inclusive',      sqlCol: 'min',         sqlType: FieldType.DOUBLE, isResult: true, isEdit: true,
            metaData: [ form: [ width: 100, minValue: 0, maxValue: 200, allowDecimals: true,  decimalPrecision: 3 ] ] ])

        init([id: 'Max_Inclusive',      sqlCol: 'max',         sqlType: FieldType.DOUBLE, isResult: true, isEdit: true,
            metaData: [ form: [ width: 100, minValue: 0, maxValue: 200, allowDecimals: true,  decimalPrecision: 3 ] ] ])

        init([id: 'Order',    sqlCol: 'order_id',  sqlType: FieldType.TEXT, isResult: true, isEdit: true,
            metaData: [ form: [ width: 100, allowBlank: true ] ] ])

        setDimensions(dimensionBeans)
        setBaseDetailsQuery('temperature_groups')
    }

}
