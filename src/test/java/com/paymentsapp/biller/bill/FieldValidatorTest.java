package com.paymentsapp.biller.bill;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.paymentsapp.biller.provider.FieldType;
import com.paymentsapp.biller.provider.ProviderDetails;
import com.paymentsapp.biller.provider.ProviderGroup;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class FieldValidatorTest {

    private static final int MONTH_RANGE = 12;
    private final FieldValidator validator = new FieldValidator(
            Clock.fixed(Instant.parse("2026-11-05T09:00:00Z"), ZoneId.of("Europe/Moscow")),
            new BillerProperties(Providers.rub("10"), MONTH_RANGE, 50));

    private final ProviderDetails troika = Providers.details(Providers.provider(1, "troika", ProviderGroup.TRANSPORT), List.of(
            Providers.field("card", FieldType.DIGITS, true, "\\d{10}"),
            Providers.field("email", FieldType.EMAIL, false, null),
            Providers.field("period", FieldType.MONTH, false, null)), null);

    @Test
    void digitsAreCleanedAndFirstMeaningfulFieldIsMain() {
        FieldValidator.Validated v = validator.validate(troika, troika.fields(),
                Map.of("card", "12345 67890", "email", "a@b.ru", "period", "2026-12"));
        assertThat(v.fields()).extracting(BillCheck.BillField::value).containsExactly("1234567890", "a@b.ru", "2026-12");
        assertThat(v.fields().get(2).display()).isEqualTo("декабрь 2026");
        assertThat(v.main()).isEqualTo("1234567890");
    }

    @Test
    void requiredWrongFormatAndPeriodOutOfRange() {
        assertThatThrownBy(() -> validator.validate(troika, troika.fields(), Map.of()))
                .hasMessage("Заполните поле «card»");
        assertThatThrownBy(() -> validator.validate(troika, troika.fields(), Map.of("card", "123")))
                .hasMessage("Проверьте поле «card»");
        assertThatThrownBy(() -> validator.validate(troika, troika.fields(), Map.of("card", "1234567890", "period", "2028-01")))
                .hasMessageContaining("не дальше 12 месяцев");
    }
}
