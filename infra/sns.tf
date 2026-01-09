resource "aws_sns_topic" "feedback_urgente" {
  name = "feedback_urgente-sns"
}

resource "aws_sns_topic_subscription" "feedback_email" {
  topic_arn = aws_sns_topic.feedback_urgente.arn
  protocol  = "email"
  endpoint  = "email@email.com"  # endereço que vai receber as mensagens
}