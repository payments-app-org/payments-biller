package com.paymentsapp.biller.bill;

import static com.paymentsapp.biller.bill.BillException.Code.*;

import com.paymentsapp.biller.bill.BillCheck.BillField;
import com.paymentsapp.biller.bill.InvoiceRepository.Invoice;
import com.paymentsapp.biller.gov.GovCharge;
import com.paymentsapp.biller.gov.GovChargeService;
import com.paymentsapp.biller.provider.GovDetails;
import com.paymentsapp.biller.provider.Provider;
import com.paymentsapp.biller.provider.ProviderDetails;
import com.paymentsapp.biller.provider.ProviderDetails.Field;
import com.paymentsapp.biller.provider.ProviderDetails.ForeignMobile;
import com.paymentsapp.biller.provider.ProviderDetails.Nominal;
import com.paymentsapp.biller.provider.ProviderDetails.Option;
import com.paymentsapp.biller.provider.ProviderRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

// Проверка платежа поставщику перед созданием платежа: поля по шаблону поставщика, сумма, номинал.
// Связь — оператор по номеру, госуслуги — начисление из ГИС ГМП или реквизиты квитанции.
@Service
public class BillService {

    private static final Pattern EMAIL = Pattern.compile("[^@\\s]+@[^@\\s]+\\.[^@\\s]{2,}");
    private static final Pattern RU_PHONE = Pattern.compile("\\+7\\d{10}");

    private final ProviderRepository providers;
    private final GovChargeService charges;
    private final InvoiceRepository invoices;

    public BillService(ProviderRepository providers, GovChargeService charges, InvoiceRepository invoices) {
        this.providers = providers;
        this.charges = charges;
        this.invoices = invoices;
    }

    public BillCheck check(BillCheckRequest req) {
        ProviderDetails details = providers.details(req.providerCode() == null ? "" : req.providerCode())
                .orElseThrow(() -> new BillException(INVALID_REQUISITES, "Поставщик не найден"));
        Provider p = details.provider();
        Map<String, String> input = req.fields() == null ? Map.of() : req.fields();
        BigDecimal amount = req.amount() == null ? BigDecimal.ZERO : req.amount();
        return switch (p.groupCode()) {
            case "MOBILE" -> mobile(p, input);
            case "GOV" -> details.gov() != null && "FREE".equals(details.gov().lookup())
                    ? govFee(p, details.gov(), input)
                    : govCharge(p, req, amount);
            default -> byFields(details, input, amount, req.chargeId());
        };
    }

    // Связь: номер должен принадлежать этому оператору
    private BillCheck mobile(Provider p, Map<String, String> input) {
        String phone = input.getOrDefault("phone", "").trim();
        require(RU_PHONE.matcher(phone).matches(), "Телефон в формате +7XXXXXXXXXX");
        Provider operator = providers.mobileOperator(phone.substring(2, 5), Integer.parseInt(phone.substring(5)))
                .orElseThrow(() -> new BillException(RECIPIENT_NOT_FOUND, "Не удалось определить оператора по номеру"));
        if (operator.id() != p.id()) {
            throw new BillException(RECIPIENT_NOT_FOUND, "Номер обслуживается оператором " + operator.name());
        }
        return new BillCheck(p.code(), p.groupCode(), p.name(), "Пополнение счёта " + ruPhone(phone),
                List.of(new BillField("phone", "Номер телефона", phone, ruPhone(phone))), null, null, phone);
    }

    // Найденное начисление: сумма и получатель — из него, а не из запроса
    private BillCheck govCharge(Provider p, BillCheckRequest req, BigDecimal amount) {
        require(req.chargeId() != null, "Не выбрано начисление");
        GovCharge c = charges.get(req.chargeId()).orElseThrow(() -> new BillException(BILL_NOT_PAYABLE, "Начисление не найдено"));
        if (!"UNPAID".equals(c.status())) {
            throw new BillException(BILL_NOT_PAYABLE, "Начисление уже оплачено");
        }
        if (c.payableAmount().compareTo(amount) != 0) {
            throw new BillException(BILL_NOT_PAYABLE, "Сумма не совпадает с начислением: " + c.payableAmount() + " ₽");
        }
        Map<String, String> requisites = requisites(c.uin(), c.payeeName(), c.payeeInn(), c.payeeKpp(), c.payeeAccount(),
                c.payeeBik(), c.kbk(), c.oktmo());
        return new BillCheck(p.code(), p.groupCode(), c.payeeName(), c.purpose(),
                List.of(new BillField("uin", "УИН", c.uin(), c.uin())), requisites, c.uin() + "/" + c.id(), null);
    }

