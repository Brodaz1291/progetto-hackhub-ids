# HackHub

A REST platform for managing hackathons: an organizer creates an event, teams enrol, upload
their work and get it judged, and the platform follows the event from registration to the
proclamation of the winner.

Software Engineering project, Computer Science, University of Camerino, 2025/26.


## Requirements

Java 21. Nothing else: the Gradle wrapper is versioned in the repository, so no local Gradle
installation is needed.


## Running the application

From the root of the repository:

```
./gradlew :app:bootRun
```

The application listens on `http://localhost:8080`.

The database is H2, in memory, created at startup and dropped when the application stops.
Nothing survives a restart, and the generated ids start over from 1 every time.


## Users

The `dev` profile, active by default, preloads seven users. They all have the same password,
`password`:

| Username | Used in the tests as |
|---|---|
| `organizer1` | the organizer of the hackathon |
| `judge1` | the judge |
| `mentor1`, `mentor2` | the mentors |
| `member1`, `member2`, `member3` | the members of the teams |

The role is not an attribute of the person. The usernames above only say how each account is used in the Postman collection.

What a user cannot do is cover two roles at once: taking part in a hackathon, in any role, keeps
them busy until that event is over.


## Trying it out

`postman/HackHub.postman_collection.json` is a Postman collection that walks the whole life
cycle of an event, from the login of the users to the payment of the prize, without any data
typed by hand. See `postman/README.md` for how to import and run it.


## How time works

A hackathon changes state on its own, following its dates: it is in `REGISTRATION` until the
registration deadline, `RUNNING` between the deadline and the end date, and `EVALUATION`
afterwards. Only the last transition, to `CONCLUDED`, is commanded by somebody, when the
organizer proclaims the winner.

The registration phase ends on the deadline and not on the start date, because that is the
moment the teams taking part are settled. Between the deadline and the start date the hackathon
is therefore already `RUNNING` while the event has not begun: the operations of the event — a
submission, a support request, a call, a report — check the start date as well as the phase, and
are refused until it arrives. The platform has four states and none of them is named after that
window, which is a phase of preparation in which nothing can be done anyway.

The platform does not read the clock of the machine. It reads a `Clock` bean configured in
`app/src/main/resources/application.properties`:

```
app.time.mode=fixed
app.time.fixed=2026-02-01T18:00:01
```

With `mode=fixed` the application starts frozen on that instant and the clock can be moved:

```
PUT /api/dev/clock?instant=2026-02-16T10:00:00
```

With `mode=system` the platform follows the real time instead, and the clock cannot be moved.

This matters because a hackathon created with dates a couple of weeks away stays open to
registrations until its deadline arrives. Trying to upload a submission before moving the clock
past the start date gets refused, and the refusal is hard to read if one does not know that the
phase and the start date are what is being checked.

The endpoints under `/api/dev/clock` are a development tool, not a use case: they exist only in
the `dev` profile.


## API

The identity of the caller travels as an explicit parameter (`organizerId`, `userId`,
`creatorId`, ...) rather than being read from a session.

**Response codes.** A `POST` that creates a resource answers `201 Created`; everything else that
succeeds answers `200 OK`. The two exceptions are the upload of a submission and the evaluation
of one, which answer `200` because the same request creates the entry the first time and
replaces it afterwards. A failure carries `timestamp`, `status`, `error`, `message` and `path`,
and the status says what kind of failure it is: `400` for data that does not pass a domain
check, `401` for credentials that do not match, `403` for a caller who is known but does not
cover the role the operation requires, `404` for a resource that does not exist, `409` for a
request that collides with what has already happened, and `502` when an external system refuses.

### Authentication — `/api/auth`

| Method | Path | Description |
|---|---|---|
| POST | `/register` | Registers a new user |
| POST | `/login` | Authenticates a user and returns it |
| POST | `/logout` | Closes the session of a user |

### Hackathons — `/api/hackathons`

| Method | Path | Description |
|---|---|---|
| GET | `/` | Lists all the hackathons with their staff |
| GET | `/open` | Lists the hackathons still open to registrations |
| GET | `/public` | Lists the hackathons as a visitor who is not authenticated sees them |
| GET | `/{hackathonId}` | Returns one hackathon |
| POST | `/` | Creates a hackathon on behalf of an organizer |
| PUT | `/{hackathonId}` | Replaces the information of a hackathon still open to registrations |
| POST | `/{hackathonId}/mentors` | Assigns a further mentor to the hackathon |
| DELETE | `/{hackathonId}/mentors` | Removes a mentor from the hackathon |
| PUT | `/{hackathonId}/judge` | Replaces the judge with another user |
| PUT | `/{hackathonId}/winner` | Proclaims the winning team, which concludes the hackathon; the registration is optional, because a hackathon where no submission was evaluated is concluded without a winner |

