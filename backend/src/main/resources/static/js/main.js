// 'use strict';

// // --- CONFIGURATION ---
// var username = localStorage.getItem('username');
// var jwtToken = localStorage.getItem('jwtToken');
// var stompClient = null;
// var selectedUser = null;

// // --- DOM ELEMENTS ---
// var usersList = document.querySelector('#usersList');
// var messageForm = document.querySelector('#messageForm');
// var messageInput = document.querySelector('#message');
// var messageArea = document.querySelector('#messageArea');
// var chatTitle = document.querySelector('#chat-title');

// // --- 1. INITIALIZATION ---
// if (username && jwtToken) {
//     connect();
//     // Delay fetching users slightly to ensure DOM is ready
//     setTimeout(fetchAllUsers, 500); 
// } else {
//     window.location.href = '/login.html';
// }

// // --- 2. CONNECT TO WEBSOCKET ---
// function connect() {
//     var socket = new SockJS('/ws');
//     stompClient = Stomp.over(socket);
//     stompClient.connect({'Authorization': 'Bearer ' + jwtToken}, onConnected, onError);
// }

// function onConnected() {
//     stompClient.subscribe('/topic/public', onMessageReceived);
//     stompClient.subscribe('/user/queue/messages', onPrivateMessageReceived);
//     stompClient.send("/app/chat.addUser", {}, JSON.stringify({sender: username, type: 'JOIN'}));
// }

// function onError(error) {
//     console.log('Could not connect to WebSocket server. ' + error);
// }

// // --- 3. FETCH USERS (The Logic that was missing) ---
// // Inside main.js

// function fetchAllUsers() {
//     fetch('/api/users', {
//         method: 'GET',
//         headers: { 'Authorization': 'Bearer ' + jwtToken }
//     })
//     .then(response => response.json())
//     .then(users => {
//         usersList.innerHTML = ''; 
//         users.forEach(user => {
//             // FIX: Only add user if name is NOT empty and NOT myself
//             if (user && user.trim() !== "" && user !== username) {
//                 addUserToSidebar(user);
//             }
//         });
//     })
//     .catch(error => console.error('Error fetching users:', error));
// }

// // --- 4. SIDEBAR UI HELPER ---
// function addUserToSidebar(newUsername) {
//     var li = document.createElement('li');
//     li.id = "user-" + newUsername;
//     li.innerHTML = `
//         <div class="user-avatar">${newUsername.charAt(0).toUpperCase()}</div>
//         <span>${newUsername}</span>
//         <div class="user-status"></div>
//     `;
//     li.onclick = function() { selectUser(newUsername); };
//     usersList.appendChild(li);
// }

// // --- 5. CHAT LOGIC ---
// function selectUser(user) {
//     selectedUser = user;
//     chatTitle.innerText = "Private Chat with " + user;
//     messageArea.innerHTML = ""; // Clear area
    
//     // Highlight active user
//     document.querySelectorAll('#usersList li').forEach(item => item.classList.remove('active'));
//     var activeLi = document.getElementById("user-" + user);
//     if(activeLi) activeLi.classList.add('active');

//     // --- NEW: FETCH CHAT HISTORY ---
//     fetch(`/api/messages/${username}/${user}`, {
//         method: 'GET',
//         headers: { 
//             'Authorization': 'Bearer ' + jwtToken,
//             'Content-Type': 'application/json'
//         }
//     })
//     .then(response => {
//         if (!response.ok) throw new Error("Could not load history");
//         return response.json();
//     })
//     .then(messages => {
//         messages.forEach(msg => {
//             // true if I am the sender, false otherwise
//             displayMessage(msg, msg.sender === username); 
//         });
//     })
//     .catch(error => console.error('Error loading history:', error));
// }

// function sendMessage(event) {
//     var messageContent = messageInput.value.trim();
//     if(messageContent && stompClient) {
//         var chatMessage = {
//             sender: username,
//             content: messageInput.value,
//             type: 'CHAT'
//         };

//         if (selectedUser) {
//             chatMessage.receiver = selectedUser;
//             stompClient.send("/app/chat.private", {}, JSON.stringify(chatMessage));
//             displayMessage(chatMessage, true);
//         } else {
//             stompClient.send("/app/chat.sendMessage", {}, JSON.stringify(chatMessage));
//         }
//         messageInput.value = '';
//     }
//     event.preventDefault();
// }

// function onMessageReceived(payload) {
//     var message = JSON.parse(payload.body);
//     if(selectedUser === null) displayMessage(message, false);
// }

// function onPrivateMessageReceived(payload) {
//     var message = JSON.parse(payload.body);
//     if (selectedUser !== message.sender) {
//         alert("New private message from " + message.sender);
//     } else {
//         displayMessage(message, false);
//     }
// }

// function displayMessage(message, isSelf) {
//     var messageElement = document.createElement('li');
//     messageElement.classList.add('message-item');
    
//     if (message.sender === username || isSelf) {
//         messageElement.classList.add('message-self');
//     } else {
//         messageElement.classList.add('message-other');
//     }

