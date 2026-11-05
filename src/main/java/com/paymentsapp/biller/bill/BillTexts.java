package com.paymentsapp.biller.bill;

import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

// Как значения показываются людям: телефон, период оплаты; реквизиты получателя в бюджете
public final class BillTexts {

    private static final Locale RUSSIAN = Locale.forLanguageTag("ru");
    private static final int AREA_FROM = 2;
    private static final int AREA_TO = 5;
    private static final int FIRST_TO = 8;
    private static final int SECOND_TO = 10;

    private BillTexts() {
    }

    // +79964480061 → +7 996 448-80-61
    public static String ruPhone(String phone) {
        return "+7 " + phone.substring(AREA_FROM, AREA_TO) + " " + phone.substring(AREA_TO, FIRST_TO) + "-"
                + phone.substring(FIRST_TO, SECOND_TO) + "-" + phone.substring(SECOND_TO);
    }

    // 2026-11 → «ноябрь 2026»
    public static String month(YearMonth ym) {
        return ym.getMonth().getDisplayName(TextStyle.FULL_STANDALONE, RUSSIAN) + " " + ym.getYear();
    }

    // Реквизиты получателя в бюджете — в платёж (payment хранит их в recipient_details)
    public static Map<String, String> budgetRequisites(String uin, String name, String inn, String kpp, String account,
                                                       String bik, String kbk, String oktmo) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put(Requisite.UIN, uin);
        m.put(Requisite.NAME, name);
        m.put(Requisite.INN, inn);
        m.put(Requisite.KPP, kpp);
        m.put(Requisite.ACCOUNT, account);
        m.put(Requisite.BIK, bik);
        m.put(Requisite.KBK, kbk);
        m.put(Requisite.OKTMO, oktmo);
        return m;
    }

    static final class Requisite {
        static final String UIN = "uin";
        static final String NAME = "name";
        static final String INN = "inn";
        static final String KPP = "kpp";
        static final String ACCOUNT = "account";
        static final String BIK = "bik";
        static final String KBK = "kbk";
        static final String OKTMO = "oktmo";

        private Requisite() {
        }
    }
}
