# Postman collection

`HackHub.postman_collection.json` covers the REST API exposed by the project: the happy path
from end to end, and the requests that are meant to be refused, with the status each error
answers with. It is meant to be run from start to finish without typing any data by hand.

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
| `04 Staff` | Adds `member3` as a mentor of the hackathon and removes them, then makes them its judge in place of `judge1`, moving `judgeId` onto the new judge |
| `05 Invitations` | `member1` invites `member2` into the team, who reads the invitations received and accepts; a second invitation goes to `newcomer`, who refuses it |
| `06 Consultation` | Reads the hackathons back: the whole list with its staff, the ones open to registrations, the public projection a visitor sees, and the detail of one of them |
| `07 Leave team` | `member2` leaves the team, which survives with `member1`, its creator, as the only member left |
| `08 Registration` | Registers Byte Runners in the hackathon, then creates Null Pointers with `member2` and registers it too, saving the ids in `registrationId`, `teamId2` and `registrationId2` |
| `09 Clock` | Reads the time the platform is living in and moves it to the day the hackathon starts, which takes the event from `REGISTRATION` to `RUNNING` (see below) |
| `10 Submissions` | Uploads the submission of each of the two teams, then reads them back from the list reserved to the staff of the hackathon and one by one through the id |
| `11 Support requests` | Sends a support request and plans the call a mentor holds in answer to it, then reads it back from the list the mentors work on, from the list of the team, and one by one through its id; it closes with a second request whose call asks for the slot already booked, which the calendar refuses |
| `12 Reports` | A mentor reports the team to the organizer, who reads the reports of the hackathon back: the whole list and the detail of one of them |
| `13 Disqualification` | Refuses a disqualification with no reason, then disqualifies the registration saved by `08`, closing the participation of the team, then refuses the same disqualification a second time |
| `14 Clock` | Moves the time past the end of the hackathon, which takes the event from `RUNNING` to `EVALUATION` |
| `15 Evaluation` | Reads the submissions left to evaluate, scores the one of the team still in the running and scores it again, replacing the first judgment |
| `16 Proclamation` | Proclaims `Null Pointers` the winner, which concludes the hackathon, then reads it back to see the terminal phase hold |
| `17 Prize payment` | Pays the prize of the concluded hackathon to `Null Pointers`, transferring it to the iban the team was created with |
| `18 Error handling` | The requests that are meant to be refused, each one showing the status a domain error answers with: `400`, `401`, `404` and `409` |

No id is written by hand: every request that creates something saves the returned `id` in a
collection variable through a script in its **Tests** tab, and the following requests refer to
it as `{{hackathonId}}`, `{{teamId}}`, and so on.

## Why the data is what it is

**The team is created by `member1`, not by one of the staff users.** A user takes part in one
thing at a time: creating a hackathon makes `organizer1` its organizer, `judge1` its judge and
`mentor1`/`mentor2` its mentors, and from that moment on none of them can join a team. Of the
seeded users only `member1`, `member2` and `member3` are still free when `03 Team` runs, so the
team is created by `member1`.

**The invitation that gets refused is addressed to `newcomer`.** Sending an invitation requires
the invited user to be free, and by `05 Invitations` almost nobody is: `organizer1`, `mentor1` and
`mentor2` are staff of the hackathon, `member3` has become its judge in `04 Staff`, and `member1`
is in the team it would be invited into. `member2` is free there, but it is the one that accepts,
and it cannot serve twice: a user already in a team can no longer be invited. `newcomer`, the user
registered in `01 Auth`, is the one that takes part in nothing for the whole run, so the second
invitation goes to it and stays pending until the refusal that closes the folder. Being addressed
to a different user is also what keeps the two invitations apart: `Get invitations` reads the list
of `member2`, which holds its own invitation and nothing else.

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
disqualification marks the registration instead of removing it. The folders that close the
event run after — `14 Clock`, `15 Evaluation`, `16 Proclamation` and `17 Prize payment` — and
they work on the hackathon and on the team left in the running, not on the one that is out.
`18 Error handling` comes last of all.

