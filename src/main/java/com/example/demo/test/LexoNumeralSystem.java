package com.example.demo.test;

/**
 * @author dinghuang123@gmail.com
 * @since 2018/5/23
 */
public interface LexoNumeralSystem {
    LexoNumeralSystem BASE_10 = new LexoNumeralSystem() {
        @Override
        public int getBase() {
            return 10;
        }

        @Override
        public char getPositiveChar() {
            return '+';
        }

        @Override
        public char getNegativeChar() {
            return '-';
        }

        @Override
        public char getRadixPointChar() {
            return '.';
        }

        @Override
        public int toDigit(char ch) {
            if (ch >= 48 && ch <= 57) {
                return ch - 48;
            } else {
                throw new IllegalArgumentException("Not valid digit: " + ch);
            }
        }

        @Override
        public char toChar(int digit) {
            return (char) (digit + 48);
        }
    };
    LexoNumeralSystem BASE_36 = new LexoNumeralSystem() {
        private final char[] DIGITS = "0123456789abcdefghijklmnopqrstuvwxyz".toCharArray();

        @Override
        public int getBase() {
            return 36;
        }

        @Override
        public char getPositiveChar() {
            return '+';
        }

        @Override
        public char getNegativeChar() {
            return '-';
        }

        @Override
        public char getRadixPointChar() {
            return ':';
        }

        @Override
        public int toDigit(char ch) {
            if (ch >= 48 && ch <= 57) {
                return ch - 48;
            } else if (ch >= 97 && ch <= 122) {
                return ch - 97 + 10;
            } else {
                throw new IllegalArgumentException("Not valid digit: " + ch);
            }
        }

        @Override
        public char toChar(int digit) {
            return this.DIGITS[digit];
        }
    };
    LexoNumeralSystem BASE_64 = new LexoNumeralSystem() {
        private final char[] DIGITS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ^_abcdefghijklmnopqrstuvwxyz".toCharArray();

        @Override
        public int getBase() {
            return 64;
        }

        @Override
        public char getPositiveChar() {
            return '+';
        }

        @Override
        public char getNegativeChar() {
            return '-';
        }

        @Override
        public char getRadixPointChar() {
            return ':';
        }

        @Override
        public int toDigit(char ch) {
            if (ch >= 48 && ch <= 57) {
                return ch - 48;
            } else if (ch >= 65 && ch <= 90) {
                return ch - 65 + 10;
            } else if (ch == 94) {
                return 36;
            } else if (ch == 95) {
                return 37;
            } else if (ch >= 97 && ch <= 122) {
                return ch - 97 + 38;
            } else {
                throw new IllegalArgumentException("Not valid digit: " + ch);
            }
        }

        @Override
        public char toChar(int digit) {
            return this.DIGITS[digit];
        }
    };

    int getBase();

    char getPositiveChar();

    char getNegativeChar();

    char getRadixPointChar();

    int toDigit(char var1);

    char toChar(int var1);
}
