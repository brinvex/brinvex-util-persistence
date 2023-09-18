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

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Selection;
import jakarta.persistence.metamodel.SingularAttribute;

import java.io.Serializable;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAccessor;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

public interface EntityDaoSupport {

    <ENTITY, ID extends Serializable> ENTITY findById(EntityManager em, Class<ENTITY> entityType, ID id);

    <ENTITY, ID extends Serializable> List<ENTITY> findByIds(
            EntityManager em,
            Class<ENTITY> entityType,
            Collection<ID> ids,
            SingularAttribute<? super ENTITY, ID> idAttribute
    );

    <ENTITY, ID extends Serializable> ENTITY findByIdAndVersion(
            EntityManager em,
            Class<ENTITY> entityType,
            ID id,
            short optLockVersion,
            Function<ENTITY, Short> optLockVersionGetter
    );

    <ENTITY, ID, DTO> DTO findByIdAsDTO(
            EntityManager em,
            Class<ENTITY> entityType,
            ID id,
            SingularAttribute<? super ENTITY, ID> idAttribute,
            Class<DTO> dtoType,
            List<SingularAttribute<ENTITY, ?>> constructorParameters
    );

    <ENTITY, ID extends Serializable> ENTITY findByIdForUpdateSkipLocked(
            EntityManager em,
            Class<ENTITY> entityType,
            ID id,
            SingularAttribute<? super ENTITY, ID> idAttribute
    );

    <ENTITY, ID extends Serializable> ENTITY getReference(
            EntityManager em, Class<ENTITY> entityType, ID id
    );

    <ENTITY> void persist(EntityManager em, ENTITY entity);

    <ENTITY> void detach(EntityManager em, ENTITY entity);

    void flush(EntityManager em);

    void clear(EntityManager em);

    void flushAndClear(EntityManager em);

    <ENTITY> void remove(EntityManager em, ENTITY entity);

    <ENTITY, ID extends Serializable> int bulkDeleteByIds(
            EntityManager em,
            Class<ENTITY> entityType,
            SingularAttribute<? super ENTITY, ID> idAttribute,
            Collection<ID> ids
    );

    <F, T> Join<F, T> fetchJoin(From<?, F> from, SingularAttribute<? super F, T> attribute);

    <R> List<R> getResults(EntityManager em, CriteriaQuery<R> query);

    <R> List<R> getResults(EntityManager em, CriteriaQuery<R> query, Integer offset, Integer limit);

    <R> List<R> getResults(EntityManager em, CriteriaQuery<R> query, QueryCacheMode queryCacheMode);

    <R> List<R> getResults(
            EntityManager em,
            CriteriaQuery<R> query,
            Integer offset,
            Integer limit,
            QueryCacheMode queryCacheMode
    );

    <R> CriteriaQuery<R> applySelections(
            CriteriaBuilder cb,
            CriteriaQuery<R> q,
            Class<R> resultType,
            Collection<Selection<?>> selections
    );

    <ENTITY, ID extends Serializable> long count(
            EntityManager em,
            CriteriaQuery<Long> query,
            QueryCacheMode queryCacheMode,
            SingularAttribute<? super ENTITY, ID> idAttribute
    );

    <R> R getUniqueResult(EntityManager em, CriteriaQuery<R> q);

    <R> R getUniqueResult(EntityManager em, CriteriaQuery<R> q, QueryCacheMode queryCacheMode);

    <R> R getFirstResult(EntityManager em, CriteriaQuery<R> q);

    <R> R getFirstResult(EntityManager em, CriteriaQuery<R> q, Integer offset);

    <R> R getFirstResult(EntityManager em, CriteriaQuery<R> q, QueryCacheMode queryCacheMode);

    <R> R getFirstResultForUpdate(
            EntityManager em,
            CriteriaQuery<R> q,
            Duration lockTimeout
    );

    <R> R getFirstResultForUpdateSkipLocked(EntityManager em, CriteriaQuery<R> q);

    <NUMBER extends Number> Predicate asPredicate(
            CriteriaBuilder cb,
            Expression<NUMBER> attribute,
            NumberFilter numberFilter
    );

    <E> Predicate inCollection(
            CriteriaBuilder cb,
            Expression<E> attribute,
            Collection<E> filterItems
    );

    Predicate betweenLeftInclRightExcl(
            CriteriaBuilder cb,
            Path<LocalDateTime> leftAttribute, Path<LocalDateTime> rightAttribute, LocalDate testDate
    );

    Predicate betweenLeftInclRightExcl(
            CriteriaBuilder cb,
            Path<LocalDateTime> leftAttribute, Path<LocalDateTime> rightAttribute, LocalDateTime testDate
    );

    <T extends Number> Expression<T> sum(
            CriteriaBuilder cb,
            Expression<T> expression1,
            Expression<T> expression2,
            Expression<T> expression3
    );

    Expression<Integer> least(CriteriaBuilder cb, Integer literal1, Expression<Integer> expression2);

    Expression<Integer> greatest(CriteriaBuilder cb, Integer literal1, Expression<Integer> expression2);

    Expression<Integer> day(CriteriaBuilder cb, Expression<? extends TemporalAccessor> datetimeExpression);

    Expression<Integer> month(CriteriaBuilder cb, Expression<? extends TemporalAccessor> datetimeExpression);

    Expression<Integer> year(CriteriaBuilder cb, Expression<? extends TemporalAccessor> datetimeExpression);

    Expression<Integer> epochSeconds(
            CriteriaBuilder cb,
            Expression<? extends TemporalAccessor> datetimeExpression
    );

    Expression<Integer> dayDiff(
            CriteriaBuilder cb,
            Expression<? extends TemporalAccessor> leftDatetimeExpression,
            Expression<? extends TemporalAccessor> rightDatetimeExpression
    );

}
