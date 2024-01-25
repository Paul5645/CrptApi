package org.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


public class CrptApi {

    // Планировщик для выполнения задач по расписанию
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    // Замок для обеспечения потокобезопасности
    private final Lock lock = new ReentrantLock();

    // Единица измерения времени и ограничение на количество запросов
    private final TimeUnit timeUnit;
    private final int requestLimit;

    // Логирование с использованием SLF4J
    private static final Logger logger = LoggerFactory.getLogger(CrptApi.class);

    // Переменная для отслеживания количества запросов
    private int requestCounter = 0;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.timeUnit = timeUnit;
        this.requestLimit = requestLimit;
        scheduleReset();
    }

//    private void scheduleReset() {
//        scheduler.scheduleAtFixedRate(() -> {
//            lock.lock();
//            try {
//                // Сбросить счетчик запросов
//                resetRequestCounter();
//            } finally {
//                lock.unlock();
//            }
//        }, 0, 1, timeUnit);
//    }
private void scheduleReset() {
    scheduler.scheduleAtFixedRate(() -> {
        lock.lock();
        try {
            // Сбросить счетчик запросов
            resetRequestCounter();
        } finally {
            lock.unlock();
        }
    }, 0, 1, TimeUnit.valueOf(timeUnit.toString()));
}



    private void incrementRequestCounter() {
        requestCounter++;
    }
    private void resetRequestCounter() {
        requestCounter = 0;
    }
    public void createDocument(Document document, String signature) {
        // Переводим объект Document в формат JSON
        String jsonDocument = convertDocumentToJson(document);

        // Формируем URL для запроса
        String apiUrl = "https://ismp.crpt.ru/api/v3/lk/documents/create";

        try {
            if (lock.tryLock(1, TimeUnit.SECONDS)) {
                try {
                    // Увеличиваем счетчик запросов при каждом вызове
                    incrementRequestCounter();

                    // Отправляем запрос
                    sendRequest(apiUrl, jsonDocument, signature);
                } finally {
                    lock.unlock();
                }
            } else {
                logger.warn("Unable to acquire lock");
                // Обработка невозможности получения блокировки
            }
        } catch (InterruptedException e) {
            logger.error("Error during lock acquisition: {}", e.getMessage());
            // Обработка исключения
        }
    }
    private void handleResponse(HttpURLConnection connection) throws IOException {
        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            logger.info("Successful response: {}", responseCode);
            // Обработка успешного ответа
        } else {
            logger.error("Error response: {}", responseCode);
            // Обработка ошибочного ответа
        }
    }

    // Метод для отправки запроса
    private void sendRequest(String apiUrl, String jsonDocument, String signature) {
        try {
            // Отправляем POST-запрос по HTTPS
            URL url = new URL(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            // Передаем JSON-документ и подпись в теле запроса
            connection.getOutputStream().write(jsonDocument.getBytes(StandardCharsets.UTF_8));
            connection.getOutputStream().write(signature.getBytes(StandardCharsets.UTF_8));

            // Обработка ответа
            handleResponse(connection);
        } catch (IOException e) {
            logger.error("Error sending request: {}", e.getMessage());
            // Обработка исключения
        }
    }

    // Метод для преобразования объекта Document в JSON-строку
    private String convertDocumentToJson(Document document) {
        try {
            // Используем ObjectMapper из библиотеки Jackson для преобразования объекта в JSON
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(document);
        } catch (Exception e) {
            // Обработка исключений, например, логирование ошибки
            e.printStackTrace();
            return ""; // Вернуть пустую строку или другой индикатор ошибки
        }
    }

    // Внутренний класс для представления документа
    private static class Document {
        private String participantInn;
        private String docId;
        private String docStatus;
        private String docType;
        private boolean importRequest;
        private String ownerInn;
        private String participant_inn;
        private String producerInn;
        private String productionDate;
        private String productionType;
        private Product[] products;
        private String regDate;
        private String regNumber;

        // Конструктор для создания объекта Document
        public Document(String participantInn, String docId, String docStatus, String docType,
                        boolean importRequest, String ownerInn, String participant_inn, String producerInn,
                        String productionDate, String productionType, Product[] products,
                        String regDate, String regNumber) {
            this.participantInn = participantInn;
            this.docId = docId;
            this.docStatus = docStatus;
            this.docType = docType;
            this.importRequest = importRequest;
            this.ownerInn = ownerInn;
            this.participantInn = participantInn;
            this.producerInn = producerInn;
            this.productionDate = productionDate;
            this.productionType = productionType;
            this.products = products;
            this.regDate = regDate;
            this.regNumber = regNumber;
        }

        // Внутренний класс для представления продукта
        private static class Product {
            private String certificateDocument;
            private String certificateDocumentDate;
            private String certificateDocumentNumber;
            private String ownerInn;
            private String producerInn;
            private String productionDate;
            private String tnvedCode;
            private String uitCode;
            private String uituCode;

            // Конструктор для создания объекта Product
            public Product(String certificateDocument, String certificateDocumentDate,
                           String certificateDocumentNumber, String ownerInn, String producerInn,
                           String productionDate, String tnvedCode, String uitCode, String uituCode) {
                this.certificateDocument = certificateDocument;
                this.certificateDocumentDate = certificateDocumentDate;
                this.certificateDocumentNumber = certificateDocumentNumber;
                this.ownerInn = ownerInn;
                this.producerInn = producerInn;
                this.productionDate = productionDate;
                this.tnvedCode = tnvedCode;
                this.uitCode = uitCode;
                this.uituCode = uituCode;
            }
        }
    }
}