//     if(message.type === 'JOIN') {
//         messageElement.classList.add('event-message');
//         message.content = message.sender + ' joined!';
//         if(message.sender !== username) fetchAllUsers(); // Refresh list on join
//     } else if (message.type === 'LEAVE') {
//         messageElement.classList.add('event-message');
//         message.content = message.sender + ' left!';
//     } else {
//         var text = document.createElement('p');
//         text.innerText = message.content;
//         messageElement.appendChild(text);
//     }

//     messageArea.appendChild(messageElement);
//     messageArea.scrollTop = messageArea.scrollHeight;
// }

// if(messageForm) messageForm.addEventListener('submit', sendMessage, true);
// 'use strict';

// // --- 1. CONFIGURATION & STATE ---
// var username = localStorage.getItem('username');
// var jwtToken = localStorage.getItem('jwtToken');
// var stompClient = null;
// var selectedUser = null;

// // --- DOM ELEMENTS ---
// var usersList = document.querySelector('#usersList');
// var messageForm = document.querySelector('#messageForm');
// var messageInput = document.querySelector('#message');
// var messageArea = document.querySelector('#messageArea');
// var chatTitle = document.querySelector('#chat-title');

// // --- 2. INITIALIZATION ---
// if (username && jwtToken) {
//     // Request notification permission immediately on load
//     if (Notification.permission !== "granted") {
//         Notification.requestPermission();
//     }
//     connect();
//     setTimeout(fetchAllUsers, 500); 
// } else {
//     window.location.href = '/login.html';
// }

// // --- 3. THE TUNNEL: WEBSOCKET CONNECTION ---
// function connect() {
//     var socket = new SockJS('/ws');
//     stompClient = Stomp.over(socket);

//     // FIX: Send the token in the CONNECT headers
//     var headers = {
//         'Authorization': 'Bearer ' + jwtToken
//     };

//     stompClient.connect(headers, onConnected, onError);
// }

// function onConnected() {
//     // Subscribe to public events (like user joins/leaves)
//     stompClient.subscribe('/topic/public', onMessageReceived);
//     // Subscribe to the private tunnel for instant message pushes
//     stompClient.subscribe('/user/queue/messages', onPrivateMessageReceived);
    
//     stompClient.send("/app/chat.addUser", {}, JSON.stringify({sender: username, type: 'JOIN'}));
// }

// function onError(error) {
//     console.error('WebSocket Error: ' + error);
// }

// // --- 4. DATA FETCHING ---
// function fetchAllUsers() {
//     fetch('/api/users', {
//         method: 'GET',
//         headers: { 'Authorization': 'Bearer ' + jwtToken }
//     })
//     .then(response => response.json())
//     .then(users => {
//         usersList.innerHTML = ''; 
//         users.forEach(user => {
//             if (user && user !== username) {
//                 addUserToSidebar(user);
//             }
//         });
//     })
//     .catch(error => console.error('Error fetching users:', error));
// }

// // --- 5. AUTOMATIC UI UPDATES (The logic you were missing) ---
// function onPrivateMessageReceived(payload) {
//     var message = JSON.parse(payload.body);
//     console.log("New message received via tunnel:", message); // Debugging line

//     // 1. DYNAMIC UI UPDATE
//     // Check if the sender of the incoming message is the user you are currently looking at
//     if (selectedUser === message.sender) {
//         // Add the bubble to the screen immediately!
//         displayMessage(message, false); 
        
//         // Auto-scroll so the user sees the new text
//         messageArea.scrollTop = messageArea.scrollHeight;
//     } else {
//         // 2. BACKGROUND NOTIFICATION
//         // If you are talking to someone else, show the red alert in the sidebar
//         updateSidebarAlert(message.sender, message.content);
//         showBrowserNotification(message.sender, message.content);
//     }
// }

// function updateSidebarAlert(sender, content) {
//     var userLi = document.getElementById("user-" + sender);
    
//     if (userLi) {
//         // 1. Add the "Red Alert" class automatically (Needs CSS)
//         userLi.classList.add('has-new-message');
        
//         // 2. Trigger Browser Popup if the tab is hidden
//         showBrowserNotification(sender, content);
//     } else {
//         // If a new user messaged us who isn't in the list, refresh the list
//         fetchAllUsers();
//     }
// }

// // --- 6. CHAT HISTORY & SELECTION ---
// function selectUser(user) {
//     selectedUser = user;
//     chatTitle.innerText = "Private Chat with " + user;
//     messageArea.innerHTML = ""; 
    
//     // UI: Remove active highlights and clear the red alert for THIS user
//     document.querySelectorAll('#usersList li').forEach(item => item.classList.remove('active'));
//     var activeLi = document.getElementById("user-" + user);
//     if(activeLi) {
//         activeLi.classList.add('active');
//         activeLi.classList.remove('has-new-message'); // Manual check is gone! Clearing alert here.
//     }

//     // Load History Road
//     fetch(`/api/messages/${username}/${user}`, {
//         method: 'GET',
//         headers: { 
//             'Authorization': 'Bearer ' + jwtToken,
//             'Content-Type': 'application/json'
//         }
//     })
//     .then(res => res.json())
//     .then(messages => {
//         messages.forEach(msg => {
//             displayMessage(msg, msg.sender === username); 
//         });
//     })
//     .catch(err => console.error('Error loading history:', err));
// }

