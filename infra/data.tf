data "aws_sns_topic" "feedback_urgente" {
  count = var.sns_topic_exists ? 1 : 0
  name  = "feedback_urgente-sns"
}

data "aws_sqs_queue" "feedback_urgente" {
  name = "feedback_urgente-sqs"
}
