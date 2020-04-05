# secrethitler-api
The API for the Secret Hitler game.

If everything is okay, you will receive a 200 response. If something is wrong, you will receive a 4xx response with a `message` element containing the error.

The value in the brackets in pusher events are the channel name the event will be published to.

All requests (except for create and join game) need to have a API token in the HTTP authorization header in the Basic format. 

`POST /chancellor/nominate`

- channelName (string)
- chancellorId (integer)
- userId (integer)

Response:

Pusher events:
- chancellorNominated (channelName)
    - chancellorId (integer)
    
---

`POST /chancellor/vote`

- channelName (string)
- votedYes (boolean)
- userId (integer)

Response:

Pusher events:

- chancellorVote (channelName)
    - userId (integer)
    - votedYes (boolean)
    
---

`POST /chancellor/voting-completed`

- channelName (string)
- userId (integer)

Response:

Pusher events:
    
- chancellorElected (channelName)
    - elected (boolean)
    
- gameWon (channelName, if this ends the game)
    - party (string)
    - reason (string)
    
- presidentReceivePolicies (private channel of the president, if the vote didn't fail)
    - policies (string[])
    
- electionTracker (channelName, if the vote fails and the election tracker advances)

- resetElectionTracker (channelName, if the vote fails and the election tracker needs to be reset)

- policyEnacted (channelName, if the vote fails and the election tracker needs to be reset)
    - policy (string)
    
---

`POST /game/create`

- userName (string)

Response:

- userName (string)
- userId (integer)
- channelName (string)
- token (string)

Pusher events:

---

`POST /game/join`

- userName (string)
- channelName (string)

Response:

- userName (string)
- userId (integer)
- channelName (string)
- creatorId (integer)
- token (string)

Pusher events:

---

`POST /game/start`

- channelName (string)
- userId (integer)

Response:

Pusher events:

- gameStart (private channel of each player)
    - userId (integer)
    - roleId (integer)
    - roleName (String)
    - userName (string)

---

`POST /policy/president-pick`

- channelName (string)
- discardedPolicy (string)
- userId (integer)

Response:

Pusher events:

- chancellorReceivePolicies (private channel of the chancellor)
    - policies (string[])

- vetoPossible (private channel of the chancellor, if more than five fascist policies are currently enacted)

---

`POST /policy/chancellor-pick`

- channelName (string)
- discardedPolicy (string)
- userId (integer)

Response:

Pusher events:

- policyEnacted (channelName)
    - policy (string)

- gameWon (channelName, if this ends the game)
    - party (string)
    - reason
    
- policyPeek (to president's private channel, if executive action was unlocked)
- executePlayer (to president's private channel, if executive action was unlocked)
- loyaltyInvestigation (to president's private channel, if executive action was unlocked)
- specialElection (to president's private channel, if executive action was unlocked)

---

`GET /policy/peek`

- channelName (string)
- userId (integer)

Response:

policies (string[])

Pusher events:

---

`POST /round/next`

- channelName (string)
- userId (integer)

Response:

- presidentId (integer)

Pusher events:

- nextRound (channelName)
    - presidentId (integer)
    
- notifyPresident (private channel of the next round's president)
    - electable (integer[], the ids of all the electable players)

---

`POST /round/special-election`

- channelName (string)
- nextPresidentId (integer)
- userId (integer)

Response:

Pusher events:

- nextRound (channelName)
    - presidentId (integer)
---

`POST /round/request-veto` (called by the chancellor)

- channelName (string)
- userId (integer)

Response:

Pusher events:

- requestVeto (private channel of the president)

---

`POST /round/veto` (called by the president)

- channelName (string)
- accepted (boolean)
- userId (integer)

Response:

Pusher events:

- vetoAccepted (channelName, if he accepts the veto. This should advance the election tracker)

- electionTracker (channelName, if the veto was accepted and the election tracker advances)

- resetElectionTracker (channelName, if the veto was accepted and the election tracker needs to be reset)

- policyEnacted (channelName, if the veto was accepted and the election tracker needs to be reset)
    - policy (string)
    
- vetoDenied (private channel of the chancellor)

---

`POST /pusher/auth`

- channelName (string)
- socketId (integer)
- userId (integer)

Response:

- Pusher data...

Pusher events:

---

`POST /player/execute`

- channelName (string)
- executedUserId (integer)
- userId (integer)

Response:

Pusher events:

- playerKilled (channelName)
    - userId (integer)

- gameWon (channelName, if this ends the game)
    - party (string)
    - reason

---

`GET /player/investigate/:investigatedUserId`

- channelName (string)
- userId (integer)

Response:

- party (string)

Pusher events:

---