// // --- 7. UTILITY FUNCTIONS ---
// function showBrowserNotification(sender, content) {
//     if (Notification.permission === "granted" && document.hidden) {
//         new Notification("New Message from " + sender, {
//             body: content,
//             icon: "/favicon.ico"
//         });
//     }
// }

// function sendMessage(event) {
//     var messageContent = messageInput.value.trim();
//     if(messageContent && stompClient) {
//         var chatMessage = {
//             sender: username,
//             content: messageContent,
//             type: 'CHAT'
//         };

//         if (selectedUser) {
//             chatMessage.receiver = selectedUser;
//             stompClient.send("/app/chat.private", {}, JSON.stringify(chatMessage));
//             displayMessage(chatMessage, true);
//         } else {
//             stompClient.send("/app/chat.sendMessage", {}, JSON.stringify(chatMessage));
//         }
//         messageInput.value = '';
//     }
//     event.preventDefault();
// }

// function onMessageReceived(payload) {
//     var message = JSON.parse(payload.body);
//     if (message.type === 'JOIN' && message.sender !== username) {
//         fetchAllUsers(); // Auto-update user list when someone new enters the tunnel
//     }
// }

// function displayMessage(message, isSelf) {
//     var messageElement = document.createElement('li');
//     messageElement.classList.add('message-item');
//     messageElement.classList.add(isSelf ? 'message-self' : 'message-other');

//     var text = document.createElement('p');
//     text.innerText = message.content;
//     messageElement.appendChild(text);

//     messageArea.appendChild(messageElement);
//     messageArea.scrollTop = messageArea.scrollHeight;
// }

// function addUserToSidebar(newUsername) {
//     var li = document.createElement('li');
//     li.id = "user-" + newUsername;
//     li.innerHTML = `
//         <div class="user-avatar">${newUsername.charAt(0).toUpperCase()}</div>
//         <span>${newUsername}</span>
//         <div class="user-status"></div>
//     `;
//     li.onclick = function() { selectUser(newUsername); };
//     usersList.appendChild(li);
// }

// if(messageForm) messageForm.addEventListener('submit', sendMessage, true);
// 'use strict';

// // --- 1. CONFIGURATION & STATE ---
// var username = localStorage.getItem('username');
// var jwtToken = localStorage.getItem('jwtToken');
// var stompClient = null;
// var selectedUser = null;

// // --- DOM ELEMENTS ---
// var usersList = document.querySelector('#usersList');
// var messageForm = document.querySelector('#messageForm');
// var messageInput = document.querySelector('#message');
// var messageArea = document.querySelector('#messageArea');
// var chatTitle = document.querySelector('#chat-title');

// // --- 2. INITIALIZATION ---
// if (username && jwtToken) {
//     // Request notification permission immediately
//     if (Notification.permission !== "granted") {
//         Notification.requestPermission();
//     }
//     connect();
//     // Fetch users after a small delay to let the connection stabilize
//     setTimeout(fetchAllUsers, 500); 
// } else {
//     window.location.href = '/login.html';
// }

// // --- 3. THE TUNNEL: WEBSOCKET CONNECTION ---
// function connect() {
//     var socket = new SockJS('/ws');
//     stompClient = Stomp.over(socket);

//     // Secure headers for the WebSocket Handshake
//     var headers = {
//         'Authorization': 'Bearer ' + jwtToken
//     };

//     stompClient.connect(headers, onConnected, onError);
// }

// function onConnected() {
//     console.log("Connected to WebSocket Tunnel!");
    
//     // Subscribe to Global messages
//     stompClient.subscribe('/topic/public', onMessageReceived);
    
//     // Subscribe to Private messages (Instant Popup Logic)
//     stompClient.subscribe('/user/queue/messages', onPrivateMessageReceived);
    
//     // Notify server that we are online
//     stompClient.send("/app/chat.addUser", {}, JSON.stringify({sender: username, type: 'JOIN'}));
// }

// function onError(error) {
//     console.error('WebSocket Error: ' + error);
// }

// // --- 4. DATA FETCHING ---
// // function fetchAllUsers() {
// //     fetch('/api/users', {
// //         method: 'GET',
// //         headers: { 'Authorization': 'Bearer ' + jwtToken }
// //     })
// //     .then(response => response.json())
// //     .then(users => {
// //         usersList.innerHTML = ''; 
// //         users.forEach(user => {
// //             // Don't show myself in the contact list
// //             if (user && user.toLowerCase() !== username.toLowerCase()) {
// //                 addUserToSidebar(user);
// //             }
// //         });
// //     })
// //     .catch(error => console.error('Error fetching users:', error));
// // }
// function fetchAllUsers() {
//     // 1. Always get the LATEST token from storage
//     const currentToken = localStorage.getItem('jwtToken');
    
//     if (!currentToken) {
//         console.error("No token found in storage! Redirecting to login.");
//         window.location.href = '/login.html';
//         return;
//     }

