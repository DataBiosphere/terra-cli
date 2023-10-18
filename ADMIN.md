# terra-cli

1. [Spend](#spend)
    * [Grant spend access](#grant-spend-access)
    * [Setup spend profile](#setup-spend-profile)
2. [Break-glass](#break-glass)
    * [Grant break-glass](#grant-break-glass)
    * [Requests catalog](#requests-catalog)
3. [Users](#users)
    * [Invite user](#invite-user)
    * [Check registration status](#check-registration-status)

-----

## Spend

```
Usage: terra spend [COMMAND]
Manage spend profiles.
Commands:
  create-profile  Create a spend profile.
  enable          Enable use of a spend profile for a user or group.
  delete-profile  Delete a spend profile.
  disable         Disable use of a spend profile for a user or group.
  list-users      List the users enabled on a spend profile.
```

In order to spend money (e.g. by creating a project and resources within it) in
Terra, you need access to a billing account via a spend profile. In the future,
there will be a dedicated Spend Profile Manager service that will allow users to
set up different profiles that map to different billing accounts, and to grant
access to users.

Until that new service is built, setting up a new spend profile entails:

* Adding WSM configuration (helm charts) which associates spend profile name
  with a billing account ID
* Creating the SAM spend profile resource with `terra spend create-profile`

Note that these commands are intended for admin users. In the context of spend,
admin means a user who is an owner of the target spend profile resource.

### Grant spend access

* [Preferred] Add a user to a Terra group that is a user of the spend profile.
  To also grant permission to add new members to the group, use `policy=ADMIN`
  instead.
  ```shell
  terra group add-user --name=enterprise-pilot-testers --policy=MEMBER --email=testuser@gmail.com
  ```

* Add a user directly to a spend profile. To also grant permission to add new
  users to the spend profile, user `policy=OWNER` instead. This will target the
  WSM default spend profile.
  ```shell
  terra spend enable --policy=USER --email=testuser@gmail.com
  ```

* To use an alternative spend profile, add the `--profile` option.
  ```shell
  terra spend enable --policy=USER --email=testuser@gmail.com --profile=wm-alt-spend-profile
  ```

### Setup spend profile

To create the spend profile:

```shell
terra spend create-profile
```

This command uses the `wm-default-spend-profile` by default, or the name
specified by the `--profile` option.

The user who runs the create command will automatically be added as an OWNER of
the spend profile, with permissions to add new users. You should also make sure
that other people have access to the profile, so that changes can be made when
you're unavailable. The recommended way to do this is to create an admins group
and grant that group OWNER access to the spend profile. e.g.:

```shell
terra group create --name=developer-admins
terra group add-user --name=developer-admins --email=admin1@gmail.com --policy=ADMIN
terra group add-user --name=developer-admins --email=admin2@gmail.com --policy=ADMIN

terra spend create-profile
terra spend enable --email=developer-admins@gmail.com --policy=OWNER
```

To delete the spend profile:

```shell
terra spend delete-profile
```

This is helpful if you accidentally create the spend profile with e.g. the wrong
name and need to start over.

-----

## Break-glass

A break-glass implementation means that there is a way for users to request
elevated permissions on a workspace. These elevated permissions invalidate the
contract with WSM. Any guarantees about policy or access enforcement are off.

The purpose of break-glass access is to grant select trusted users the ability
to try out cloud features that are not yet available via WSM. The goal of this
experimentation is to understand what use cases WSM could support in the future.

Break-glass is intended for non-production environments only. This contributed
to the decision to implement this on the client side, instead of e.g. as a new
WSM endpoint.

### Grant break-glass

To grant break-glass access to someone:

1. Ask the requester to:
    * Make your Terra user an owner of the workspace they want break-glass
      access to.
      ```shell
      terra workspace add-user --email=breakglassgranter@gmail.com --role=OWNER
      ```
    * Confirm that they are an owner of the workspace.
      ```shell
      terra workspace list-users
      ```
    * Relay any brief notes about the reason for the request.

2. Download two SA key files:
    * One that has permission to set IAM policy on all workspace projects. This
      will be specific to the server (i.e. WSM deployment) where the workspaces
      live.
    * One that has permission to update a central BigQuery dataset that tracks
      break-glass requests.
    * The `tools/render-config.sh` script downloads two SA key files that will
      work for workspaces on the `broad-dev-*` servers and the central BigQuery
      dataset in the `terra-cli-dev` project.

3. Run the `terra workspace break-glass` command.

Example commands for granting break-glass access for a workspace in
the `broad-dev-cli-testing` deployment:

```shell
./tools/render-config.sh
terra auth login # login as yourself, the break-glass granter
terra workspace break-glass --email=breakglassrequester@gmail.com --big-query-project=terra-cli-dev --big-query-sa=rendered/broad/ci-account.json --user-project-admin-sa=rendered/broad/wsm-sa.json --notes="Testing break-glass command."
```

### Requests catalog

The `terra workspace break-glass` command updates a central BigQuery dataset to
track break-glass requests. This dataset was set up by a script:

```shell
gcloud auth activate-service-account dev-ci-sa@broad-dsde-dev.iam.gserviceaccount.com --key-file=./rendered/broad/ci-account.json
./tools/create-break-glass-bq.sh terra-cli-dev
```

In the future, we can run the same script with different credentials and project
id to set up another central BigQuery dataset somewhere else. (e.g. one for
Verily deployments, one for Broad deployments). The SA activated in the first
command needs to have BigQuery admin privileges in the target project.

The current central BigQuery dataset exists in the `terra-cli-dev` project.

-----

## Users

```
Usage: terra user [COMMAND]
Manage users.
Commands:
  invite  Invite a new user.
  status  Check the registration status of a user.
  ssh-key Get or create a Terra ssh key for user's GitHub account.
```

Note that `invite` and `status` commands are intended for admin users. In the
context of user management, admin means a user who is a member of
the `fc-admins` Google group in the GSuite domain that SAM manages.

The terra user can only get the ssh-key for themselves.

### Invite user

In Broad deployments, registration is open to anyone with a Google account. In
Verily deployments, registration is not open to anyone with a Google account.
Instead, users must accept the Terms of Service associated with the VerilyGroup
of Terra users. Then they must be invited into the system (`terra user invite`)
before they can register.

When inviting a new user, admins can also optionally enable the user on the
default spend profile.

```shell
> terra user invite --email=newuser@gmail.com --enable-spend
Successfully invited user.
User enabled on the default spend profile.
```

Registration happens automatically with the first CLI login. `terra auth login`
or any other command that requires being logged in (e.g. `terra workspace list`)
will trigger the login flow.

### Check registration status

The `terra user status` command indicates whether:

* A user has no record in the system (i.e. not been invited or registered).
  ```shell
  > terra user status --email=notinvited@gmail.com
  User not found: notinvited@gmail.com
  ```

* A user has been invited, but not yet registered by logging in for the first
  time.
  ```shell
  > terra user status --email=invited@gmail.com
  Email: invited@gmail.com
  Subject ID: 263543418278082e7fc11
  NOT REGISTERED
  DISABLED
  ```

* A user has registered by logging in for the first time.
  ```shell
  > terra user status --email=registered@gmail.com
  Email: registered@gmail.com
  Subject ID: 263543418278082e7fc11
  REGISTERED
  ENABLED
  ```
