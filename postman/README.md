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
| `09 Clock` | Reads the time the platform is living in and moves it to the day the hackathon starts, which takes the event from `REGISTRATION` to `RUNNING` (see below) |
| `10 Submissions` | Uploads the submission of the team, then reads it back from the list the judge sees and one by one through its id |
| `11 Support requests` | Sends a support request and plans the call a mentor holds in answer to it, then reads it back from the list the mentors work on, from the list of the team, and one by one through its id |
| `12 Reports` | A mentor reports the team to the organizer, who reads the reports of the hackathon back: the whole list and the detail of one of them |
| `13 Disqualification` | Disqualifies the registration saved by `08`, closing the participation of the team |
| `14 Clock` | Moves the time past the end of the hackathon, which takes the event from `RUNNING` to `EVALUATION` |
| `15 Evaluation` | Reads the submissions left to evaluate, scores the one of the team still in the running and scores it again, replacing the first judgment |
| `16 Proclamation` | Proclaims `Null Pointers` the winner, which concludes the hackathon, then reads it back to see the terminal phase hold |

No id is written by hand: every request that creates something saves the returned `id` in a
collection variable through a script in its **Tests** tab, and the following requests refer to
it as `{{hackathonId}}`, `{{teamId}}`, and so on.

## Why the data is what it is

**The team is created by `member1`, not by one of the staff users.** A user takes part in one
thing at a time: creating a hackathon makes `organizer1` its organizer, `judge1` its judge and
`mentor1`/`mentor2` its mentors, and from that moment on none of them can join a team. Only
`member1`, `member2` and `member3` are still free when `03 Team` runs, so the team is created
by `member1`.

**The dates of the hackathon are in February 2026.** What the application checks at creation is
that they are coherent with one another — `registrationDeadline <= startDate < endDate` —
otherwise the request is rejected. They are close to the instant the clock starts from because
the phases of an event follow its dates: keeping them a couple of weeks away makes the jumps of
`09 Clock` short and the sequence easy to read. Note that `02 Hackathon` updates them, so the
ones that decide the phases are those of the update: start on `2026-02-16T09:00`, end on
`2026-02-18T18:00`.

**The disqualification runs last, in a folder of its own.** It is a terminal operation: once
the team is disqualified it can no longer submit anything nor be evaluated, so keeping it in
the middle of the collection would block everything that comes after it. `13 Disqualification`
still works on the `registrationId` saved by `08 Registration`, which stays valid because the
disqualification marks the registration instead of removing it. Only the folders that close the
event run after — `14 Clock`, `15 Evaluation` and `16 Proclamation` — and they work on the
hackathon and on the team left in the running, not on the one that is out.

**The calendar keeps its agenda in memory, so no instant may be booked twice.** The slot of the
call in `11 Support requests` is reserved on an external calendar the platform reaches through
an adapter, and in this project that calendar is simulated: it remembers the slots it has
already given out and refuses a second event on the same people at the same instant, as a real
one would. The call therefore books `2026-02-16T15:00` and no other request may reuse it — a
folder added later needs an instant of its own, or the booking comes back refused. The agenda
lives as long as the application does, so it is emptied by the same restart the database needs.

**The reports come before the disqualification.** `12 Reports` is what the organizer decides
upon, so the sanction reads naturally as its consequence. It also has to run while the team is
still participating: a report is accepted only while the hackathon is `RUNNING`, and the
disqualification would leave the team out of the event.

## How time works in the collection

The platform does not read the clock of the machine. It reads a `Clock` bean configured in
`application.properties`:

```
app.time.mode=fixed
app.time.fixed=2026-02-01T18:00:01
```

With `mode=fixed` the application starts frozen on that instant, and the collection can move it
where it needs to. With `mode=system` it would follow the real time instead, and the requests of
`09 Clock` would be refused with a message saying so.

**Why the collection needs to move the time.** The phases of a hackathon are not commanded by
anyone: an event passes from `REGISTRATION` to `RUNNING` when its start date arrives, and from
`RUNNING` to `EVALUATION` when its end date does. A submission is accepted only while the event
is `RUNNING`, and so are a support request, the call that answers it and a report. Without
moving the clock the hackathon of the collection would stay open to registrations forever, and
`10 Submissions`, `11 Support requests` and `12 Reports` would be refused every time.

`09 Clock` therefore takes the time to `2026-02-16T10:00`, an hour after the start date set by
the update in `02 Hackathon`. From there on the hackathon is `RUNNING`, and the three folders
that follow work. `14 Clock` closes the sequence at the other end, moving the time to
`2026-02-18T19:00`, past the end date: the hackathon reaches `EVALUATION` and the elaborates are
frozen. The two folders together cover both the transitions that happen on their own — the
third one, towards `CONCLUDED`, is the only one somebody commands, and it is what
`16 Proclamation` does.

**The transitions happen while reading.** There is no scheduler: every service that loads a
hackathon brings its phase up to date first. So the passage to `RUNNING` is not the effect of
the `PUT` on the clock, but of the first request that reads the hackathon afterwards — which is
why in `14 Clock` the assertion on the state sits on the `GET` and not on the `PUT` that moves
the time. This is also why the submission uploaded by `10 Submissions` is dated `2026-02-16`:
the platform stamps it with the time of its clock, not with the real one.

**The endpoints of the clock are a development tool, not a use case.** They live under
`/api/dev/clock` and exist only in the `dev` profile: in any other profile the controller is not
created at all, so the paths do not answer. They are there to let a demonstration walk through
the whole life cycle of an event in a single run, without waiting for the real dates.

## What the collection does not cover yet

**The payment of the prize to the winning team**, which belongs to a feature not implemented
yet. The collection now leaves the hackathon `CONCLUDED` with its winner recorded, which is the
state the payment starts from, so it will graft onto the end of it.

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
  is already broken well before the invitations. The registrations are the one thing that does
  not stand in the way: `16 Proclamation` concludes the hackathon, and a team is tied to the
  event it takes part in only until that event is over, so both teams end the run free to enrol
  again — which changes nothing, since the run is refused well before reaching them. To run the
  collection again, restart the application first.

## Running it from the command line

The same collection can be run with [newman](https://github.com/postmanlabs/newman), with the
application already up:

```
npx newman run postman/HackHub.postman_collection.json
```
