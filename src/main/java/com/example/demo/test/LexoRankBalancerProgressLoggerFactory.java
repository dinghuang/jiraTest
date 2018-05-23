package com.example.demo.test;

import org.springframework.stereotype.Service;

/**
 * @author dinghuang123@gmail.com
 * @since 2018/5/23
 */
@Service
class LexoRankBalancerProgressLoggerFactory {
    LexoRankBalancerProgressLoggerFactory() {
    }

    LexoRankBalancerProgressLogger create(long rankFieldId, long numRows, long numRowsBalanced) {
        return new LexoRankBalancerProgressLogger(rankFieldId, numRows, numRowsBalanced);
    }
}
