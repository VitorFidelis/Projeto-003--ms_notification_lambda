# NotificationLambda

## 📌 Visão Geral

Este projeto implementa uma **AWS Lambda** escrita em **Java** que atua como **consumidora de mensagens de uma fila SQS** e **publicadora de notificações em um tópico SNS**.

O objetivo principal da Lambda é **processar mensagens de feedback**, transformar essas informações em uma notificação estruturada e **enviar um e-mail (ou outro tipo de notificação configurada no SNS)** para os assinantes do tópico.

---

## 🧱 Arquitetura Envolvida

Fluxo resumido da solução:

1. Uma aplicação publica mensagens na **AWS SQS**.
2. A **AWS Lambda NotificationLambda** é acionada automaticamente pela SQS.
3. A Lambda:

    * Lê as mensagens da fila
    * Converte o JSON recebido em um DTO
    * Monta o conteúdo da notificação
    * Publica a mensagem em um **tópico SNS**
4. O **SNS** encaminha a notificação para os assinantes (por exemplo, e-mail).

---

## ⚙️ Tecnologias Utilizadas

* **Java 17+**
* **AWS Lambda**
* **AWS SQS**
* **AWS SNS**
* **AWS SDK v2 (SNS Client)**
* **Jackson (ObjectMapper)**

---

## 📂 Estrutura da Classe Principal

### Classe: `NotificationLambda`

```java
public class NotificationLambda implements RequestHandler<SQSEvent, String>
```

A classe implementa a interface `RequestHandler<SQSEvent, String>`, o que indica que:

* **Entrada:** Evento da SQS (`SQSEvent`)
* **Saída:** Uma `String` indicando o resultado da execução

---

## ▶️ Método `handleRequest`

```java
public String handleRequest(SQSEvent event, Context context)
```

Este é o método principal executado pela AWS Lambda quando a fila SQS dispara o evento.

### 🔄 Passo a Passo da Execução

1. **Início da execução**

    * Um log é registrado indicando o início do processamento

2. **Iteração das mensagens da SQS**

   ```java
   for (SQSEvent.SQSMessage msg : event.getRecords())
   ```

    * A Lambda pode receber **uma ou várias mensagens** em uma única execução

3. **Desserialização do payload**

   ```java
   FeedbackMessageDto dto = objectMapper.readValue(msg.getBody(), FeedbackMessageDto.class);
   ```

    * O corpo da mensagem (JSON) é convertido para um DTO Java

4. **Criação do assunto da notificação**

   ```java
   String subject = "Novo feedback - Urgência: " + dto.urgencia();
   ```

5. **Criação do corpo da mensagem**

    * Utiliza **Text Blocks** do Java para melhorar a legibilidade
    * Contém informações como:

        * Descrição
        * Urgência
        * Nota
        * Data de envio

6. **Publicação no SNS**

   ```java
   snsClient.publish(PublishRequest.builder()
       .topicArn(topicArn)
       .subject(subject)
       .message(body)
       .build());
   ```

7. **Logs de sucesso**

    * Confirma que a notificação foi enviada com sucesso para o tópico SNS

8. **Retorno da execução**

   ```java
   return "Ok";
   ```

---

## 📦 DTO Esperado (`FeedbackMessageDto`)

O payload da mensagem enviada para a SQS deve seguir a estrutura esperada pelo DTO, por exemplo:

```json
{
  "descricao": "Ótimo atendimento",
  "urgencia": "ALTA",
  "nota": 9.5,
  "date": "2025-01-05"
}
```

---

## ✅ Conclusão

Esta Lambda implementa um **padrão de arquitetura orientada a eventos**, promovendo o desacoplamento entre:

* **Produção de feedbacks (SQS)**
* **Processamento e orquestração (Lambda)**
* **Entrega das notificações (SNS)**

Essa abordagem garante **escalabilidade**, **resiliência** e **baixo acoplamento** entre os serviços da solução.

---

## 🔗 Integração com Outro Repositório

Este projeto faz parte de uma **solução distribuída**, desenvolvida em parceria com outro repositório que contém o **microserviço responsável pela criação e envio dos feedbacks para a fila SQS**.

Enquanto este repositório é responsável pelo **processamento das mensagens e envio de notificações (SQS → Lambda → SNS)**, o outro microserviço cuida da **origem dos dados de feedback**.

📌 Repositório parceiro (microserviço de criação de feedback):
👉 **[Acessar repositório de criação de feedback](https://github.com/lcvinicius/fiap-tech-challenge-parte4)**

A integração entre os dois projetos permite uma arquitetura **desacoplada, escalável e orientada a eventos**, onde cada microserviço possui uma responsabilidade bem definida.

---

## 📄 Licença

Este projeto é parte de um desafio educacional da FIAP. Uso livre para fins acadêmicos. Para outros fins, consulte a **MIT License**.

---

✍️ Autor: Vitor
