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
import com.brinvex.util.persistence.api.NumberFilter;
import com.brinvex.util.persistence.impl.test.dm.Employee;
import com.brinvex.util.persistence.impl.test.dm.Employee_;
import com.brinvex.util.persistence.impl.test.dm.Salary;
import com.brinvex.util.persistence.impl.test.dm.Salary_;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public class SalaryDao extends AbstractEntityDao<Salary, Long> {

    private final EntityManager em;

    protected SalaryDao(EntityManager em) {
        super(Salary.class, Long.class);
        this.em = em;
    }

    @Override
    protected EntityManager entityManager() {
        return em;
    }

    public List<Salary> findByEmployeeId(long employeeId, boolean fetchEmployee) {
        CriteriaBuilder cb = this.cb();
        CriteriaQuery<Salary> q = cb.createQuery(Salary.class);
        Root<Salary> r = q.from(Salary.class);
        Join<Salary, Employee> employeeJoin;
        if (fetchEmployee) {
            employeeJoin = fetchJoin(r, Salary_.employee);
        } else {
            employeeJoin = r.join(Salary_.employee);
        }
        q.where(cb.equal(employeeJoin.get(Employee_.id), employeeId));
        return getResults(q);
    }

    public List<Salary> findByNumberFilter(long employeeId, NumberFilter salaryFilter) {
        CriteriaBuilder cb = this.cb();
        CriteriaQuery<Salary> q = cb.createQuery(Salary.class);
        Root<Salary> r = q.from(Salary.class);
        Join<Salary, Employee> employeeJoin = fetchJoin(r, Salary_.employee);
        q.where(
                cb.equal(employeeJoin.get(Employee_.id), employeeId),
                asPredicate(r.get(Salary_.amount), salaryFilter)
        );
        return getResults(q);
    }

    public List<Salary> findByDates(long employeeId, Collection<LocalDate> dateFilter) {
        CriteriaBuilder cb = this.cb();
        CriteriaQuery<Salary> q = cb.createQuery(Salary.class);
        Root<Salary> r = q.from(Salary.class);
        Join<Salary, Employee> employeeJoin = fetchJoin(r, Salary_.employee);
        q.where(
                cb.equal(employeeJoin.get(Employee_.id), employeeId),
                inCollection(r.get(Salary_.date), dateFilter)
        );
        return getResults(q);
    }

    public Salary findForUpdate(long employeeId, LocalDate date, Duration lockTimeout) {
        CriteriaBuilder cb = this.cb();
        CriteriaQuery<Salary> q = cb.createQuery(Salary.class);
        Root<Salary> r = q.from(Salary.class);
        Join<Salary, Employee> employeeJoin = fetchJoin(r, Salary_.employee);
        q.where(
                cb.equal(employeeJoin.get(Employee_.id), employeeId),
                cb.equal(r.get(Salary_.date), date)
        );
        return getFirstResultForUpdate(q, lockTimeout);
    }

    public Salary findForUpdateSkipLocked(long employeeId, LocalDate date) {
        CriteriaBuilder cb = this.cb();
        CriteriaQuery<Salary> q = cb.createQuery(Salary.class);
        Root<Salary> r = q.from(Salary.class);
        Join<Salary, Employee> employeeJoin = fetchJoin(r, Salary_.employee);
        q.where(
                cb.equal(employeeJoin.get(Employee_.id), employeeId),
                cb.equal(r.get(Salary_.date), date)
        );
        return getFirstResultForUpdateSkipLocked(q);
    }

    public List<Integer> findDays(long employeeId) {
        CriteriaBuilder cb = this.cb();
        CriteriaQuery<Integer> q = cb.createQuery(Integer.class);
        Root<Salary> r = q.from(Salary.class);
        q.where(cb.equal(r.get(Salary_.employee).get(Employee_.id), employeeId));
        q.select(day(r.get(Salary_.date)));
        return getResults(q);
    }

    public List<Integer> findMonths(long employeeId) {
        CriteriaBuilder cb = this.cb();
        CriteriaQuery<Integer> q = cb.createQuery(Integer.class);
        Root<Salary> r = q.from(Salary.class);
        q.where(cb.equal(r.get(Salary_.employee).get(Employee_.id), employeeId));
        q.select(month(r.get(Salary_.date)));
        return getResults(q);
    }

    public List<Integer> findYears(long employeeId) {
        CriteriaBuilder cb = this.cb();
        CriteriaQuery<Integer> q = cb.createQuery(Integer.class);
        Root<Salary> r = q.from(Salary.class);
        q.where(cb.equal(r.get(Salary_.employee).get(Employee_.id), employeeId));
        q.select(year(r.get(Salary_.date)));
        return getResults(q);
    }

    public Salary findByIdAsDTO(long salaryId) {
        return findByIdAsDTO(Salary.class, salaryId, List.of(Salary_.id, Salary_.date, Salary_.amount));
    }
}
