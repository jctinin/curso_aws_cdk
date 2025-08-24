package com.myorg;

import software.amazon.awscdk.App;


public class CursoAwsCdkApp {
        public static void main(final String[] args) {
                App app = new App();


                // Environment env = Environment.builder()
                //                 .account(System.getenv("CDK_DEFAULT_ACCOUNT"))
                //                 .region(System.getenv("CDK_DEFAULT_REGION"))
                //                 .build();

                // new CursoAwsCdkStack(app, "CursoAwsCdkStack", StackProps.builder()
                //                 .env(env)
                //                 .build());

                VpcStack vpcStack = new VpcStack(app, "Vpc");

                ClusterStack clusterStack = new ClusterStack(app, "Cluster", vpcStack.getVpc());
                clusterStack.addDependency(vpcStack); // Ensure VPC is created before the cluster

                RdsStack rdsStack = new RdsStack(app, "Rds", vpcStack.getVpc());
                rdsStack.addDependency(vpcStack); // Ensure VPC is created before the RDS instance

                Service01Stack service01Stack = new Service01Stack(app, "Service01", clusterStack.getCluster(), vpcStack.getVpc());

                service01Stack.addDependency(clusterStack); // Ensure cluster is created before the service
                service01Stack.addDependency(rdsStack); // Ensure RDS is created before the service

                app.synth();

        }

}
