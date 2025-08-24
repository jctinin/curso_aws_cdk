package com.myorg;

import software.constructs.Construct;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import software.amazon.awscdk.Duration;
import software.amazon.awscdk.Fn;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.applicationautoscaling.EnableScalingProps;
import software.amazon.awscdk.services.ec2.ISecurityGroup;
import software.amazon.awscdk.services.ec2.Port;
import software.amazon.awscdk.services.ec2.SecurityGroup;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.ecs.AwsLogDriverProps;
import software.amazon.awscdk.services.ecs.Cluster;
import software.amazon.awscdk.services.ecs.ContainerImage;
import software.amazon.awscdk.services.ecs.CpuUtilizationScalingProps;
import software.amazon.awscdk.services.ecs.LogDriver;
import software.amazon.awscdk.services.ecs.ScalableTaskCount;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedFargateService;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedTaskImageOptions;
import software.amazon.awscdk.services.elasticloadbalancingv2.HealthCheck;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.secretsmanager.ISecret;
import software.amazon.awscdk.services.secretsmanager.Secret;
import software.amazon.awscdk.services.ecs.RepositoryImageProps;

public class Service01Stack extends Stack {
        public Service01Stack(final Construct scope, final String id, Cluster cluster, Vpc vpc) {
                this(scope, id, null, cluster, vpc); // Chama o construtor da linha 13(abaixo) com StackProps nulo
                                                     // utilizando
                                                     // chaining de construtores
        }

        public Service01Stack(final Construct scope, final String id, final StackProps props, Cluster cluster,
                        Vpc vpc) {
                super(scope, id, props); // Chama o construtor da classe Stack com os parâmetros fornecidos e o executa
                                         // sem o cluster, pois o cluster não é um parâmetro esperado pela classe Stack
                // Esse construtor agora aceita o cluster como um parâmetro adicional e é
                // executado corretamente.
                ISecret dockerHubSecret = Secret.fromSecretNameV2(this, "DockerHubSecret", "jctinin/services");

                String secretArn = Fn.importValue("rds-password-secret-arn");
                ISecret rdsSecret = Secret.fromSecretCompleteArn(this, "ImportedSecret", secretArn);

                // Importe o security group do RDS
                String rdsSecurityGroupId = Fn.importValue("RdsSecurityGroupId");
                ISecurityGroup rdsSecurityGroup = SecurityGroup.fromSecurityGroupId(this, "ImportedRdsSg",
                                rdsSecurityGroupId);

                SecurityGroup ecsSecurityGroup = SecurityGroup.Builder.create(this, "EcsSecurityGroup")
                                .vpc(vpc)
                                .description("Security group for ECS service")
                                .build();

                // Permita que o ECS acesse o RDS
                rdsSecurityGroup.addIngressRule(
                                ecsSecurityGroup,
                                Port.tcp(3306),
                                "Allow ECS to connect to RDS");

                Map<String, String> envVariables = new HashMap<>();
                envVariables.put("SPRING_DATASOURCE_URL", "jdbc:mysql://" + Fn.importValue("rds-endpoint")
                                + ":3306/aws_project01?createDatabaseIfNotExist=true");
                envVariables.put("SPRING_DATASOURCE_USERNAME", "Admin");

                Map<String, software.amazon.awscdk.services.ecs.Secret> secretVariables = new HashMap<>();
                secretVariables.put("SPRING_DATASOURCE_PASSWORD",
                                software.amazon.awscdk.services.ecs.Secret.fromSecretsManager(rdsSecret, "password"));

                ApplicationLoadBalancedFargateService service01 = ApplicationLoadBalancedFargateService.Builder
                                .create(this, "ALB01") // Cria o serviço Fargate com o nome lógico "ALB01" no CDK
                                .serviceName("service01") // Define o nome do serviço ECS
                                .cluster(cluster) // Associa o serviço ao cluster ECS passado como parâmetro
                                .cpu(512) // Define a quantidade de CPU para cada tarefa (512 unidades = 0.5 vCPU)
                                .memoryLimitMiB(1024) // Define o limite de memória para cada tarefa (1024 MiB)
                                .desiredCount(2) // Número desejado de tarefas rodando simultaneamente
                                .listenerPort(8080) // Porta do listener do Load Balancer
                                .healthCheckGracePeriod(Duration.minutes(3)) // Período de carência para verificações de
                                                                             // integridade
                                .taskImageOptions(ApplicationLoadBalancedTaskImageOptions.builder()
                                                .containerName("aws_project01") // Nome do container dentro da tarefa
                                                                                // ECS
                                                .image(ContainerImage.fromRegistry("jctinin/aws_project01:0.0.8",
                                                                RepositoryImageProps.builder()
                                                                                .credentials(dockerHubSecret)
                                                                                .build())) // Imagem do
                                                                                           // container
                                                                                           // (Docker Hub)

                                                .containerPort(8080)
                                                .logDriver(LogDriver.awsLogs(AwsLogDriverProps.builder()
                                                                .logGroup(LogGroup.Builder
                                                                                .create(this, "Service01Group")
                                                                                .logGroupName("Service01") // Nome do
                                                                                                           // grupo de
                                                                                                           // logs no
                                                                                                           // CloudWatch
                                                                                .removalPolicy(RemovalPolicy.DESTROY) // Remove
                                                                                                                      // o
                                                                                                                      // grupo
                                                                                                                      // de
                                                                                                                      // logs
                                                                                                                      // ao
                                                                                                                      // destruir
                                                                                                                      // o
                                                                                                                      // stack
                                                                                .build())
                                                                .streamPrefix("Service01") // Prefixo do stream de logs
                                                                .build())) // Configura o driver de logs para AWS
                                                .environment(envVariables)
                                                // CloudWatch
                                                .secrets(secretVariables)
                                                .build()) // Finaliza as opções de imagem da tarefa
                                .publicLoadBalancer(true)
                                .securityGroups(List.of(ecsSecurityGroup))
                                .build(); // Finaliza a construção do serviço Fargate

                rdsSecret.grantRead(service01.getTaskDefinition().getTaskRole());

                service01.getTargetGroup().configureHealthCheck(new HealthCheck.Builder()
                                .path("/actuator/health") // Caminho para o endpoint de verificação de saúde
                                .port("8080") // Porta onde o serviço está escutando
                                .healthyHttpCodes("200") // Códigos HTTP considerados saudáveis
                                .build());

                ScalableTaskCount scalableTaskCount = service01.getService()
                                .autoScaleTaskCount(EnableScalingProps.builder()
                                                .minCapacity(2) // Define a capacidade mínima de instâncias de tarefa
                                                .maxCapacity(4) // Define a capacidade máxima de instâncias de tarefa
                                                .build());

                scalableTaskCount.scaleOnCpuUtilization("Service01AutoScaling", CpuUtilizationScalingProps.builder()
                                .targetUtilizationPercent(50) // Define a porcentagem de utilização de CPU alvo para
                                                              // escalonamento
                                .scaleInCooldown(Duration.seconds(60)) // Tempo de espera antes de permitir o
                                                                       // escalonamento para baixo
                                .scaleOutCooldown(Duration.seconds(60)) // Tempo de espera antes de permitir o
                                                                        // escalonamento para cima
                                .build());

        }
}
