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
import edu.jhuapl.openessence.groovy.GroovyDataSource

class ADiagnoses extends GroovyDataSource {

    ADiagnoses() {

        setMetaData([grid: [sortcolumn: 'Order', sortorder: 'asc']])

        init([id: 'Id', sqlCol: 'id', sqlType: FieldType.INTEGER, isResult: true, isFilter: true,
                metaData: [grid: [width: 100]]])

        init([id: 'Name', sqlCol: 'name', sqlType: FieldType.TEXT, isResult: true, isFilter: true,
                metaData: [grid: [width: 300]]])

        init([id: 'Order', sqlCol: 'order_id', sqlType: FieldType.INTEGER, isResult: true,
                metaData: [grid: [width: 100]]])

        setDimensions(dimensionBeans)
        setBaseDetailsQuery('diagnoses')
    }
}