**The calendar keeps its agenda in memory, so no instant may be booked twice.** The slot of the
call in `11 Support requests` is reserved on an external calendar the platform reaches through
an adapter, and in this project that calendar is simulated: it remembers the slots it has
already given out and refuses a second event on the same people at the same instant, as a real
one would. The call therefore books `2026-02-16T15:00`, and the request that closes
`11 Support requests` asks for that very instant a second time on purpose: same slot, same
mentor, same team, so the calendar refuses it and the platform answers `502`. What the agenda
holds is an entry per attendee rather than per instant, so a folder added later needs an instant
of its own, or a mentor and a team that share nobody with this call, or the booking comes back
refused. The agenda lives as long as the application does, so it is emptied
by the same restart the database needs.

**The prize is paid to the iban the winning team was created with.** The transfer of `17 Prize
payment` goes through an adapter as well, and the payment system behind it is simulated like the
calendar — but it keeps no memory of what it has transferred: a bank does not refuse a transfer
because it has already made a similar one, and whether a prize has already been paid is a
question the platform answers on the payment it recorded. What the stub does refuse is an
account it cannot credit and an amount of zero, so the iban `Null Pointers` carries from
`08 Registration` is what lets the request through. The amount is typed nowhere: it is the prize
of the hackathon, `6000` after the update of `02 Hackathon` and not the `5000` it was created
with.

**The reports come before the disqualification.** `12 Reports` is what the organizer decides
upon, so the sanction reads naturally as its consequence. It also has to run before `14 Clock`:
a report is accepted only while the hackathon is `RUNNING`, and that phase is the one condition
the platform puts on it.

**The submissions are read by the organizer, not by the judge.** Reading them is the one
operation of the platform that depends on who is asking: it is granted to the staff of that
hackathon, whatever role they cover, so the request has to carry the id of one of them. The
organizer is the staff member that stays such for the whole collection, while the judge changes
hands in `04 Staff` and the mentors are two. Passing `{{organizerId}}` therefore keeps the two
readings in `10 Submissions` and `15 Evaluation` independent of what the staff folder did.

## How time works in the collection

The platform does not read the clock of the machine. It reads a `Clock` bean configured in
`application.properties`:

```
app.time.mode=fixed
app.time.fixed=2026-02-01T18:00:01
```

With `mode=fixed` the application starts frozen on that instant, and the collection can move it
where it needs to. With `mode=system` it would follow the real time instead, and the `PUT` of
`09 Clock` would be refused with a message saying so, while its `GET` would answer with the real
time.

**Why the collection needs to move the time.** The phases of a hackathon are not commanded by
anyone: an event passes from `REGISTRATION` to `RUNNING` when its start date arrives, and from
`RUNNING` to `EVALUATION` when its end date does. A submission is accepted only while the event
is `RUNNING`, and so are a support request, the call that answers it and a report. Without
moving the clock the hackathon of the collection would stay open to registrations forever, and
every request that uploads, asks for support or reports would be refused. The readings of those
same folders answer whatever the phase is: there it would be their assertions to fail, not the
requests themselves.

`09 Clock` therefore takes the time to `2026-02-16T10:00`, an hour after the start date set by
the update in `02 Hackathon`. From there on the hackathon is `RUNNING`, and the three folders
that follow work. `14 Clock` closes the sequence at the other end, moving the time to
`2026-02-18T19:00`, past the end date: the hackathon reaches `EVALUATION` and the elaborates are
frozen. The two folders together cover both the transitions that happen on their own — the
third one, towards `CONCLUDED`, is the only one somebody commands, and it is what
`16 Proclamation` does.

**The transitions happen while reading.** There is no scheduler: a service that gates on a phase
the clock produces brings it up to date before reading it. The ones that only ask whether the
event is concluded do not, because no passing of time ever reaches that phase: the proclamation
is what writes it. So the passage to `RUNNING` is not the effect of
the `PUT` on the clock, but of the first request that reads the hackathon afterwards — which is
why in `14 Clock` the assertion on the state sits on the `GET` and not on the `PUT` that moves
the time. This is also why the submission uploaded by `10 Submissions` is dated `2026-02-16`,
and the payment of `17 Prize payment` `2026-02-18T19:00`: the platform stamps them with the
time of its clock, not with the real one.

