# secrethitler-api
The API for the Secret Hitler game.

**/api/chancellor/nominate (POST)**
 - channelName (String)
- chancellorId (long)\
Pusher: 
- chancellor_nominated: chancellorId (long)

**/api/chancellor/vote (POST)**
- channelName (String)
- votedYes (bool)\
Pusher:
- chancellor_vote: user_id (long), voted_yes (bool)
- chancellor_elected: elected (bool)
- game_won: party (String), reason (String)
- president_receive_policies: policies (String array)
- election_tracker
- policy_enacted: policy (String)

**/api/game/create (POST)**
- userName (String)\
Return:
- userName (String)
- userId (long)
- channelName (String)

**/api/game/join (POST)**
- channelName (String)
- userName (String)\
Rückgabe:
- userName (String)
- userId (long)
- channelName (String)
- creatorId (long)

**/api/game/start (POST)**
- channelName (String)\
Pusher: 
- game_start (To the private channel of every player in the game): userId (long), roleId (long), userName (String), roleName (String)

**/api/player/execute (POST)**
- channelName (String)
- userId (long)\
Pusher:
- game_won: party (String), reason (String)
- player_killed: userId (long)

**/api/player/investigate/{userId} (GET)**
- channelName (String)\
Return:
- party (String)

**/api/policy/president-pick (POST)**
- channelName (String)
- discardedPolicy (String)\
Pusher:
- chancellor_receive_policies (To the chancellor's private channel): policies (String array)

**/api/policy/chancellor-pick (POST)**
- channelName (String)
- discardedPolicy (String)\
Pusher:
- policy_enacted: policy (String)
- game_won: party (String)
- policy_peek
- execute_player
- loyalty_investigation
- special_election

**/api/policy/peek  (GET)**
- channelName (String)\
Return:
- policies (String array)

**/api/round/next (POST)**
- channelName (String)\
Pusher:
- next_round: presidentId (long)
- notify_president (To the president's private channel): electable (long array (With the IDs of all the electable chancellors))

**/api/round/special-election (POST)**
- channelName (String)
- nextPresidentId (long)
