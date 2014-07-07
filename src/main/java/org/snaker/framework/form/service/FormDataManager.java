/*
 *  Copyright 2013-2014 the original author or authors.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.snaker.framework.form.service;

import org.apache.commons.lang.StringUtils;
import org.snaker.engine.helper.StringHelper;
import org.snaker.framework.form.dao.FormDataDao;
import org.snaker.framework.form.entity.DbTable;
import org.snaker.framework.form.entity.Field;
import org.snaker.framework.form.entity.FormData;
import org.snaker.framework.form.entity.SqlData;
import org.snaker.framework.security.shiro.ShiroUtils;
import org.snaker.framework.utils.ConvertUtils;
import org.snaker.modules.base.service.SnakerEngineFacets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 动态表单的数据管理
 * @author yuqs
 * @since 1.0
 */
@Component
public class FormDataManager {
    @Autowired
    private FormDataDao formDataDao;
    @Autowired
    private SnakerEngineFacets facets;
    public void save(FormData formData) {
        List<SqlData> sqlDatas = getSqlDatas(formData);
        formDataDao.save(sqlDatas);
        if(StringUtils.isNotEmpty(formData.getProcessId())) {
            if(StringUtils.isNotEmpty(formData.getOrderId()) && StringUtils.isNotEmpty(formData.getTaskId())) {
                facets.execute(formData.getTaskId(), ShiroUtils.getUsername(), null);
            } else {
                facets.startAndExecute(formData.getProcessId(), ShiroUtils.getUsername(), null);
            }
        }
    }

    private List<SqlData> getSqlDatas(FormData formData) {
        List<SqlData> sqlDatas = new ArrayList<SqlData>();
        Map<DbTable, Map<String, String>> fieldData = formData.getFieldData();
        Map<DbTable, Map<String, String[]>> subFieldData = formData.getSubFieldData();
        for(Map.Entry<DbTable, Map<String, String>> entry : fieldData.entrySet()) {
            DbTable table = entry.getKey();
            Map<String, String> data = entry.getValue();
            SqlData sqlData = new SqlData(table);
            StringBuilder sql = new StringBuilder();
            StringBuilder fieldNames = new StringBuilder();
            StringBuilder params = new StringBuilder();
            List values = new ArrayList();
            for(Field field : table.getFields()) {
                Object dbValue = getDbValue(formData, data.get(field.getName()), field);
                fieldNames.append(field.getName()).append(",");
                params.append("?,");
                values.add(dbValue);
            }
            sql.append(" INSERT INTO T_");
            sql.append(table.getName());
            sql.append(" (ID, ");
            sql.append(fieldNames.substring(0, fieldNames.length() - 1));
            sql.append(")");
            sql.append(" VALUES ('");
            sql.append(StringHelper.getPrimaryKey());
            sql.append("', ");
            sql.append(params.substring(0, params.length() - 1));
            sql.append(")");
            sqlData.setSql(sql.toString());
            sqlData.setTable(table);
            sqlData.setValues(values.toArray());
            sqlDatas.add(sqlData);
        }
        return sqlDatas;
    }

    private Object getDbValue(FormData formData, String uiValue, Field field) {
        Object dbValue = null;
        String type = field.getType();
        if(type == null) type = "1";//字符型
        try {
            int typeNum = Integer.parseInt(type);
            switch (typeNum) {
                case 1 ://字符型
                    dbValue = uiValue;
                    break;
                case 2 ://整数型
                    int length = field.getDataLength();
                    if(length < 10) {
                        dbValue = ConvertUtils.convertStringToObject(uiValue, Integer.class);
                    } else {
                        dbValue = ConvertUtils.convertStringToObject(uiValue, Long.class);
                    }
                    break;
                case 3 ://小数型
                    dbValue = ConvertUtils.convertStringToObject(uiValue, Double.class);
                    break;
                case 4 ://日期型
                    dbValue = ConvertUtils.convertStringToObject(uiValue, Date.class);
                    break;
                case 5 ://文本型
                    dbValue = uiValue;
                    break;
                default:
                    dbValue = uiValue;
                    break;
            }
        } catch(Exception e) {
            formData.addError(field.getName() + " 类型转换失败");
        }
        return dbValue;
    }
}