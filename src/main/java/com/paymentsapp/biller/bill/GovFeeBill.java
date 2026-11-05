package com.paymentsapp.biller.bill;

import static com.paymentsapp.biller.bill.FieldValidator.require;
import static com.paymentsapp.biller.error.BillerError.*;

import com.paymentsapp.biller.bill.BillCheck.BillField;
import com.paymentsapp.biller.provider.GovDetails;
import com.paymentsapp.biller.provider.LookupMethod;
import com.paymentsapp.biller.provider.Provider;
import com.paymentsapp.biller.provider.ProviderDetails;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

// Госпошлина по квитанции (Росреестр): УИН и данные плательщика вводит пользователь, получатель — из gov_providers
@Component
@Order(2)
class GovFeeBill implements BillMethod {

    private static final Pattern UIN = Pattern.compile("\\d{20}|\\d{25}");
    private static final Pattern NON_DIGITS = Pattern.compile("\\D");
    private static final int PAYER_NAME_MAX = 150;
    private static final int PAYER_ADDRESS_MAX = 255;

    @Override
    public boolean supports(ProviderDetails provider, BillCheckRequest request) {
        return provider.gov() != null && provider.gov().lookup() == LookupMethod.FREE;
    }

    @Override
    public BillCheck check(ProviderDetails details, BillCheckRequest req) {
        Provider p = details.provider();
        GovDetails gov = details.gov();
        Map<String, String> f = req.fieldsOrEmpty();
        String uin = NON_DIGITS.matcher(f.getOrDefault(Key.UIN, "")).replaceAll("");
        String payerName = f.getOrDefault(Key.PAYER_NAME, "").trim();
        String payerAddress = f.getOrDefault(Key.PAYER_ADDRESS, "").trim();
        String payerPhone = f.getOrDefault(Key.PAYER_PHONE, "").trim();
        require(UIN.matcher(uin).matches(), UIN_FORMAT);
        require(gov.uinPrefix() == null || uin.startsWith(gov.uinPrefix()), UIN_PREFIX, gov.uinPrefix());
        require(!payerName.isEmpty() && payerName.length() <= PAYER_NAME_MAX, PAYER_NAME_REQUIRED);
        require(!payerAddress.isEmpty() && payerAddress.length() <= PAYER_ADDRESS_MAX, PAYER_ADDRESS_REQUIRED);
        require(FieldValidator.RU_PHONE.matcher(payerPhone).matches(), PAYER_PHONE_FORMAT);
        List<BillField> fields = List.of(
                new BillField(Key.UIN, Label.UIN, uin, uin),
                new BillField(Key.PAYER_NAME, Label.PAYER_NAME, payerName, payerName),
                new BillField(Key.PAYER_ADDRESS, Label.PAYER_ADDRESS, payerAddress, payerAddress),
                new BillField(Key.PAYER_PHONE, Label.PAYER_PHONE, payerPhone, BillTexts.ruPhone(payerPhone)));
        Map<String, String> requisites = BillTexts.budgetRequisites(uin, gov.payeeName(), gov.payeeInn(), gov.payeeKpp(),
                gov.payeeAccount(), gov.payeeBik(), gov.kbk(), gov.oktmo());
        return new BillCheck(p.code(), p.groupCode(), gov.payeeName(), gov.purpose(), fields, requisites, uin, null);
    }

    private static final class Key {
        static final String UIN = "uin";
        static final String PAYER_NAME = "payerName";
        static final String PAYER_ADDRESS = "payerAddress";
        static final String PAYER_PHONE = "payerPhone";
    }

    private static final class Label {
        static final String UIN = "УИН";
        static final String PAYER_NAME = "ФИО плательщика";
        static final String PAYER_ADDRESS = "Адрес плательщика";
        static final String PAYER_PHONE = "Телефон плательщика";
    }
}
