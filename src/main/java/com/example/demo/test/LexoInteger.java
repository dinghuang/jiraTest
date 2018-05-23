package com.example.demo.test;

import org.apache.commons.lang.builder.HashCodeBuilder;

import java.util.Arrays;

/**
 * @author dinghuang123@gmail.com
 * @since 2018/5/23
 */
public class LexoInteger implements Comparable<LexoInteger> {
    private static int[] ZERO_MAG = new int[]{0};
    private static int[] ONE_MAG = new int[]{1};
    public static final int NEGATIVE_SIGN = -1;
    public static final int ZERO_SIGN = 0;
    public static final int POSITIVE_SIGN = 1;
    private final LexoNumeralSystem sys;
    private final int sign;
    private final int[] mag;

    private LexoInteger(LexoNumeralSystem system, int sign, int[] mag) {
        this.sys = system;
        this.sign = sign;
        this.mag = mag;
    }

    public LexoInteger add(LexoInteger other) {
        this.checkSystem(other);
        if (this.isZero()) {
            return other;
        } else if (other.isZero()) {
            return this;
        } else if (this.sign != other.sign) {
            LexoInteger pos;
            if (this.sign == -1) {
                pos = this.negate();
                LexoInteger val = pos.subtract(other);
                return val.negate();
            } else {
                pos = other.negate();
                return this.subtract(pos);
            }
        } else {
            int[] result = add(this.sys, this.mag, other.mag);
            return make(this.sys, this.sign, result);
        }
    }

    private static int[] add(LexoNumeralSystem sys, int[] l, int[] r) {
        int estimatedSize = Math.max(l.length, r.length);
        int[] result = new int[estimatedSize];
        int carry = 0;

        for (int i = 0; i < estimatedSize; ++i) {
            int lnum = i < l.length ? l[i] : 0;
            int rnum = i < r.length ? r[i] : 0;
            int sum = lnum + rnum + carry;

            for (carry = 0; sum >= sys.getBase(); sum -= sys.getBase()) {
                ++carry;
            }

            result[i] = sum;
        }

        return extendWithCarry(result, carry);
    }

    private static int[] extendWithCarry(int[] mag, int carry) {
        int[] result = mag;
        if (carry > 0) {
            int[] extendedMag = new int[mag.length + 1];
            System.arraycopy(mag, 0, extendedMag, 0, mag.length);
            extendedMag[extendedMag.length - 1] = carry;
            result = extendedMag;
        }

        return result;
    }

    public LexoInteger subtract(LexoInteger other) {
        this.checkSystem(other);
        if (this.isZero()) {
            return other.negate();
        } else if (other.isZero()) {
            return this;
        } else if (this.sign != other.sign) {
            LexoInteger negate;
            if (this.sign == -1) {
                negate = this.negate();
                LexoInteger sum = negate.add(other);
                return sum.negate();
            } else {
                negate = other.negate();
                return this.add(negate);
            }
        } else {
            int cmp = compare(this.mag, other.mag);
            return cmp == 0 ? zero(this.sys) : (cmp < 0 ? make(this.sys, this.sign == -1 ? 1 : -1, subtract(this.sys, other.mag, this.mag)) : make(this.sys, this.sign == -1 ? -1 : 1, subtract(this.sys, this.mag, other.mag)));
        }
    }

    private static int[] subtract(LexoNumeralSystem sys, int[] l, int[] r) {
        int[] rComplement = complement(sys, r, l.length);
        int[] rSum = add(sys, l, rComplement);
        rSum[rSum.length - 1] = 0;
        return add(sys, rSum, ONE_MAG);
    }

    public LexoInteger multiply(LexoInteger other) {
        this.checkSystem(other);
        if (this.isZero()) {
            return this;
        } else if (other.isZero()) {
            return other;
        } else if (this.isOneish()) {
            return this.sign == other.sign ? make(this.sys, 1, other.mag) : make(this.sys, -1, other.mag);
        } else if (other.isOneish()) {
            return this.sign == other.sign ? make(this.sys, 1, this.mag) : make(this.sys, -1, this.mag);
        } else {
            int[] newMag = multiply(this.sys, this.mag, other.mag);
            return this.sign == other.sign ? make(this.sys, 1, newMag) : make(this.sys, -1, newMag);
        }
    }

    private static int[] multiply(LexoNumeralSystem sys, int[] l, int[] r) {
        int[] result = new int[l.length + r.length];

        for (int li = 0; li < l.length; ++li) {
            for (int ri = 0; ri < r.length; ++ri) {
                int resultIndex = li + ri;

                for (result[resultIndex] += l[li] * r[ri]; result[resultIndex] >= sys.getBase(); result[resultIndex] -= sys.getBase()) {
                    ++result[resultIndex + 1];
                }
            }
        }

        return result;
    }

    public LexoInteger negate() {
        return this.isZero() ? this : make(this.sys, this.sign == 1 ? -1 : 1, this.mag);
    }

    public LexoInteger shiftLeft() {
        return this.shiftLeft(1);
    }

    public LexoInteger shiftLeft(int times) {
        if (times == 0) {
            return this;
        } else if (times < 0) {
            return this.shiftRight(Math.abs(times));
        } else {
            int[] nmag = new int[this.mag.length + times];
            System.arraycopy(this.mag, 0, nmag, times, this.mag.length);
            return make(this.sys, this.sign, nmag);
        }
    }

