package com.myorg;

import software.constructs.Construct;

import java.util.Collections;

import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.Fn;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
// import software.amazon.awscdk.Duration;
// import software.amazon.awscdk.services.sqs.Queue;
import software.amazon.awscdk.services.ec2.ISecurityGroup;
import software.amazon.awscdk.services.ec2.InstanceClass;
import software.amazon.awscdk.services.ec2.InstanceSize;
import software.amazon.awscdk.services.ec2.InstanceType;
import software.amazon.awscdk.services.ec2.Peer;
import software.amazon.awscdk.services.ec2.Port;
import software.amazon.awscdk.services.ec2.SecurityGroup;
import software.amazon.awscdk.services.ec2.SubnetSelection;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.rds.Credentials;
import software.amazon.awscdk.services.rds.CredentialsFromUsernameOptions;
import software.amazon.awscdk.services.rds.DatabaseInstance;
import software.amazon.awscdk.services.rds.DatabaseInstanceEngine;
import software.amazon.awscdk.services.rds.MySqlInstanceEngineProps;
import software.amazon.awscdk.services.rds.MysqlEngineVersion;

public class RdsStack extends Stack {
        public RdsStack(final Construct scope, final String id, Vpc vpc) {
                this(scope, id, null, vpc);
        }

        public RdsStack(final Construct scope, final String id, final StackProps props, Vpc vpc) {
                super(scope, id, props); // Inicializa a stack base do CDK

                String defaultSgId = Fn.importValue("VpcDefaultSecurityGroupId");
                ISecurityGroup securityGroup = SecurityGroup.fromSecurityGroupId(this, "ImportedDefaultSG",
                                defaultSgId);

                // CfnParameter dataBasePassword = CfnParameter.Builder
                // .create(this, "databasePassword") // Cria um parâmetro de senha para o banco
                // de dados
                // .type("String") // Define o tipo do parâmetro como String
                // .description("The name of the database to create") // Descrição do parâmetro
                // .build(); // Finaliza a construção do parâmetro

                securityGroup.addIngressRule(Peer.anyIpv4(), Port.tcp(3306)); // Permite conexões TCP na porta 3306
                                                                              // (MySQL) de
                                                                              // qualquer IP

                DatabaseInstance databaseInstance = DatabaseInstance.Builder
                                .create(this, "Rds01") // Cria a instância do banco de dados com o nome lógico "Rds01"
                                .instanceIdentifier("aws-project01-db") // Define o identificador da instância do banco
                                                                        // de dados
                                .engine(DatabaseInstanceEngine.mysql(MySqlInstanceEngineProps.builder()
                                                .version(MysqlEngineVersion.VER_5_7) // Define a versão do MySQL
                                                .build())) // Finaliza a configuração do engine
                                .vpc(vpc) // Associa a instância do banco de dados à VPC fornecida
                                .credentials(
                                                Credentials.fromGeneratedSecret("Admin",
                                                                CredentialsFromUsernameOptions.builder()
                                                                                // .password(SecretValue.unsafePlainText(dataBasePassword.getValueAsString()))
                                                                                // // Define
                                                                                // // a
                                                                                // // senha
                                                                                // // do
                                                                                // // usuário
                                                                                // // Admin
                                                                                .build())) // Finaliza as credenciais do
                                                                                           // banco de dados
                                .instanceType(InstanceType.of(InstanceClass.BURSTABLE3, InstanceSize.MICRO)) // Define o
                                                                                                             // tipo de
                                                                                                             // instância
                                .multiAz(false) // Define se a instância é Multi-AZ
                                .allocatedStorage(10) // Define o armazenamento alocado em GB
                                .securityGroups(Collections.singletonList(securityGroup)) // Associa o Security Group à
                                                                                          // instância do
                                                                                          // banco
                                // de dados
                                .vpcSubnets(SubnetSelection.builder()
                                                .subnets(vpc.getPrivateSubnets()) // Define as sub-redes privadas da VPC
                                                .build()) // Finaliza a seleção de sub-redes
                                .build(); // Finaliza a construção da instância do banco de dados

                CfnOutput.Builder.create(this, "rds-endpoint")
                                .exportName("rds-endpoint")
                                .value(databaseInstance.getDbInstanceEndpointAddress()) // Exporta o endpoint da
                                                                                        // instância do banco de
                                                                                        // dados
                                .build(); // Finaliza a construção do output

                CfnOutput.Builder.create(this, "rds-password-secret-arn")
                                .exportName("rds-password-secret-arn")
                                .value(databaseInstance.getSecret().getSecretArn())
                                .build();
        }

}
