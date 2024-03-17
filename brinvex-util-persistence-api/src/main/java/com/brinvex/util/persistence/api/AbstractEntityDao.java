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
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;

import java.io.Serializable;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAccessor;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

@SuppressWarnings({"unused", "resource"})
public abstract class AbstractEntityDao<ENTITY, ID extends Serializable> implements EntityDao<ENTITY, ID> {

    protected final Class<ENTITY> entityType;

    protected final Class<ID> idType;

    private EntityManager em;

    private CriteriaBuilder cb;

    private final EntityDaoSupport support;

    private SingularAttribute<? super ENTITY, ID> idAttribute;

    protected AbstractEntityDao(
            Class<ENTITY> entityType,
            Class<ID> idType
    ) {
        this(entityType, idType, EntityDaoSupportFactory.INSTANCE.getEntityDaoSupport());
    }

    protected AbstractEntityDao(
            Class<ENTITY> entityType,
            Class<ID> idType,
            EntityDaoSupport entityDaoSupport
    ) {
        this.entityType = entityType;
        this.idType = idType;
        this.support = entityDaoSupport;
    }

    protected abstract EntityManager entityManager();

    protected EntityManager em() {
        if (this.em == null) {
            this.em = entityManager();
            Objects.requireNonNull(this.em);
        }
        return this.em;
    }

    protected CriteriaBuilder cb() {
        if (this.cb == null) {
            this.cb = em().getCriteriaBuilder();
        }
        return this.cb;
    }

    protected EntityDaoSupport support() {
        return support;
    }

    protected SingularAttribute<? super ENTITY, ID> idAttribute() {
        if (idAttribute == null) {
            EntityType<ENTITY> entityMetamodel = em().getEntityManagerFactory().getMetamodel().entity(entityType);
            idAttribute = entityMetamodel.getId(idType);
        }
        return idAttribute;
    }

    @Override
    public ENTITY getById(ID id) {
        return support.getById(em(), entityType, id);
    }

    @Override
    public ENTITY getByIdForUpdate(ID id, Duration lockDuration) {
        return support.getByIdForUpdate(em(), entityType, id, lockDuration);
    }

    @Override
    public ENTITY getByIdForUpdateSkipLocked(ID id) {
        return support.getByIdForUpdateSkipLocked(em(), entityType, id);
    }

    @Override
    public ENTITY getByIdAndCheckVersion(ID id, short optLockVersion, Function<ENTITY, Short> optLockVersionGetter) {
        return support.getByIdAndCheckVersion(em(), entityType, id, optLockVersion, optLockVersionGetter);
    }

    @Override
    public ENTITY getByIdAndCheckVersion(ID id, int optLockVersion, Function<ENTITY, Integer> optLockVersionGetter) {
        return support.getByIdAndCheckVersion(em(), entityType, id, optLockVersion, optLockVersionGetter);
    }

    @Override
    public List<ENTITY> findByIds(Collection<ID> ids) {
        return support.findByIds(em(), entityType, ids, idAttribute());
    }

    @Override
    public ENTITY getReference(ID id) {
        return support.getReference(em(), entityType, id);
    }

    @Override
    public <OTHER_ENTITY, OTHER_ID extends Serializable> OTHER_ENTITY getReference(
            Class<OTHER_ENTITY> entityType,
            OTHER_ID id
    ) {
        return support.getReference(em(), entityType, id);
    }

    @Override
    public void persist(ENTITY entity) {
        support.persist(em(), entity);
    }

    @Override
    public ENTITY merge(ENTITY entity) {
        return support.merge(em(), entity);
    }

    @Override
    public void detach(ENTITY entity) {
        support.detach(em(), entity);
    }

    @Override
    public void flush() {
        support.flush(em());
    }

    @Override
    public void clear() {
        support.clear(em());
    }

    @Override
    public void flushAndClear() {
        support.flushAndClear(em());
    }

    @Override
    public void remove(ENTITY entity) {
        support.remove(em(), entity);
    }

    @Override
    public int bulkDeleteByIds(Collection<ID> ids) {
        return support.bulkDeleteByIds(em(), entityType, idAttribute(), ids);
    }

    protected <F, T> Join<F, T> fetchJoin(From<?, F> from, SingularAttribute<? super F, T> attribute) {
        return support.fetchJoin(from, attribute);
    }

    protected <R> List<R> getResults(CriteriaQuery<R> query) {
        return support.getResults(em(), query);
    }

    protected <R> List<R> getResults(CriteriaQuery<R> query, Integer offset, Integer limit) {
        return support.getResults(em(), query, offset, limit);
    }