**The endpoints of the clock are a development tool, not a use case.** They live under
`/api/dev/clock` and exist only in the `dev` profile: in any other profile the controller is not
created at all, so the paths do not answer. They are there to let a demonstration walk through
the whole life cycle of an event in a single run, without waiting for the real dates.

## The requests that are meant to be refused

Every domain error of the platform answers with a status of its own and a body that carries the
message of the exception. The collection shows them where each one can be shown.

`18 Error handling` sits at the very end, after the happy path is over, and covers four of the
five statuses:

| Status | What it answers | Where the collection shows it |
|---|---|---|
| `400 Bad Request` | data that does not pass a check of the domain | a disqualification, a submission and a support request on a hackathon that is over |
| `401 Unauthorized` | credentials that do not match | a login with the wrong password |
| `404 Not Found` | a resource that does not exist | a hackathon and a submission asked for by an id nobody ever created |
| `409 Conflict` | a request that collides with what has already happened | a prize paid twice and a username already in use |

Every request of that folder is refused, so none of them changes anything and the folder can be
run again as many times as wanted. The body of an error carries `timestamp`, `status`, `error`,
`message` and `path`, and the folder asserts that shape once, on its first request, because it is
the same for every error the platform answers with.

Three cases cannot live down there, because each of them needs a phase of the event that the
tail of the collection no longer has. They sit inside the folder that still has it:

- **`502 Bad Gateway`**, at the end of `11 Support requests`: the call that asks for a slot the
  calendar has already given out. It has to run while the hackathon is `RUNNING`, otherwise the
  check on the phase refuses the call before the calendar is ever asked. It is the one request
  that shows the boundary with the external systems: the refusal comes from the calendar, not
  from the platform, and a `500` would have blamed the wrong side.
- **`400` on a disqualification with no reason**, at the head of `13 Disqualification`: it has to
  run before the disqualification that succeeds, because afterwards the registration would
  already be out and the answer would be `409`.
- **`409` on the same disqualification repeated**, at the foot of the same folder, for the
  opposite reason.

The position of the two requests at the end of `11 Support requests` is a constraint and not a
preference: `List pending support requests` asserts that nothing is waiting for a mentor and
`List team support requests` reads the first element of the list, so both have to run while the
second request does not exist yet. Moving the pair earlier to improve the narrative breaks those
two assertions.

**`500 Internal Server Error` is not covered.** It answers an inconsistent state of the stored
data — a hackathon whose organizer is missing, for instance — which no use case of the platform
is able to produce: forcing one would mean writing the inconsistency straight into the database.
Outside the use cases the development clock does produce it: `PUT /api/dev/clock` answers `500`
on a malformed instant, and that body carries no `message`, because `IllegalArgumentException`
is not among the types the handler maps. It is a development tool that sits outside the model,
and the collection does not go down that path.

## The database is emptied at every restart

The database is H2 in memory with `ddl-auto=create-drop`: it is created when the application
starts and dropped when it stops. Nothing survives a restart, and the generated ids start over
from the beginning.

This has two consequences:

- after every restart the whole sequence must be run again from `00 Setup`, otherwise the
  collection variables still hold ids of a database that no longer exists;
- the sequence cannot be run twice on the same running application: the second run would try
  to register a username that already exists and to create a team with a name already taken,
  and both are rejected. `member1` and `member2` would be refused as well, because each of them
  ends the run tied to a team: `member1` to Byte Runners, and `member2` to Null Pointers, which
  `08 Registration` creates with them after `07 Leave team` has taken them out of the first one.
  `member3` is the exception: `16 Proclamation` concludes the hackathon they judge, and a staff
  role in a concluded event no longer counts as a participation, so they are free again — but
  that changes nothing, since the run is already broken well before `04 Staff`. The registrations are the one thing that does
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
