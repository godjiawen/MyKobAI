$ErrorActionPreference = 'Stop'

function Post-Form($uri, $body, $headers = @{}) {
  return Invoke-RestMethod -Method Post -Uri $uri -Body $body -ContentType 'application/x-www-form-urlencoded' -Headers $headers
}

function Get-Json($uri, $headers = @{}) {
  return Invoke-RestMethod -Method Get -Uri $uri -Headers $headers
}

function Connect-Ws($uri) {
  $ws = [System.Net.WebSockets.ClientWebSocket]::new()
  $ws.ConnectAsync([Uri]$uri, [Threading.CancellationToken]::None).Wait()
  return $ws
}

function Receive-WsText($ws, $timeoutMs = 3000) {
  $buffer = New-Object byte[] 8192
  $segment = [System.ArraySegment[byte]]::new($buffer)
  $task = $ws.ReceiveAsync($segment, [Threading.CancellationToken]::None)
  if (-not $task.Wait($timeoutMs)) { return $null }
  $result = $task.Result
  if ($result.Count -le 0) { return $null }
  return [Text.Encoding]::UTF8.GetString($buffer, 0, $result.Count)
}

$base = 'http://127.0.0.1:3010'
$stamp = Get-Date -Format 'HHmmssfff'
$userA = "u${stamp}a"
$userB = "u${stamp}b"
$pass = 'Test@123A'

$regA = Post-Form "$base/api/user/account/register/" @{ username = $userA; password = $pass; confirmedPassword = $pass }
$regB = Post-Form "$base/api/user/account/register/" @{ username = $userB; password = $pass; confirmedPassword = $pass }
if ($regA.error_message -ne 'success' -or $regB.error_message -ne 'success') {
  throw "register failed: $($regA.error_message) / $($regB.error_message)"
}

$loginA = Post-Form "$base/api/user/account/token/" @{ username = $userA; password = $pass }
$loginB = Post-Form "$base/api/user/account/token/" @{ username = $userB; password = $pass }
$tokenA = $loginA.token
$tokenB = $loginB.token
$headersA = @{ Authorization = "Bearer $tokenA" }
$headersB = @{ Authorization = "Bearer $tokenB" }

$infoA = Get-Json "$base/api/user/account/info/" $headersA
$infoB = Get-Json "$base/api/user/account/info/" $headersB

$wsA = Connect-Ws "ws://127.0.0.1:3010/websocket/$tokenA/"
$wsB = Connect-Ws "ws://127.0.0.1:3010/websocket/$tokenB/"
Start-Sleep -Milliseconds 300

$requestSend = Post-Form "$base/api/friends/request/send/" @{ receiver_id = $infoB.id; message = '来打一局？' } $headersA
if ($requestSend.error_message -ne 'success') { throw "friend request send failed: $($requestSend.error_message)" }
$wsRequest = Receive-WsText $wsB
$received = Get-Json "$base/api/friends/request/received/?status=pending&page=1&page_size=10" $headersB
$requestId = $received.requests[0].request_id
$accept = Post-Form "$base/api/friends/request/accept/" @{ request_id = $requestId } $headersB
if ($accept.error_message -ne 'success') { throw "friend request accept failed: $($accept.error_message)" }
$wsHandled = Receive-WsText $wsA

$friendsA = Get-Json "$base/api/friends/list/?page=1&page_size=1&keyword=$userB&status=online&sort_by=created_at_desc" $headersA
$friendsEmpty = Get-Json "$base/api/friends/list/?page=1&page_size=1&keyword=nomatch_$stamp&sort_by=created_at_desc" $headersA

$inviteSend = Post-Form "$base/api/friends/invite/send/" @{ receiver_id = $infoB.id; sender_bot_id = -1; game_mode = 'pk' } $headersA
if ($inviteSend.error_message -ne 'success') { throw "invite send failed: $($inviteSend.error_message)" }
$inviteId = $inviteSend.invite.invite_id
$wsInvite = Receive-WsText $wsB
$cancelInvite = Post-Form "$base/api/friends/invite/respond/" @{ invite_id = $inviteId; action = 'cancel' } $headersA
if ($cancelInvite.error_message -ne 'success') { throw "invite cancel failed: $($cancelInvite.error_message)" }
$wsInviteUpdatedA = Receive-WsText $wsA
$wsInviteUpdatedB = Receive-WsText $wsB

$chatSend = Post-Form "$base/api/friends/chat/send/" @{ friend_id = $infoB.id; content = '联调私聊消息' } $headersA
if ($chatSend.error_message -ne 'success') { throw "chat send failed: $($chatSend.error_message)" }
$wsChatA = Receive-WsText $wsA
$wsChatB = Receive-WsText $wsB
$conversationsB = Get-Json "$base/api/friends/chat/conversations/" $headersB
$historyB = Get-Json "$base/api/friends/chat/history/?friend_id=$($infoA.id)&page=1&page_size=20" $headersB

$summary = [ordered]@{
  users = @($userA, $userB)
  friend_request_event = $wsRequest
  request_handled_event = $wsHandled
  filtered_friend_count = $friendsA.friends_count
  filtered_friend_names = @($friendsA.friends | ForEach-Object { $_.friend_username })
  empty_filter_count = $friendsEmpty.friends_count
  invite_received_event = $wsInvite
  invite_updated_sender_event = $wsInviteUpdatedA
  invite_updated_receiver_event = $wsInviteUpdatedB
  chat_sender_event = $wsChatA
  chat_receiver_event = $wsChatB
  conversation_preview = $conversationsB.conversations[0].last_message
  conversation_unread_before_history = $conversationsB.conversations[0].unread_count
  history_count = $historyB.messages_count
  latest_history_message = $historyB.messages[-1].content
}

$wsA.Dispose()
$wsB.Dispose()
$summary | ConvertTo-Json -Depth 6
