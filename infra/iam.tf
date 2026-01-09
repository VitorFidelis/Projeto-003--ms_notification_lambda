resource "aws_iam_role" "lambda_exec_role" {
  name = "lambda-java-sqs-sns-role"

  description = "IAM Role para execução de funções Lambda em Java com permissão de publicar no SNS"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Principal = {
          Service = "lambda.amazonaws.com"
        }
        Action = "sts:AssumeRole"
      }
    ]
  })
}

#####################################
# Policy para Lambda
#####################################

resource "aws_iam_policy" "lambda_policy" {
  name        = "lambda-sqs-sns-policy"
  description = "Permite Lambda publicar mensagens no SNS"
  policy      = file("${path.module}/policy/lambda.json")
}

#####################################
# Anexos de Policy para Lambda
#####################################

resource "aws_iam_role_policy_attachment" "lambda_policy_attach" {
  role       = aws_iam_role.lambda_exec_role.name
  policy_arn = aws_iam_policy.lambda_policy.arn
}

resource "aws_iam_role_policy_attachment" "lambda_basic_exec" {
  role       = aws_iam_role.lambda_exec_role.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole"
}

resource "aws_iam_role_policy_attachment" "lambda_vpc_access" {
  role       = aws_iam_role.lambda_exec_role.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSLambdaVPCAccessExecutionRole"
}