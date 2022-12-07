package bio.terra.cli.utils;

import bio.terra.workspace.model.CloudPlatform;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class CloudPlatformCandidates1 implements Iterable<String> {
  static java.util.List<String> supportedCloudPlatforms = new ArrayList<>();

  public CloudPlatformCandidates1() {
    // this.supportedCloudPlatforms =
    //  Arrays.stream(CloudPlatform.values())
    //    .map(Objects::toString)
    //  .collect(Collectors.toList());
  }
  ;

  public static void setSupportedCloudPlatforms(List<CloudPlatform> cloudPlatforms) {
    supportedCloudPlatforms =
        cloudPlatforms.stream().map(Objects::toString).collect(Collectors.toList());
  }

  @Override
  public Iterator<String> iterator() {
    return supportedCloudPlatforms.iterator();
  }
}
