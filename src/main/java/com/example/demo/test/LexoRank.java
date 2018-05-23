package com.example.demo.test;

import com.google.common.collect.ImmutableList;

import java.util.Iterator;
import java.util.List;

/**
 * @author dinghuang123@gmail.com
 * @since 2018/5/23
 */
public class LexoRank implements Comparable<LexoRank> {
    public static final LexoNumeralSystem NUMERAL_SYSTEM;
    public static final LexoDecimal ZERO_DECIMAL;
    public static final LexoDecimal ONE_DECIMAL;
    public static final LexoDecimal EIGHT_DECIMAL;
    public static final LexoDecimal MIN_DECIMAL;
    public static final LexoDecimal MAX_DECIMAL;
    public static final LexoDecimal MID_DECIMAL;
    public static final LexoDecimal INITIAL_MIN_DECIMAL;
    public static final LexoDecimal INITIAL_MAX_DECIMAL;
    private final String value;
    private LexoRankBucket bucket;
    private LexoDecimal decimal;

    private LexoRank(String value) {
        this.value = value;
    }

    private LexoRank(LexoRankBucket bucket, LexoDecimal decimal) {
        this.value = bucket.format() + "|" + formatDecimal(decimal);
        this.bucket = bucket;
        this.decimal = decimal;
    }

    public static LexoRank min() {
        return from(LexoRankBucket.BUCKET_0, MIN_DECIMAL);
    }

    public static LexoRank max() {
        return max(LexoRankBucket.BUCKET_0);
    }

    public static LexoRank max(LexoRankBucket bucket) {
        return from(bucket, MAX_DECIMAL);
    }

    public static LexoRank initial(LexoRankBucket bucket) {
        return bucket == LexoRankBucket.BUCKET_0 ? from(bucket, INITIAL_MIN_DECIMAL) : from(bucket, INITIAL_MAX_DECIMAL);
    }

    public LexoRank genPrev() {
        this.fillDecimal();
        if (this.isMax()) {
            return new LexoRank(this.bucket, INITIAL_MAX_DECIMAL);
        } else {
            LexoInteger floorInteger = this.decimal.floor();
            LexoDecimal floorDecimal = LexoDecimal.from(floorInteger);
            LexoDecimal nextDecimal = floorDecimal.subtract(EIGHT_DECIMAL);
            if (nextDecimal.compareTo(MIN_DECIMAL) <= 0) {
                nextDecimal = between(MIN_DECIMAL, this.decimal);
            }

            return new LexoRank(this.bucket, nextDecimal);
        }
    }

    public LexoRank genNext() {
        this.fillDecimal();
        if (this.isMin()) {
            return new LexoRank(this.bucket, INITIAL_MIN_DECIMAL);
        } else {
            LexoInteger ceilInteger = this.decimal.ceil();
            LexoDecimal ceilDecimal = LexoDecimal.from(ceilInteger);
            LexoDecimal nextDecimal = ceilDecimal.add(EIGHT_DECIMAL);
            if (nextDecimal.compareTo(MAX_DECIMAL) >= 0) {
                nextDecimal = between(this.decimal, MAX_DECIMAL);
            }

            return new LexoRank(this.bucket, nextDecimal);
        }
    }

    public LexoRank between(LexoRank other) {
        return this.between(other, 0);
    }

    public LexoRank between(LexoRank other, int capacity) {
        this.fillDecimal();
        other.fillDecimal();
        if (!this.bucket.equals(other.bucket)) {
            throw new IllegalArgumentException("Between works only within the same bucket");
        } else {
            int cmp = this.decimal.compareTo(other.decimal);
            if (cmp > 0) {
                return new LexoRank(this.bucket, between(other.decimal, this.decimal, capacity));
            } else if (cmp == 0) {
                throw new IllegalArgumentException("Try to rank between issues with same rank this=" + this + " other=" + other + " this.decimal=" + this.decimal + " other.decimal=" + other.decimal);
            } else {
                return new LexoRank(this.bucket, between(this.decimal, other.decimal, capacity));
            }
        }
    }

