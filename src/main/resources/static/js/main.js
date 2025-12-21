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
//         <div class="user-avatar">${newUsername.charAt(0).toUpperCase()}</div>
//         <span>${newUsername}</span>
//         <div class="user-status"></div>
//     `;
//     li.onclick = function() { selectUser(newUsername); };
//     usersList.appendChild(li);
// }

// function onMessageReceived(payload) {
//     var message = JSON.parse(payload.body);
//     if (message.type === 'JOIN' && message.sender.toLowerCase() !== username.toLowerCase()) {
//         fetchAllUsers(); // Refresh when new users join
//     }
// }

// if(messageForm) messageForm.addEventListener('submit', sendMessage, true);
//



'use strict';

// --- CONFIGURATION ---
var username = localStorage.getItem('username');
var stompClient = null;
var selectedUser = null;

// --- DOM ELEMENTS ---
var usersList = document.querySelector('#usersList');
var messageForm = document.querySelector('#messageForm');
var messageInput = document.querySelector('#message');
var messageArea = document.querySelector('#messageArea');
var chatTitle = document.querySelector('#chat-title');

// --- 1. INITIALIZATION ---
if (username) {
    connect();
    // Fetch users slightly delayed to ensure DOM is ready
    setTimeout(fetchAllUsers, 500); 
} else {
    window.location.href = '/login.html';
}

// --- 2. CONNECT TO WEBSOCKET ---
function connect() {
    // RELATIVE URL: Works on Cloudflare/Ngrok/Localhost automatically
    var socket = new SockJS('/ws'); 
    stompClient = Stomp.over(socket);

    // Disable debug logs in console to keep it clean
    stompClient.debug = null; 

    var headers = {
        'Authorization': 'Bearer ' + localStorage.getItem('jwtToken')
    };

    stompClient.connect(headers, onConnected, onError);
}

function onConnected() {
    console.log("Connected to WebSocket!");
    stompClient.subscribe('/topic/public', onMessageReceived);
    stompClient.subscribe('/user/queue/messages', onPrivateMessageReceived);
    
    // Notify server that I joined
    stompClient.send("/app/chat.addUser", {}, JSON.stringify({sender: username, type: 'JOIN'}));
}

function onError(error) {
    console.error('WebSocket Error:', error);
    // Optional: Redirect to login if auth fails specifically (uncomment if needed)
    // window.location.href = '/login.html'; 
}

// --- 3. FETCH USERS ---
function fetchAllUsers() {
    const token = localStorage.getItem('jwtToken');
    if (!token) return;

    fetch('/api/users', {
        method: 'GET',
        headers: { 
            'Authorization': 'Bearer ' + token.trim() 
        }
    })
    .then(response => {
        if (response.status === 401 || response.status === 403) {
            console.log("Session expired.");
            return [];
        }
        return response.json();
    })
    .then(users => {
        if (!usersList) return; // Guard clause
        usersList.innerHTML = ''; // Clear list
        let foundOtherUsers = false;

        if (Array.isArray(users)) {
            users.forEach(user => {
                // Show everyone EXCEPT myself
                if (user && user.toLowerCase() !== username.toLowerCase()) {
                    addUserToSidebar(user);
                    foundOtherUsers = true;
                }
            });
        }
        
        if (!foundOtherUsers) {
            var li = document.createElement('li');
            li.innerHTML = '<span style="padding:10px; font-size:0.8em; color:#ccc;">No other users online.</span>';
            usersList.appendChild(li);
        }
    })
    .catch(err => console.error("Fetch Error:", err));
}

// --- 4. SIDEBAR UI ---
function addUserToSidebar(user) {
    var li = document.createElement('li');
    li.id = "user-" + user;
    li.innerHTML = `
        <div class="user-avatar">${user.charAt(0).toUpperCase()}</div>
        <span>${user}</span>
    `;
    li.onclick = function() { selectUser(user); };
    usersList.appendChild(li);
}

// --- 5. SELECT USER ---
function selectUser(user) {
    selectedUser = user;
    if(chatTitle) chatTitle.innerText = "Chat with " + user;
    messageArea.innerHTML = "";
    
    document.querySelectorAll('#usersList li').forEach(li => li.classList.remove('active'));
    var activeLi = document.getElementById("user-" + user);
    if (activeLi) {
        activeLi.classList.add('active');
        activeLi.classList.remove('has-new-message');
    }

    // Fetch History
    const token = localStorage.getItem('jwtToken');
    fetch(`/api/messages/${username}/${user}`, {
        headers: { 'Authorization': 'Bearer ' + token }
    })
    .then(res => res.json())
    .then(msgs => {
        msgs.forEach(msg => displayMessage(msg, msg.sender === username));
    })
    .catch(err => console.error("History Error:", err));
}

// --- 6. SEND MESSAGE ---
function sendMessage(event) {
    event.preventDefault();
    var messageContent = messageInput.value.trim();
    
    if (messageContent && stompClient) {
        var chatMessage = {
            sender: username,
            content: messageContent,
            type: 'CHAT',
            receiver: selectedUser
        };

        if (selectedUser) {
            // Private Message
            stompClient.send("/app/chat.private", {}, JSON.stringify(chatMessage));
            displayMessage(chatMessage, true); // Instant Echo
        } else {
            // Public Message (if you have global chat enabled)
            stompClient.send("/app/chat.sendMessage", {}, JSON.stringify(chatMessage));
        }
        messageInput.value = '';
    }
}

function onPrivateMessageReceived(payload) {
    var message = JSON.parse(payload.body);
    if (selectedUser && selectedUser.toLowerCase() === message.sender.toLowerCase()) {
        displayMessage(message, false);
    } else {
        var li = document.getElementById("user-" + message.sender);
        if(li) li.classList.add('has-new-message');
    }
}

function onMessageReceived(payload) {
    var message = JSON.parse(payload.body);
    if (message.type === 'JOIN') fetchAllUsers();
}

function displayMessage(message, isSelf) {
    var li = document.createElement('li');
    li.classList.add('message-item', isSelf ? 'message-self' : 'message-other');
    li.innerHTML = `<span class="message-sender">${isSelf ? 'You' : message.sender}</span><p>${message.content}</p>`;
    messageArea.appendChild(li);
    messageArea.scrollTop = messageArea.scrollHeight;
}

if(messageForm) messageForm.addEventListener('submit', sendMessage, true);