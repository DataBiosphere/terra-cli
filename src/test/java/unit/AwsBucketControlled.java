package unit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import bio.terra.cli.serialization.userfacing.input.GcsStorageClass;
import bio.terra.cli.serialization.userfacing.resource.UFAwsBucket;
import bio.terra.cli.service.utils.CrlUtils;
import bio.terra.workspace.model.AccessScope;
import bio.terra.workspace.model.CloningInstructionsEnum;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.cloud.storage.Bucket;
import harness.TestCommand;
import harness.baseclasses.SingleWorkspaceUnitAws;
import harness.utils.ExternalGCSBuckets;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.hamcrest.CoreMatchers;
import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Tests for the `terra resource` commands that handle controlled AWS buckets. */
@Tag("unit-aws")
public class AwsBucketControlled extends SingleWorkspaceUnitAws {
    /**
     * Helper method to call `terra resources list` and expect one resource with this name. Uses the
     * current workspace.
     */
    static UFAwsBucket listOneBucketResourceWithNameAws(String resourceName)
            throws JsonProcessingException {
        return listOneBucketResourceWithNameAws(resourceName, null);
    }

    /**
     * Helper method to call `terra resources list` and expect one resource with this name. Filters on
     * the specified workspace id; Uses the current workspace if null.
     */
    static UFAwsBucket listOneBucketResourceWithNameAws(String resourceName, String userFacingId)
            throws JsonProcessingException {
        List<UFAwsBucket> matchedResources = listBucketResourcesWithNameAws(resourceName, userFacingId);

        assertEquals(1, matchedResources.size(), "found exactly one resource with this name");
        return matchedResources.get(0);
    }

    /**
     * Helper method to call `terra resources list` and filter the results on the specified resource
     * name. Uses the current workspace.
     */
    static List<UFAwsBucket> listBucketResourcesWithNameAws(String resourceName)
            throws JsonProcessingException {
        return listBucketResourcesWithNameAws(resourceName, null);
    }

    /**
     * Helper method to call `terra resources list` and filter the results on the specified resource
     * name and workspace (uses the current workspace if null).
     */
    static List<UFAwsBucket> listBucketResourcesWithNameAws(
            String resourceName, String workspaceUserFacingId) throws JsonProcessingException {
        // `terra resources list --type=AWS_BUCKET --format=json`
        List<UFAwsBucket> listedResources =
                workspaceUserFacingId == null
                        ? TestCommand.runAndParseCommandExpectSuccess(
                        new TypeReference<>() {}, "resource", "list", "--type=AWS_BUCKET")
                        : TestCommand.runAndParseCommandExpectSuccess(
                        new TypeReference<>() {},
                        "resource",
                        "list",
                        "--type=AWS_BUCKET",
                        "--workspace=" + workspaceUserFacingId);

        // find the matching bucket in the list
        return listedResources.stream()
                .filter(resource -> resource.name.equals(resourceName))
                .collect(Collectors.toList());
    }

    @Test
    @DisplayName("list and describe reflect creating a new controlled AWS bucket")
    void listDescribeReflectCreateAws() throws IOException {
        workspaceCreator.login();

        // `terra workspace set --id=$id`
        TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getUserFacingId());

        // `terra resource create aws-bucket --name=$name --bucket-name=$bucketName`
        String bucketName = UUID.randomUUID().toString();
        UFAwsBucket createdBucket =
                TestCommand.runAndParseCommandExpectSuccess(
                        UFAwsBucket.class,
                        "resource",
                        "create",
                        "aws-bucket",
                        "--name=" + bucketName);

        // check that the name and bucket name match
        assertEquals(bucketName, createdBucket.name, "create output matches name");

        // check that the bucket is in the list
        UFAwsBucket matchedResource = listOneBucketResourceWithNameAws(bucketName);
        assertEquals(bucketName, matchedResource.name, "list output matches name");
        assertEquals(bucketName, matchedResource.bucketName, "list output matches bucket name");

        // `terra resource describe --name=$name --format=json`
        UFAwsBucket describeResource =
                TestCommand.runAndParseCommandExpectSuccess(
                        UFAwsBucket.class, "resource", "describe", "--name=" + bucketName);

        // check that the name and bucket name match
        assertEquals(bucketName, describeResource.name, "describe resource output matches name");
        assertEquals(
                bucketName, describeResource.bucketName, "describe resource output matches bucket name");

