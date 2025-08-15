package com.myorg;

import software.constructs.Construct;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.Stack;

public class VpcStack extends Stack {
    // This class will define the VPC stack
    // You can add resources like VPC, subnets, etc. here

    private Vpc vpc;

    public VpcStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public VpcStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        this.vpc = Vpc.Builder.create(this, "Vpc01")
                .maxAzs(3) // Default is all AZs in the region
                .build();

        CfnOutput.Builder.create(this, "VpcDefaultSecurityGroupId")
                .exportName("VpcDefaultSecurityGroupId")
                .value(this.vpc.getVpcDefaultSecurityGroup())
                .build();

        // The code that defines your VPC stack goes here
        // Example: Create a VPC
        // final Vpc vpc = Vpc.Builder.create(this, "MyVpc")
        // .maxAzs(3) // Default is all AZs in the region
        // .build();
    }

    public Vpc getVpc() {
        return vpc;
    }

}
