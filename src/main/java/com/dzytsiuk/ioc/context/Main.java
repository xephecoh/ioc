package com.dzytsiuk.ioc.context;


import com.dzytsiuk.ioc.service.MailService;
import com.dzytsiuk.ioc.service.PaymentService;

public class Main {
    public static void main(String[] args) {
        ClassPathApplicationContext classPathApplicationContext = new ClassPathApplicationContext("src/test/resources/context.xml");
        MailService mailService = classPathApplicationContext.getBean("mailService");
        System.out.println(mailService.getPort());
        PaymentService paymentService = classPathApplicationContext.getBean("paymentService");
    }
}
