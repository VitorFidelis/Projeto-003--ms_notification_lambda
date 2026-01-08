package br.com.lambda;

import br.com.lambda.dtos.FeedbeckMessageDto;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

import java.util.logging.Logger;

public class NotificationLambda implements RequestHandler<SQSEvent, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private final SnsClient snsClient;
    private final String topicArn;
    private final Logger logger = Logger.getLogger(NotificationLambda.class.getName());

    public NotificationLambda() {
        this.snsClient = SnsClient.create();
        this.topicArn = System.getenv("SNS_TOPIC_ARN");
    }

    @Override
    public String handleRequest(SQSEvent event, com.amazonaws.services.lambda.runtime.Context context) {

        try {
            for (SQSEvent.SQSMessage msg : event.getRecords()) {

                FeedbeckMessageDto dto =
                        MAPPER.readValue(msg.getBody(), FeedbeckMessageDto.class);

                String subject = "Novo feedback - Urgência: " + dto.urgencia();

                String body = """
                    Um novo feedback foi recebido:

                    Descrição: %s
                    Urgência: %s
                    Nota: %.2f
                    Data do envio: %s

                    Att,
                    Serviço de Notificação
                    """.formatted(
                        dto.descricao(),
                        dto.urgencia(),
                        dto.nota(),
                        dto.dataEnvio()
                );

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
