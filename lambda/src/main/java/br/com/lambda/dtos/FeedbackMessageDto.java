package br.com.lambda.dtos;

import java.time.LocalDateTime;
import java.time.Instant;

public record FeedbackMessageDto(
        String descricao,
        Double nota,
        String urgencia,
        Instant dataEnvio
) {

}
