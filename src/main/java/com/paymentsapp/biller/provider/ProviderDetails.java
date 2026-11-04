package com.paymentsapp.biller.provider;

// Карточка поставщика: общие поля + поля его группы (у госуслуг — gov, у остальных пока ничего)
public record ProviderDetails(Provider provider, GovDetails gov) {
}
