package unit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.cloud.aws.AwsStorageBucketsCow;
import bio.terra.cli.serialization.userfacing.resource.UFAwsBucket;
import bio.terra.cli.service.WorkspaceManagerService;
import bio.terra.workspace.model.AwsCredentialAccessScope;
import harness.TestCommand;
import harness.baseclasses.SingleWorkspaceUnitAws;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Tests for including the number of objects in a AWS bucket resource's description. */
@Tag("unit-aws")
public class AwsBucketNumObjects extends SingleWorkspaceUnitAws {

  @Disabled("TERRA-412 - add support for put object (permission error with current credentials")
  @Test
  @DisplayName("controlled bucket displays the number of objects")
  void numObjectsForControlled() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getUserFacingId());

    // `terra resource create aws-bucket --name=$bucketName`
    String resourceName = UUID.randomUUID().toString();
    UFAwsBucket createdBucket =
        TestCommand.runAndParseCommandExpectSuccess(
            UFAwsBucket.class, "resource", "create", "aws-bucket", "--name=" + resourceName);

    // check that there are initially 0 objects reported in the bucket
    assertEquals(0, createdBucket.numObjects, "created bucket contains 0 objects");

    AwsStorageBucketsCow storageCow =
        AwsStorageBucketsCow.create(
            WorkspaceManagerService.fromContext()
                .getAwsBucketCredential(
                    Context.requireWorkspace().getUuid(),
                    createdBucket.id,
                    AwsCredentialAccessScope.WRITE_READ,
                    WorkspaceManagerService.AWS_CREDENTIAL_EXPIRATION_SECONDS_DEFAULT),
            createdBucket.location);

    // write a blob to the bucket
    storageCow.putBlob(
        createdBucket.bucketName,
        createdBucket.bucketPrefix,
        new AwsStorageBucketsCow.AwsS3Blob(
            "TestString".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8.name()));

    // `terra resource describe --name=$name`
    UFAwsBucket describedBucket =
        TestCommand.runAndParseCommandExpectSuccess(
            UFAwsBucket.class, "resource", "describe", "--name=" + resourceName);

    // check that there is now 1 object reported in the bucket
    assertEquals(1, describedBucket.numObjects, "described bucket contains 1 object");

    // `terra resource delete --name=$name`
    TestCommand.runCommandExpectSuccess("resource", "delete", "--name=" + resourceName, "--quiet");
  }

  // TODO(TERRA-196) Add support for referenced bucket
}
