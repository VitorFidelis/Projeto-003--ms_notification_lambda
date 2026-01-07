resource "aws_lambda_function" "hello_lambda" {
  function_name = "sqs-urgente-sns"
  role          = aws_iam_role.lambda_exec_role.arn
  handler       = "br.com.lambda.NotificationLambda::handleRequest"
  runtime       = "java17"

  filename      = "${path.module}/build/lambda/ms-notification.jar"

  memory_size = 512
  timeout     = 10

  environment {
    variables = {
      SNS_TOPIC_ARN = "arn:aws:sns:us-east-1:757367947438:feedback_urgente-sns"
    }
  }
}

resource "aws_lambda_event_source_mapping" "sqs_trigger" {
  event_source_arn  = data.aws_sqs_queue.feedback_urgente.arn
  function_name     = aws_lambda_function.hello_lambda.function_name
  batch_size        = 10  # quantas mensagens a Lambda recebe por vez
  enabled           = true
}