    // Госпошлина по квитанции (Росреестр): УИН и данные плательщика вводит пользователь, получатель — из gov_providers
    private BillCheck govFee(Provider p, GovDetails gov, Map<String, String> f) {
        String uin = f.getOrDefault("uin", "").replaceAll("\\D", "");
        String payerName = f.getOrDefault("payerName", "").trim();
        String payerAddress = f.getOrDefault("payerAddress", "").trim();
        String payerPhone = f.getOrDefault("payerPhone", "").trim();
        require(uin.matches("\\d{20}|\\d{25}"), "УИН — 20 или 25 цифр");
        require(gov.uinPrefix() == null || uin.startsWith(gov.uinPrefix()), "УИН этого получателя начинается с " + gov.uinPrefix());
        require(!payerName.isEmpty() && payerName.length() <= 150, "Укажите ФИО плательщика");
        require(!payerAddress.isEmpty() && payerAddress.length() <= 255, "Укажите адрес плательщика");
        require(RU_PHONE.matcher(payerPhone).matches(), "Телефон плательщика в формате +7XXXXXXXXXX");
        Map<String, String> requisites = requisites(uin, gov.payeeName(), gov.payeeInn(), gov.payeeKpp(), gov.payeeAccount(),
                gov.payeeBik(), gov.kbk(), gov.oktmo());
        List<BillField> fields = List.of(
                new BillField("uin", "УИН", uin, uin),
                new BillField("payerName", "ФИО плательщика", payerName, payerName),
                new BillField("payerAddress", "Адрес плательщика", payerAddress, payerAddress),
                new BillField("payerPhone", "Телефон плательщика", payerPhone, ruPhone(payerPhone)));
        return new BillCheck(p.code(), p.groupCode(), gov.payeeName(), gov.purpose(), fields, requisites, uin, null);
    }

    // Проверенные поля: что отдать поставщику и показать людям, выбранный номинал, главное значение для назначения
    public record Validated(List<BillField> fields, Nominal nominal, String main) {
    }

    // Транспорт, игры, зарубежная связь, образование, магазины, путешествия, страхование: поля по шаблону поставщика.
    // Если у поставщика есть поля поиска (stage = SEARCH), платим только найденный у него счёт — invoiceId.
    private BillCheck byFields(ProviderDetails d, Map<String, String> input, BigDecimal amount, UUID invoiceId) {
        Provider p = d.provider();
        Validated v = validate(d, d.fields(), input);
        boolean lookup = d.fields().stream().anyMatch(f -> "SEARCH".equals(f.stage()));
        Nominal nominal = v.nominal();

        String billReference = null;
        String purpose;
        if (lookup) {
            require(invoiceId != null, "Не выбран счёт");
            Invoice inv = invoices.get(invoiceId).filter(i -> i.providerId() == p.id())
                    .orElseThrow(() -> new BillException(BILL_NOT_PAYABLE, "Счёт не найден"));
            if (!"UNPAID".equals(inv.status())) {
                throw new BillException(BILL_NOT_PAYABLE, "Счёт уже оплачен");
            }
            if (inv.amount().compareTo(amount) != 0) {
                throw new BillException(BILL_NOT_PAYABLE, "Сумма не совпадает со счётом: " + rub(inv.amount()) + " ₽");
            }
            billReference = "inv/" + inv.id();
            purpose = p.name() + " — " + inv.title();
        } else if (nominal != null) {
            if (nominal.price().compareTo(amount) != 0) {
                throw new BillException(INVALID_AMOUNT, "Цена номинала — " + rub(nominal.price()) + " ₽");
            }
            // «Genshin Impact — 300 + 30 кристаллов»
            purpose = p.name() + " — " + nominal.name();
        } else {
            boolean tooLow = p.minAmount() != null && amount.compareTo(p.minAmount()) < 0;
            boolean tooHigh = p.maxAmount() != null && amount.compareTo(p.maxAmount()) > 0;
            if (tooLow || tooHigh) {
                throw new BillException(INVALID_AMOUNT, "Сумма у этого поставщика — от " + rub(p.minAmount()) + " до " + rub(p.maxAmount()) + " ₽");
            }
            // «Карта Тройка · 1234567890»
            purpose = v.main() != null ? p.name() + " · " + v.main() : p.name();
        }
        String phone = v.fields().stream().filter(x -> x.key().equals("phone")).map(BillField::display).findFirst().orElse(null);
        return new BillCheck(p.code(), p.groupCode(), p.name(), purpose, v.fields(), null, billReference,
                phone != null && phone.length() <= 20 ? phone.replace(" ", "").replace("-", "") : null);
    }

