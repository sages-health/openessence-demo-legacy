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

import edu.jhuapl.openessence.groovy.GroovyOeDataEntrySource
import edu.jhuapl.openessence.datasource.jdbc.DimensionBean
import edu.jhuapl.openessence.datasource.FieldType
import edu.jhuapl.openessence.datasource.map.GisMapDataSource

public class DistrictDetection_Entry extends GroovyOeDataEntrySource {

    DistrictDetection_Entry() {
        addMasterTable([tableName: 'district_detection', pks: ['RequestId', 'DistrictId'] as HashSet,
                mapRequestId: 'RequestId', sequence: 'district_detection_request_id_seq'])

        init([id: 'RequestId', sqlCol: 'request_id', sqlType: FieldType.INTEGER, isResult: true,  isEdit: true, isFilter: true ])
		init([id: 'TimeRequested', sqlCol: 'time_requested', sqlType: FieldType.DATE, isResult: false, isEdit: true ])
        init([id: 'District', sqlCol: 'district_id', sqlType: FieldType.TEXT, isResult: true, isEdit: true, isFilter: true ])
        init([id: 'Count', sqlCol: 'count', sqlType: FieldType.INTEGER, isResult: true, isEdit: true, isFilter: true ])
          
        setDimensions(dimensionBeans)
        setBaseDetailsQuery('district_detection')
    }
}