    public LexoInteger shiftRight() {
        return this.shiftRight(1);
    }

    public LexoInteger shiftRight(int times) {
        if (times == 0) {
            return this;
        } else if (times < 0) {
            return this.shiftLeft(Math.abs(times));
        } else if (this.mag.length - times <= 0) {
            return zero(this.sys);
        } else {
            int[] nmag = new int[this.mag.length - times];
            System.arraycopy(this.mag, times, nmag, 0, nmag.length);
            return make(this.sys, this.sign, nmag);
        }
    }

    public LexoInteger complement() {
        return this.complement(this.mag.length);
    }

    public LexoInteger complement(int digits) {
        return make(this.sys, this.sign, complement(this.sys, this.mag, digits));
    }

    private static int[] complement(LexoNumeralSystem sys, int[] mag, int digits) {
        if (digits <= 0) {
            throw new IllegalArgumentException("Expected at least 1 digit");
        } else {
            int[] nmag = new int[digits];
            Arrays.fill(nmag, sys.getBase() - 1);

            for (int i = 0; i < mag.length; ++i) {
                nmag[i] = sys.getBase() - 1 - mag[i];
            }

            return nmag;
        }
    }

    public boolean isZero() {
        return this.sign == 0 && this.mag.length == 1 && this.mag[0] == 0;
    }

    private boolean isOneish() {
        return this.mag.length == 1 && this.mag[0] == 1;
    }

    public boolean isOne() {
        return this.sign == 1 && this.mag.length == 1 && this.mag[0] == 1;
    }

    int getMag(int index) {
        return this.mag[index];
    }

    int getMagSize() {
        return this.mag.length;
    }

    public int compareTo(LexoInteger o) {
        if (this.sign == -1) {
            if (o.sign == -1) {
                int cmp = compare(this.mag, o.mag);
                return cmp == -1 ? 1 : (cmp == 1 ? -1 : 0);
            } else {
                return -1;
            }
        } else {
            return this.sign == 1 ? (o.sign == 1 ? compare(this.mag, o.mag) : 1) : (o.sign == -1 ? 1 : (o.sign == 1 ? -1 : 0));
        }
    }

    private static int compare(int[] l, int[] r) {
        if (l.length < r.length) {
            return -1;
        } else if (l.length > r.length) {
            return 1;
        } else {
            for (int i = l.length - 1; i >= 0; --i) {
                if (l[i] < r[i]) {
                    return -1;
                }

                if (l[i] > r[i]) {
                    return 1;
                }
            }

            return 0;
        }
    }

    public LexoNumeralSystem getSystem() {
        return this.sys;
    }

    private void checkSystem(LexoInteger other) {
        if (this.sys != other.sys) {
            throw new IllegalArgumentException("Expected numbers of same numeral sys");
        }
    }

    public String format() {
        if (this.isZero()) {
            return "" + this.sys.toChar(0);
        } else {
            StringBuilder sb = new StringBuilder();
            int[] var2 = this.mag;
            int var3 = var2.length;

            for (int var4 = 0; var4 < var3; ++var4) {
                int digit = var2[var4];
                sb.insert(0, this.sys.toChar(digit));
            }

            if (this.sign == -1) {
                sb.insert(0, this.sys.getNegativeChar());
            }

            return sb.toString();
        }
    }

    public static LexoInteger parse(String strFull, LexoNumeralSystem system) {
        String str = strFull;
        int sign = 1;
        if (strFull.indexOf(system.getPositiveChar()) == 0) {
            str = strFull.substring(1);
        } else if (strFull.indexOf(system.getNegativeChar()) == 0) {
            str = strFull.substring(1);
            sign = -1;
        }

        int[] mag = new int[str.length()];
        int strIndex = mag.length - 1;

        for (int magIndex = 0; strIndex >= 0; ++magIndex) {
            mag[magIndex] = system.toDigit(str.charAt(strIndex));
            --strIndex;
        }

        return make(system, sign, mag);
    }

    protected static LexoInteger zero(LexoNumeralSystem sys) {
        return new LexoInteger(sys, 0, ZERO_MAG);
    }

    protected static LexoInteger one(LexoNumeralSystem sys) {
        return make(sys, 1, ONE_MAG);
    }

    public static LexoInteger make(LexoNumeralSystem sys, int sign, int[] mag) {
        int actualLength;
        for (actualLength = mag.length; actualLength > 0 && mag[actualLength - 1] == 0; --actualLength) {
            ;
        }

        if (actualLength == 0) {
            return zero(sys);
        } else if (actualLength == mag.length) {
            return new LexoInteger(sys, sign, mag);
        } else {
            int[] nmag = new int[actualLength];
            System.arraycopy(mag, 0, nmag, 0, actualLength);
            return new LexoInteger(sys, sign, nmag);
        }
    }
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof LexoInteger)) {
            return false;
        } else {
            LexoInteger o = (LexoInteger) obj;
            return this.sys == o.sys && this.compareTo(o) == 0;
        }
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this, false);
    }

    @Override
    public String toString() {
        return this.format();
    }
}