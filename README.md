# secrethitler-api
The API for the Secret Hitler game.

If everything is okay, you will receive a 200 response. If something is wrong, you will receive a 4xx response with a `message` element containing the error.

The value in the brackets in pusher events are the channel name the event will be published to.

All requests (except for create and join game and pusher auth) need to have a API token in the HTTP authorization header in the Basic format. 

`POST /chancellor/nominate`

- channelName (string)
- chancellorId (integer)
- userId (integer)

Response:

Pusher events:
- chancellor_nominated (channelName)
    - chancellorId (integer)
    
---

`POST /chancellor/vote`

- channelName (string)
- votedYes (boolean)
- userId (integer)

Response:

Pusher events:

- chancellor_vote (channelName)
    - user_id (integer)
    - voted_yes (boolean)
    
- chancellor_elected (channelName, if all players have voted)
    - elected (boolean)
    
- game_won (channelName, if this ends the game)
    - party (string)
    - reason
    
- president_receive_policies (private channel of the president, if the vote didn't fail)
    - policies (string[])

- election_tracker (channelName, if the vote fails and the election tracker needs to be reset)

- policy_enacted (channelName, if the vote fails and the election tracker needs to be reset)
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

- game_start (private channel of each player)
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

- chancellor_receive_policies (private channel of the chancellor)
    - policies (string[])

---

`POST /policy/chancellor-pick`

- channelName (string)
- discardedPolicy (string)
- userId (integer)

Response:

Pusher events:

- policy_enacted (channelName)
    - policy (string)

- game_won (channelName, if this ends the game)
    - party (string)
    - reason

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

- president_id (integer)

Pusher events:

- next_round (channelName)
    - presidentId (integer)
    
- notify_president (private channel of the next round's president)
    - electable (integer[], the ids of all the electable players)

---

`POST /round/special-election`

- channelName (string)
- nextPresidentId (integer)
- userId (integer)

Response:

Pusher events:

- next_round (channelName)
    - presidentId (integer)
---

`POST /pusher/auth`

- channelName (string)
- socketId (integer)

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

- player_killed (channelName)
    - userId (integer)

---

`GET /player/investigate/:userId`

- channelName (string)
- investigatedUser (integer)
- userId (integer)

Response:

- party (string)

Pusher events:

---