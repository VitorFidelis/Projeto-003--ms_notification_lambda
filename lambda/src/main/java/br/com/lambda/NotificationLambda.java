package br.com.lambda;

import br.com.lambda.dtos.FeedbackMessageDto;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;


public class NotificationLambda implements RequestHandler<SQSEvent, String> {

    private final ObjectMapper objectMapper;
    private final SnsClient snsClient;
    private final String topicArn;
    private final Logger logger = Logger.getLogger(this.getClass().getName());

    // Construtor padrão (AWS)
    public NotificationLambda() {
        this(
                new ObjectMapper()
                        .registerModule(new JavaTimeModule())
                        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS),
                SnsClient.create(),
                System.getenv("SNS_TOPIC_ARN"));
    }

    // Construtor para testes
    public NotificationLambda(ObjectMapper objectMapper, SnsClient snsClient, String topicArn) {
        this.objectMapper = objectMapper;
        this.snsClient = snsClient;
        this.topicArn = topicArn;
    }

    @Override
    public String handleRequest(SQSEvent event, com.amazonaws.services.lambda.runtime.Context context) {
        logger.info("Lambda NotificationLambda iniciada");
        logger.info("Mensagens recebidas da SQS: " + event.getRecords().size());

        try {
            for (SQSEvent.SQSMessage msg : event.getRecords()) {

                logger.info("Processando mensagem SQS. MessageId: " + msg.getMessageId());
                logger.fine("Payload da mensagem: " + msg.getBody());

                FeedbackMessageDto dto = objectMapper.readValue(msg.getBody(), FeedbackMessageDto.class);

                logger.info("Feedback desserializado com sucesso. Urgência: " + dto.urgencia());

                String subject = "Novo feedback - Urgência: " + dto.urgencia();

                String body = """
                    Um novo feedback foi recebido:

                    Descrição: %s
                    Urgência: %s
                    Nota: %.2f
                    Data do envio: %s

                    Att,
                    Serviço de Notificação
                    """.formatted(dto.descricao(), dto.urgencia(), dto.nota(), dto.dataEnvio());

                snsClient.publish(PublishRequest.builder()
                        .topicArn(topicArn)
                        .subject(subject)
                        .message(body)
                        .build());

                logger.info("Mensagem publicada no SNS com sucesso. TopicArn: " + topicArn);
            }
            logger.info("Processamento finalizado com sucesso");
            return "Ok";

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erro ao processar evento SQS", e);
            throw new RuntimeException("Erro ao processar evento SQS", e);
        }
    }
}