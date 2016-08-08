/*
 * Copyright (c) 2013 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;
import com.commonsware.cwac.wakeful.WakefulIntentService;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.model.*;
import ru.orangesoftware.financisto.model.Currency;
import ru.orangesoftware.financisto.service.FinancistoService;
import ru.orangesoftware.financisto.utils.MyPreferences;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created with IntelliJ IDEA.
 * User: Lyubomyr Vyhovskyy
 * Date: 04.03.13
 * Time: 0:03
 * To change this template use File | Settings | File Templates.
 */
public class PrivatbankReceiver extends BroadcastReceiver {
//    private static final String pbSMSSender = "10060";

    private static final String TAG = "PrivatbankReceiver";

    private DatabaseAdapter db;

    class MoneyInfo {
        String infoType;
        String amount;
        String currency;

        public MoneyInfo(String infoType, String amount, String currency){
            this.infoType = infoType;
            this.amount = amount;
            this.currency = currency;
        }

        public String getInfoType(){
            return infoType;
        }

        public void setInfoType(String infoType){
            this.infoType = infoType;
        }

        public String getAmount(){
            return amount;
        }

        public void setAmount(String amount){
            this.amount = amount;
        }

        public String getCurrency(){
            return currency;
        }

        public void setCurrency(String currency){
            this.currency = currency;
        }
    }

    @Override
    protected void finalize() throws Throwable {
        if (db != null) {
            db.close();
        }
        Log.d(TAG, "Finalized..");

        super.finalize();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "Intent received: " + intent.getAction());

        db = new DatabaseAdapter(context);
        db.open();
        Log.d(TAG, "DB created..");

//        Bundle pudsBundle = intent.getExtras();
//        Object[] pdus = (Object[]) pudsBundle.get("pdus");
//        SmsMessage messages = SmsMessage.createFromPdu((byte[]) pdus[0]);
//        String messageSender = messages.getOriginatingAddress();
//        String messageBody = messages.getMessageBody();

        String messageBody = intent.getStringExtra("messageBody");

        if (MyPreferences.isUseSmsParser(context)){
            long accountId = MyPreferences.getMainPrivatCardAccountId(context);

            String transactionTime = GetTransactionTime(messageBody);
            String payee = GetPayee(messageBody);
            PayeeCategoryResult payeeCategoryResult = GetPayeeCategory(payee);

            List<MoneyInfo> moneyInfos = GetMoneyInfos(messageBody);
            double kurs = GetKurs(moneyInfos);

            for (MoneyInfo info : moneyInfos) {
                if ("".equals(info.infoType) || "Splata".equals(info.infoType) || "Internet-splata".equals(info.infoType)) {

                    Transaction newTran = createNewTransactionFromSMS(accountId, transactionTime, info.amount, info.currency, payeeCategoryResult.payeeId, payeeCategoryResult.categoryId, "Imported from SMS", kurs);
                    long newTransactionId = db.insertOrUpdate(newTran);
                    requestNotification(context, newTransactionId);

                } else if ("Na Skarbnichku".equals(info.infoType)) {

                    long savingsAccountId = MyPreferences.getSavingsAccountId(context);

                    Transaction newTransferTran = createNewTransferTransactionFromSMS(accountId, savingsAccountId, transactionTime, info.amount, info.currency, 0, 0, "Kopilka (parsed from SMS)");
                    long newTransferTransactionId = db.insertOrUpdate(newTransferTran);
                    requestNotification(context, newTransferTransactionId);

                } else if ("Znyattya".equals(info.infoType)) {

                    long cacheAccountId = MyPreferences.getCacheAccountId(context);

                    Transaction newTransferTran = createNewTransferTransactionFromSMS(accountId, cacheAccountId, transactionTime, info.amount, info.currency, 0, 0, "Znyatta gotivky (parsed from SMS)");
                    long newTransferTransactionId = db.insertOrUpdate(newTransferTran);
                    requestNotification(context, newTransferTransactionId);

                } else if ("Komissia".equals(info.infoType)) {
                    long comissiyaCategoryId;
                    if (messageBody.contains("Znyattya")){
                        comissiyaCategoryId = MyPreferences.getAtmCommisionCategoryId(context);
                    }else{
                        comissiyaCategoryId = MyPreferences.getBankCommisionCategoryId(context);
                    }

                    PayeeCategoryResult payeeResult = GetPayeeCategory("Privatbank");

                    Transaction newTran = createNewTransactionFromSMS(accountId, transactionTime, info.amount, info.currency, payeeResult.payeeId, comissiyaCategoryId, "Za poslugy (Imported from SMS)", kurs);
                    long newTransactionId = db.insertOrUpdate(newTran);
                    requestNotification(context, newTransactionId);
                } else if ("Bal".equals(info.infoType) || "Kurs".equals(info.infoType)) {
                    continue;
                }
            }
        }