//     fetch('/api/users', {
//         method: 'GET',
//         headers: { 
//             // 2. CRITICAL: Ensure exactly ONE space after 'Bearer' and trim the token
//             'Authorization': 'Bearer ' + currentToken.trim() 
//         }
//     })
//     .then(response => {
//         if (response.status === 401) {
//             console.error("Access Denied: Token invalid. Clearing storage.");
//             localStorage.clear(); // Force logout so user gets a fresh token
//             window.location.href = '/login.html';
//             return;
//         }
//         return response.json();
//     })
//     .then(users => {
//         // Clear list and rebuild
//         if (users) {
//             usersList.innerHTML = ''; 
//             users.forEach(user => {
//                 // Don't show myself
//                 if (user && user.toLowerCase() !== username.toLowerCase()) {
//                     addUserToSidebar(user);
//                 }
//             });
//         }
//     })
//     .catch(error => console.error('Error fetching users:', error));
// }

// // // --- 5. THE INSTANT UPDATE LOGIC (CRITICAL) ---
// // function onPrivateMessageReceived(payload) {
// //     var message = JSON.parse(payload.body);
// //     console.log("New message arrived via tunnel:", message);

// //     // Bulletproof comparison: Ignore case and trim spaces
// //     var isChattingWithSender = selectedUser && 
// //         selectedUser.trim().toLowerCase() === message.sender.trim().toLowerCase();

// //     if (isChattingWithSender) {
// //         // INSTANT POPUP: Draw it now
// //         displayMessage(message, false); 
// //         messageArea.scrollTop = messageArea.scrollHeight;
// //     } else {
// //         // NOTIFICATION: Trigger sidebar alert and browser popup
// //         updateSidebarAlert(message.sender, message.content);
// //     }
// // }
// function onPrivateMessageReceived(payload) {
//     var message = JSON.parse(payload.body);
    
//     // Check if the names match, ignoring Capital/Small letters
//     if (selectedUser && selectedUser.toLowerCase() === message.sender.toLowerCase()) {
//         displayMessage(message, false); // <--- THIS POPS UP THE MESSAGE INSTANTLY
//         messageArea.scrollTop = messageArea.scrollHeight;
//     } else {
//         updateSidebarAlert(message.sender, message.content);
//     }
// }

// function updateSidebarAlert(sender, content) {
//     var userLi = document.getElementById("user-" + sender);
//     if (userLi) {
//         userLi.classList.add('has-new-message'); // CSS will handle the red glow
//         showBrowserNotification(sender, content);
//     } else {
//         // If a message comes from someone not in list, refresh list
//         fetchAllUsers();
//     }
// }

// // --- 6. CHAT HISTORY & SELECTION ---
// function selectUser(user) {
//     selectedUser = user;
//     chatTitle.innerText = "Private Chat with " + user;
//     messageArea.innerHTML = ""; 
    
//     // UI: Clear alerts and set active state
//     document.querySelectorAll('#usersList li').forEach(item => {
//         item.classList.remove('active');
//     });
    
//     var activeLi = document.getElementById("user-" + user);
//     if(activeLi) {
//         activeLi.classList.add('active');
//         activeLi.classList.remove('has-new-message'); // Clear red alert
//     }

//     // Load saved messages from MongoDB
//     fetch(`/api/messages/${username}/${user}`, {
//         method: 'GET',
//         headers: { 
//             'Authorization': 'Bearer ' + jwtToken,
//             'Content-Type': 'application/json'
//         }
//     })
//     .then(res => res.json())
//     .then(messages => {
//         messages.forEach(msg => {
//             displayMessage(msg, msg.sender.toLowerCase() === username.toLowerCase()); 
//         });
//     })
//     .catch(err => console.error('Error loading history:', err));
// }

// // --- 7. SENDING & DISPLAYING ---
// // function sendMessage(event) {
// //     var messageContent = messageInput.value.trim();
// //     if(messageContent && stompClient) {
// //         var chatMessage = {
// //             sender: username,
// //             content: messageContent,
// //             type: 'CHAT'
// //         };

// //         if (selectedUser) {
// //             chatMessage.receiver = selectedUser;
// //             stompClient.send("/app/chat.private", {}, JSON.stringify(chatMessage));
// //             displayMessage(chatMessage, true); // Display my own message instantly
// //         }
// //         messageInput.value = '';
// //     }
// //     event.preventDefault();
// // }
// function sendMessage(event) {
//     var messageContent = messageInput.value.trim();
    
//     // PRINT 2: Check frontend state
//     console.log("Attempting to send. SelectedUser is: ", selectedUser);

//     if(messageContent && stompClient && selectedUser) {
//         var chatMessage = {
//             sender: username,
//             content: messageContent,
//             type: 'CHAT',
//             receiver: selectedUser 
//         };

//         console.log("Payload being sent to /app/chat.private:", chatMessage);
        
//         stompClient.send("/app/chat.private", {}, JSON.stringify(chatMessage));
//         displayMessage(chatMessage, true); 
//         messageInput.value = '';
//     } else {
//         console.warn("Send failed: messageContent, stompClient, or selectedUser is missing.");
//     }
//     event.preventDefault();
// }
// function displayMessage(message, isSelf) {
//     var messageElement = document.createElement('li');
//     messageElement.classList.add('message-item');
//     messageElement.classList.add(isSelf ? 'message-self' : 'message-other');

//     var text = document.createElement('p');
//     text.innerText = message.content;
//     messageElement.appendChild(text);

//     messageArea.appendChild(messageElement);
//     messageArea.scrollTop = messageArea.scrollHeight;
// }

