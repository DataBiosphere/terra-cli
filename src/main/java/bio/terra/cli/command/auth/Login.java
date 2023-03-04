package bio.terra.cli.command.auth;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.User;
import bio.terra.cli.command.shared.BaseCommand;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra auth login" command. */
@Command(
    name = "login",
    description = "Authorize the CLI to access Terra APIs and data with user credentials.",
    showDefaultValues = true)
public class Login extends BaseCommand {
  @CommandLine.Option(
      names = "--mode",
      description = "Set the log in mode: ${COMPLETION-CANDIDATES}. Default to BROWSER",
      defaultValue = "BROWSER")
  private LogInMode mode;

  /** Login the user and print out a success message. */
  @Override
  protected void execute() {
    // if the user is already logged in, log them out first
    if (Context.getUser().isPresent()) {
      Context.requireUser().logout();
    }

    User.login(mode);
    // Auth0HttpClient http =
    //     DefaultHttpClient.newBuilder()
    //         .withConnectTimeout(10)
    //         .withReadTimeout(10)
    //         // additional configurations as needed
    //         .build();
    // AuthAPI auth =
    //     AuthAPI.newBuilder(
    //             "terra-sandbox.us.auth0.com",
    //             "9iX5StTZSzVGTxwPw94F47pCG77AXpeF",
    //             "61Dmg1ut-N60GjYfBokmpSSoK4__5BnPo3jzA4dAaDU133u7_DqqxGbfY4cSQMQJ")
    //         .withHttpClient(http)
    //         .build();
    // SecureRandom sr = new SecureRandom();
    // byte[] code = new byte[32];
    // sr.nextBytes(code);
    // String verifier = Base64.getUrlEncoder().withoutPadding().encodeToString(code);
    //
    // try {
    //   byte[] bytes = verifier.getBytes("US-ASCII");
    //   MessageDigest md = MessageDigest.getInstance("SHA-256");
    //   md.update(bytes, 0, bytes.length);
    //   byte[] digest = md.digest();
    //   String challenge = Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
    //   String url =
    //       auth.authorizeUrl("https://github.com/DataBiosphere/terra-cli/blob/main/README.md")
    //           .withCodeChallenge(challenge)
    //           .build();
    //   OUT.println(url);
    //   CountDownLatch latch = new CountDownLatch(1);
    //   // Try to open the website in the default browser.
    //   try {
    //     // Get the Desktop object.
    //     Desktop desktop = Desktop.getDesktop();
    //     desktop.setOpenURIHandler(
    //         e -> {
    //           OUT.println("hello");
    //           OUT.println(e.getURI());
    //           String token = e.getURI().getQuery().split("=")[1];
    //           OUT.println(token);
    //           TokenHolder result = null;
    //           try {
    //             result =
    //                 auth.exchangeCodeWithVerifier(
    //                         token,
    //                         verifier,
    //                         "https://github.com/DataBiosphere/terra-cli/blob/main/README.md")
    //                     .setScope("openid profile email")
    //                     .execute()
    //                     .getBody();
    //             OUT.println("result " + result.getAccessToken());
    //             latch.countDown();
    //           } catch (Auth0Exception ex) {
    //             throw new RuntimeException(ex);
    //           }
    //         });
    //
    //     // Open the website in the default browser.
    //     desktop.browse(new URI(url));
    //
    //   } catch (URISyntaxException | IOException e) {
    //     throw new RuntimeException(e);
    //   }
    //   latch.await();
    // } catch (UnsupportedEncodingException e) {
    //   throw new RuntimeException(e);
    // } catch (NoSuchAlgorithmException e) {
    //   throw new RuntimeException(e);
    // } catch (InterruptedException e) {
    //   throw new RuntimeException(e);
    // }

    //   TokenHolder result =
    //       auth.exchangeCodeWithVerifier(
    //               "CODE",
    //               verifier,
    //               "https://github.com/DataBiosphere/terra-cli/blob/main/README.md")
    //           .setScope("openid profile email")
    //           .execute()
    //           .getBody();
    //   OUT.println(result.getAccessToken());
    // } catch (UnsupportedEncodingException | NoSuchAlgorithmException e) {
    //   OUT.println("should not happens " + e);
    // } catch (Auth0Exception e) {
    //   OUT.println("auth0 excepton " + e);
    // } catch (IOException e) {
    //   throw new RuntimeException(e);
    // }
    //
    // // TokenRequest tokenRequest =
    // auth.requestToken("https://terra-sandbox.us.auth0.com/api/v2/");
    // // try {
    // //   TokenHolder holder = tokenRequest.execute().getBody();
    // //   String accessToken = holder.getAccessToken();
    // //   OUT.println("Your access token is: ");
    // //   OUT.println(accessToken);
    // // } catch (Auth0Exception e) {
    // //   OUT.println("error getting token");
    // // }
    // // // ManagementAPI mgmt = ManagementAPI.newBuilder("{YOUR_DOMAIN}", accessToken).build();
    // // OUT.println("Login successful: " + Context.requireUser().getEmail());

  }

  /** Suppress the login by the super class, so that we can logout the user first, if needed. */
  @Override
  protected boolean requiresLogin() {
    return false;
  }

  public enum LogInMode {
    BROWSER,
    APP_DEFAULT_CREDENTIALS
  }
}
