package com.dzytsiuk.ioc.context;



import com.dzytsiuk.ioc.service.PaymentService;

public class Main {
    public static void main(String[] args) {
        ClassPathApplicationContext classPathApplicationContext = new ClassPathApplicationContext("src/test/resources/context.xml");

        PaymentService paymentService = classPathApplicationContext.getBean("paymentService");
        System.out.println(paymentService.getMailService().getPort());
    }
}
