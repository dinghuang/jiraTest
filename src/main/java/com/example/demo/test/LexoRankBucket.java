package com.example.demo.test;

import com.atlassian.greenhopper.model.lexorank.LexoInteger;
import com.atlassian.greenhopper.model.lexorank.LexoRank;

/**
 * @author dinghuang123@gmail.com
 * @since 2018/5/23
 */
public enum LexoRankBucket {
    BUCKET_0("0"),
    BUCKET_1("1"),
    BUCKET_2("2");

    private final LexoInteger value;

    private LexoRankBucket(String val) {
        this.value = LexoInteger.parse(val, LexoRank.NUMERAL_SYSTEM);
    }

    public static LexoRankBucket resolve(int bucketId) {
        LexoRankBucket[] var1 = values();
        int var2 = var1.length;

        for (int var3 = 0; var3 < var2; ++var3) {
            LexoRankBucket bucket = var1[var3];
            if (bucket.value.equals(String.valueOf(bucketId))) {
                return bucket;
            }
        }

        throw new IllegalArgumentException("No bucket found with id " + bucketId);
    }

    public String format() {
        return this.value.format();
    }

    public LexoRankBucket next() {
        switch (this.ordinal()) {
            case 1:
                return BUCKET_1;
            case 2:
                return BUCKET_2;
            case 3:
                return BUCKET_0;
            default:
                throw new RuntimeException("Unknown bucket: " + this.format());
        }
    }

    public LexoRankBucket prev() {
        switch (this.ordinal()) {
            case 1:
                return BUCKET_2;
            case 2:
                return BUCKET_0;
            case 3:
                return BUCKET_1;
            default:
                throw new RuntimeException("Unknown bucket: " + this.format());
        }
    }

    public static LexoRankBucket fromRank(String rank) {
        String bucket = rank.substring(0, rank.indexOf("|"));
        return from(bucket);
    }

    public static LexoRankBucket from(String str) {
        LexoInteger val = LexoInteger.parse(str, LexoRank.NUMERAL_SYSTEM);
        LexoRankBucket[] var2 = values();
        int var3 = var2.length;

        for (int var4 = 0; var4 < var3; ++var4) {
            LexoRankBucket bucket = var2[var4];
            if (bucket.value.equals(val)) {
                return bucket;
            }
        }

        throw new RuntimeException("Unknown bucket: " + str);
    }

    public static LexoRankBucket max() {
        LexoRankBucket[] values = values();
        return values[values.length - 1];
    }

    public Integer intValue() {
        switch (this.ordinal()) {
            case 1:
                return Integer.valueOf(0);
            case 2:
                return Integer.valueOf(1);
            case 3:
                return Integer.valueOf(2);
            default:
                throw new IllegalStateException(String.format("Illegal Lexo rank value %s", new Object[]{this}));
        }
    }
}