// // --- 8. UTILITIES ---
// function showBrowserNotification(sender, content) {
//     if (Notification.permission === "granted" && document.hidden) {
//         new Notification("Nexus: " + sender, { body: content });
//     }
// }

// function addUserToSidebar(newUsername) {
//     var li = document.createElement('li');
//     li.id = "user-" + newUsername;
//     li.innerHTML = `
//        
//update
'use strict';

// ==========================================
// 1. CONFIGURATION & STATE
// ==========================================
var username = localStorage.getItem('username');
var stompClient = null;
var selectedUser = null;
var friendList = []; 
var requestList = []; 

// --- WEBRTC STATE ---
var peerConnection = null;
var localStream = null;
var dataChannel = null;
var candidateQueue = []; 
var isVideoCall = false;

// STUN Servers
const rtcConfig = {
    iceServers: [ { urls: 'stun:stun.l.google.com:19302' } ]
};

// DOM Elements
var usersList = document.querySelector('#usersList');
var messageForm = document.querySelector('#messageForm');
var messageInput = document.querySelector('#message');
var messageArea = document.querySelector('#messageArea');
var chatTitle = document.querySelector('#chat-title');
var btnCall = document.getElementById('btnCall');
var videoOverlay = document.getElementById('video-overlay');

// Initialization
if (username) {
    connect();
    dragElement(videoOverlay);
    setTimeout(fetchFriends, 500); 
    setTimeout(fetchRequests, 1000); 
} else {
    window.location.href = '/login.html';
}

// ==========================================
// 2. WEBSOCKET CONNECTION
// ==========================================
function connect() {
    var socket = new SockJS('/ws'); 
    stompClient = Stomp.over(socket);
    var headers = { 'Authorization': 'Bearer ' + localStorage.getItem('jwtToken') };
    stompClient.connect(headers, onConnected, onError);
}

function onConnected() {
    console.log("Connected to WebSocket!");
    stompClient.subscribe('/topic/public', onMessageReceived); 
    stompClient.subscribe('/user/queue/messages', onPrivateMessageReceived);
    stompClient.subscribe('/user/queue/ack', onAckReceived); 
    stompClient.subscribe('/user/queue/signal', onSignalReceived);
    stompClient.send("/app/chat.addUser", {}, JSON.stringify({sender: username, type: 'JOIN'}));
}

function onError(error) { console.error('WebSocket Error:', error); }

// ==========================================
// 3. UI, FRIENDS & SEARCH
// ==========================================
function fetchFriends() {
    const token = localStorage.getItem('jwtToken');
    fetch('/api/users/users', { headers: { 'Authorization': 'Bearer ' + token }})
    .then(res => res.json())
    .then(users => {
        friendList = [];
        if (Array.isArray(users)) {
            users.forEach(user => {
                let name = user.username || user;
                if (name && name.toLowerCase() !== username.toLowerCase()) {
                    friendList.push({ username: name, unread: 0 });
                }
            });
        }
        // Only render if we are on the chats tab
        if(document.getElementById("btn-chats").classList.contains("active-tab")) {
            renderUserList(friendList, false);
        }
    })
    .catch(err => console.error("Could not fetch friends", err));
}

function fetchRequests() {
    const token = localStorage.getItem('jwtToken');
    fetch('/api/friends/requests', { headers: { 'Authorization': 'Bearer ' + token }})
    .then(res => res.json())
    .then(data => {
        requestList = data.map(name => ({ username: name }));
        const badge = document.getElementById('req-badge');
        if(badge) {
            badge.innerText = requestList.length;
            badge.style.display = requestList.length > 0 ? 'inline-block' : 'none';
        }
    });
}

// THIS FUNCTION RENDERS THE LISTS
function renderUserList(list, isSearch) {
    usersList.innerHTML = '';
    if (!list || list.length === 0) {
        usersList.innerHTML = '<li style="padding:15px; color:#999; text-align:center;">No users found</li>';
        return;
    }
    list.forEach(user => {
        var li = document.createElement('li');
        li.id = "user-" + user.username;
        
        // Check if this person is already my friend
        const isFriend = friendList.some(f => f.username === user.username);
        
        let actionHtml = '';
        if (isSearch && !isFriend) {
            // IF SEARCHING AND NOT FRIEND -> SHOW "ADD" BUTTON
            actionHtml = `<button onclick="sendFriendRequest('${user.username}', event)" class="btn-add" style="float:right;">Add</button>`;
        } else {
            // IF FRIEND -> SHOW UNREAD BADGE
            let badgeStyle = (user.unread && user.unread > 0) ? 'inline-block' : 'none';
            actionHtml = `<span class="unread-badge" style="display:${badgeStyle}">${user.unread || 0}</span>`;
        }

        li.innerHTML = `
            <div class="user-avatar">${user.username.charAt(0).toUpperCase()}</div>
            <div style="flex-grow:1; margin-left:10px;">
                <span class="username-text">${user.username}</span>
            </div>
            ${actionHtml}
        `;
        
        // Click to chat (only if friend or you want to allow chatting with strangers)
        li.onclick = function() { selectUser(user.username); };
        
        usersList.appendChild(li);
    });
}

