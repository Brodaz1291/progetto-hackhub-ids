# Postman collection

`HackHub.postman_collection.json` covers the happy paths of the REST API exposed by the
project. It is meant to be run from start to finish without typing any data by hand.

## Importing the collection

1. Open Postman.
2. **Import** > **Files**, and select `postman/HackHub.postman_collection.json`.
3. The collection appears as **HackHub**, with its variables already defined: no environment
   needs to be created.

`baseUrl` is a collection variable set to `http://localhost:8080`. If the application runs on
another port, change it there and every request follows.

## Starting the application

From the root of the repository:

```
./gradlew :app:bootRun
```

The application listens on `http://localhost:8080` and starts with the `dev` profile, which
preloads the users used by the collection (`organizer1`, `judge1`, `mentor1`, `mentor2`,
`member1`, `member2`, `member3`), all with password `password`.

## Order of execution

The folders must be run in order, because each one uses the ids produced by the previous
ones. In Postman, running the whole collection with the **Run collection** button already
respects this order.

| Folder | What it does |
|---|---|
| `00 Setup` | Logs in the preloaded users and stores their ids in `organizerId`, `judgeId`, `mentorId1`, `mentorId2`, `memberId1`, `memberId2` |
| `01 Auth` | Registers a new user, then logs it in and out |
| `02 Hackathon` | Creates a hackathon with the staff logged in at step `00`, then updates its information |
| `03 Team` | Creates a team with `member1` as its first member |
| `04 Staff` | Adds `member3` as a mentor of the hackathon and removes them, then makes them its judge in place of `judge1` |
| `05 Invitations` | `member1` invites `member2` into the team, who reads the invitations received and accepts |
| `06 Consultation` | Reads the hackathons back: the whole list with its staff, the ones open to registrations, the public projection a visitor sees, and the detail of one of them |
| `07 Leave team` | `member2` leaves the team, which survives with `member1`, its creator, as the only member left |
| `08 Registration` | Registers the team in the hackathon, saving the returned id in `registrationId` |
| `09 Submissions` | Reads back the submissions of the hackathon, a list that is still empty (see below) |
| `10 Disqualification` | Disqualifies the registration saved by `08`, closing the participation of the team |

No id is written by hand: every request that creates something saves the returned `id` in a
collection variable through a script in its **Tests** tab, and the following requests refer to
it as `{{hackathonId}}`, `{{teamId}}`, and so on.

## Why the data is what it is

**The team is created by `member1`, not by one of the staff users.** A user takes part in one
thing at a time: creating a hackathon makes `organizer1` its organizer, `judge1` its judge and
`mentor1`/`mentor2` its mentors, and from that moment on none of them can join a team. Only
`member1`, `member2` and `member3` are still free when `03 Team` runs, so the team is created
by `member1`.

**The dates of the hackathon are in September 2026.** What the application checks is that they
are coherent with one another — `registrationDeadline <= startDate < endDate` — otherwise the
creation is rejected. It does not compare them with the current date, so they are in the future
only to make the scenario realistic, not because an earlier date would be refused.

**The disqualification runs last, in a folder of its own.** It is a terminal operation: once
the team is disqualified it can no longer submit anything nor be evaluated, so keeping it in
the middle of the collection would block everything that comes after it. `10 Disqualification`
still works on the `registrationId` saved by `08 Registration`, which stays valid because the
disqualification marks the registration instead of removing it.

## What the collection does not cover yet

**The upload of a submission is missing.** A submission is accepted only while the hackathon is
`RUNNING`, but the hackathon of the collection never leaves `REGISTRATION`: the state of an
event follows its dates, and no endpoint moves it forward because the temporal mechanism is not
implemented yet. A `POST /api/submissions` would therefore be refused every time, so the folder
`09 Submissions` only reads the list — which for the same reason comes back empty. The request
will be added once an event can reach `RUNNING`.

This is a limit of the platform, not of the collection: today no hackathon can change state at
all. The same limit will apply to the support requests, which are sent during the event as
well.

## The database is emptied at every restart

The database is H2 in memory with `ddl-auto=create-drop`: it is created when the application
starts and dropped when it stops. Nothing survives a restart, and the generated ids start over
from the beginning.

This has two consequences:

- after every restart the whole sequence must be run again from `00 Setup`, otherwise the
  collection variables still hold ids of a database that no longer exists;
- the sequence cannot be run twice on the same running application: the second run would try
  to register a username that already exists and to create a team with a name already taken,
  and both are rejected. `member1` would be refused as well, because it is already tied to the
  team created by the first run, and so would `member3`, left as the judge of the hackathon by
  `04 Staff`. `member2` is the exception, because `07 Leave team` takes them out of the team at
  the very end, leaving them free to be invited again — but that changes nothing, since the run
  is already broken well before the invitations. `08 Registration` adds one more reason: the
  team ends the run registered in a hackathon that is not concluded, and a team takes part in
  one hackathon at a time, so a second registration would be refused even if everything before
  it went through. To run the collection again, restart the application first.

## Running it from the command line

The same collection can be run with [newman](https://github.com/postmanlabs/newman), with the
application already up:

```
npx newman run postman/HackHub.postman_collection.json
```