        //TODO: Create SMSProcessor with different templates (including Privatbank)
    }

    private double GetKurs(List<MoneyInfo> moneyInfos) {
        double kurs = 0;

        for (MoneyInfo info : moneyInfos){
            if ("Kurs".equals(info.infoType)){
                kurs = Double.parseDouble(info.amount);
                break;
            }
        }

        return kurs;
    }

    private List<MoneyInfo> GetMoneyInfos(String messageBody){
        List<MoneyInfo> result = new ArrayList<MoneyInfo>();

        Pattern pattern = Pattern.compile("([aA-zZ\\-\\s]*)?\\.?[\\s:]?([0-9]*\\.\\d+)\\s?(\\w+/?\\w+)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher matcher = pattern.matcher(messageBody);
        while (matcher.find())
        {
            String moneyType = matcher.group(1).trim();
            String amount = matcher.group(2).trim();
            String currency = matcher.group(3).trim();

            MoneyInfo info = new MoneyInfo(moneyType, amount, currency);
            result.add(info);
        }

        return result;
    }

    private String GetPayee(String messageBody) {
        String payee = "";

        Pattern payeePattern = Pattern.compile("^([0-9]*\\.\\d+\\w{3})(.+)", Pattern.CASE_INSENSITIVE);
        Matcher payeeMatcher = payeePattern.matcher(messageBody);
        if (payeeMatcher.find())
        {
            payee=payeeMatcher.group(2).trim();
        } else {
            Pattern payeeInBracesPattern = Pattern.compile("\\((.*?)\\)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
            Matcher payeeInBracesMatcher = payeeInBracesPattern.matcher(messageBody);
            if (payeeInBracesMatcher.find())
            {
                payee=payeeInBracesMatcher.group(1).trim();
            }
        }

        return payee;
    }

    private String GetTransactionTime(String messageBody){
        String transactionTime = "";

        Pattern timePattern = Pattern.compile("\\d\\*\\d{2}\\s(\\d{1,2}[:-]\\d{2})", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher timeMatcher = timePattern.matcher(messageBody);
        if (timeMatcher.find())
        {
            transactionTime=timeMatcher.group(1).trim();
        }

        return transactionTime;
    }

    private void requestNotification(Context context, long newTransactionId) {
        Intent serviceIntent = new Intent(FinancistoService.ACTION_CREATE_FROM_SMS);
        serviceIntent.putExtra(FinancistoService.SMS_NEW_TRANSACTION_ID, newTransactionId);
        WakefulIntentService.sendWakefulWork(context, serviceIntent);
    }

    private Transaction createNewTransactionFromSMS(long fromAccountId, String transactionTime, String amount, String currency, long payeeId, long categoryId, String note, double kurs) {
        Date transactionDate = geTransactionDateTime(transactionTime);

        Transaction newTran = new Transaction();
        newTran.dateTime = transactionDate.getTime();// default = System.currentTimeMillis();
        newTran.fromAccountId = fromAccountId;
        newTran.fromAmount = (long)(Double.parseDouble(amount) * -100);
        newTran.payeeId = payeeId;
        newTran.categoryId = categoryId;
        newTran.note = note;


        Currency homeCurrency = db.em().getHomeCurrency();

        long currencyId = Currency.EMPTY.id; //
        for (Currency c : db.em().getAllCurrenciesList()){
            if (c.name.compareTo(currency) == 0){
                currencyId = c.id;
                break;
            }
        }

        if (homeCurrency.id != currencyId){
            newTran.originalFromAmount = (long)(Double.parseDouble(amount) * -100);
            newTran.originalCurrencyId = currencyId;
            newTran.fromAmount = (long)(Double.parseDouble(amount) * kurs * -100);
        }

        return newTran;
    }

    private Transaction createNewTransferTransactionFromSMS(long fromAccountId, long toAccountId, String transactionTime, String amountKopilky, String currencyKopilky, long payeeId, long categoryId, String note) {
        Transaction newTran = createNewTransactionFromSMS(fromAccountId, transactionTime, amountKopilky, currencyKopilky, payeeId, categoryId, note, 0);
        newTran.toAccountId = toAccountId;
        newTran.toAmount = (long)(Double.parseDouble(amountKopilky) * 100);

        return newTran;
    }

    private Date geTransactionDateTime(String transactionTime) {
        Calendar cal = new GregorianCalendar(Locale.getDefault());

        if (transactionTime.indexOf(":") > 0) {
            String[] timeParts = transactionTime.split(":"); //transactionTime HH:MM
            cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(timeParts[0]));
            cal.set(Calendar.MINUTE, Integer.parseInt(timeParts[1]));
        }
        return cal.getTime();
    }

    private PayeeCategoryResult GetPayeeCategory(String payee) {
        long payeeId = 0;
        long categoryId = 0;

        for (Payee p : db.em().getAllPayeeList()){
            if (p.smsPayeeName != null &&  p.smsPayeeName.compareTo(payee) == 0){
                payeeId = p.id;
                categoryId = p.lastCategoryId;
                break;
            }
        }

        if (payeeId == 0 && !payee.toLowerCase().contains("bankomat")) {
            payeeId = db.insertPayee(payee);
        }

        return new PayeeCategoryResult(payeeId, categoryId);
    }

    private class PayeeCategoryResult {
        public long payeeId;
        public long categoryId;

        public PayeeCategoryResult(long payeeId, long categoryId) {
            this.payeeId = payeeId;
            this.categoryId = categoryId;
        }
    }
}
