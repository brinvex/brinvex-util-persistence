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
package com.brinvex.util.persistence.api;

import java.lang.reflect.InvocationTargetException;
import java.util.ServiceLoader;

public enum EntityDaoSupportFactory {

    INSTANCE;

    private EntityDaoSupport entityDaoSupport;

    public EntityDaoSupport getEntityDaoSupport() {
        if (entityDaoSupport == null) {
            ServiceLoader<EntityDaoSupport> loader = ServiceLoader.load(EntityDaoSupport.class);
            for (EntityDaoSupport provider : loader) {
                this.entityDaoSupport = provider;
                break;
            }
        }
        if (entityDaoSupport != null) {
            return entityDaoSupport;
        }
        try {
            Class<?> defaultImplClass = Class.forName("com.brinvex.util.persistence.impl.EntityDaoSupportImpl");
            return (EntityDaoSupport) defaultImplClass.getConstructor().newInstance();
        } catch (ClassNotFoundException
                 | InvocationTargetException
                 | InstantiationException
                 | IllegalAccessException
                 | NoSuchMethodException
                ignored) {
        }

        throw new IllegalStateException(String.format("Not found any implementation of '%s'", EntityDaoSupport.class));
    }

}
