package com.myorg;

import software.constructs.Construct;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.applicationautoscaling.EnableScalingProps;
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

public class Service01Stack extends Stack {
        public Service01Stack(final Construct scope, final String id, Cluster cluster) {
                this(scope, id, null, cluster); // Chama o construtor da linha 13(abaixo) com StackProps nulo utilizando
                                                // chaining de construtores
        }

        public Service01Stack(final Construct scope, final String id, final StackProps props, Cluster cluster) {
                super(scope, id, props); // Chama o construtor da classe Stack com os parâmetros fornecidos e o executa
                                         // sem o cluster, pois o cluster não é um parâmetro esperado pela classe Stack
                // Esse construtor agora aceita o cluster como um parâmetro adicional e é
                // executado corretamente.
                // use o cluster aqui
                // exemplo:
                // ApplicationLoadBalancedFargateService service01 =
                // ApplicationLoadBalancedFargateService.Builder.create(this, "Service01")
                // .cluster(cluster)
                // .build();

                ApplicationLoadBalancedFargateService service01 = ApplicationLoadBalancedFargateService.Builder
                                .create(this, "ALB01") // Cria o serviço Fargate com o nome lógico "ALB01" no CDK
                                .serviceName("service01") // Define o nome do serviço ECS
                                .cluster(cluster) // Associa o serviço ao cluster ECS passado como parâmetro
                                .cpu(512) // Define a quantidade de CPU para cada tarefa (512 unidades = 0.5 vCPU)
                                .memoryLimitMiB(1024) // Define o limite de memória para cada tarefa (1024 MiB)
                                .desiredCount(2) // Número desejado de tarefas rodando simultaneamente
                                .listenerPort(8080) // Porta do listener do Load Balancer
                                .taskImageOptions(ApplicationLoadBalancedTaskImageOptions.builder()
                                                .containerName("aws_project01") // Nome do container dentro da tarefa
                                                                                // ECS
                                                .image(ContainerImage.fromRegistry("jctinin/aws_project01:0.0.3")) // Imagem
                                                                                                                   // Docker
                                                                                                                   // do
                                                                                                                   // container
                                                .containerPort(8080) // Porta exposta pelo container
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
                                                                           // CloudWatch
                                                .build()) // Finaliza as opções de imagem da tarefa
                                .publicLoadBalancer(true)
                                .build(); // Finaliza a construção do serviço Fargate

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
