# terra-cli

1. [Spend profile](#spend-profile)
    * [Grant spend access](#grant-spend-access)
    * [Setup spend profile](#setup-spend-profile)
2. [Break-glass](#break-glass)
    * [Grant break-glass](#grant-break-glass)
    * [Requests catalog](#requests-catalog)

-----

### Spend profile
In order to spend money (e.g. by creating a project and resources within it) in Terra, you need
access to a billing account via a spend profile. In the future, there will be a dedicated Spend
Profile Manager service that will allow users to setup different profiles that map to different
billing accounts, and to grant access to users.

Until that new service is build, Workspace Manager recognizes a single spend profile per deployment.
This single spend profile corresponds to a SAM resource. The name of this resource is specified in
the Workspace Manager deployment configuration (e.g. Helm charts) but needs to be setup and managed
manually in SAM (i.e. there are no WSM endpoints for managing spend profiles, you have to talk to SAM
directly). The CLI provides convenience commands for this setup and management.

#### Grant spend access
- [Preferred] Add a user to a Terra group that is a user of the spend profile. To also grant permission
  to add new members to the group, use `policy=ADMIN` instead.
  `terra group add-user --name=enterprise-pilot-testers --policy=MEMBER --email=testuser@gmail.com`

- Add a user directly to the spend profile. To also grant permission to add new users to the spend profile,
  user `policy=OWNER` instead.
  `terra spend enable --policy=USER --email=testuser@gmail.com`

#### Setup spend profile
To create the spend profile:
  `terra spend create-profile`
This command uses the spend profile name specified in the `src/main/resources/servers` file for the current
server. For most servers, the name is `wm-default-spend-profile`.

The user who runs the create command will automatically be added as an OWNER of the spend profile, with
permissions to add new users. You should also make sure that other people have access to the profile,
so that changes can be made when you're unavailable. The recommended way to do this is to create an admins
group and grant that group OWNER access to the spend profile. e.g.:
```
terra spend create-profile
terra group create --name=developer-admins
terra group add-user --name=developer-admins --email=admin1@gmail.com --policy=ADMIN
terra group add-user --name=developer-admins --email=admin2@gmail.com --policy=ADMIN
terra spend enable --email=developer-admins@gmail.com --policy=OWNER
```

To delete the spend profile:
  `terra spend delete-profile`
This is helpful if you accidentally create the spend profile with the wrong name and need to start over.

### Break-glass
A break-glass implementation means that there is a way for users to request elevated permissions on a workspace.
These elevated permissions invalidate the contract with WSM. Any guarantees about policy or access enforcement
are off.

The purpose of break-glass access is to grant select trusted users the ability to try out cloud features that
are not yet available via WSM. The goal of this experimentation is to understand what use cases WSM could
support in the future.

Break-glass is intended for non-production environments only. This contributed to the decision to implement
this on the client side, instead of e.g. as a new WSM endpoint.

#### Grant break-glass
To grant break-glass access to someone:
1. Ask the requester to:
    - Make your Terra user an owner of the workspace they want break-glass access to.
      `terra workspace add-user --email=breakglassgranter@gmail.com --role=OWNER`
    - Confirm that they are an owner of the workspace.
      `terra workspace list-users`
    - Relay any brief notes about the reason for the request.
2. Download two SA key files:
    - One that has permission to set IAM policy on all workspace projects. This will be specific
      to the server (i.e. WSM deployment) where the workspaces live.
    - One that has permission to update a central BigQuery dataset that tracks break-glass requests.
    - The `tools/render-config.sh` script downloads two SA key files that will work for workspaces
      on the `verily-cli` server and the central BigQuery dataset in the `terra-cli-dev` project.
3. Run the `terra workspace break-glass` command.

Example commands for granting break-glass access for a workspace in the `verily-cli` deployment:
```
./tools/render-config.sh
terra auth login # login as yourself, the break-glass granter
terra workspace break-glass --email=breakglassrequester@gmail.com --big-query-project=terra-cli-dev --big-query-sa=rendered/ci-account.json --user-project-admin-sa=rendered/verilycli-wsm-sa.json --notes="Testing break-glass command."
```

#### Requests catalog
The `terra workspace break-glass` command updates a central BigQuery dataset to track break-glass requests.
This dataset was setup by a script:
```
gcloud auth activate-service-account dev-ci-sa@broad-dsde-dev.iam.gserviceaccount.com --key-file=./rendered/ci-account.json
./tools/create-break-glass-bq.sh terra-cli-dev
```

In the future, we can run the same script with different credentials and project id to setup another central
BigQuery dataset somewhere else. (e.g. one for Verily deployments, one for Broad deployments). The SA activated
in the first command needs to have BigQuery admin privileges in the target project.

The current central BigQuery dataset exists in the `terra-cli-dev` project.

