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

import com.brinvex.util.persistence.api.NumberFilter;
import com.brinvex.util.persistence.impl.test.dm.Employee;
import com.brinvex.util.persistence.impl.test.dm.Employee_;
import com.brinvex.util.persistence.impl.test.dm.Salary;
import com.brinvex.util.persistence.impl.test.infra.AbstractTest;
import jakarta.persistence.LockTimeoutException;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PessimisticLockException;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.hibernate.LazyInitializationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static java.time.LocalDate.parse;
import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class EntityDaoTest extends AbstractTest {

    private final Employee emp1;
    private final Employee emp2;
    private final Salary salary1_1;
    private final Salary salary1_2;
    private final Salary salary1_3;
    private final Salary salary2_1;
    private final Salary salary2_2;
    private final Salary salary2_3;

    {
        emp1 = new Employee();
        emp1.setName("Alice");
        emp1.setValidFrom(parse("2023-01-01").atStartOfDay());
        emp1.setValidTo(parse("2200-01-01").atStartOfDay());
        emp1.setPhoneNumbers(List.of("0911 111 111"));

        salary1_1 = new Salary();
        salary1_1.setEmployee(emp1);
        salary1_1.setDate(parse("2023-02-01"));
        salary1_1.setAmount(new BigDecimal("150"));

        salary1_2 = new Salary();
        salary1_2.setEmployee(emp1);
        salary1_2.setDate(parse("2023-02-02"));
        salary1_2.setAmount(new BigDecimal("152"));

        salary1_3 = new Salary();
        salary1_3.setEmployee(emp1);
        salary1_3.setDate(parse("2023-02-03"));
        salary1_3.setAmount(new BigDecimal("151"));

        emp2 = new Employee();
        emp2.setName("Bob");
        emp2.setValidFrom(parse("2023-01-02").atStartOfDay());
        emp2.setValidTo(parse("2200-01-01").atStartOfDay());
        emp2.setPhoneNumbers(List.of("0911 222 222", "0911 222 333"));

        salary2_1 = new Salary();
        salary2_1.setEmployee(emp2);
        salary2_1.setDate(parse("2023-02-01"));
        salary2_1.setAmount(new BigDecimal("250"));

        salary2_2 = new Salary();
        salary2_2.setEmployee(emp2);
        salary2_2.setDate(parse("2023-02-02"));
        salary2_2.setAmount(new BigDecimal("252"));

        salary2_3 = new Salary();
        salary2_3.setEmployee(emp2);
        salary2_3.setDate(parse("2023-02-03"));
        salary2_3.setAmount(new BigDecimal("251"));
    }


    @BeforeEach
    void initData() {
        doInTx(em -> {
            em.persist(emp1);
            em.persist(salary1_1);
            em.persist(salary1_2);
            em.persist(salary1_3);
            em.persist(emp2);
            em.persist(salary2_1);
            em.persist(salary2_2);
            em.persist(salary2_3);
        });
    }

    @Test
    void numberFilter() {
        List<Salary> salaries = doInTx(em -> {
            return new SalaryDao(em)
                    .findByNumberFilter(emp1.getId(), NumberFilter.biggerThan(150));
        });
        assertEquals(2, salaries.size());
        assertEquals(salary1_2.getId(), salaries.get(0).getId());
        assertEquals(salary1_3.getId(), salaries.get(1).getId());
    }

    @Test
    void dateFilter() {
        List<Employee> employees = doInTx(em -> {
            return new EmployeeDao(em)
                    .findByDate(emp1.getValidFrom().plusHours(1));
        });
        assertEquals(1, employees.size());
        assertEquals(emp1.getId(), employees.get(0).getId());
        assertEquals(emp1.getPhoneNumbers(), employees.get(0).getPhoneNumbers());
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void fetchJoin() {
        {
            List<Salary> salaries = doInTx(em -> {
                return new SalaryDao(em)
                        .findByEmployeeId(emp1.getId(), false);
            });
            assertEquals(3, salaries.size());
            assertThrows(LazyInitializationException.class, () -> salaries.get(0).getEmployee().getName());
            assertThrows(LazyInitializationException.class, () -> salaries.get(1).getEmployee().getName());
            assertThrows(LazyInitializationException.class, () -> salaries.get(2).getEmployee().getName());
        }
        {
            List<Salary> salaries = doInTx(em -> {
                return new SalaryDao(em)
                        .findByEmployeeId(emp1.getId(), true);
            });
            assertEquals(3, salaries.size());
            assertEquals(emp1.getName(), salaries.get(0).getEmployee().getName());
            assertEquals(emp1.getName(), salaries.get(1).getEmployee().getName());
            assertEquals(emp1.getName(), salaries.get(2).getEmployee().getName());
        }
    }

    @Test
    void findByIdAsDto() {
        long salaryId = salary1_1.getId();
        Salary p1 = doInTx(em -> {
            SalaryDao salaryDao = new SalaryDao(em);
            Salary p = salaryDao.findByIdAsDTO(salaryId);
            p.setAmount(p.getAmount().multiply(BigDecimal.TEN));
            salaryDao.flush();
            return p;
        });
        assertNotNull(p1);
        assertEquals(salaryId, p1.getId());
        assertEquals(salary1_1.getDate(), p1.getDate());
        assertEquals(0, salary1_1.getAmount().multiply(BigDecimal.TEN).compareTo(p1.getAmount()));

        Salary p2 = doInTx(em -> {
            SalaryDao salaryDao = new SalaryDao(em);
            return salaryDao.getById(salaryId);
        });
        assertNotNull(p2);
        assertEquals(salaryId, p2.getId());
        assertEquals(salary1_1.getDate(), p2.getDate());
        assertEquals(0, salary1_1.getAmount().compareTo(p2.getAmount()));

        Salary p3 = doInTx(em -> {
            SalaryDao salaryDao = new SalaryDao(em);
            Salary p = salaryDao.findByIdAsDTO(salaryId);
            p.setAmount(p.getAmount().multiply(BigDecimal.TEN));
            salaryDao.flush();
            return p;
        });
        assertNotNull(p3);
        assertEquals(salaryId, p3.getId());
        assertEquals(salary1_1.getDate(), p3.getDate());
        assertEquals(0, salary1_1.getAmount().multiply(BigDecimal.TEN).compareTo(p3.getAmount()));

    }

    @Test
    void collectionFilter() {
        {
            List<Salary> salaries = doInTx(em -> {
                return new SalaryDao(em).findByDates(emp1.getId(), null);
            });
            assertEquals(3, salaries.size());
        }
        {
            List<Salary> salaries = doInTx(em -> {
                return new SalaryDao(em).findByDates(emp1.getId(), emptyList());
            });
            assertEquals(0, salaries.size());
        }
        {
            List<Salary> salaries = doInTx(em -> {
                return new SalaryDao(em).findByDates(emp1.getId(), List.of(salary1_2.getDate()));
            });
            assertEquals(1, salaries.size());
            assertEquals(salary1_2.getId(), salaries.get(0).getId());
        }
        {
            List<Salary> salaries = doInTx(em -> {
                return new SalaryDao(em).findByDates(emp1.getId(), null);
            });
            assertEquals(3, salaries.size());
            assertEquals(salary1_1.getId(), salaries.get(0).getId());
            assertEquals(salary1_2.getId(), salaries.get(1).getId());
            assertEquals(salary1_3.getId(), salaries.get(2).getId());
        }
    }

    @Test
    void datePartsExtract() {
        {
            List<Integer> days = doInTx(em -> {
                return new SalaryDao(em).findDays(emp1.getId());
            });
            assertEquals(List.of(salary1_1.getDate().getDayOfMonth(), salary1_2.getDate().getDayOfMonth(), salary1_3.getDate().getDayOfMonth()), days);
        }
        {
            List<Integer> months = doInTx(em -> {
                return new SalaryDao(em).findMonths(emp1.getId());
            });
            assertEquals(List.of(salary1_1.getDate().getMonthValue(), salary1_2.getDate().getMonthValue(), salary1_3.getDate().getMonthValue()), months);
        }
        {
            List<Integer> years = doInTx(em -> {
                return new SalaryDao(em).findYears(emp1.getId());
            });
            assertEquals(List.of(salary1_1.getDate().getYear(), salary1_2.getDate().getYear(), salary1_3.getDate().getYear()), years);
        }
    }

    @Test
    void dateDiff() {
        doInTx(em -> {
            EmployeeDao employeeDao = new EmployeeDao(em);

            Employee s1 = employeeDao.getById(this.emp1.getId());
            Employee s2 = employeeDao.getById(this.emp2.getId());
            {
                Integer d = employeeDao.findValidFromDayDiff(s1.getId(), s1.getId());
                assertEquals(0, d);
            }
            {
                Integer d = employeeDao.findValidFromDayDiff(s2.getId(), s2.getId());
                assertEquals(0, d);
            }
            {
                s2.setValidFrom(s1.getValidFrom());
                Integer d = employeeDao.findValidFromDayDiff(s2.getId(), s1.getId());
                assertEquals((int) ChronoUnit.DAYS.between(s1.getValidFrom(), s2.getValidFrom()), d);
                assertEquals(0, d);
            }
            {
                s2.setValidFrom(s1.getValidFrom().plusNanos(1));
                Integer d = employeeDao.findValidFromDayDiff(s2.getId(), s1.getId());
                assertEquals((int) ChronoUnit.DAYS.between(s1.getValidFrom(), s2.getValidFrom()), d);
                assertEquals(0, d);
            }
            {
                s2.setValidFrom(s1.getValidFrom().plusHours(12));
                Integer d = employeeDao.findValidFromDayDiff(s2.getId(), s1.getId());
                assertEquals((int) ChronoUnit.DAYS.between(s1.getValidFrom(), s2.getValidFrom()), d);
                assertEquals(0, d);
            }
            {
                s2.setValidFrom(s1.getValidFrom().plusHours(13));
                Integer d = employeeDao.findValidFromDayDiff(s2.getId(), s1.getId());
                assertEquals((int) ChronoUnit.DAYS.between(s1.getValidFrom(), s2.getValidFrom()), d);
                assertEquals(0, d);
            }
            {
                s2.setValidFrom(s1.getValidFrom().plusHours(24).minusNanos(1000));
                Integer d = employeeDao.findValidFromDayDiff(s2.getId(), s1.getId());
                assertEquals((int) ChronoUnit.DAYS.between(s1.getValidFrom(), s2.getValidFrom()), d);
                assertEquals(0, d);
            }
            {
                s2.setValidFrom(s1.getValidFrom().plusHours(24));
                Integer d = employeeDao.findValidFromDayDiff(s2.getId(), s1.getId());
                assertEquals((int) ChronoUnit.DAYS.between(s1.getValidFrom(), s2.getValidFrom()), d);
                assertEquals(1, d);
            }
            {
                s2.setValidFrom(s1.getValidFrom().plusDays(2));
                Integer d = employeeDao.findValidFromDayDiff(s2.getId(), s1.getId());
                assertEquals((int) ChronoUnit.DAYS.between(s1.getValidFrom(), s2.getValidFrom()), d);
                assertEquals(2, d);
            }
        });
    }

    @Test
    void findForUpdate() throws InterruptedException, ExecutionException {

        Callable<Object> c1 = () -> doInTx(em -> {
            SalaryDao salaryDao = new SalaryDao(em);
            LOG.debug("before thread1-findForUpdate");
            Salary salary = salaryDao.findForUpdate(emp1.getId(), salary1_1.getDate(), Duration.ofSeconds(2));
            LOG.debug("after thread1-findForUpdate - result: {}", salary);
            sleep(5);
            LOG.debug("after thread1 sleep");
            return null;
        });

        Callable<Object> c2 = () -> doInTx(em -> {
            sleep(1);
            LOG.debug("after thread2 sleep");
            SalaryDao salaryDao = new SalaryDao(em);
            try {
                LOG.debug("before thread2-findForUpdate");
                Salary salary = salaryDao.findForUpdate(emp1.getId(), salary1_1.getDate(), Duration.ofSeconds(2));
                LOG.debug("after thread2-findForUpdate - result: {}", salary);
                fail("Expecting LockTimeoutException");
            } catch (LockTimeoutException e) {
                LOG.debug("after failed thread2-findForUpdate - expected exception: {}", e.getMessage());
            }
            return null;
        });

        ExecutorService executorService = Executors.newFixedThreadPool(2);
        List<Future<Object>> futures = executorService.invokeAll(List.of(c1, c2));
        for (Future<Object> future : futures) {
            future.get();
        }
    }

    @Test
    void getForUpdate() throws InterruptedException, ExecutionException {

        Callable<Object> c1 = () -> doInTx(em -> {
            SalaryDao salaryDao = new SalaryDao(em);
            LOG.debug("before thread1-findForUpdate");
            Salary salary = salaryDao.getByIdForUpdate(salary1_1.getId(), Duration.ofSeconds(2));
            LOG.debug("after thread1-findForUpdate - result: {}", salary);
            sleep(5);
            LOG.debug("after thread1 sleep");
            return null;
        });

        Callable<Object> c2 = () -> doInTx(em -> {
            sleep(1);
            LOG.debug("after thread2 sleep");
            SalaryDao salaryDao = new SalaryDao(em);
            try {
                LOG.debug("before thread2-findForUpdate");
                Salary salary = salaryDao.getByIdForUpdate(salary1_1.getId(), Duration.ofSeconds(2));
                LOG.debug("after thread2-findForUpdate - result: {}", salary);
                fail("Expecting PessimisticLockException");
            } catch (PessimisticLockException e) {
                LOG.debug("after failed thread2-findForUpdate - expected exception: {}", e.getMessage());
            }
            return null;
        });

        ExecutorService executorService = Executors.newFixedThreadPool(2);
        List<Future<Object>> futures = executorService.invokeAll(List.of(c1, c2));
        for (Future<Object> future : futures) {
            future.get();
        }
    }

    @Test
    void findForUpdateSkipLocked() throws InterruptedException, ExecutionException {

        Callable<Object> c1 = () -> doInTx(em -> {
            SalaryDao salaryDao = new SalaryDao(em);
            LOG.debug("before thread1-findForUpdate");
            Salary salary = salaryDao.findForUpdateSkipLocked(emp1.getId(), salary1_1.getDate());
            LOG.debug("after thread1-findForUpdate - result: {}", salary);
            sleep(5);
            LOG.debug("after thread1 sleep");
            return null;
        });

        Callable<Object> c2 = () -> doInTx(em -> {
            sleep(1);
            LOG.debug("after thread2 sleep");
            SalaryDao salaryDao = new SalaryDao(em);
            LOG.debug("before thread2-findForUpdate");
            Salary salary = salaryDao.findForUpdateSkipLocked(emp1.getId(), salary1_1.getDate());
            LOG.debug("after thread2-findForUpdate - result (should be null): {}", salary);
            assertNull(salary);
            return null;
        });

        ExecutorService executorService = Executors.newFixedThreadPool(2);
        List<Future<Object>> futures = executorService.invokeAll(List.of(c1, c2));
        for (Future<Object> future : futures) {
            future.get();
        }
    }

    @Test
    void optimisticLocking() {
        {
            doInTx(em -> {
                EmployeeDao employeeDao = new EmployeeDao(em);
                Employee s1 = employeeDao.getById(emp1.getId());
                assertEquals(0, s1.getVersion());
            });
            doInTx(em -> {
                EmployeeDao employeeDao = new EmployeeDao(em);
                Employee s1 = employeeDao.getById(emp1.getId());
                assertEquals(0, s1.getVersion());
                s1.setValidFrom(s1.getValidFrom().minusDays(1));
            });
            doInTx(em -> {
                EmployeeDao employeeDao = new EmployeeDao(em);
                Employee s1 = employeeDao.getById(emp1.getId());
                assertEquals(1, s1.getVersion());
                s1.setValidFrom(s1.getValidFrom().minusDays(1));
            });
            doInTx(em -> {
                EmployeeDao employeeDao = new EmployeeDao(em);
                try {
                    employeeDao.getByIdAndCheckVersion(emp1.getId(), emp1.getVersion(), Employee::getVersion);
                } catch (OptimisticLockException e) {
                    LOG.debug("Expecting exception: {}", e.getMessage());
                }
            });
        }
    }

    @SuppressWarnings({"rawtypes"})
    @Test
    void collectionColumnConverter_jpql() {
        doInTx(em -> {
            Object phoneNumbers = em
                    .createQuery("select emp.phoneNumbers from Employee emp where emp.id = :EMP_ID")
                    .setParameter("EMP_ID", emp2.getId())
                    .getSingleResult();

            assertTrue(phoneNumbers instanceof List);
            assertTrue(((List) phoneNumbers).get(0) instanceof String);
            assertEquals(emp2.getPhoneNumbers(), phoneNumbers);
        });
    }


    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    void collectionColumnConverter_criteria() {
        doInTx(em -> {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<List> q = cb.createQuery(List.class);
            Root<Employee> r = q.from(Employee.class);
            q.select(r.get(Employee_.phoneNumbers));
            q.where(cb.equal(r.get(Employee_.id), emp2.getId()));
            List<Object> phoneNumbers = em.createQuery(q).getSingleResult();

            if  (phoneNumbers.get(0) instanceof String) {
                // Holds for Hibernate <= 6.2
                assertEquals(emp2.getPhoneNumbers(), phoneNumbers);
            } else if (phoneNumbers.get(0) instanceof List) {
                // Holds for Hibernate >= 6.3, but it seems as a bug
                // https://discourse.hibernate.org/t/criteria-api-query-involving-collection-attributeconverter-stopped-working-in-hibernate-orm-6-3/8504
                assertEquals(emp2.getPhoneNumbers(), phoneNumbers.get(0));
            }
        });
    }

}