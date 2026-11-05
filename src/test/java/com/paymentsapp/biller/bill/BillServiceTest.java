package com.paymentsapp.biller.bill;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.paymentsapp.biller.gov.GovChargeService;
import com.paymentsapp.biller.provider.LookupMethod;
import com.paymentsapp.biller.provider.Provider;
import com.paymentsapp.biller.provider.ProviderDetails;
import com.paymentsapp.biller.provider.ProviderGroup;
import com.paymentsapp.biller.provider.ProviderRepository;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class BillServiceTest {

    private final ProviderRepository providers = mock(ProviderRepository.class);
    private final BillerProperties properties = new BillerProperties(Providers.rub("10"), 12, 50);
    private final BillService service = new BillService(providers, List.of(
            new MobileBill(providers),
            new GovFeeBill(),
            new GovChargeBill(mock(GovChargeService.class)),
            new GovDirectBill(properties),
            new FieldsBill(new FieldValidator(Clock.systemUTC(), properties), mock(InvoiceRepository.class))));

    private final Provider yota = Providers.provider(1, "yota", ProviderGroup.MOBILE);
    private final Provider mts = Providers.provider(2, "mts", ProviderGroup.MOBILE);

    @Test
    void mobileNumberOfAnotherOperatorIsRejected() {
        when(providers.details("yota")).thenReturn(Optional.of(Providers.details(yota, List.of(), null)));
        when(providers.mobileOperator("916", 1234567)).thenReturn(Optional.of(mts));
        when(providers.mobileOperator("996", 4488061)).thenReturn(Optional.of(yota));

        assertThatThrownBy(() -> service.check(new BillCheckRequest("yota", Map.of("phone", "+79161234567"), Providers.rub("100"), null)))
                .hasMessage("Номер обслуживается оператором mts");
        BillCheck ok = service.check(new BillCheckRequest("yota", Map.of("phone", "+79964488061"), Providers.rub("100"), null));
        assertThat(ok.purpose()).isEqualTo("Пополнение счёта +7 996 448-80-61");
        assertThat(ok.providerGroup()).isEqualTo(ProviderGroup.MOBILE);
    }

    @Test
    void govWithoutChargePaysAdministratorRequisites() {
        Provider fns = Providers.provider(3, "fns-inn", ProviderGroup.GOV);
        ProviderDetails details = Providers.details(fns, List.of(), Providers.gov(LookupMethod.INN, "УФК по г. Москве"));
        when(providers.details("fns-inn")).thenReturn(Optional.of(details));

        BillCheck check = service.check(new BillCheckRequest("fns-inn", Map.of("inn", "771234567859"), Providers.rub("5000"), null));
        assertThat(check.recipientName()).isEqualTo("УФК по г. Москве");
        assertThat(check.requisites()).containsEntry("kbk", "18811601123010001140");
        assertThatThrownBy(() -> service.check(new BillCheckRequest("fns-inn", Map.of("inn", "771234567859"), Providers.rub("5"), null)))
                .hasMessage("Минимальная сумма — 10 ₽");
    }

    @Test
    void unknownProvider() {
        when(providers.details("nope")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.check(new BillCheckRequest("nope", Map.of(), Providers.rub("1"), null)))
                .hasMessage("Поставщик не найден");
    }
}
