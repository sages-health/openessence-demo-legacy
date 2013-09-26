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

package edu.jhuapl.openessence.controller;

import edu.jhuapl.openessence.datasource.Dimension;
import edu.jhuapl.openessence.datasource.OeDataSourceAccessException;
import edu.jhuapl.openessence.datasource.entry.ChildRecordSet;
import edu.jhuapl.openessence.datasource.entry.CompleteRecord;
import edu.jhuapl.openessence.datasource.entry.DbKeyValMap;
import edu.jhuapl.openessence.datasource.jdbc.JdbcOeDataSource;
import edu.jhuapl.openessence.datasource.jdbc.entry.JdbcOeDataEntrySource;
import edu.jhuapl.openessence.datasource.jdbc.entry.TableAwareQueryRecord;
import edu.jhuapl.openessence.model.DeleteRequest;
import edu.jhuapl.openessence.upload.FileImporter;
import edu.jhuapl.openessence.upload.FileImporterRegistry;
import edu.jhuapl.openessence.web.util.ControllerUtils;
import edu.jhuapl.openessence.web.util.ErrorMessageException;

import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;

@Controller
@RequestMapping("/input")
public class InputController extends OeController {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Inject
    private FileImporterRegistry fileImporters;

    /**
     * Add record to the database with the provided values. Response should be a JSON formatted collection with #success
     * and #record fields indicating status of request and generated keys (if appropriate) for new record.
     *
     * @param ds      data source to be updated
     * @param request request object containing parameters such as data source, field values, etc.
     */
    @RequestMapping(value = "/add", method = {POST, PUT}) // POST and PUT b/c we're very un-RESTful
    @ResponseBody
    public Map<String, Object> add(@RequestParam("dsId") JdbcOeDataSource ds, final HttpServletRequest request)
            throws ErrorMessageException, OeDataSourceAccessException, IOException {
        JdbcOeDataEntrySource jdes = (JdbcOeDataEntrySource) ds;
        Set<String> pks = jdes.getParentTableDetails().getPks();

        // get parent dimensions/values
        Map<String, Dimension> dimensions = new HashMap<String, Dimension>();
        Map<String, Object> values = new HashMap<String, Object>();
        for (Dimension dimension : jdes.getEditDimensions()) {
            dimensions.put(dimension.getId(), dimension);

            // Auto generated, special sql, and pk dimensions are not required on adds
            boolean isRequired = (jdes.getAutoGeneratedDimension(dimension.getId()) == null
                                  && jdes.getSpecialSqlDimension(dimension.getId()) == null
                                  && pks.contains(dimension.getId()));

            values.putAll(ControllerUtils.formatData(dimension.getId(), request.getParameter(dimension.getId()),
                                                     dimension.getSqlType(), isRequired));
        }

        CompleteRecord completeRecord = new CompleteRecord(new TableAwareQueryRecord(jdes.getTableName(), pks,
                                                                                     dimensions, values),
                                                           ControllerUtils.getChildRecordSets(jdes, request, true));

        return jdes.addCompleteRecord(completeRecord, false);
    }

    @RequestMapping(value = "/update", method = {POST, PUT})
    @ResponseBody
    public Map<String, Object> update(@RequestParam("dsId") JdbcOeDataSource ds, WebRequest request,
                               HttpServletRequest servletRequest)
            throws ErrorMessageException, OeDataSourceAccessException, IOException {
        JdbcOeDataEntrySource jdes = (JdbcOeDataEntrySource) ds;

        // find primary keys for record
        DbKeyValMap dbKeyValMap = ControllerUtils.parseKeyValueMap(jdes, request.getParameterMap());

        // retrieve existing record and children
        CompleteRecord
                completeRecord =
                jdes.getCompleteRecord(dbKeyValMap, new ArrayList<String>(jdes.getChildTableMap().keySet()));

        // Option to only update parameter values on the completeRecord that
        // are included as part of the request (when merge parameter is true)
        // Defaults to false (nullify parameter values not included on request)
        boolean merge = Boolean.valueOf(request.getParameter("merge")).booleanValue();

        // parent record's values are replaced with request param values
        for (String field : completeRecord.getParentRecord().getValues().keySet()) {
            String parameter = request.getParameter(field);
            TableAwareQueryRecord parentRecord = completeRecord.getParentRecord();
            if (parameter != null) {
                parentRecord.getValues().putAll(ControllerUtils.formatData(field, parameter,
                                                                           parentRecord.getEditDimensions().get(field)
                                                                                   .getSqlType(),
                                                                           dbKeyValMap.keySet().contains(field)));
            } else if (merge == false) {
                // NEEDS additional flags for data sources using default input panels
                // nullify parameter values on the complete record, if it is an edit dimension
                parentRecord.getValues().put(field, null);
            }
        }

        // remove existing children
        for (ChildRecordSet childRecordSet : completeRecord.getChildrenRecordSets()) {
            childRecordSet.removeAllChildRecords();
        }

        completeRecord.setChildrenRecordSets(ControllerUtils.getChildRecordSets(jdes, servletRequest, false));
        jdes.updateCompleteRecord(dbKeyValMap, completeRecord);

        Map<String, Object> data = data(ds, request);// new HashMap<String, Object>();
        data.put("success", true);
        return data; // TODO return RESTful response, i.e. data actually updated
    }