    public static LexoDecimal between(LexoDecimal oLeft, LexoDecimal oRight) {
        return between(oLeft, oRight, 0);
    }

    public static LexoDecimal between(LexoDecimal left, LexoDecimal right, int spaceToRemain) {
        LexoNumeralSystem system = left.getSystem();
        if (system != right.getSystem()) {
            throw new IllegalArgumentException("Expected same system");
        } else {
            LexoDecimal space = right.subtract(left);
            int capacity = spaceToRemain + 2;
            LexoDecimal spacing = findSpacing(space, capacity);
            LexoDecimal floor = floorToSpacingDivisor(left, spacing);
            return roundToSpacing(left, floor, spacing);
        }
    }

    private static LexoDecimal findSpacing(LexoDecimal space, int capacity) {
        int capacityMagnitude = (int) Math.floor(Math.log((double) capacity) / Math.log((double) space.getSystem().getBase()));
        int spacingMagnitude = space.getOrderOfMagnitude() - capacityMagnitude;
        LexoDecimal lexoCapacity = LexoDecimal.fromInt(capacity, space.getSystem());
        Iterator var5 = getSystemBaseDivisors(space.getSystem(), spacingMagnitude).iterator();

        LexoDecimal spacingCandidate;
        do {
            if (!var5.hasNext()) {
                throw new RuntimeException("Could not find suitable distance for rank.");
            }

            spacingCandidate = (LexoDecimal) var5.next();
        } while (space.compareTo(spacingCandidate.multiply(lexoCapacity)) < 0);

        return spacingCandidate;
    }

    @SuppressWarnings("unchecked")
    private static List<LexoDecimal> getSystemBaseDivisors(LexoNumeralSystem lexoNumeralSystem, int magnitude) {
        int fractionMagnitude = magnitude * -1;
        int adjacentFractionMagnitude = fractionMagnitude + 1;
        return (List)ImmutableList.builder().addAll(LexoNumeralSystemHelper.getBaseDivisors(lexoNumeralSystem, fractionMagnitude)).addAll(LexoNumeralSystemHelper.getBaseDivisors(lexoNumeralSystem, adjacentFractionMagnitude)).build();
    }

    private static LexoDecimal floorToSpacingDivisor(LexoDecimal number, LexoDecimal spacing) {
        LexoDecimal zero = LexoDecimal.from(LexoInteger.zero(number.getSystem()));
        if (zero.equals(number)) {
            return spacing;
        } else {
            LexoInteger spacingsMag = spacing.getMag();
            int scaleDifference = number.getScale() + spacing.getOrderOfMagnitude();
            int spacingsMostSignificantDigit = spacingsMag.getMagSize() - 1;
//            int ceilLeastSignificantDigit = false;
            LexoInteger floor;
            for (floor = number.getMag().shiftRight(scaleDifference).add(LexoInteger.one(number.getSystem())); floor.getMag(0) % spacingsMag.getMag(spacingsMostSignificantDigit) != 0; floor = floor.add(LexoInteger.one(number.getSystem()))) {
//                ;
            }

            return number.getScale() - scaleDifference > 0 ? LexoDecimal.make(floor, number.getScale() - scaleDifference) : LexoDecimal.make(floor.shiftLeft(scaleDifference), number.getScale());
        }
    }

    private static LexoDecimal roundToSpacing(LexoDecimal number, LexoDecimal floor, LexoDecimal spacing) {
        LexoDecimal halfSpacing = spacing.multiply(LexoDecimal.half(spacing.getSystem()));
        LexoDecimal difference = floor.subtract(number);
        return difference.compareTo(halfSpacing) >= 0 ? floor : floor.add(spacing);
    }

