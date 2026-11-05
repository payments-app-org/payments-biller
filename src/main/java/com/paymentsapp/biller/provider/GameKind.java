package com.paymentsapp.biller.provider;

// Что получит покупатель в играх и сервисах. Ваучер и код подписки выпускает поставщик и шлёт на почту.
public enum GameKind {
    ACCOUNT_TOPUP,
    IN_GAME_CURRENCY,
    SUBSCRIPTION,
    VOUCHER;

    public boolean issuesVoucher() {
        return this == VOUCHER || this == SUBSCRIPTION;
    }
}
