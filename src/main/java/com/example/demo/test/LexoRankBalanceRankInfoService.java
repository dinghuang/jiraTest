package com.example.demo.test;

import com.atlassian.fugue.Option;
import com.atlassian.greenhopper.global.LoggerWrapper;
import com.atlassian.greenhopper.manager.lexorank.LexoRankDao;
import com.atlassian.greenhopper.manager.lexorank.LexoRankRow;
import com.atlassian.jira.exception.DataAccessException;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.MutableIssue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author dinghuang123@gmail.com
 * @since 2018/5/23
 */
@Service
public class LexoRankBalanceRankInfoService {
    protected final LoggerWrapper log = LoggerWrapper.with(this.getClass());
    @Autowired
    private LexoRankDao lexoRankDao;
    @Autowired
    private IssueManager issueManager;

    public LexoRankBalanceRankInfoService() {
    }

    private LexoRankBalanceRankInfoService.LexoRankRebalanceOperation getNextRebalanceOperation(LexoRankRow maxRank) {
        int rankLength = maxRank.getRank().length();
        return rankLength < 50?new LexoRankBalanceRankInfoService.LexoRankRebalanceOperation(false, 50, LexoRankBalanceRankInfoService.RebalanceDangerRating.OK):new LexoRankBalanceRankInfoService.LexoRankRebalanceOperation(true, 100, LexoRankBalanceRankInfoService.RebalanceDangerRating.WARNING);
    }

    private String getIssueKey(Long issueId) {
        try {
            MutableIssue issue = this.issueManager.getIssueObject(issueId);
            return issue.getKey();
        } catch (DataAccessException var3) {
            this.log.warn("Error while retrieving issue with ID %d", new Object[]{issueId});
            this.log.exception(var3);
            return "";
        }
    }

    public LexoRankBalanceRankInfoService.LexoRankMaxRank getMaxRank(Long fieldId) {
        Option<LexoRankRow> maybeMaximumRankLengthRow = this.lexoRankDao.findMaximumRankLengthRow(fieldId.longValue());
        if(maybeMaximumRankLengthRow.isEmpty()) {
            return LexoRankBalanceRankInfoService.LexoRankMaxRank.empty();
        } else {
            LexoRankRow maxRank = (LexoRankRow)maybeMaximumRankLengthRow.get();
            LexoRankBalanceRankInfoService.LexoRankRebalanceOperation nextRebalanceOperation = this.getNextRebalanceOperation(maxRank);
            String issueKey = this.getIssueKey(maxRank.getIssueId());
            return new LexoRankBalanceRankInfoService.LexoRankMaxRank(maxRank.getRank().length(), nextRebalanceOperation, issueKey);
        }
    }

    public static class LexoRankRebalanceOperation {
        public final boolean isImmediate;
        public final int limit;
        public final String status;

        public LexoRankRebalanceOperation(boolean isImmediate, int limit, LexoRankBalanceRankInfoService.RebalanceDangerRating status) {
            this.isImmediate = isImmediate;
            this.limit = limit;
            this.status = status.name();
        }

        public static LexoRankBalanceRankInfoService.LexoRankRebalanceOperation empty() {
            return new LexoRankBalanceRankInfoService.LexoRankRebalanceOperation(false, 50, LexoRankBalanceRankInfoService.RebalanceDangerRating.OK);
        }
    }

    public static class LexoRankMaxRank {
        public final int rankLength;
        public final int maxLength;
        public final LexoRankBalanceRankInfoService.LexoRankRebalanceOperation nextRebalance;
        public final String issueKey;

        public LexoRankMaxRank(int rankLength, LexoRankBalanceRankInfoService.LexoRankRebalanceOperation nextRebalance, String issueKey) {
            this.rankLength = rankLength;
            this.maxLength = 254;
            this.nextRebalance = nextRebalance;
            this.issueKey = issueKey;
        }

        public static LexoRankBalanceRankInfoService.LexoRankMaxRank empty() {
            return new LexoRankBalanceRankInfoService.LexoRankMaxRank(0, LexoRankBalanceRankInfoService.LexoRankRebalanceOperation.empty(), "");
        }
    }

    private static enum RebalanceDangerRating {
        WARNING,
        OK;

        private RebalanceDangerRating() {
        }
    }
}