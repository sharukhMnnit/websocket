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
'use strict';

// --- 1. CONFIGURATION & STATE ---
var username = localStorage.getItem('username');
var jwtToken = localStorage.getItem('jwtToken');
var stompClient = null;
var selectedUser = null;

// --- DOM ELEMENTS ---
var usersList = document.querySelector('#usersList');
var messageForm = document.querySelector('#messageForm');
var messageInput = document.querySelector('#message');
var messageArea = document.querySelector('#messageArea');
var chatTitle = document.querySelector('#chat-title');

// --- 2. INITIALIZATION ---
if (username && jwtToken) {
    // Request notification permission immediately on load
    if (Notification.permission !== "granted") {
        Notification.requestPermission();
    }
    connect();
    setTimeout(fetchAllUsers, 500); 
} else {
    window.location.href = '/login.html';
}

// --- 3. THE TUNNEL: WEBSOCKET CONNECTION ---
function connect() {
    var socket = new SockJS('/ws');
    stompClient = Stomp.over(socket);

    // FIX: Send the token in the CONNECT headers
    var headers = {
        'Authorization': 'Bearer ' + jwtToken
    };

    stompClient.connect(headers, onConnected, onError);
}

function onConnected() {
    // Subscribe to public events (like user joins/leaves)
    stompClient.subscribe('/topic/public', onMessageReceived);
    // Subscribe to the private tunnel for instant message pushes
    stompClient.subscribe('/user/queue/messages', onPrivateMessageReceived);
    
    stompClient.send("/app/chat.addUser", {}, JSON.stringify({sender: username, type: 'JOIN'}));
}

function onError(error) {
    console.error('WebSocket Error: ' + error);
}

// --- 4. DATA FETCHING ---
function fetchAllUsers() {
    fetch('/api/users', {
        method: 'GET',
        headers: { 'Authorization': 'Bearer ' + jwtToken }
    })
    .then(response => response.json())
    .then(users => {
        usersList.innerHTML = ''; 
        users.forEach(user => {
            if (user && user !== username) {
                addUserToSidebar(user);
            }
        });
    })
    .catch(error => console.error('Error fetching users:', error));
}

// --- 5. AUTOMATIC UI UPDATES (The logic you were missing) ---
function onPrivateMessageReceived(payload) {
    var message = JSON.parse(payload.body);
    console.log("Real-time message arrived:", message);

    // If I'm currently looking at the sender, draw it NOW
    if (selectedUser && selectedUser.toLowerCase() === message.sender.toLowerCase()) {
        displayMessage(message, false); 
        messageArea.scrollTop = messageArea.scrollHeight;
    } else {
        // If I'm not looking at them, trigger the sidebar alert
        updateSidebarAlert(message.sender, message.content);
    }
}

function updateSidebarAlert(sender, content) {
    var userLi = document.getElementById("user-" + sender);
    
    if (userLi) {
        // 1. Add the "Red Alert" class automatically (Needs CSS)
        userLi.classList.add('has-new-message');
        
        // 2. Trigger Browser Popup if the tab is hidden
        showBrowserNotification(sender, content);
    } else {
        // If a new user messaged us who isn't in the list, refresh the list
        fetchAllUsers();
    }
}

// --- 6. CHAT HISTORY & SELECTION ---
function selectUser(user) {
    selectedUser = user;
    chatTitle.innerText = "Private Chat with " + user;
    messageArea.innerHTML = ""; 
    
    // UI: Remove active highlights and clear the red alert for THIS user
    document.querySelectorAll('#usersList li').forEach(item => item.classList.remove('active'));
    var activeLi = document.getElementById("user-" + user);
    if(activeLi) {
        activeLi.classList.add('active');
        activeLi.classList.remove('has-new-message'); // Manual check is gone! Clearing alert here.
    }

    // Load History Road
    fetch(`/api/messages/${username}/${user}`, {
        method: 'GET',
        headers: { 
            'Authorization': 'Bearer ' + jwtToken,
            'Content-Type': 'application/json'
        }
    })
    .then(res => res.json())
    .then(messages => {
        messages.forEach(msg => {
            displayMessage(msg, msg.sender === username); 
        });
    })
    .catch(err => console.error('Error loading history:', err));
}

// --- 7. UTILITY FUNCTIONS ---
function showBrowserNotification(sender, content) {
    if (Notification.permission === "granted" && document.hidden) {
        new Notification("New Message from " + sender, {
            body: content,
            icon: "/favicon.ico"
        });
    }
}

function sendMessage(event) {
    var messageContent = messageInput.value.trim();
    if(messageContent && stompClient) {
        var chatMessage = {
            sender: username,
            content: messageContent,
            type: 'CHAT'
        };

        if (selectedUser) {
            chatMessage.receiver = selectedUser;
            stompClient.send("/app/chat.private", {}, JSON.stringify(chatMessage));
            displayMessage(chatMessage, true);
        } else {
            stompClient.send("/app/chat.sendMessage", {}, JSON.stringify(chatMessage));
        }
        messageInput.value = '';
    }
    event.preventDefault();
}

function onMessageReceived(payload) {
    var message = JSON.parse(payload.body);
    if (message.type === 'JOIN' && message.sender !== username) {
        fetchAllUsers(); // Auto-update user list when someone new enters the tunnel
    }
}

function displayMessage(message, isSelf) {
    var messageElement = document.createElement('li');
    messageElement.classList.add('message-item');
    messageElement.classList.add(isSelf ? 'message-self' : 'message-other');

    var text = document.createElement('p');
    text.innerText = message.content;
    messageElement.appendChild(text);

    messageArea.appendChild(messageElement);
    messageArea.scrollTop = messageArea.scrollHeight;
}

function addUserToSidebar(newUsername) {
    var li = document.createElement('li');
    li.id = "user-" + newUsername;
    li.innerHTML = `
        <div class="user-avatar">${newUsername.charAt(0).toUpperCase()}</div>
        <span>${newUsername}</span>
        <div class="user-status"></div>
    `;
    li.onclick = function() { selectUser(newUsername); };
    usersList.appendChild(li);
}

if(messageForm) messageForm.addEventListener('submit', sendMessage, true);