    private static LexoDecimal mid(LexoDecimal left, LexoDecimal right) {
        LexoDecimal sum = left.add(right);
        LexoDecimal mid = sum.multiply(LexoDecimal.half(left.getSystem()));
        int scale = Math.max(left.getScale(), right.getScale());
        if (mid.getScale() > scale) {
            LexoDecimal roundDown = mid.setScale(scale, false);
            if (roundDown.compareTo(left) > 0) {
                return roundDown;
            }

            LexoDecimal roundUp = mid.setScale(scale, true);
            if (roundUp.compareTo(right) < 0) {
                return roundUp;
            }
        }

        return mid;
    }

    private void fillDecimal() {
        if (this.decimal == null) {
            String[] parts = this.value.split("\\|");
            this.bucket = LexoRankBucket.from(parts[0]);
            this.decimal = LexoDecimal.parse(parts[1], NUMERAL_SYSTEM);
        }

    }

    public LexoRankBucket getBucket() {
        this.fillDecimal();
        return this.bucket;
    }

    public LexoDecimal getDecimal() {
        this.fillDecimal();
        return this.decimal;
    }

    public LexoRank inNextBucket() {
        this.fillDecimal();
        return from(this.bucket.next(), this.decimal);
    }

    public LexoRank inPrevBucket() {
        this.fillDecimal();
        return from(this.bucket.prev(), this.decimal);
    }

    public boolean isMin() {
        this.fillDecimal();
        return this.decimal.equals(MIN_DECIMAL);
    }

    public boolean isMax() {
        this.fillDecimal();
        return this.decimal.equals(MAX_DECIMAL);
    }

    public String format() {
        return this.value;
    }

    public static String formatDecimal(LexoDecimal decimal) {
        String formatVal = decimal.format();
        StringBuilder val = new StringBuilder(formatVal);
        int partialIndex = formatVal.indexOf(NUMERAL_SYSTEM.getRadixPointChar());
        char zero = NUMERAL_SYSTEM.toChar(0);
        if (partialIndex < 0) {
            partialIndex = formatVal.length();
            val.append(NUMERAL_SYSTEM.getRadixPointChar());
        }

        while (partialIndex < 6) {
            val.insert(0, zero);
            ++partialIndex;
        }

        while (val.charAt(val.length() - 1) == zero) {
            val.setLength(val.length() - 1);
        }

        return val.toString();
    }

    public static LexoRank parse(String str) {
        return new LexoRank(str);
    }

    public static LexoRank from(LexoRankBucket bucket, LexoDecimal decimal) {
        if (decimal.getSystem() != NUMERAL_SYSTEM) {
            throw new IllegalArgumentException("Expected different system");
        } else {
            return new LexoRank(bucket, decimal);
        }
    }

    @Override
    public boolean equals(Object o) {
        return !(o instanceof LexoRank) ? false : this == o || this.value.equals(((LexoRank) o).value);
    }
    @Override
    public int hashCode() {
        return this.value.hashCode();
    }
    @Override
    public String toString() {
        return this.value;
    }
    @Override
    public int compareTo(LexoRank o) {
        return this.value.compareTo(o.value);
    }

    static {
        NUMERAL_SYSTEM = LexoNumeralSystem.BASE_36;
        ZERO_DECIMAL = LexoDecimal.parse("0", NUMERAL_SYSTEM);
        ONE_DECIMAL = LexoDecimal.parse("1", NUMERAL_SYSTEM);
        EIGHT_DECIMAL = LexoDecimal.parse("8", NUMERAL_SYSTEM);
        MIN_DECIMAL = ZERO_DECIMAL;
        MAX_DECIMAL = LexoDecimal.parse("1000000", NUMERAL_SYSTEM).subtract(ONE_DECIMAL);
        MID_DECIMAL = mid(MIN_DECIMAL, MAX_DECIMAL);
        INITIAL_MIN_DECIMAL = LexoDecimal.parse("100000", NUMERAL_SYSTEM);
        INITIAL_MAX_DECIMAL = LexoDecimal.parse(NUMERAL_SYSTEM.toChar(NUMERAL_SYSTEM.getBase() - 2) + "00000", NUMERAL_SYSTEM);
    }
}