function renderRequestList() {
    usersList.innerHTML = '';
    if (requestList.length === 0) {
        usersList.innerHTML = '<li style="padding:15px; color:#999; text-align:center;">No pending requests</li>';
        return;
    }
    requestList.forEach(req => {
        var li = document.createElement('li');
        li.innerHTML = `
            <div class="user-avatar">${req.username.charAt(0).toUpperCase()}</div>
            <div style="flex-grow:1; margin-left:10px;">
                <span class="username-text">${req.username}</span>
            </div>
            <div>
                <button onclick="acceptRequest('${req.username}')" style="background:#2ecc71; color:white; border:none; padding:5px 10px; margin-right:5px; border-radius:4px; cursor:pointer;">✓</button>
                <button onclick="rejectRequest('${req.username}')" style="background:#e74c3c; color:white; border:none; padding:5px 10px; border-radius:4px; cursor:pointer;">✕</button>
            </div>
        `;
        usersList.appendChild(li);
    });
}

function selectUser(user) {
    selectedUser = user;
    chatTitle.innerText = user; 
    messageArea.innerHTML = "";
    if(btnCall) btnCall.style.display = 'block'; 

    // Find friend data to reset unread count
    var friend = friendList.find(f => f.username === user);
    if (friend) { friend.unread = 0; renderUserList(friendList, false); } 

    const token = localStorage.getItem('jwtToken');
    fetch(`/api/messages/${username}/${user}`, { headers: { 'Authorization': 'Bearer ' + token } })
    .then(res => res.json())
    .then(msgs => {
        msgs.forEach(msg => {
            if (msg.sender !== username && msg.status !== 'READ') {
                sendAck(msg.id, 'READ');
                msg.status = 'READ';
            }
            displayMessage(msg, msg.sender === username);
        });
    });
}

// ==========================================
// 4. MESSAGING
// ==========================================
function sendMessage(event) {
    event.preventDefault();
    var content = messageInput.value.trim();
    if (content && stompClient) {
        var tempId = "temp-" + Date.now();
        var chatMessage = {
            sender: username, content: content, type: 'CHAT', receiver: selectedUser,
            timestamp: new Date().toISOString(), status: 'SENT', frontId: tempId
        };
        displayMessage(chatMessage, true); 
        stompClient.send("/app/chat.private", {}, JSON.stringify(chatMessage));
        messageInput.value = '';
    }
}

function displayMessage(message, isSelf) {
    var li = document.createElement('li');
    li.classList.add('message-item', isSelf ? 'message-self' : 'message-other');
    li.id = "msg-" + (message.id || message.frontId || ("temp-" + Date.now()));

    let bodyContent = `<span class="msg-text">${message.content}</span>`;
    if (message.content.startsWith("[IMAGE]")) {
        const url = message.content.split("|")[0].replace("[IMAGE]", "").trim();
        bodyContent = `<img src="${url}" class="msg-image" onclick="window.open('${url}')" />`;
    } 

    var tickHtml = isSelf ? `<span class="status-tick ${getStatusClass(message.status)}">${getStatusIcon(message.status)}</span>` : '';

    li.innerHTML = `
        <span class="msg-sender-name">${message.sender}</span>
        ${bodyContent}
        <div class="message-meta">
            <span class="message-time">${formatTime(message.timestamp)}</span>
            ${tickHtml}
        </div>
    `;

    messageArea.appendChild(li);
    messageArea.scrollTop = messageArea.scrollHeight;
}

function onPrivateMessageReceived(payload) {
    var message = JSON.parse(payload.body);
    if (message.sender === username) {
        if(message.frontId) {
            var el = document.getElementById("msg-" + message.frontId);
            if(el) el.id = "msg-" + message.id; 
        }
    } else if (selectedUser === message.sender) {
        displayMessage(message, false);
        sendAck(message.id, 'READ'); 
    } else {
        var friend = friendList.find(f => f.username === message.sender);
        if (friend) friend.unread++;
        renderUserList(friendList, false);
        sendAck(message.id, 'DELIVERED'); 
    }
}

function onMessageReceived(payload) {
    var message = JSON.parse(payload.body);
    if(message.type === 'JOIN' && message.sender !== username) {
       // Optional: fetchFriends(); 
    }
}

function sendAck(messageId, status) {
    if(!stompClient || !messageId) return;
    stompClient.send("/app/chat.ack", {}, JSON.stringify({ messageId: messageId, status: status }));
}

function onAckReceived(payload) {
    var ack = JSON.parse(payload.body); 
    var msgElement = document.getElementById("msg-" + ack.messageId);
    if (msgElement) {
        var tickElement = msgElement.querySelector('.status-tick');
        if (tickElement) {
            tickElement.innerText = getStatusIcon(ack.status);
            tickElement.className = `status-tick ${getStatusClass(ack.status)}`;
        }
    }
}

// ==========================================
// 5. WEBRTC (VIDEO & DATA)
// ==========================================
async function startVideoCall() {
    if(!selectedUser) return alert("Select a user first!");
    isVideoCall = true;
    videoOverlay.style.display = 'block';

    try {
        localStream = await navigator.mediaDevices.getUserMedia({ video: true, audio: true });
        document.getElementById('localVideo').srcObject = localStream;

        createPeerConnection();
        localStream.getTracks().forEach(track => peerConnection.addTrack(track, localStream));
        
        dataChannel = peerConnection.createDataChannel("nexus-files");
        setupDataChannelEvents(dataChannel);

        const offer = await peerConnection.createOffer();
        await peerConnection.setLocalDescription(offer);
        sendSignal("offer", offer);
        
    } catch (err) {
        console.error("Error starting video:", err);
        alert("Camera access denied or not found.");
        videoOverlay.style.display = 'none';
    }
}

