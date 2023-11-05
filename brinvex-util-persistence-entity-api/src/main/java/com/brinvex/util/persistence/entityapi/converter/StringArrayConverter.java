/*
 * Copyright © 2023 Brinvex (dev@brinvex.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.brinvex.util.persistence.entityapi.converter;

import jakarta.persistence.AttributeConverter;

public class StringArrayConverter implements AttributeConverter<String[], String> {

    public StringArrayConverter() {
    }

    public String convertToDatabaseColumn(String[] elements) {
        return elements == null ? null : elements.length == 0 ? null : String.join(",", elements);
    }

    public String[] convertToEntityAttribute(String dbData) {
        return dbData == null ? null : dbData.split(",");
    }
}