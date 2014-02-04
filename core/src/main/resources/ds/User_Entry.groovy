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
import edu.jhuapl.openessence.groovy.GroovyOeDataEntrySource
import edu.jhuapl.openessence.security.OEPasswordEncoder
import edu.jhuapl.openessence.security.EncryptionDetails
import org.springframework.security.crypto.bcrypt.BCrypt
import java.security.SecureRandom

class User_Entry extends GroovyOeDataEntrySource {

    private static final String currentAlgo = "BCrypt"
    private static final int logRounds = 13;
    private static final SecureRandom secureRand = new SecureRandom();

    Set roles = ['ROLE_ADMIN']

    User_Entry() {
        DimensionBean d

        setMetaData([grid: [sortcolumn: 'UserName', sortorder: 'asc'],
                menuCfg: [[parent: 'administration', src: 'OE.input.datasource.main', order: 16]]])

        setTableName('users')
        addMasterTable([tableName: 'users', pks: ['Id'] as HashSet])

        addChildTable([tableName: 'user_role_mapping',
                columns: ['c1_UserId', 'c1_RoleId'],
                pks: ['c1_UserId', 'c1_RoleId'] as HashSet,
                fksToParent: ['c1_UserId': 'Id'],
                possibleValuesDsName: 'Role',
                possibleValuesDsFks: ['c1_RoleId': 'Id'],
                metaData: [form: [xtype: 'grid']]
        ])

        /* REQUIRED FIELDS - do not remove - values can be statically injected */
        init([id: 'Id', sqlCol: 'id', sqlType: FieldType.INTEGER, isResult: true, isEdit: true, isFilter: true, isAutoGen: true,
                metaData: [form: [xtype: 'hidden', allowBlank: true]]])
        init([id: 'UserName', sqlCol: 'name', sqlType: FieldType.TEXT, isResult: true, isEdit: true, isFilter: true])
        init([id: 'Password', sqlCol: 'password', sqlType: FieldType.TEXT, isResult: false, isEdit: true, isFilter: false,
                metaData: [form: [password: true]]])
        init([id: 'Salt', sqlCol: 'salt', sqlType: FieldType.TEXT, isResult: true, isEdit: true, isFilter: false,
                     metaData: [grid: [xtype: 'hidden'], form: [xtype: 'hidden', allowBlank: true]]] as HashMap)
        init([id: 'Algorithm', sqlCol: 'algorithm', sqlType: FieldType.TEXT, isResult: true, isEdit: true, isFilter: false,
                     metaData: [grid: [xtype: 'hidden'], form: [xtype: 'hidden', allowBlank: true]]] as HashMap)
        init([id: 'Enabled', sqlCol: 'enabled', sqlType: FieldType.BOOLEAN, isResult: true, isEdit: true, isFilter: true])
        init([id: 'NonExpired', sqlCol: 'non_expired', sqlType: FieldType.BOOLEAN, isResult: true, isEdit: true, isFilter: true,
                     metaData: [ form: [ checked: true, inputType: 'hidden' ] ] ])
        init([id: 'CredentialsNonExpired', sqlCol: 'credentials_non_expired', sqlType: FieldType.BOOLEAN, isResult: true, isEdit: true, isFilter: true,
                     metaData: [ form: [ checked: true, inputType: 'hidden' ] ] ])
        init([id: 'AccountNonLocked', sqlCol: 'account_non_locked', sqlType: FieldType.BOOLEAN, isResult: true, isEdit: true, isFilter: true,
                     metaData: [ form: [ checked: true, inputType: 'hidden' ] ] ])

        init([id: 'LastName', sqlCol: 'lastname', sqlType: FieldType.TEXT, isResult: true, isEdit: true, isFilter: true,
                metaData: [grid: [width: 120], form: [allowBlank: true]]])

        init([id: 'FirstName', sqlCol: 'firstname', sqlType: FieldType.TEXT, isResult: true, isEdit: true, isFilter: true,
                metaData: [grid: [width: 120], form: [allowBlank: true]]])

        init([id: 'RealName', sqlCol: 'realname', sqlType: FieldType.TEXT, isResult: true, isEdit: true, isFilter: false,
                metaData: [grid: [width: 120], form: [allowBlank: true]]])

        init([id: 'Organization', sqlCol: 'organization', sqlType: FieldType.TEXT, isResult: true, isEdit: true, isFilter: false,
                metaData: [grid: [width: 120], form: [allowBlank: true]]])

        init([id: 'Email', sqlCol: 'email', sqlType: FieldType.TEXT, isResult: true, isEdit: true, isFilter: false,
                metaData: [grid: [width: 120], form: [vtype: 'email', allowBlank: true]]])

        init([id: 'Telephone', sqlCol: 'telephone', sqlType: FieldType.TEXT, isResult: true, isEdit: true, isFilter: false,
                metaData: [grid: [width: 120], form: [allowBlank: true]]])

        // Child table columns
        init([id: 'c1_UserId', sqlCol: 'user_id', sqlType: FieldType.INTEGER, isChildResult: true, isChildEdit: true, isFilter: true])
        init([id: 'c1_RoleId', sqlCol: 'role_id', sqlType: FieldType.TEXT, isChildResult: true, isChildEdit: true, isFilter: true])

        setDimensions(dimensionBeans)
        /* must include users table containing username and password, others joined based on implementation */
        setBaseDetailsQuery('users')
    }

    private String makePwd(String pwd, String salt) {
        EncryptionDetails encryptDetails = new EncryptionDetails(salt, currentAlgo)
        new OEPasswordEncoder().encodePassword(pwd, encryptDetails)
    }

    @Override
    public void updateCompleteRecord(DbKeyValMap recordPks, CompleteRecord replacementRecord) {
        String newpwd = (String) replacementRecord.getParentRecord().getValue("Password")
        if (!getParentRecord(recordPks).getValue("Password").equals(newpwd)) {
            String salt = BCrypt.gensalt(logRounds, secureRand) //TODO make sure to change, if using different encryption
            replacementRecord.getParentRecord().getValues().put("Salt", salt)
            replacementRecord.getParentRecord().getValues().put("Password", makePwd(newpwd, salt));
            replacementRecord.getParentRecord().getValues().put("Algorithm", currentAlgo)
        }
        super.updateCompleteRecord(recordPks, replacementRecord);
    }

    @Override
    public Map addCompleteRecord(CompleteRecord completeRecord, boolean ignoreSpecialSql) {
        String newpwd = (String) completeRecord.getParentRecord().getValue("Password")
        String salt = BCrypt.gensalt(logRounds, secureRand) //TODO make sure to change, if using different encryption
        completeRecord.getParentRecord().getValues().put("Salt", salt)
        completeRecord.getParentRecord().getValues().put("Password", makePwd(newpwd, salt));
        completeRecord.getParentRecord().getValues().put("Algorithm", currentAlgo)
        return super.addCompleteRecord(completeRecord, ignoreSpecialSql);
    }
}
