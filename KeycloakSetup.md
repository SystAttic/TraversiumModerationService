# Keycloak Setup Guide for Traversium

This document describes how to configure **Keycloak** to secure gRPC-based service-to-service communication in **Traversium** using **OAuth2 client credentials**.

The goal is:


- Each backend service (TripService, SocialService, etc.) authenticates using **its own service account**.

- ModerationService **validates JWTs** issued by Keycloak.

- **No user login** flows are involved.

#### 1. Create Realm

- Open Keycloak Admin Console

- Create a new realm
  ```
  Name: traversium
  ```
#### 2. Create Clients (Service Accounts)

Each backend service gets its own client.

##### 2.1 TripService Client

- Go to Clients → Create client

- Set:

  ```
  Client ID: trip-service

  Client type: OpenID Connect
  ```
- Click Next

- Enable:

    ✅ Client authentication

    ✅ Service accounts roles

- Click Save

###### Service Account

- Open Service account roles tab

- Ensure the service account user exists

##### 2.2 SocialService Client

- Repeat the same steps with:
  ```
  Client ID: social-service
  ```
#### 3. Create Roles

Roles are used by ModerationService to authorize calls.

##### 3.1 Realm Roles

- Go to Realm roles → Create role

- Create:
  ```
  moderation:access
  ```

#### 4. Assign Roles to Services
##### TripService

- Go to Clients → trip-service → Service account roles

- Assign:
  ```
  moderation:access
  ```

##### SocialService

- Go to Clients → social-service → Service account roles

- Assign:
  ```
  moderation:access
  ```

#### 5. Token Configuration

Ensure roles appear in access tokens.

- Go to Clients → trip-service → Client scopes

- Ensure roles scope is assigned

If missing:

- Go to Client scopes → roles

- Enable:

    ✅ Include in OpenID Provided Metadata

Ensure an audience mapper is added (*do the same for `social-service`*).

- Go to Clients → `trip-service` → Client scopes 
- Click `trip-service-dedicated`
- Go to Mappers
- Create a new mapper:
  - Mapper type: Audience
  - Name: `moderation-audience`
  - Included Client Audience: select `moderation-service`
  - Enable:
   ✅ Add to access token
  
#### 6. Obtain Token (Manual Test)

Send a token request:

```
curl -X POST http://localhost:8080/realms/traversium/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=client_credentials" \
  -d "client_id=trip-service" \
  -d "client_secret=YOUR_SECRET"
```
Response contains:

```
{
  "access_token": "eyJhbGciOiJSUzI1NiIs...",
  "expires_in": 300,
  "token_type": "Bearer"
}
```

The following `access_token` is what is used within Moderation Service to enable specific service accounts to access its services. 
This is the extra protection layer besides the Firebase authentication of the actual user accessing the caller services (e.g. Trip or Social Service).

#### 7. ModerationService Validation Expectations

ModerationService expects:

- Header: Authorization: Bearer <JWT>

- JWT signed by Keycloak

- Role moderation:access present

If missing or invalid → **gRPC UNAUTHENTICATED (16)**

#### 8. Recommended Production Hardening

- Enable HTTPS in Keycloak

- Configure short token lifetimes (5–10 minutes)

- Rotate client secrets

- Restrict allowed audiences

- Use mTLS for internal gRPC if possible

#### 9. Summary

- OAuth2 client_credentials only

- No user flows

- One client per service

- Role-based authorization

- Tokens cached per service

This setup enables secure, scalable, service-to-service authentication for Traversium.