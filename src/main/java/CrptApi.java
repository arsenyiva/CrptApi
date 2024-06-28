import com.google.gson.Gson;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Класс для взаимодействия с API Честного знака.
 * Поддерживает ограничение на количество запросов в заданном интервале времени.
 */
public class CrptApi {

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();
    private final ExecutorService executor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, queue);
    private final AtomicInteger requestCounter = new AtomicInteger(0);
    private final int requestLimit;
    private final long timeIntervalMillis;
    private final HttpClient httpClient;

    public static void main(String[] args) {
        CrptApi api = new CrptApi(TimeUnit.SECONDS, 10);
        Document document = new Document();
        document.setParticipantInn("participantInn_value");
        document.setDocId("doc_id_value");
        document.setDocStatus("doc_status_value");
        document.setDocType("LP_INTRODUCE_GOODS");
        document.setImportRequest(true);
        document.setOwnerInn("owner_inn_value");
        document.setParticipant_inn("participant_inn_value");
        document.setProducerInn("producer_inn_value");
        document.setProductionDate("2020-01-23");
        document.setProductionType("production_type_value");
        document.setProducts(new Product[]{
                new Product(
                        "certificate_document_value",
                        "2020-01-23",
                        "certificate_document_number_value",
                        "owner_inn_value",
                        "producer_inn_value",
                        "2020-01-23",
                        "tnved_code_value",
                        "uit_code_value",
                        "uitu_code_value"
                )
        });
        document.setRegDate("2020-01-23");
        document.setRegNumber("reg_number_value");
        String signature = "signature";
        String accessToken = "token";
        api.createDocument(document, signature, accessToken);
    }

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.requestLimit = requestLimit;
        this.timeIntervalMillis = timeUnit.toMillis(1);
        scheduler.scheduleAtFixedRate(this::resetCounter, timeIntervalMillis, timeIntervalMillis, TimeUnit.MILLISECONDS);
        this.httpClient = HttpClients.createDefault();
    }

    /**
     * Метод для создания документа.
     *
     * @param document    Объект документа для отправки.
     * @param signature   Строка с подписью.
     * @param accessToken Токен доступа для авторизации.
     */
    public void createDocument(Document document, String signature, String accessToken) {
        try {
            while (requestCounter.get() >= requestLimit) {
                Thread.sleep(100);
            }

            executor.submit(() -> {
                try {
                    String json = serializeDocument(document);

                    HttpPost request = new HttpPost("https://ismp.crpt.ru/api/v3/lk/documents/create");
                    request.setHeader("Content-Type", "application/json");
                    request.setHeader("Authorization", "Bearer " + accessToken);
                    request.setHeader("Signature", signature);
                    request.setEntity(new StringEntity(json));

                    HttpResponse response = httpClient.execute(request);
                    int statusCode = response.getStatusLine().getStatusCode();

                    if (statusCode == 200) {
                        System.out.println("Document created successfully: " + document.toString());
                    } else {
                        System.out.println("Failed to create document. Status code: " + statusCode);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    incrementCounter();
                }
            });
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Увеличивает счетчик количества запросов.
     */
    private synchronized void incrementCounter() {
        requestCounter.incrementAndGet();
    }

    /**
     * Сбрасывает счетчик количества запросов.
     */
    private synchronized void resetCounter() {
        requestCounter.set(0);
    }

    /**
     * Сериализует объект документа в формат JSON с помощью Gson.
     *
     * @param document Объект документа для сериализации.
     * @return Строка в формате JSON.
     */
    private String serializeDocument(Document document) {
        Gson gson = new Gson();
        return gson.toJson(document);
    }

    /**
     * Класс, представляющий документ для создания в API.
     */
    @Data
    public static class Document {
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
    }


    /**
     * Класс, представляющий продукт в документе.
     */
    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    public static class Product {
        private String certificateDocument;
        private String certificateDocumentDate;
        private String certificateDocumentNumber;
        private String ownerInn;
        private String producerInn;
        private String productionDate;
        private String tnvedCode;
        private String uitCode;
        private String uituCode;
    }
}
