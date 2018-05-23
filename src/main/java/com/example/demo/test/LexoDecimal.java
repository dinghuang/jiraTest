package com.example.demo.test;

import org.apache.commons.lang.builder.HashCodeBuilder;

/**
 * @author dinghuang123@gmail.com
 * @since 2018/5/23
 */
public class LexoDecimal implements Comparable<LexoDecimal> {
    private final LexoInteger mag;
    private final int sig;

    private LexoDecimal(LexoInteger mag, int sig) {
        this.mag = mag;
        this.sig = sig;
    }

    public static LexoDecimal half(LexoNumeralSystem sys) {
        int mid = sys.getBase() / 2;
        return make(LexoInteger.make(sys, 1, new int[]{mid}), 1);
    }

    public static LexoDecimal parse(String str, LexoNumeralSystem system) {
        int partialIndex = str.indexOf(system.getRadixPointChar());
        if (str.lastIndexOf(system.getRadixPointChar()) != partialIndex) {
            throw new NumberFormatException("More than one " + system.getRadixPointChar());
        } else if (partialIndex < 0) {
            return make(LexoInteger.parse(str, system), 0);
        } else {
            String intStr = str.substring(0, partialIndex) + str.substring(partialIndex + 1);
            return make(LexoInteger.parse(intStr, system), str.length() - 1 - partialIndex);
        }
    }

    public static LexoDecimal from(LexoInteger integer) {
        return make(integer, 0);
    }

    public static LexoDecimal make(LexoInteger integer, int sig) {
        if (integer.isZero()) {
            return new LexoDecimal(integer, 0);
        } else {
            int zeroCount = 0;

            for (int i = 0; i < sig && integer.getMag(i) == 0; ++i) {
                ++zeroCount;
            }

            LexoInteger newInteger = integer.shiftRight(zeroCount);
            int newSig = sig - zeroCount;
            return new LexoDecimal(newInteger, newSig);
        }
    }

    public static LexoDecimal fromInt(int value, LexoNumeralSystem system) {
        char decimalPoint = LexoNumeralSystem.BASE_10.getRadixPointChar();
        char targetSystemPoint = system.getRadixPointChar();
        String lexoString = Integer.toString(value, system.getBase()).replace(decimalPoint, targetSystemPoint);
        return parse(lexoString, system);
    }

    public LexoNumeralSystem getSystem() {
        return this.mag.getSystem();
    }

    public int getOrderOfMagnitude() {
        return this.mag.getMagSize() - this.sig - 1;
    }

    public LexoDecimal add(LexoDecimal other) {
        LexoInteger tmag = this.mag;
        int tsig = this.sig;
        LexoInteger omag = other.mag;

        int osig;
        for (osig = other.sig; tsig < osig; ++tsig) {
            tmag = tmag.shiftLeft();
        }

        while (tsig > osig) {
            omag = omag.shiftLeft();
            ++osig;
        }

        return make(tmag.add(omag), tsig);
    }

    public LexoDecimal subtract(LexoDecimal other) {
        LexoInteger thisMag = this.mag;
        int thisSig = this.sig;
        LexoInteger otherMag = other.mag;

        int otherSig;
        for (otherSig = other.sig; thisSig < otherSig; ++thisSig) {
            thisMag = thisMag.shiftLeft();
        }

        while (thisSig > otherSig) {
            otherMag = otherMag.shiftLeft();
            ++otherSig;
        }

        return make(thisMag.subtract(otherMag), thisSig);
    }

    public LexoDecimal multiply(LexoDecimal other) {
        return make(this.mag.multiply(other.mag), this.sig + other.sig);
    }

    public LexoInteger floor() {
        return this.mag.shiftRight(this.sig);
    }

    public LexoInteger ceil() {
        if (this.isExact()) {
            return this.mag;
        } else {
            LexoInteger floor = this.floor();
            return floor.add(LexoInteger.one(floor.getSystem()));
        }
    }

    public boolean isExact() {
        if (this.sig == 0) {
            return true;
        } else {
            for (int i = 0; i < this.sig; ++i) {
                if (this.mag.getMag(i) != 0) {
                    return false;
                }
            }

            return true;
        }
    }

    public LexoInteger getMag() {
        return this.mag;
    }

    public int getScale() {
        return this.sig;
    }

    public LexoDecimal setScale(int nsig) {
        return this.setScale(nsig, false);
    }

    public LexoDecimal setScale(int nsig, boolean ceiling) {
        if (nsig >= this.sig) {
            return this;
        } else {
            if (nsig < 0) {
                nsig = 0;
            }

            int diff = this.sig - nsig;
            LexoInteger nmag = this.mag.shiftRight(diff);
            if (ceiling) {
                nmag = nmag.add(LexoInteger.one(nmag.getSystem()));
            }

            return make(nmag, nsig);
        }
    }

    public int compareTo(LexoDecimal o) {
        LexoInteger tMag = this.mag;
        LexoInteger oMag = o.mag;
        if (this.sig > o.sig) {
            oMag = oMag.shiftLeft(this.sig - o.sig);
        } else if (this.sig < o.sig) {
            tMag = tMag.shiftLeft(o.sig - this.sig);
        }

        return tMag.compareTo(oMag);
    }

    public String format() {
        String intStr = this.mag.format();
        if (this.sig == 0) {
            return intStr;
        } else {
            StringBuilder sb = new StringBuilder(intStr);
            char head = sb.charAt(0);
            boolean specialHead = head == this.mag.getSystem().getPositiveChar() || head == this.mag.getSystem().getNegativeChar();
            if (specialHead) {
                sb.deleteCharAt(0);
            }

            while (sb.length() < this.sig + 1) {
                sb.insert(0, this.mag.getSystem().toChar(0));
            }

            sb.insert(sb.length() - this.sig, this.mag.getSystem().getRadixPointChar());
            if (sb.length() - this.sig == 0) {
                sb.insert(0, this.mag.getSystem().toChar(0));
            }

            if (specialHead) {
                sb.insert(0, head);
            }

            return sb.toString();
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof LexoDecimal)) {
            return false;
        } else {
            LexoDecimal o = (LexoDecimal) obj;
            return this.mag.equals(o.mag) && this.sig == o.sig;
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