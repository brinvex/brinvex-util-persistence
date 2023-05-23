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
package com.brinvex.util.persistence.impl.test;

import com.brinvex.util.persistence.api.AbstractEntityDao;
import com.brinvex.util.persistence.impl.test.dm.Employee;
import com.brinvex.util.persistence.impl.test.dm.Employee_;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import java.time.LocalDateTime;
import java.util.List;

public class EmployeeDao extends AbstractEntityDao<Employee, Long> {

    private final EntityManager em;

    protected EmployeeDao(EntityManager em) {
        super(Employee.class, Long.class);
        this.em = em;
    }

    @Override
    protected EntityManager entityManager() {
        return em;
    }

    public List<Employee> findByDate(LocalDateTime date) {
        CriteriaBuilder cb = this.cb();
        CriteriaQuery<Employee> q = cb.createQuery(Employee.class);
        Root<Employee> r = q.from(Employee.class);
        q.where(
                cb.lessThanOrEqualTo(r.get(Employee_.validFrom), date),
                cb.greaterThan(r.get(Employee_.validTo), date)
        );
        return getResults(q);
    }

    public Integer findValidFromDayDiff(long employeeId1, long employeeId2) {
        CriteriaBuilder cb = this.cb();
        CriteriaQuery<Integer> q = cb.createQuery(Integer.class);
        Root<Employee> r1 = q.from(Employee.class);
        Root<Employee> r2 = q.from(Employee.class);
        q.where(
                cb.equal(r1.get(Employee_.id), employeeId1),
                cb.equal(r2.get(Employee_.id), employeeId2)
        );
        q.select(dayDiff(r1.get(Employee_.validFrom), r2.get(Employee_.validFrom)));
        return getUniqueResult(q);

    }
}