### Teams and invitations — `/api/teams`

| Method | Path | Description |
|---|---|---|
| POST | `/` | Creates a team, with its creator as the first member |
| DELETE | `/members` | Takes a user out of the team they belong to |
| POST | `/{teamId}/invitations` | Invites a user into the team |
| GET | `/invitations` | Lists the invitations a user has received |
| PUT | `/invitations/{invitationId}` | Accepts an invitation |
| PUT | `/invitations/{invitationId}/refusal` | Refuses an invitation, which creates no membership |

### Registrations — `/api/registrations`

| Method | Path | Description |
|---|---|---|
| POST | `/` | Registers a team in a hackathon |
| PUT | `/{registrationId}/disqualification` | Excludes a team from the event, with a reason |
| POST | `/prize` | Pays the prize of a concluded hackathon to the winning team |

### Submissions and evaluations — `/api/submissions`

| Method | Path | Description |
|---|---|---|
| POST | `/` | Uploads the submission of a team, replacing the previous one |
| GET | `/` | Lists the submissions of a hackathon, for its staff only |
| GET | `/{submissionId}` | Returns one submission |
| POST | `/{submissionId}/evaluation` | Records the score and the judgment of a judge |

### Support requests — `/api/support-requests`

| Method | Path | Description |
|---|---|---|
| POST | `/` | Sends a support request to the mentors of the hackathon |
| GET | `/pending` | Lists the requests of a hackathon no mentor has taken over yet |
| GET | `/` | Lists the requests sent by a team |
| GET | `/{supportRequestId}` | Returns one request |
| POST | `/{supportRequestId}/call` | Plans the call a mentor holds in answer to the request |

### Reports — `/api/reports`

| Method | Path | Description |
|---|---|---|
| POST | `/` | Reports a team that violated the rules |
| GET | `/` | Lists the reports collected by a hackathon |
| GET | `/{reportId}` | Returns one report |

### Development clock — `/api/dev/clock`

Available only in the `dev` profile.

| Method | Path | Description |
|---|---|---|
| GET | `/` | Reads the instant the platform is living in |
| PUT | `/` | Moves the clock to the given instant |


## Architecture

The application is layered: a controller routes the request and converts the answer, a service
holds the logic, a repository talks to the database.

- Entities are anemic: fields, constructors, getters and setters, and no business logic. All the
  behaviour lives in the services.
- Controllers return DTOs, never entities, so that the password of a user never leaves the
  application and the bidirectional associations do not turn into serialization cycles. The
  conversion goes through a single class, `DTOMapper`.
- Validations are private methods of the service that needs them, named `checkXxx`.
- States are plain enums, checked by the services.

Three design patterns are implemented, all under `designPatterns`:

| Pattern | Class | What it does |
|---|---|---|
| Builder | `HackathonBuilder` | Assembles the eight attributes of a hackathon |
| Facade | `TeamFacade` | Coordinates the services involved in the invitations |
| Adapter | `CalendarAdapter`, `PaymentAdapter` | Translate between the domain and the external calendar and payment system |

The external systems behind the adapters are simulated in memory: the calendar refuses a slot it
has already given out, and the payment system refuses an empty account or a zero amount, so that
a failure of an external system stays a case the platform has to cope with.

The package layout follows the layers:

```
it.unicam.cs.hackhub
├── model            entities, enums, repositories
├── application      services, DTOs, mappers, exceptions
├── controllers      REST endpoints and request objects
├── designPatterns   builder, facade, adapters
├── external         the simulated third party systems
└── configs          Spring configuration, clock, development seeder
```

The UML model of the project is in `docs/`.


## A note on security

Authentication is not implemented: every endpoint is open, passwords are stored in clear text
and the H2 console answers without credentials. This is a university project about the domain
of hackathons, not about authentication, and it is not ready to be used as it is.

The identity of the caller is therefore an assumption about the context, not a fact the
platform verifies: no operation checks that whoever calls it really is the organizer, the judge
or the mentor it claims to be, and the logout accepts any identifier and answers `200`, because
there is no session to close. The identifiers that do get verified are the ones the work needs —
a user assigned to a hackathon, the recipient of an invitation — and they are read to be used,
not to prove who is asking. The day an operation is meant to act on somebody else, that
identifier becomes the selector of a resource and has to be validated as such: it would be a
different use case, with an actor the model does not have.


## License

See `LICENSE`.
