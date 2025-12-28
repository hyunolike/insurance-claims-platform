package com.insurance.claim.domain.vo;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

public class ClaimNumber {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final Pattern FORMAT =
            Pattern.compile("^CLM-\\d{8}-[A-Z0-9]{5}$", Pattern.CASE_INSENSITIVE);
    private static final char[] CHARSET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();

    private final String value;

    private ClaimNumber(String value) {
        if (!FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException("잘못된 청구 번호 형식입니다.");
        }
        this.value = value.toUpperCase(Locale.ROOT);
    }

    public static ClaimNumber generate() {
        String date = DATE_FORMATTER.format(LocalDate.now());
        StringBuilder suffix = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            suffix.append(CHARSET[RANDOM.nextInt(CHARSET.length)]);
        }
        return new ClaimNumber("CLM-" + date + "-" + suffix);
    }

    public static ClaimNumber from(String value) {
        Objects.requireNonNull(value, "청구 번호는 필수입니다.");
        return new ClaimNumber(value);
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ClaimNumber claimNumber)) {
            return false;
        }
        return value.equals(claimNumber.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
