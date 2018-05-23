package com.example.demo.test;

import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author dinghuang123@gmail.com
 * @since 2018/5/23
 */
public class LexoNumeralSystemHelper {

    private static final List<LexoInteger> BASE_36_DIVIDERS;

    public LexoNumeralSystemHelper() {
    }

    @SuppressWarnings("unchecked")
    public static List<LexoDecimal> getBaseDivisors(LexoNumeralSystem lexoNumeralSystem, int fractionMagnitude) {
        int base = lexoNumeralSystem.getBase();
        if (base == LexoNumeralSystem.BASE_36.getBase()) {
            return fractionMagnitude < 0 ? (List) BASE_36_DIVIDERS.stream().map((lexoInteger) -> {
                return LexoDecimal.make(lexoInteger.shiftLeft(fractionMagnitude * -1), 0);
            }).collect(Collectors.toList()) : (List) BASE_36_DIVIDERS.stream().map((lexoInteger) -> {
                return LexoDecimal.make(lexoInteger, fractionMagnitude);
            }).collect(Collectors.toList());
        } else {
            throw new RuntimeException("Unsupported numeral system base: " + base);
        }
    }

    static {
        BASE_36_DIVIDERS = (List) ImmutableList.builder().add(LexoInteger.make(LexoNumeralSystem.BASE_36, 1, new int[]{18}))
                .add(LexoInteger.make(LexoNumeralSystem.BASE_36, 1, new int[]{12})).
                        add(LexoInteger.make(LexoNumeralSystem.BASE_36, 1, new int[]{9})).
                        add(LexoInteger.make(LexoNumeralSystem.BASE_36, 1, new int[]{6})).
                        add(LexoInteger.make(LexoNumeralSystem.BASE_36, 1, new int[]{4})).
                        add(LexoInteger.make(LexoNumeralSystem.BASE_36, 1, new int[]{3})).
                        add(LexoInteger.make(LexoNumeralSystem.BASE_36, 1, new int[]{2})).
                        add(LexoInteger.make(LexoNumeralSystem.BASE_36, 1, new int[]{1})).build();
    }

}