        // `terra resource delete --name=$name`
        TestCommand.runCommandExpectSuccess("resource", "delete", "--name=" + bucketName, "--quiet");
    }





    @Test
    @DisplayName("create a new controlled GCS bucket without specifying the bucket name")
    void createBucketWithoutSpecifyingBucketNameAws() throws IOException {
        // TODO-Dex
        workspaceCreator.login();

        // `terra workspace set --id=$id`
        TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getUserFacingId());

        // `terra resource create gcs-bucket --name=$name --bucket-name=$bucketName`
        String name = "GcsBucketWithoutSpecifyingBucketName";
        UFAwsBucket createdBucket =
                TestCommand.runAndParseCommandExpectSuccess(
                        UFAwsBucket.class, "resource", "create", "aws-bucket", "--name=" + name);

        // check that the name and bucket name match
        assertEquals(name, createdBucket.name, "create output matches name");
        String bucketName = createdBucket.bucketName;
        assertNotNull(bucketName, "a random bucket name is generated");
        assertTrue(bucketName.contains(name.toLowerCase()));

        // check that the bucket is in the list
        UFAwsBucket matchedResource = listOneBucketResourceWithNameAws(name);
        assertEquals(name, matchedResource.name, "list output matches name");
        assertEquals(bucketName, matchedResource.bucketName, "list output matches bucket name");

        // `terra resource describe --name=$name --format=json`
        UFAwsBucket describeResource =
                TestCommand.runAndParseCommandExpectSuccess(
                        UFAwsBucket.class, "resource", "describe", "--name=" + name);

        // check that the name and bucket name match
        assertEquals(name, describeResource.name, "describe resource output matches name");
        assertEquals(
                bucketName, describeResource.bucketName, "describe resource output matches bucket name");

        // `terra resource delete --name=$name`
        TestCommand.runCommandExpectSuccess("resource", "delete", "--name=" + name, "--quiet");
    }

    @Test
    @DisplayName("list reflects deleting a controlled AWS bucket")
    void listReflectsDeleteAws() {
        // TODO(TERRA-148) Support bucket deletion
    }

    @Test
    @DisplayName("resolve a controlled AWS bucket")
    void resolveAws() throws IOException {
        workspaceCreator.login();

        // `terra workspace set --id=$id`
        TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getUserFacingId());

        // `terra resource create gcs-bucket --name=$name --bucket-name=$bucketName`
        String name = "resolve";
        String bucketName = UUID.randomUUID().toString();
        TestCommand.runCommandExpectSuccess(
                "resource", "create", "aws-bucket", "--name=" + name, "--bucket-name=" + bucketName);

        // `terra resource resolve --name=$name --format=json`
        JSONObject resolved =
                TestCommand.runAndGetJsonObjectExpectSuccess("resource", "resolve", "--name=" + name);
        assertEquals(
                ExternalGCSBuckets.getGsPath(bucketName),
                resolved.get(name),
                "default resolve includes gs:// prefix");

        // `terra resource resolve --name=$name --exclude-bucket-prefix --format=json`
        JSONObject resolvedExcludePrefix =
                TestCommand.runAndGetJsonObjectExpectSuccess(
                        "resource", "resolve", "--name=" + name, "--exclude-bucket-prefix");
        assertEquals(
                bucketName,
                resolvedExcludePrefix.get(name),
                "exclude prefix resolve only includes bucket name");

        // `terra resources delete --name=$name`
        TestCommand.runCommandExpectSuccess("resource", "delete", "--name=" + name, "--quiet");
    }

    @Test
    @DisplayName("check-access for a controlled AWS bucket")
    void checkAccessAws() throws IOException {
        workspaceCreator.login();

        // `terra workspace set --id=$id`
        TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getUserFacingId());

        // `terra resources create gcs-bucket --name=$name --bucket-name=$bucketName`
        String name = "checkAccess";
        String bucketName = UUID.randomUUID().toString();
        TestCommand.runCommandExpectSuccess(
                "resource", "create", "aws-bucket", "--name=" + name, "--bucket-name=" + bucketName);

        // `terra resources check-access --name=$name`
        String stdErr =
                TestCommand.runCommandExpectExitCode(1, "resource", "check-access", "--name=" + name);
        assertThat(
                "error message includes wrong stewardship type",
                stdErr,
                CoreMatchers.containsString("Checking access is intended for REFERENCED resources only"));

        // `terra resources delete --name=$name`
        TestCommand.runCommandExpectSuccess("resource", "delete", "--name=" + name, "--quiet");
    }

    @Test
    @DisplayName("create a controlled AWS bucket, specifying all options except lifecycle")
    void createWithAllOptionsExceptLifecycleAws() throws IOException, InterruptedException {
        workspaceCreator.login();

        // `terra workspace set --id=$id`
        TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getUserFacingId());

        // `terra resources create gcs-bucket --name=$name --bucket-name=$bucketName --access=$access
        // --cloning=$cloning --description=$description --location=$location --storage=$storage
        // --format=json`
        String name = "createWithAllOptionsExceptLifecycle";
        String bucketName = UUID.randomUUID().toString();
        AccessScope access = AccessScope.PRIVATE_ACCESS;
        CloningInstructionsEnum cloning = CloningInstructionsEnum.RESOURCE;
        String description = "\"create with all options except lifecycle\"";
        String location = "US";
        GcsStorageClass storage = GcsStorageClass.NEARLINE;
        UFAwsBucket createdBucket =
                TestCommand.runAndParseCommandExpectSuccess(
                        UFAwsBucket.class,
                        "resource",
                        "create",
                        "aws-bucket",
                        "--name=" + name,
                        "--bucket-name=" + bucketName,
                        "--access=" + access,
                        "--cloning=" + cloning,
                        "--description=" + description,
                        "--location=" + location,
                        "--storage=" + storage);

        // check that the properties match
        assertEquals(name, createdBucket.name, "create output matches name");
        assertEquals(bucketName, createdBucket.bucketName, "create output matches bucket name");
        assertEquals(access, createdBucket.accessScope, "create output matches access");
        assertEquals(cloning, createdBucket.cloningInstructions, "create output matches cloning");
        assertEquals(description, createdBucket.description, "create output matches description");
        assertEquals(
                workspaceCreator.email.toLowerCase(),
                createdBucket.privateUserName.toLowerCase(),
                "create output matches private user name");

        Bucket createdBucketOnCloud =
                CrlUtils.callGcpWithPermissionExceptionRetries(
                        () ->
                                ExternalGCSBuckets.getStorageClient(
                                                workspaceCreator.getCredentialsWithCloudPlatformScope())
                                        .get(bucketName));
        assertNotNull(createdBucketOnCloud, "looking up bucket via GCS API succeeded");
        assertEquals(
                location, createdBucketOnCloud.getLocation(), "bucket location matches create input");
        assertEquals(
                storage.toString(),
                createdBucketOnCloud.getStorageClass().toString(),
                "bucket storage class matches create input");

        // `terra resources describe --name=$name --format=json`
        UFAwsBucket describeResource =
                TestCommand.runAndParseCommandExpectSuccess(
                        UFAwsBucket.class, "resource", "describe", "--name=" + name);

        // check that the properties match
        assertEquals(name, describeResource.name, "describe resource output matches name");
        assertEquals(
                bucketName, describeResource.bucketName, "describe resource output matches bucket name");
        assertEquals(access, describeResource.accessScope, "describe output matches access");
        assertEquals(cloning, describeResource.cloningInstructions, "describe output matches cloning");
        assertEquals(description, describeResource.description, "describe output matches description");
        assertEquals(
                workspaceCreator.email.toLowerCase(),
                describeResource.privateUserName.toLowerCase(),
                "describe output matches private user name");

        // `terra resources delete --name=$name`
        TestCommand.runCommandExpectSuccess("resource", "delete", "--name=" + name, "--quiet");
    }

    @Test
    @DisplayName("update a controlled AWS bucket, one property at a time, except for lifecycle")
    void updateIndividualPropertiesAws() throws IOException {
        workspaceCreator.login();

        // `terra workspace set --id=$id`
        TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getUserFacingId());

        // `terra resources create gcs-bucket --name=$name --description=$description
        // --bucket-name=$bucketName`
        String name = "updateIndividualProperties";
        String description = "updateDescription";
        String bucketName = UUID.randomUUID().toString();
        TestCommand.runCommandExpectSuccess(
                "resource",
                "create",
                "aws-bucket",
                "--name=" + name,
                "--description=" + description,
                "--bucket-name=" + bucketName,
                "--cloning=" + CloningInstructionsEnum.RESOURCE);

        // update just the name
        // `terra resources update gcs-bucket --name=$name --new-name=$newName`
        String newName = "updateIndividualProperties_NEW";
        UFAwsBucket updateBucket =
                TestCommand.runAndParseCommandExpectSuccess(
                        UFAwsBucket.class,
                        "resource",
                        "update",
                        "aws-bucket",
                        "--name=" + name,
                        "--new-name=" + newName);
        assertEquals(newName, updateBucket.name);
        assertEquals(description, updateBucket.description);

        // `terra resources describe --name=$newName`
        UFAwsBucket describeBucket =
                TestCommand.runAndParseCommandExpectSuccess(
                        UFAwsBucket.class, "resource", "describe", "--name=" + newName);
        assertEquals(description, describeBucket.description);

        // update just the description
        // `terra resources update gcs-bucket --name=$newName --new-description=$newDescription`
        String newDescription = "updateDescription_NEW";
        updateBucket =
                TestCommand.runAndParseCommandExpectSuccess(
                        UFAwsBucket.class,
                        "resource",
                        "update",
                        "aws-bucket",
                        "--name=" + newName,
                        "--new-description=" + newDescription,
                        "--new-cloning=" + CloningInstructionsEnum.NOTHING);
        assertEquals(newName, updateBucket.name);
        assertEquals(newDescription, updateBucket.description);
        // see if the returned structure is up-to-date for cloning instructions
        assertEquals(CloningInstructionsEnum.NOTHING, updateBucket.cloningInstructions);

        // `terra resources describe --name=$newName`
        describeBucket =
                TestCommand.runAndParseCommandExpectSuccess(
                        UFAwsBucket.class, "resource", "describe", "--name=" + newName);
        assertEquals(newDescription, describeBucket.description);

        // update just the storage class
        // `terra resources update gcs-bucket --name=$newName --storageClass
        GcsStorageClass newStorage = GcsStorageClass.ARCHIVE;
        TestCommand.runCommandExpectSuccess(
                "resource", "update", "aws-bucket", "--name=" + newName, "--storage=" + newStorage);

        // check the updated storage class from GCS directly
        Bucket bucketOnCloud =
                ExternalGCSBuckets.getStorageClient(workspaceCreator.getCredentialsWithCloudPlatformScope())
                        .get(bucketName);
        assertNotNull(bucketOnCloud, "looking up bucket via GCS API succeeded");
        assertEquals(
                newStorage.toString(),
                bucketOnCloud.getStorageClass().toString(),
                "bucket storage class matches update input");
    }

    @Test
    @DisplayName("update a controlled AWS bucket, specifying multiple properties, except for lifecycle")
    void updateMultiplePropertiesAws() throws IOException {
        workspaceCreator.login();

        // `terra workspace set --id=$id`
        TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getUserFacingId());

        // `terra resources create gcs-bucket --name=$name --description=$description
        // --bucket-name=$bucketName`
        String name = "updateMultipleProperties";
        String description = "updateDescription";
        String bucketName = UUID.randomUUID().toString();
        TestCommand.runCommandExpectSuccess(
                "resource",
                "create",
                "aws-bucket",
                "--name=" + name,
                "--description=" + description,
                "--bucket-name=" + bucketName);

        // update the name, description, and storage class
        // `terra resources update gcs-bucket --name=$newName --new-name=$newName
        // --new-description=$newDescription --storage=$newStorage`
        String newName = "updateMultipleProperties_NEW";
        String newDescription = "updateDescription_NEW";
        GcsStorageClass newStorage = GcsStorageClass.NEARLINE;
        UFAwsBucket updatedBucket =
                TestCommand.runAndParseCommandExpectSuccess(
                        UFAwsBucket.class,
                        "resource",
                        "update",
                        "aws-bucket",
                        "--name=" + name,
                        "--new-name=" + newName,
                        "--new-description=" + newDescription,
                        "--storage=" + newStorage);
        assertEquals(newName, updatedBucket.name);
        assertEquals(newDescription, updatedBucket.description);

        // `terra resources describe --name=$newName`
        UFAwsBucket describeBucket =
                TestCommand.runAndParseCommandExpectSuccess(
                        UFAwsBucket.class, "resource", "describe", "--name=" + newName);
        assertEquals(newDescription, describeBucket.description);

        // check the storage class from GCS directly
        Bucket bucketOnCloud =
                ExternalGCSBuckets.getStorageClient(workspaceCreator.getCredentialsWithCloudPlatformScope())
                        .get(bucketName);
        assertNotNull(bucketOnCloud, "looking up bucket via GCS API succeeded");
        assertEquals(
                newStorage.toString(),
                bucketOnCloud.getStorageClass().toString(),
                "bucket storage class matches update input");
    }
}
