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

    public NotificationLambda() {
        this.objectMapper = new ObjectMapper();
        this.snsClient = SnsClient.create(); // inicializa o SDK
        this.topicArn = System.getenv("SNS_TOPIC_ARN"); // variável de ambiente
    }

    @Override
    public String handleRequest(SQSEvent event, com.amazonaws.services.lambda.runtime.Context context) {

        this.logger.info("Inicio do metodo");

        try {
            // passa por todas as mensagens na fila SQS
            for (SQSEvent.SQSMessage msg : event.getRecords()) {

                //popula o Dto com as informações recebidas no PayLoad do SQS
                FeedbeckMessageDto dto = objectMapper.readValue(msg.getBody(), FeedbeckMessageDto.class);

                this.logger.info("Informações do DTO populado: " + dto);

                // cria o ASSUNTO do email
                String subject = "Novo feedback - Urgência: " + dto.urgencia();

                // cria o CORPO do email
                String body = """
                    Um novo feedback foi recebido:

                    Descrição: %s
                    Urgência: %s
                    Nota: %.2f
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
                this.logger.info("Notificacao enviada para o SNS: " + dto.descricao());
            }

            this.logger.info("Fim do metodo");

            // retorna ok
            return "Ok";

        } catch (Exception e) {

            // caso não processe as mensagens no SQS
            throw new RuntimeException("Erro ao processar evento SQS", e);
        }
    }
}
