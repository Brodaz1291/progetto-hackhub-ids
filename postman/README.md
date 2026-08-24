# Postman collection

`HackHub.postman_collection.json` walks the whole life cycle of an event, from the login of
the users to the payment of the prize, with no data typed by hand. Every endpoint the
application exposes is exercised at least once, and so are the requests that are meant to be
refused, each one showing the status a domain error answers with.

## Running it

1. Start the application, from the root of the repository:

   ```
   ./gradlew :app:bootRun
   ```

   It listens on `http://localhost:8080` and preloads the users the collection logs in. Their
   names and their password are in the [root README](../README.md#users).

2. In Postman, **Import** > **Files**, and pick `postman/HackHub.postman_collection.json`. The
   collection variables come with it, so no environment has to be created. `baseUrl` is one of
   them: change it there and every request follows.

3. Press **Run collection**.

With the application already up, [newman](https://github.com/postmanlabs/newman) does the same
from the command line:

```
npx newman run postman/HackHub.postman_collection.json
```

## Before you run it

- **Run it from the beginning, in order.** Every request that creates something saves the
  returned id in a collection variable, and what follows refers to it as `{{hackathonId}}`,
  `{{teamId}}`, and so on. Starting halfway leaves those variables empty.
- **Restart the application between two runs.** The database lives in memory and dies with the
  process: a second run on the same instance would register a username and create a team name
  that already exist, and both are refused.
- **The dates of the hackathon are in February 2026** because the clock starts at
  `2026-02-01`. How the platform reads time is explained in the
  [root README](../README.md#how-time-works).
- **Two folders move the clock**, `09 Clock` and `14 Clock`. The hackathon changes phase on the
  first request that reads it afterwards, not on the `PUT` that moves the time — which is why
  the assertion on the new phase sits on the `GET`.

## The folders

| Folder | What it does |
|---|---|
| `00 Setup` | Logs in the preloaded users and stores their ids in `organizerId`, `judgeId`, `mentorId1`, `mentorId2`, `memberId1`, `memberId2` |
| `01 Auth` | Registers a new user, then logs it in and out |
| `02 Hackathon` | Creates a hackathon with the staff logged in at step `00`, refuses two creations that must not go through — one with a staff member already busy, one where a user covers two roles — then updates the hackathon created |
| `03 Team` | Creates a team with `member1` as its first member |
| `04 Staff` | Adds `member3` as a mentor and removes them, then makes them judge in place of `judge1` |
| `05 Invitations` | `member1` invites `member2`, who reads the invitations received and accepts; a second invitation goes to `newcomer`, who refuses it |
| `06 Consultation` | Reads the hackathons back: the whole list, the ones open to registrations, the public projection, the detail of one |
| `07 Leave team` | `member2` leaves the team, which survives with `member1`, its creator, as the only member left |
| `08 Registration` | Registers Byte Runners, then creates Null Pointers with `member2` and registers it too |
| `09 Clock` | Reads the time the platform is living in, moves it past the registration deadline — where the hackathon is already `RUNNING` but the event has not begun, so a submission is refused and the list of the open ones no longer offers it — and then to the day the hackathon starts |
| `10 Submissions` | Uploads the submission of each team, then reads them back from the list reserved to the staff and one by one |
| `11 Support requests` | Sends a support request, plans the call that answers it, reads it back three ways, and closes with a call on a slot already booked |
| `12 Reports` | A mentor reports a team to the organizer, who reads the reports back: the list and the detail of one |
| `13 Disqualification` | Reads the teams enrolled in the hackathon, disqualifies the registration saved by `08` between two refusals — one with no reason, one repeated — and reads the list back with the sanction on it |
| `14 Clock` | Moves the time past the end of the hackathon, which takes it from `RUNNING` to `EVALUATION` |
| `15 Evaluation` | Reads the submissions left to evaluate, scores the one still in the running, then scores it again |
| `16 Proclamation` | Proclaims `Null Pointers` the winner, which concludes the hackathon |
| `17 Prize payment` | Pays the prize of the concluded hackathon to the winning team |
| `18 Error handling` | The requests that are meant to be refused: `400`, `401`, `403`, `404` and `409` |

## Why it is built this way

- **The team is created by `member1`, not by one of the staff users.** Creating a hackathon
  makes `organizer1` its organizer, `judge1` its judge and `mentor1`/`mentor2` its mentors, and
  taking part in an event, in any role, keeps a user busy until it is over.
- **The refused invitation is addressed to `newcomer`**, the one user that takes part in
  nothing for the whole run, so the refusal depends on nothing the earlier folders did.
- **The submissions are read by `organizer1`, not by the judge.** Reading them is granted to
  the staff of that hackathon whatever the role, and the organizer is the only staff member
  that never changes hands, while `04 Staff` replaces the judge.
- **The reports come before the disqualification.** They are what the organizer decides upon,
  so the sanction reads as their consequence, and a report is accepted only while the hackathon
  is `RUNNING`.
- **The disqualification runs last, in a folder of its own.** It is terminal — a disqualified
  team can no longer submit nor be evaluated — so in the middle of the collection it would
  block everything after it. It marks the registration instead of removing it, which is why the
  `registrationId` saved by `08 Registration` stays valid.
- **The prize payment is the last act.** It needs a hackathon already concluded with a winner
  proclaimed, and it transfers to the iban `Null Pointers` was created with.
- **The simulated calendar remembers the slots it has given out**, so a folder added later
  needs an instant of its own, or a mentor and a team that share nobody with the call of
  `11 Support requests`.
- **The simulated payment system remembers nothing**, as a bank does not refuse a transfer
  because it has already made a similar one: whether a prize is already paid is a question the
  platform answers on its own records.

## The requests that are meant to be refused

`18 Error handling` sits at the very end, after the happy path is over. Every request of it is
refused, so none of them changes anything and the folder can be run again as many times as
wanted. The body of an error carries `timestamp`, `status`, `error`, `message` and `path`, and
the folder asserts that shape once, because it is the same for every error.

| Status | What it answers | Where the collection shows it |
|---|---|---|
| `400 Bad Request` | data that does not pass a check of the domain | a disqualification, a submission and a support request on a hackathon that is over, and an invitation sent into a team the sender does not belong to |
| `401 Unauthorized` | credentials that do not match | a login with the wrong password |
| `403 Forbidden` | a caller who is known but does not cover the role the operation requires | the submissions of a hackathon read by a user who is not staff of it |
| `404 Not Found` | a resource that does not exist | a hackathon and a submission asked for by an id nobody ever created |
| `409 Conflict` | a request that collides with what has already happened | a prize paid twice and a username already in use |

Five cases cannot live down there, because each of them needs a phase of the event that the
tail of the collection no longer has. They sit inside the folder that still has it:

- **`502 Bad Gateway`**, closing `11 Support requests`: a call asking for a slot the calendar
  has already given out. It is the one request whose refusal comes from an external system
  rather than from the platform.
- **`400` on a disqualification with no reason**, opening `13 Disqualification`: afterwards the
  registration would already be out and the answer would be `409` instead.
- **`409` on the same disqualification repeated**, closing the same folder, for the opposite
  reason.
- **`409` on a hackathon whose staff is already busy**, in `02 Hackathon`: the check looks at
  the participations that are still open, and at the end of the collection the event is
  concluded, so the same request would go through.
- **`400` on a submission uploaded before the hackathon starts**, in `09 Clock`: it needs the
  window between the registration deadline and the start date, which lasts three days of the
  simulated calendar and is gone as soon as the clock moves on. It is the request that shows why
  the phase alone does not decide: there the hackathon is already `RUNNING`.

**`400` on a hackathon where one user covers two roles** keeps that one company, though it
would be refused anywhere: the check compares the identifiers of the request, before any user
is even read. It sits next to the creation it refuses, so the two ways of getting a staff wrong
are read together.

`500 Internal Server Error` is not covered: it answers an inconsistent state of the stored
data, such as a hackathon whose organizer is missing, which no use case is able to produce.
