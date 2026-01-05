package br.com.lambda.dtos;

import java.time.LocalDateTime;

public record FeedbeckMessageDto(
        String descricao,
        Double nota,
        String urgencia,
        LocalDateTime date
) {

}
