package br.com.lambda;

import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class NotificationLambdaTest {

    @Mock
    private SnsClient snsClient;

    private NotificationLambda lambda;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule());

        lambda = new NotificationLambda(objectMapper, snsClient, "arn:aws:sns:test");
    }

    @Test
    void testSimpleLambdaSuccess() {
        // Arrange
        SQSEvent event = new SQSEvent();
        SQSEvent.SQSMessage message = new SQSEvent.SQSMessage();

        message.setBody("""
                         {
                          "descricao": "Ótimo atendimento",
                          "urgencia": "ALTA",
                          "nota": 9.5,
                          "date": "2025-01-05T10:30:00"
                        }
                        """);

        event.setRecords(List.of(message));

        // Act
        String result = lambda.handleRequest(event, null);

        // Assert
        assertEquals("Ok", result);
        verify(snsClient, times(1)).publish(any(PublishRequest.class));
    }
}
