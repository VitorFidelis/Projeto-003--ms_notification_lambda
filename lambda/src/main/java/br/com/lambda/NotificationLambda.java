package br.com.lambda;

import br.com.lambda.dtos.FeedbeckMessageDto;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

import java.util.logging.Logger;

public class NotificationLambda implements RequestHandler<SQSEvent, String> {

    private final ObjectMapper objectMapper;
    private final SnsClient snsClient;
    private final String topicArn;
    private final Logger logger = Logger.getLogger(this.getClass().getName());

    // Construtor padrão (AWS)
    public NotificationLambda() {
        this(new ObjectMapper(), SnsClient.create(), System.getenv("SNS_TOPIC_ARN"));
    }

    // Construtor para testes
    public NotificationLambda(ObjectMapper objectMapper, SnsClient snsClient, String topicArn) {
        this.objectMapper = objectMapper;
        this.snsClient = snsClient;
        this.topicArn = topicArn;
    }

    @Override
    public String handleRequest(SQSEvent event, com.amazonaws.services.lambda.runtime.Context context) {

        try {
            for (SQSEvent.SQSMessage msg : event.getRecords()) {

                FeedbeckMessageDto dto =
                        objectMapper.readValue(msg.getBody(), FeedbeckMessageDto.class);

                String subject = "Novo feedback - Urgência: " + dto.urgencia();

                String body = """
                    Um novo feedback foi recebido:

                    Descrição: %s
                    Urgência: %s
                    Nota: %.2f
                    Data do envio: %s

                    Att,
                    Serviço de Notificação
                    """.formatted(dto.descricao(), dto.urgencia(), dto.nota(), dto.dateEnvio());

                snsClient.publish(PublishRequest.builder()
                        .topicArn(topicArn)
                        .subject(subject)
                        .message(body)
                        .build());
            }

            return "Ok";

        } catch (Exception e) {
            throw new RuntimeException("Erro ao processar evento SQS", e);
        }
    }
}