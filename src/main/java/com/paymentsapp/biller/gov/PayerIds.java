package com.paymentsapp.biller.gov;

import static com.paymentsapp.biller.error.BillerError.*;

import com.paymentsapp.biller.error.BillerError;
import com.paymentsapp.biller.provider.LookupMethod;
import com.paymentsapp.platform.error.DomainException;
import java.util.LinkedHashMap;
import java.util.Map;

// Проверка того, по чему ищем начисления: УИН, СТС, ВУ, ИНН физлица, паспорт
public final class PayerIds {

    // СТС и ВУ: серия из 4 символов (цифры или буквы) и номер из 6 цифр
    private static final String DOC = "\\d{2}[0-9А-ЯЁA-Z]{2}\\d{6}";

    private static final int PASSPORT_DIGITS = 10;

    private PayerIds() {
    }

    public static Map<String, String> check(LookupMethod lookup, String uinPrefix, Map<String, String> raw) {
        Map<String, String> ids = new LinkedHashMap<>();
        switch (lookup) {
            case UIN -> {
                String uin = digits(raw.get("uin"));
                require(uin != null && uin.matches("\\d{20}|\\d{25}"), UIN_FORMAT);
                require(uinPrefix == null || uin.startsWith(uinPrefix), UIN_PREFIX, uinPrefix);
                ids.put("uin", uin);
            }
            case STS_VU -> {
                String sts = doc(raw.get("sts"));
                String vu = doc(raw.get("vu"));
                require(sts != null || vu != null, STS_OR_VU_REQUIRED);
                require(sts == null || sts.matches(DOC), STS_FORMAT);
                require(vu == null || vu.matches(DOC), VU_FORMAT);
                ids.put("sts", sts);
                ids.put("vu", vu);
            }
            case INN -> {
                String inn = digits(raw.get("inn"));
                require(inn != null && personInn(inn), PERSON_INN);
                ids.put("inn", inn);
            }
            case PASSPORT_INN -> {
                String passport = digits(raw.get("passport"));
                String inn = digits(raw.get("inn"));
                require(passport != null || inn != null, PASSPORT_OR_INN_REQUIRED);
                require(passport == null || passport.length() == PASSPORT_DIGITS, PASSPORT_FORMAT);
                require(inn == null || personInn(inn), PERSON_INN);
                ids.put("passport", passport);
                ids.put("inn", inn);
            }
            case FREE -> throw new DomainException(NO_CHARGE_LOOKUP);
        }
        return ids;
    }

    // ИНН физлица — 12 цифр с двумя контрольными
    static boolean personInn(String inn) {
        if (!inn.matches("\\d{12}")) {
            return false;
        }
        int[] w1 = {7, 2, 4, 10, 3, 5, 9, 4, 6, 8};
        int[] w2 = {3, 7, 2, 4, 10, 3, 5, 9, 4, 6, 8};
        return check(inn, w1) == inn.charAt(10) - '0' && check(inn, w2) == inn.charAt(11) - '0';
    }

    private static int check(String inn, int[] weights) {
        int sum = 0;
        for (int i = 0; i < weights.length; i++) {
            sum += (inn.charAt(i) - '0') * weights[i];
        }
        return sum % 11 % 10;
    }

    private static String digits(String value) {
        String d = value == null ? "" : value.replaceAll("\\D", "");
        return d.isEmpty() ? null : d;
    }

    private static String doc(String value) {
        String d = value == null ? "" : value.replaceAll("\\s", "").toUpperCase();
        return d.isEmpty() ? null : d;
    }

    private static void require(boolean condition, BillerError error, Object... args) {
        if (!condition) {
            throw new DomainException(error, args);
        }
    }
}