async function startDataConnection() {
    if(!selectedUser) return alert("Select a user first!");
    displayMessage({sender: 'System', content: `🔗 Establishing File Connection with ${selectedUser}...`}, true);
    try {
        createPeerConnection();
        dataChannel = peerConnection.createDataChannel("nexus-files");
        setupDataChannelEvents(dataChannel);
        const offer = await peerConnection.createOffer();
        await peerConnection.setLocalDescription(offer);
        sendSignal("offer", offer);
    } catch (err) { console.error("Error starting data connection:", err); }
}

async function onSignalReceived(payload) {
    const signal = JSON.parse(payload.body);
    if (signal.sender === username) return;
    try {
        if (signal.type === 'offer') {
            const isVideoOffer = signal.data.sdp.includes("m=video");
            const msg = isVideoOffer ? `📞 Incoming Video Call from ${signal.sender}. Accept?` : `📂 Incoming File Connection from ${signal.sender}. Accept?`;
            
            if (confirm(msg)) {
                selectedUser = signal.sender;
                if(isVideoOffer) videoOverlay.style.display = 'block';
                
                createPeerConnection();
                peerConnection.ondatachannel = (e) => setupDataChannelEvents(e.channel);
                await peerConnection.setRemoteDescription(new RTCSessionDescription(signal.data));
                processCandidateQueue();

                if (isVideoOffer) {
                    isVideoCall = true;
                    localStream = await navigator.mediaDevices.getUserMedia({ video: true, audio: true });
                    document.getElementById('localVideo').srcObject = localStream;
                    localStream.getTracks().forEach(track => peerConnection.addTrack(track, localStream));
                }

                const answer = await peerConnection.createAnswer();
                await peerConnection.setLocalDescription(answer);
                sendSignal("answer", answer);
                
                if(!isVideoOffer) displayMessage({sender: 'System', content: `✅ Connected to ${selectedUser} for files.`}, true);
            }
        } else if (signal.type === 'answer') {
            await peerConnection.setRemoteDescription(new RTCSessionDescription(signal.data));
            processCandidateQueue();
        } else if (signal.type === 'candidate') {
            if (peerConnection && peerConnection.remoteDescription) {
                await peerConnection.addIceCandidate(new RTCIceCandidate(signal.data));
            } else {
                candidateQueue.push(signal.data);
            }
        }
    } catch (error) { console.error("Signaling Error:", error); }
}

function createPeerConnection() {
    candidateQueue = [];
    peerConnection = new RTCPeerConnection(rtcConfig);
    peerConnection.ontrack = (event) => {
        if (event.streams && event.streams[0]) {
            document.getElementById('remoteVideo').srcObject = event.streams[0];
        }
    };
    peerConnection.onicecandidate = (event) => {
        if (event.candidate) sendSignal("candidate", event.candidate);
    };
}

function sendSignal(type, data) {
    stompClient.send("/app/chat.signal", {}, JSON.stringify({ type: type, sender: username, receiver: selectedUser, data: data }));
}

function endCall() {
    if (peerConnection) peerConnection.close();
    if (localStream) localStream.getTracks().forEach(track => track.stop());
    videoOverlay.style.display = 'none';
    peerConnection = null;
    isVideoCall = false;
}

function processCandidateQueue() {
    while (candidateQueue.length > 0) {
        peerConnection.addIceCandidate(new RTCIceCandidate(candidateQueue.shift())).catch(e=>{});
    }
}

// ==========================================
// 6. FILE TRANSFER LOGIC
// ==========================================
function handleFileSelect(input) {
    const file = input.files[0];
    if (!file) return;
    if (dataChannel && dataChannel.readyState === 'open') {
        sendP2PFile(file);
    } else {
        if(confirm("P2P Tunnel is closed. Connect to " + selectedUser + " now to send files?")) {
            startDataConnection();
        }
    }
    input.value = ''; 
}

function sendP2PFile(file) {
    displayMessage({
        sender: username, 
        content: `⚡ Sending file: ${file.name} (${(file.size/1024).toFixed(1)} KB)...`, 
        timestamp: new Date().toISOString(), status: 'SENT'
    }, true);

    const meta = JSON.stringify({ type: 'file-meta', name: file.name, size: file.size, fileType: file.type });
    dataChannel.send(meta);

    const chunkSize = 16 * 1024; 
    const fileReader = new FileReader();
    let offset = 0;

    fileReader.onload = (e) => {
        dataChannel.send(e.target.result);
        offset += e.target.result.byteLength;
        if (offset < file.size) {
            readSlice(offset);
        } else {
            console.log("File sent!");
        }
    };
    const readSlice = o => {
        const slice = file.slice(offset, o + chunkSize);
        fileReader.readAsArrayBuffer(slice);
    };
    readSlice(0);
}

let incomingFileInfo = null;
let incomingFileBuffer = [];
let receivedSize = 0;

