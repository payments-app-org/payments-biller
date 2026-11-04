package com.paymentsapp.biller.gov;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

// Проверка того, по чему ищем начисления: УИН, СТС, ВУ, ИНН физлица, паспорт
public final class PayerIds {

    // СТС и ВУ: серия из 4 символов (цифры или буквы) и номер из 6 цифр
    private static final String DOC = "\\d{2}[0-9А-ЯЁA-Z]{2}\\d{6}";

    private PayerIds() {
    }

    public static Map<String, String> check(String lookup, String uinPrefix, Map<String, String> raw) {
        Map<String, String> ids = new LinkedHashMap<>();
        switch (lookup) {
            case "UIN" -> {
                String uin = digits(raw.get("uin"));
                require(uin != null && uin.matches("\\d{20}|\\d{25}"), "УИН — 20 или 25 цифр");
                require(uinPrefix == null || uin.startsWith(uinPrefix), "УИН этого получателя начинается с " + uinPrefix);
                ids.put("uin", uin);
            }
            case "STS_VU" -> {
                String sts = doc(raw.get("sts"));
                String vu = doc(raw.get("vu"));
                require(sts != null || vu != null, "Укажите свидетельство о регистрации ТС или водительское удостоверение");
                require(sts == null || sts.matches(DOC), "Номер СТС — 10 символов: XX XX XXXXXX");
                require(vu == null || vu.matches(DOC), "Номер водительского удостоверения — 10 символов: XX XX XXXXXX");
                ids.put("sts", sts);
                ids.put("vu", vu);
            }
            case "INN" -> {
                String inn = digits(raw.get("inn"));
                require(inn != null && personInn(inn), "Некорректный ИНН физического лица");
                ids.put("inn", inn);
            }
            case "PASSPORT_INN" -> {
                String passport = digits(raw.get("passport"));
                String inn = digits(raw.get("inn"));
                require(passport != null || inn != null, "Укажите паспорт или ИНН");
                require(passport == null || passport.length() == 10, "Серия и номер паспорта — 10 цифр");
                require(inn == null || personInn(inn), "Некорректный ИНН физического лица");
                ids.put("passport", passport);
                ids.put("inn", inn);
            }
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "У этого получателя начисления не ищутся");
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

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
    }
}