    protected <R> List<R> getResults(CriteriaQuery<R> query, QueryCacheMode queryCacheUsage) {
        return support.getResults(em(), query, queryCacheUsage);
    }

    protected <R> List<R> getResults(
            CriteriaQuery<R> query,
            Integer offset,
            Integer limit,
            QueryCacheMode queryCacheUsage
    ) {
        return support.getResults(em(), query, offset, limit, queryCacheUsage);
    }

    protected long count(CriteriaQuery<Long> query, QueryCacheMode queryCacheUsage) {
        return support.count(em(), query, queryCacheUsage, idAttribute());
    }

    protected long count(CriteriaQuery<Long> query) {
        return support.count(em(), query, QueryCacheMode.BYPASS_QUERY_CACHE, idAttribute());
    }

    protected <R> R getFirstResult(CriteriaQuery<R> query) {
        return support.getFirstResult(em(), query);
    }

    protected <R> R getFirstResult(CriteriaQuery<R> query, Integer offset) {
        return support.getFirstResult(em(), query, offset);
    }

    protected <R> R getFirstResult(CriteriaQuery<R> query, QueryCacheMode queryCachemode) {
        return support.getFirstResult(em(), query, queryCachemode);
    }

    protected <R> R getFirstResultForUpdate(CriteriaQuery<R> query, Duration lockTimeout) {
        return support.getFirstResultForUpdate(em(), query, lockTimeout);
    }

    protected <R> R getFirstResultForUpdateSkipLocked(CriteriaQuery<R> query) {
        return support.getFirstResultForUpdateSkipLocked(em(), query);
    }

    protected <R> R getUniqueResult(CriteriaQuery<R> query) {
        return support.getUniqueResult(em(), query);
    }

    protected <R> R getUniqueResult(CriteriaQuery<R> query, QueryCacheMode queryCachemode) {
        return support.getUniqueResult(em(), query, queryCachemode);
    }

    protected <NUMBER extends Number> Predicate asPredicate(Expression<NUMBER> attribute, NumberFilter numberFilter) {
        return support.asPredicate(cb(), attribute, numberFilter);
    }

    protected <E> Predicate inCollection(Expression<E> attribute, Collection<E> filterItems) {
        return support.inCollection(cb(), attribute, filterItems);
    }

    protected Predicate betweenLeftInclRightExcl(
            Path<LocalDateTime> leftAttribute, Path<LocalDateTime> rightAttribute, LocalDate testDate
    ) {
        return support.betweenLeftInclRightExcl(cb(), leftAttribute, rightAttribute, testDate);
    }

    protected Predicate betweenLeftInclRightExcl(
            Path<LocalDateTime> leftAttribute, Path<LocalDateTime> rightAttribute, LocalDateTime testDate
    ) {
        return support.betweenLeftInclRightExcl(cb(), leftAttribute, rightAttribute, testDate);
    }

    protected <T extends Number> Expression<T> sum(
            Expression<T> expression1,
            Expression<T> expression2,
            Expression<T> expression3
    ) {
        return support.sum(cb(), expression1, expression2, expression3);
    }

    protected Expression<Integer> least(Integer literal1, Expression<Integer> expression2) {
        return support.least(cb(), literal1, expression2);
    }

    protected Expression<Integer> greatest(Integer literal1, Expression<Integer> expression2) {
        return support.greatest(cb(), literal1, expression2);
    }

    protected Expression<Integer> day(Expression<? extends TemporalAccessor> datetimeExpression) {
        return support.day(cb(), datetimeExpression);
    }

    protected Expression<Integer> month(Expression<? extends TemporalAccessor> datetimeExpression) {
        return support.month(cb(), datetimeExpression);
    }

    protected Expression<Integer> year(Expression<? extends TemporalAccessor> datetimeExpression) {
        return support.year(cb(), datetimeExpression);
    }

    /**
     * See {@link EntityDaoSupport#dayDiff}
     */
    protected Expression<Integer> dayDiff(
            Expression<? extends TemporalAccessor> leftDatetimeExpr,
            Expression<? extends TemporalAccessor> rightDatetimeExpr
    ) {
        return support.dayDiff(cb(), leftDatetimeExpr, rightDatetimeExpr);
    }

    protected <DTO> DTO findByIdAsDTO(
            Class<DTO> dtoType,
            ID id,
            List<SingularAttribute<ENTITY, ?>> constructorParameters
    ) {
        return support.findByIdAsDTO(em(), entityType, id, idAttribute(), dtoType, constructorParameters);
    }

    protected <R> CriteriaQuery<R> applySelections(
            CriteriaQuery<R> q,
            Class<R> resultType,
            Collection<Selection<?>> selections
    ) {
        return support.applySelections(cb(), q, resultType, selections);
    }

}