function setupDataChannelEvents(channel) {
    channel.onopen = () => console.log("Data Channel Open");
    channel.onmessage = (event) => {
        const data = event.data;
        if (typeof data === 'string') {
            incomingFileInfo = JSON.parse(data);
            incomingFileBuffer = [];
            receivedSize = 0;
            displayMessage({sender: 'System', content: `⬇️ Receiving ${incomingFileInfo.name}...`}, false);
        } else {
            if (!incomingFileInfo) return;
            incomingFileBuffer.push(data);
            receivedSize += data.byteLength;
            if (receivedSize >= incomingFileInfo.size) {
                const blob = new Blob(incomingFileBuffer, { type: incomingFileInfo.fileType });
                downloadFile(blob, incomingFileInfo.name);
                incomingFileInfo = null; 
            }
        }
    };
}

function downloadFile(blob, fileName) {
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url; a.download = fileName; a.click();
    displayMessage({sender: 'System', content: `✅ File ${fileName} downloaded!`}, false);
}

// ==========================================
// 7. UTILS
// ==========================================
function dragElement(elmnt) {
    var pos1 = 0, pos2 = 0, pos3 = 0, pos4 = 0;
    var header = elmnt.querySelector("h4");
    if (header) { header.onmousedown = dragMouseDown; }

    function dragMouseDown(e) {
        e = e || window.event;
        e.preventDefault();
        pos3 = e.clientX;
        pos4 = e.clientY;
        document.onmouseup = closeDragElement;
        document.onmousemove = elementDrag;
    }
    function elementDrag(e) {
        e = e || window.event;
        e.preventDefault();
        pos1 = pos3 - e.clientX;
        pos2 = pos4 - e.clientY;
        pos3 = e.clientX;
        pos4 = e.clientY;
        elmnt.style.top = (elmnt.offsetTop - pos2) + "px";
        elmnt.style.left = (elmnt.offsetLeft - pos1) + "px";
    }
    function closeDragElement() {
        document.onmouseup = null;
        document.onmousemove = null;
    }
}

function formatTime(d) { return new Date(d).toLocaleTimeString([], {hour:'2-digit', minute:'2-digit'}); }
function getStatusIcon(s) { return s === 'READ' ? '✓✓' : (s === 'DELIVERED' ? '✓✓' : '✓'); }
function getStatusClass(s) { return s === 'READ' ? 'tick-read' : (s === 'DELIVERED' ? 'tick-delivered' : 'tick-sent'); }

if(messageForm) messageForm.addEventListener('submit', sendMessage, true);

// ==========================================
// 8. GLOBAL EXPORTS & SEARCH (FIXED)
// ==========================================

// FIXED SEARCH: Now calls the Database via API
window.searchUsers = function() {
    var query = document.getElementById("userSearch").value.trim();
    
    // If empty, return to showing friends
    if(query === "") {
        renderUserList(friendList, false);
        return;
    }

    const token = localStorage.getItem('jwtToken');
    
    // Call the Backend API
    fetch(`/api/users/search?query=${encodeURIComponent(query)}`, { 
        headers: { 'Authorization': 'Bearer ' + token }
    })
    .then(res => res.json())
    .then(results => {
        // Merge with existing friend info to keep unread counts if they appear in search
        const mergedResults = results.map(u => {
            const existing = friendList.find(f => f.username === u.username);
            return existing ? existing : { username: u.username, unread: 0 };
        });
        
        renderUserList(mergedResults, true); // true = Show Add button
    })
    .catch(err => console.error("Search Error:", err));
};

window.showChatTab = function() {
    document.getElementById("btn-chats").className = "tab-btn active-tab";
    document.getElementById("btn-requests").className = "tab-btn inactive-tab";
    renderUserList(friendList, false);
};

window.showRequestsTab = function() {
    document.getElementById("btn-chats").className = "tab-btn inactive-tab";
    document.getElementById("btn-requests").className = "tab-btn active-tab";
    renderRequestList();
};

window.startVideoCall = startVideoCall;
window.endCall = endCall;
window.handleFileSelect = handleFileSelect;

window.sendFriendRequest = function(u, e) {
    if(e) e.stopPropagation(); 
    const token = localStorage.getItem('jwtToken');
    fetch(`/api/friends/add/${u}`, { method: 'POST', headers: { 'Authorization': 'Bearer ' + token }})
    .then(res => { if(res.ok) alert(`Request Sent to ${u}!`); else res.text().then(alert); });
};

window.acceptRequest = function(u) {
    const token = localStorage.getItem('jwtToken');
    fetch(`/api/friends/accept/${u}`, { method: 'POST', headers: { 'Authorization': 'Bearer ' + token }})
    .then(res => { if(res.ok) { alert("Friend Added!"); fetchFriends(); fetchRequests(); showChatTab(); }});
};

window.rejectRequest = function(u) {
    const token = localStorage.getItem('jwtToken');
    fetch(`/api/friends/reject/${u}`, { method: 'POST', headers: { 'Authorization': 'Bearer ' + token }})
    .then(() => fetchRequests());
};



//.\mvnw.cmd clean spring-boot:run
//cloudflared tunnel --protocol http2 --url http://127.0.0.1:8080