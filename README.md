# NotificationLambda

## 📌 Visão Geral

Este projeto implementa uma **AWS Lambda** escrita em **Java** que atua como **consumidora de mensagens de uma fila SQS** e **publicadora de notificações em um tópico SNS**.

O objetivo principal da Lambda é **processar mensagens de feedback**, transformar essas informações em uma notificação estruturada e **enviar um e-mail (ou outro tipo de notificação configurada no SNS)** para os assinantes do tópico.

---

## 📐 Arquitetura do Projeto

Este projeto implementa uma arquitetura serverless na AWS para processamento e envio de notificações por e-mail, utilizando os seguintes serviços:

- Amazon SQS: fila responsável por receber as mensagens de entrada.

- AWS Lambda: função que consome as mensagens da fila SQS, processa o conteúdo e formata o e-mail.

- Amazon SNS: serviço responsável por publicar e entregar o e-mail formatado aos assinantes.

- Amazon SNS Subscription (Email): inscrição que define o endereço de e-mail que receberá as notificações.

Fluxo da Arquitetura

1. Uma mensagem é enviada para a fila SQS.

2. A Lambda é acionada automaticamente ao detectar mensagens na fila.

3. A Lambda:

    - Processa o payload recebido

    - Formata o conteúdo do e-mail

4. O e-mail formatado é publicado em um tópico SNS.

5. O SNS envia o e-mail para os endereços configurados como subscribers.

Essa abordagem garante desacoplamento, escalabilidade e baixo custo operacional.

---
## 🧱 Infraestrutura como Código (Terraform)

Toda a infraestrutura do projeto é gerenciada via Terraform, permitindo versionamento, reprodutibilidade e automação do provisionamento.

***Principais arquivos Terraform***

- main.tf
    
    Arquivo principal que orquestra os recursos e providers.

- variables.tf

  Define as variáveis utilizadas no projeto.

- outputs.tf

  Exporta informações úteis após o provisionamento.

- lambda.tf

  Define a função Lambda, permissões IAM e integração com o SQS.

- sns.tf

  Criação do tópico SNS e suas subscriptions (e-mail).

- iam.tf

    Roles e policies necessárias para Lambda, SNS e SQS.

- data.tf

    Mapeamento de dados necessarios para rodar o GithubAction e outros.

---
📧 Configuração do E-mail de Subscription do SNS

O endereço de e-mail que receberá as notificações é configurado no arquivo sns.tf, no recurso aws_sns_topic_subscription.

Exemplo:

```
resource "aws_sns_topic_subscription" "feedback_email" {
topic_arn = aws_sns_topic.feedback_urgente.arn
protocol  = "email"
endpoint  = "email@email.com" # endereço que vai receber as mensagens
}
```
Na pipeline do GitHub Actions, existe uma etapa responsável por verificar a existência da subscription do SNS antes de destruir a infraestrutura.
Para isso, o mesmo endereço de e-mail configurado no Terraform deve ser informado como ENDPOINT.
```
  # Verificar SNS Subscription
  - name: Verificar SNS Subscription
    id: get_sns_subscription
    run: |
      TOPIC_ARN="arn:aws:sns:${AWS_REGION}:${AWS_ACCOUNT_ID}:${SNS_NAME}"
      ENDPOINT="email@email.com"
```
⚠️ Importante:

- Após o terraform apply, a AWS enviará um e-mail de confirmação para o endereço configurado.

- O envio de mensagens só começará após a confirmação da subscription clicando no link recebido por e-mail.

---

## 🚀 Pipeline de Deploy (GitHub Actions)

O deploy da infraestrutura é feito automaticamente através de uma GitHub Action, utilizando Terraform.

**Arquivo da Pipeline**

- .github/workflows/deploy-or-destroy.yml

Esse workflow é responsável por executar:

- terraform init

- terraform plan

- terraform apply ou terraform destroy, dependendo da variável configurada.

**Variável**: TF_ACTION

Para subir (provisionar) o projeto na AWS, é necessário:

1. Editar o arquivo:

`.github/workflows/deploy-or-destroy.yml`


2. Alterar a variável:

`TF_ACTION: apply`


3. Fazer commit da alteração.

Subir o commit na branch **develop**.

🔁 O pipeline será acionado automaticamente e realizará o deploy da infraestrutura.

Caso seja necessário destruir os recursos, basta alterar o valor para:

`TF_ACTION: destroy`

---
## 🔐 Autenticação com AWS via OIDC (GitHub Actions)

Este projeto utiliza OIDC (OpenID Connect) para autenticação segura entre o GitHub Actions e a AWS, eliminando a necessidade de armazenar credenciais estáticas (Access Key e Secret Key).

Como funciona

* O GitHub Actions assume uma IAM Role na AWS usando OIDC.
* Essa role possui permissões específicas para executar o Terraform.
* A autenticação ocorre de forma temporária e segura durante a execução da pipeline.

Benefícios do OIDC

* 🔒 Maior segurança (sem secrets sensíveis no repositório)
* ♻️ Credenciais temporárias
* 📋 Controle granular de permissões via IAM
* ✅ Padrão recomendado pela AWS

A configuração do OIDC envolve:

* Provider OIDC do GitHub na AWS
* IAM Role com trust policy para o repositório/branch
* Permissões necessárias para criação dos recursos via Terraform
---
## ⚙️ Tecnologias Utilizadas

* **Java 17+**
* **AWS Lambda**
* **AWS SQS**
* **AWS SNS**
* **AWS SDK v2 (SNS Client)**
* **Jackson (ObjectMapper)**
* **Terraform**
* **GitHub Actions** 

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

## 🧩​ Desenvolvedores

- [Vitor Fidelis-Linkedin](https://www.linkedin.com/in/vitorfidelis01)
      
- [Janaina-frv-Linkedin](http://www.linkedin.com/in/janaina-v-571ba031)