    public Validated validate(ProviderDetails d, List<Field> fields, Map<String, String> input) {
        List<BillField> out = new ArrayList<>();
        Nominal nominal = null;
        // Главное поле для назначения платежа: номер карты, счёта, телефона, логин — не почта и не выбор из списка
        String main = null;
        for (Field f : fields) {
            String raw = input.getOrDefault(f.key(), "").trim();
            if (raw.isEmpty()) {
                require(!f.required(), "Заполните поле «" + f.label() + "»");
                continue;
            }
            String value = raw;
            String display = raw;
            switch (f.type()) {
                case "DIGITS" -> {
                    value = raw.replaceAll("[\\s-]", "");
                    require(value.matches("\\d+") && (f.pattern() == null || value.matches(f.pattern())),
                            "Проверьте поле «" + f.label() + "»" + (f.hint() != null && f.hint().length() < 40 ? ": " + f.hint() : ""));
                    display = value;
                }
                case "TEXT" -> require(f.pattern() == null || raw.matches(f.pattern()), "Проверьте поле «" + f.label() + "»");
                case "EMAIL" -> require(EMAIL.matcher(raw).matches(), "Проверьте адрес электронной почты");
                case "PHONE" -> {
                    require(RU_PHONE.matcher(raw).matches(), "Телефон в формате +7XXXXXXXXXX");
                    display = ruPhone(raw);
                }
                case "LOCAL_PHONE" -> {
                    value = localPhone(d.foreignMobile(), raw);
                    display = "+" + d.foreignMobile().dialCode() + " " + value;
                }
                case "MONTH" -> display = month(raw, f.label());
                case "CHOICE" -> {
                    String v = raw;
                    Option o = f.options().stream().filter(x -> x.value().equals(v)).findFirst()
                            .orElseThrow(() -> new BillException(INVALID_REQUISITES, "Выберите значение «" + f.label() + "»"));
                    display = o.label();
                }
                case "NOMINAL" -> {
                    String region = f.dependsOn() == null ? null : input.get(f.dependsOn());
                    String v = raw;
                    nominal = d.nominals().stream()
                            .filter(n -> String.valueOf(n.id()).equals(v) && (f.dependsOn() == null || java.util.Objects.equals(n.region(), region)))
                            .findFirst()
                            .orElseThrow(() -> new BillException(INVALID_REQUISITES, "Выберите номинал"));
                    display = nominal.name();
                }
                default -> throw new IllegalStateException("Неизвестный тип поля " + f.type());
            }
            out.add(new BillField(f.key(), f.label(), value, display));
            if (main == null && !Set.of("EMAIL", "CHOICE", "NOMINAL", "MONTH").contains(f.type())) {
                main = display;
            }
        }
        return new Validated(out, nominal, main);
    }

    private static final String[] MONTHS = {"январь", "февраль", "март", "апрель", "май", "июнь", "июль", "август",
            "сентябрь", "октябрь", "ноябрь", "декабрь"};

    // 2026-11 → «ноябрь 2026»; оплатить можно за год назад и на год вперёд
    private static String month(String raw, String label) {
        java.time.YearMonth ym;
        try {
            ym = java.time.YearMonth.parse(raw);
        } catch (java.time.format.DateTimeParseException e) {
            throw new BillException(INVALID_REQUISITES, "Выберите месяц в поле «" + label + "»");
        }
        java.time.YearMonth now = java.time.YearMonth.now();
        require(!ym.isBefore(now.minusMonths(12)) && !ym.isAfter(now.plusMonths(12)), "Период оплаты — не дальше года от текущего месяца");
        return MONTHS[ym.getMonthValue() - 1] + " " + ym.getYear();
    }

    // Номер без кода страны: длина и начало — по формату страны
    private static String localPhone(ForeignMobile fm, String raw) {
        String digits = raw.replaceAll("\\D", "");
        if (digits.startsWith(fm.dialCode()) && digits.length() == fm.dialCode().length() + fm.numberLength()) {
            digits = digits.substring(fm.dialCode().length());
        }
        require(digits.length() == fm.numberLength(),
                "Номер — " + fm.numberLength() + " цифр, без кода страны " + fm.dialCode());
        String d = digits;
        require(fm.prefixes().isEmpty() || fm.prefixes().stream().anyMatch(d::startsWith),
                "Номер должен начинаться с " + String.join(", ", fm.prefixes()));
        return digits;
    }

    private static Map<String, String> requisites(String uin, String name, String inn, String kpp, String account, String bik,
                                                  String kbk, String oktmo) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("uin", uin);
        m.put("name", name);
        m.put("inn", inn);
        m.put("kpp", kpp);
        m.put("account", account);
        m.put("bik", bik);
        m.put("kbk", kbk);
        m.put("oktmo", oktmo);
        return m;
    }

    // +79964480061 → +7 996 448-80-61
    static String ruPhone(String phone) {
        return "+7 " + phone.substring(2, 5) + " " + phone.substring(5, 8) + "-" + phone.substring(8, 10) + "-" + phone.substring(10);
    }

    private static String rub(BigDecimal v) {
        return v == null ? "—" : v.stripTrailingZeros().toPlainString();
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new BillException(INVALID_REQUISITES, message);
        }
    }
}
