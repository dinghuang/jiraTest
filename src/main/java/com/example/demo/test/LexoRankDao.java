package com.example.demo.test;

import com.atlassian.fugue.Option;
import com.atlassian.greenhopper.manager.lexorank.LexoRankDaoContext;
import com.atlassian.greenhopper.manager.lexorank.LexoRankRow;
import com.atlassian.greenhopper.manager.lexorank.lock.Lock;
import com.atlassian.greenhopper.manager.lexorank.lock.LockOutcome;
import com.atlassian.greenhopper.manager.lexorank.lock.LockProcessOutcome;
import com.atlassian.greenhopper.service.ServiceOutcome;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author dinghuang123@gmail.com
 * @since 2018/5/23
 */
public interface LexoRankDao {
    void createMarkerRowsForRankField(long var1);

    LexoRankRow create(long var1, long var3, String var5);

    LexoRankRow[] findByFieldId(long var1);

    LexoRankRow[] findByIssueId(long var1);

    LexoRankRow[] findByIssueIds(Iterable<Long> var1);

    LexoRankRow[] findByIssueIds(long var1, Iterable<Long> var3);

    Option<LexoRankRow> findByFieldAndIssueId(long var1, long var3);

    List<Long> listIssueIdsByFieldIdAndIssueIds(long var1, List<Long> var3);

    List<Long> listIssueIdsBetween(long var1, long var3);

    LexoRankRow getByFieldAndIssueId(long var1, long var3);

    LexoRankRow[] getMinimumMarkerRowAndNextRow(long var1);

    LexoRankRow[] getMaximumMarkerRowAndPreviousRow(long var1);

    LexoRankRow[] getRowByRankAndNextRow(long var1, String var3);

    /**
     * @deprecated
     */
    @Deprecated
    LexoRankRow[] getRowByRankAndPreviousRow(long var1, String var3);

    LexoRankRow findNextOneByRank(long var1, String var3);

    Option<LexoRankRow> findMinimumMarkerRow(long var1);

    LexoRankRow getMinimumMarkerRow(long var1);

    Option<LexoRankRow> findMaximumMarkerRow(long var1);

    LexoRankRow getMaximumMarkerRow(long var1);

    Option<LexoRankRow> findMaximumRankLengthRow(long var1);

    Set<Long> findIssueIdsByFieldId(long var1);

    Collection<Long> findFieldIdsInLexoRankTable();

    Map<Long, Long> ranksCountByField();

    /**
     * @deprecated
     */
    @Deprecated
    LexoRankDaoContext getContext();

    LockOutcome acquireLock(Long var1);

    LockOutcome acquireLock(LexoRankRow var1);

    LockOutcome acquireLock(Set<LexoRankRow> var1);

    LockOutcome acquireLock(LexoRankRow[] var1);

    LockOutcome acquireLockByFieldId(Long var1);

    void releaseLock(Lock var1);

    LockProcessOutcome<ServiceOutcome<LexoRankRow>> save(Lock var1, LexoRankRow var2);

    ServiceOutcome<LexoRankRow> unlockedSave(LexoRankRow var1);

    LockProcessOutcome<ServiceOutcome<Void>> deleteByIssueId(Lock var1, Long var2);

    LockProcessOutcome<ServiceOutcome<Void>> deleteByLimitedIssueIds(Lock var1, List<Long> var2);

    ServiceOutcome<Void> deleteAll();

    LockProcessOutcome<ServiceOutcome<Void>> deleteByFieldId(Lock var1, Long var2);

    LockProcessOutcome<ServiceOutcome<Void>> deleteByFieldIdAndIssueId(Lock var1, Long var2, Long var3);

    LexoRankRow[] getRowsAtBalanceBoundaryForFieldId(Long var1, LexoRankBucket var2, LexoRankBucket var3);

    boolean existsRankForFieldId(Long var1, String var2);

    Map<String, Long> countDuplicateRowsForFieldId(Long var1);

    List<LexoRankRow> listByFieldIdAndRank(Long var1, String var2);

    Integer getRowCountForFieldId(Long var1);

    long getRowCountInBucket(Long var1, LexoRankBucket var2);

    long getRowCountInBucket(Long var1, LexoRankBucket var2, @Nullable LexoRankRow.RankRowType var3);

    long getNumRowsWithInvalidBucket(Long var1);
}