    @RequestMapping(value = "/data", method = GET)
    @ResponseBody
    public Map<String, Object> data(@RequestParam("dsId") JdbcOeDataSource ds, WebRequest request)
            throws ErrorMessageException, OeDataSourceAccessException {
        JdbcOeDataEntrySource jdes = (JdbcOeDataEntrySource) ds;
        DbKeyValMap dbKeyValMap = new DbKeyValMap();
        String doNotParseKeys = request.getParameter("doNotParseKeys");
        if (doNotParseKeys == null || !doNotParseKeys.equalsIgnoreCase("true")) {
            dbKeyValMap = ControllerUtils.parseKeyValueMap(jdes, request.getParameterMap());
        }
        // retrieve existing record and children
        CompleteRecord completeRecord = jdes.getCompleteRecord(dbKeyValMap,
                                                               new ArrayList<String>(jdes.getChildTableMap().keySet()));

        Map<String, Object> data = ControllerUtils.mapDataAndFormatTimeForResponse(completeRecord.getParentRecord()
                                                                                           .getValues().keySet(),
                                                                                   completeRecord.getParentRecord()
                                                                                           .getValues());

        // Children
        for (ChildRecordSet childRecordSet : completeRecord.getChildrenRecordSets()) {
            List<Object> childRecords = new ArrayList<Object>();
            for (TableAwareQueryRecord tableAwareQueryRecord : childRecordSet.getChildRecords()) {
                childRecords.add(ControllerUtils
                                         .mapDataAndFormatTimeForResponse(tableAwareQueryRecord.getValues().keySet(),
                                                                          tableAwareQueryRecord.getValues()));
            }
            data.put(childRecordSet.getChildTableName(), childRecords);
        }

        return data;
    }

    /**
     * @param ds   data source ID sent on URL
     * @param body JSON POST body
     */
    @RequestMapping(value = "/delete",
                    // FIXME this should obviously be DELETE, but we send an entity body
                    method = POST)
    @ResponseBody
    public Map<String, Object> delete(@RequestParam("dsId") JdbcOeDataSource ds, @RequestBody DeleteRequest body)
            throws IOException, ErrorMessageException, OeDataSourceAccessException {
        JdbcOeDataEntrySource jdes = (JdbcOeDataEntrySource) ds;
        List<DbKeyValMap> pksForDeletion = new ArrayList<DbKeyValMap>();

        for (Map<String, String> pks : body.getPkIds()) {
            pksForDeletion.add(ControllerUtils.parseKeyValueMap(jdes, pks));
        }

        jdes.deleteQueryRecords(jdes.getTableName(), pksForDeletion);

        // Build/write response
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("success", true);
        return data; // TODO return RESTful response, i.e. data actually deleted
    }

    @RequestMapping(value = "/importExcel", method = POST)
    public void importExcel(@RequestPart MultipartFile file, @RequestParam("dsId") JdbcOeDataSource ds,
                            HttpServletResponse response)
            throws IOException, ServletException {

        ObjectMapper mapper = new ObjectMapper();
        try {
            // Ext needs this for the crazy way it does file uploads
            // it's normally bad to manually write JSON, but dealing with a custom Spring MessageConverter seems like overkill
            response.setContentType("text/html;charset=utf-8");
            FileImporter<?> importer = fileImporters.get(ds);
            if (importer == null) {
                log.error("No file importer configured for data source {}", ds.getDataSourceId());
                throw new IllegalArgumentException("No file importer configured");
            }
            response.getWriter().write(mapper.writeValueAsString(importer.importFile(file)));
        } catch (Exception e) {
            // respond to exception as normal, but with content type text/html
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(mapper.writeValueAsString(handleException(e)));
        }

    }
}
