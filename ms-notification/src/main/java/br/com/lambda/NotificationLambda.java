package br.com.lambda;

import br.com.lambda.dtos.FeedbeckMessageDto;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

public class NotificationLambda implements RequestHandler<SQSEvent, String> {

    private final ObjectMapper objectMapper;
    private final SnsClient snsClient;
    private final String topicArn;

    public NotificationLambda() {
        this.objectMapper = new ObjectMapper();
        this.snsClient = SnsClient.create(); // inicializa o SDK
        this.topicArn = System.getenv("NOTIFICATION_TOPIC_ARN"); // variável de ambiente
    }

    @Override
    public String handleRequest(SQSEvent event, com.amazonaws.services.lambda.runtime.Context context) {
        try {
            // passa por todas as mensagens na fila SQS
            for (SQSEvent.SQSMessage msg : event.getRecords()) {
                //popula o Dto com as informações recebidas no PayLoad do SQS
                FeedbeckMessageDto dto = objectMapper.readValue(msg.getBody(), FeedbeckMessageDto.class);

                // cria o ASSUNTO do email
                String subject = "Novo feedback - Urgência: " + dto.urgencia();

                // cria o CORPO do email
                String body = """
                    Um novo feedback foi recebido:

                    Descrição: %s
                    Urgência: %s
                    Nota: %d
                    Data do envio: %s

                    Att,
                    Serviço de Notificação
                    """.formatted(dto.descricao(), dto.urgencia(), dto.nota(), dto.date());

                //Publica no topic sns
                snsClient.publish(PublishRequest.builder()
                        .topicArn(topicArn)
                        .subject(subject)
                        .message(body)
                        .build());

                // imprime no console para saber se foi enviado para o topic sns
                System.out.println("Notificação enviada para o SNS: " + dto.descricao());
            }

            // retorna ok
            return "OK";
        } catch (Exception e) {
            // caso não processe as mensagens no SQS
            throw new RuntimeException("Erro ao processar evento SQS", e);
        }
    }
}
