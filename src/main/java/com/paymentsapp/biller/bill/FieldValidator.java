package com.paymentsapp.biller.bill;

import static com.paymentsapp.biller.error.BillerError.*;

import com.paymentsapp.biller.bill.BillCheck.BillField;
import com.paymentsapp.biller.error.BillerError;
import com.paymentsapp.biller.provider.ProviderDetails;
import com.paymentsapp.biller.provider.ProviderDetails.Field;
import com.paymentsapp.biller.provider.ProviderDetails.ForeignMobile;
import com.paymentsapp.biller.provider.ProviderDetails.Nominal;
import com.paymentsapp.biller.provider.ProviderDetails.Option;
import com.paymentsapp.platform.error.DomainException;
import java.time.Clock;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

// Поля формы поставщика по шаблону (provider_fields): обязательность, формат по типу, номинал, период оплаты.
// value — что отдать поставщику, display — что показать людям.
@Component
public class FieldValidator {

    static final Pattern EMAIL = Pattern.compile("[^@\\s]+@[^@\\s]+\\.[^@\\s]{2,}");
    static final Pattern RU_PHONE = Pattern.compile("\\+7\\d{10}");
    private static final Pattern SEPARATORS = Pattern.compile("[\\s-]");
    private static final Pattern NON_DIGITS = Pattern.compile("\\D");
    private static final Pattern ONLY_DIGITS = Pattern.compile("\\d+");
    private static final String PREFIX_SEPARATOR = ", ";
    // Подсказку показываем в тексте ошибки, только если она короткая
    private static final int HINT_IN_ERROR_MAX = 40;

    private final Clock clock;
    private final BillerProperties properties;

    public FieldValidator(Clock clock, BillerProperties properties) {
        this.clock = clock;
        this.properties = properties;
    }

    // Проверенные поля, выбранный номинал и главное значение для назначения платежа
    public record Validated(List<BillField> fields, Nominal nominal, String main) {
    }

    public Validated validate(ProviderDetails details, List<Field> fields, Map<String, String> input) {
        List<BillField> out = new ArrayList<>();
        Nominal nominal = null;
        String main = null;
        for (Field f : fields) {
            String raw = input.getOrDefault(f.key(), "").trim();
            if (raw.isEmpty()) {
                require(!f.required(), FIELD_REQUIRED, f.label());
                continue;
            }
            String value = raw;
            String display = raw;
            switch (f.type()) {
                case DIGITS -> {
                    value = SEPARATORS.matcher(raw).replaceAll("");
                    boolean valid = ONLY_DIGITS.matcher(value).matches() && (f.pattern() == null || value.matches(f.pattern()));
                    if (!valid) {
                        throw f.hint() != null && f.hint().length() < HINT_IN_ERROR_MAX
                                ? new DomainException(FIELD_INVALID_HINT, f.label(), f.hint())
                                : new DomainException(FIELD_INVALID, f.label());
                    }
                    display = value;
                }
                case TEXT -> require(f.pattern() == null || raw.matches(f.pattern()), FIELD_INVALID, f.label());
                case EMAIL -> require(EMAIL.matcher(raw).matches(), EMAIL_INVALID);
                case PHONE -> {
                    require(RU_PHONE.matcher(raw).matches(), PHONE_FORMAT);
                    display = BillTexts.ruPhone(raw);
                }
                case LOCAL_PHONE -> {
                    value = localPhone(details.foreignMobile(), raw);
                    display = "+" + details.foreignMobile().dialCode() + " " + value;
                }
                case MONTH -> display = BillTexts.month(month(raw, f.label()));
                case CHOICE -> display = f.options().stream().filter(o -> o.value().equals(raw)).findFirst()
                        .map(Option::label)
                        .orElseThrow(() -> new DomainException(CHOICE_REQUIRED, f.label()));
                case NOMINAL -> {
                    String region = f.dependsOn() == null ? null : input.get(f.dependsOn());
                    nominal = details.nominals().stream()
                            .filter(n -> String.valueOf(n.id()).equals(raw)
                                    && (f.dependsOn() == null || Objects.equals(n.region(), region)))
                            .findFirst()
                            .orElseThrow(() -> new DomainException(NOMINAL_REQUIRED));
                    display = nominal.name();
                }
            }
            out.add(new BillField(f.key(), f.label(), value, display));
            if (main == null && f.type().describesPayment()) {
                main = display;
            }
        }
        return new Validated(out, nominal, main);
    }

    // Оплатить можно на monthRange месяцев назад и вперёд
    private YearMonth month(String raw, String label) {
        YearMonth ym;
        try {
            ym = YearMonth.parse(raw);
        } catch (DateTimeParseException e) {
            throw new DomainException(MONTH_REQUIRED, label);
        }
        YearMonth now = YearMonth.now(clock);
        require(!ym.isBefore(now.minusMonths(properties.monthRange())) && !ym.isAfter(now.plusMonths(properties.monthRange())),
                MONTH_OUT_OF_RANGE, properties.monthRange());
        return ym;
    }

    // Номер без кода страны: длина и начало — по формату страны
    private static String localPhone(ForeignMobile fm, String raw) {
        String digits = NON_DIGITS.matcher(raw).replaceAll("");
        if (digits.startsWith(fm.dialCode()) && digits.length() == fm.dialCode().length() + fm.numberLength()) {
            digits = digits.substring(fm.dialCode().length());
        }
        require(digits.length() == fm.numberLength(), LOCAL_PHONE_LENGTH, fm.numberLength(), fm.dialCode());
        String number = digits;
        require(fm.prefixes().isEmpty() || fm.prefixes().stream().anyMatch(number::startsWith),
                LOCAL_PHONE_PREFIX, String.join(PREFIX_SEPARATOR, fm.prefixes()));
        return digits;
    }

    static void require(boolean condition, BillerError error, Object... args) {
        if (!condition) {
            throw new DomainException(error, args);
        }
    }
}
