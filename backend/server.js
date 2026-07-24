const express = require('express');
const http = require('http');
const { Server } = require('socket.io');
const cors = require('cors');
const os = require('os');

const app = express();
app.use(cors());

// Lightweight discovery API endpoint for auto-detecting server IP
app.get('/api/info', (req, res) => {
  res.json({ status: 'ok', app: 'Calling Bell' });
});

const server = http.createServer(app);

const io = new Server(server, {
  cors: {
    origin: "*", // allow all origins for development
    methods: ["GET", "POST"]
  }
});

// In-memory room state & socket user management
// roomCode -> { isPremium: boolean, ringHistory: Array }
const roomStates = new Map();
// socket.id -> { userName: string, roomCode: string }
const socketUsers = new Map();

const FREE_USER_LIMIT = 3;

function getLocalIpAddresses() {
  const interfaces = os.networkInterfaces();
  const addresses = [];
  for (const name of Object.keys(interfaces)) {
    for (const iface of interfaces[name]) {
      if (iface.family === 'IPv4' && !iface.internal) {
        addresses.push(iface.address);
      }
    }
  }
  return addresses;
}

// REST API for discovering active available rooms
app.get('/api/rooms', (req, res) => {
  const activeRooms = [];
  for (const [code, state] of roomStates.entries()) {
    const clients = io.sockets.adapter.rooms.get(code);
    const count = clients ? clients.size : 0;
    if (count > 0) {
      activeRooms.push({
        roomCode: code,
        count,
        isPremium: state.isPremium
      });
    }
  }
  res.json({ status: 'ok', rooms: activeRooms });
});

function getRoomMembersList(roomCode) {
  const clients = io.sockets.adapter.rooms.get(roomCode);
  if (!clients) return [];
  const members = [];
  for (const socketId of clients) {
    const user = socketUsers.get(socketId);
    members.push({
      socketId,
      userName: user ? user.userName : 'Anonymous'
    });
  }
  return members;
}

function broadcastRoomUpdate(roomCode) {
  const room = roomStates.get(roomCode);
  const clients = io.sockets.adapter.rooms.get(roomCode);
  const count = clients ? clients.size : 0;
  const members = getRoomMembersList(roomCode);

  io.to(roomCode).emit('room_status', {
    count,
    isPremium: room ? room.isPremium : false,
    maxLimit: room && room.isPremium ? Infinity : FREE_USER_LIMIT
  });

  io.to(roomCode).emit('room_members_list', members);
}

io.on('connection', (socket) => {
  console.log(`User connected: ${socket.id}`);

  // Endpoint to check available rooms over socket
  socket.on('get_available_rooms', () => {
    const activeRooms = [];
    for (const [code, state] of roomStates.entries()) {
      const clients = io.sockets.adapter.rooms.get(code);
      const count = clients ? clients.size : 0;
      if (count > 0) {
        activeRooms.push({
          roomCode: code,
          count,
          isPremium: state.isPremium
        });
      }
    }
    socket.emit('available_rooms_list', activeRooms);
  });

  // Check if room code is available before creation
  socket.on('check_create_room', (roomCode) => {
    const code = (roomCode || '').trim().toUpperCase();
    const clients = io.sockets.adapter.rooms.get(code);
    if (clients && clients.size > 0) {
      socket.emit('room_code_check_result', {
        available: false,
        roomCode: code,
        message: `Room code "${code}" is already active! Please try a different code or generate a unique one.`
      });
    } else {
      socket.emit('room_code_check_result', {
        available: true,
        roomCode: code
      });
    }
  });

  // User requests to join or create a specific room by code
  socket.on('join_room', ({ roomCode, userName, isUserPremium, isCreating }) => {
    const code = (roomCode || '').trim().toUpperCase();
    const clients = io.sockets.adapter.rooms.get(code);
    const currentCount = clients ? clients.size : 0;

    // Check duplicate room code creation rule
    if (isCreating && currentCount > 0) {
      socket.emit('room_code_taken', {
        roomCode: code,
        message: `Room code "${code}" is already active! Two rooms cannot be created with the same code. Please try a different code.`
      });
      return;
    }

    // Get or initialize room state
    let room = roomStates.get(code);
    if (!room) {
      room = { isPremium: !!isUserPremium, ringHistory: [] };
      roomStates.set(code, room);
    }

    // If host or joining user is premium, mark room as premium
    if (isUserPremium) {
      room.isPremium = true;
    }

    // Check Free Tier User Limit (Limit to 3 users unless Pro)
    if (!room.isPremium && currentCount >= FREE_USER_LIMIT) {
      socket.emit('room_full_premium_required', {
        message: `Free rooms are limited to ${FREE_USER_LIMIT} devices. Upgrade to PRO to allow unlimited users!`,
        currentCount,
        maxLimit: FREE_USER_LIMIT
      });
      return;
    }

    // Save user info
    socketUsers.set(socket.id, { userName: userName || 'Anonymous', roomCode: code });

    // Join socket room
    socket.join(code);
    console.log(`User ${socket.id} (${userName || 'Anonymous'}) joined room: ${code}`);

    // Notify others in the room
    socket.to(code).emit('user_joined', { id: socket.id, name: userName || 'Partner' });

    // Send back current room status, roster, and history
    broadcastRoomUpdate(code);
    socket.emit('ring_history_sync', room.ringHistory);
  });

  // Scan members inside room
  socket.on('scan_room_members', (roomCode) => {
    const code = (roomCode || '').trim().toUpperCase();
    const members = getRoomMembersList(code);
    socket.emit('room_members_scanned', members);
    socket.emit('room_members_list', members);
  });

  // Handle room upgrade to Premium
  socket.on('upgrade_room_premium', (roomCode) => {
    const code = (roomCode || '').trim().toUpperCase();
    let room = roomStates.get(code);
    if (!room) {
      room = { isPremium: true, ringHistory: [] };
      roomStates.set(code, room);
    } else {
      room.isPremium = true;
    }

    broadcastRoomUpdate(code);
    io.to(code).emit('premium_unlocked_notice');
  });

  // Handle rich ring event with intention, custom sound, and optional target member
  socket.on('ring_bell', ({ roomCode, intention, sound, senderName, targetSocketId, targetMemberName }) => {
    const code = (roomCode || '').trim().toUpperCase();
    console.log(`Ring event from ${senderName || socket.id} in room ${code} [Intention: ${intention?.id || 'general'}, Target: ${targetSocketId || 'All'}]`);

    const ringEntry = {
      id: Date.now().toString(),
      from: socket.id,
      senderName: senderName || 'Someone',
      intention: intention || { id: 'general', label: 'Ring', emoji: '🔔' },
      sound: sound || 'classic',
      targetMemberName: targetMemberName || null,
      timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' })
    };

    // Save to history (keep last 20 rings)
    let room = roomStates.get(code);
    if (room) {
      room.ringHistory.unshift(ringEntry);
      if (room.ringHistory.length > 20) room.ringHistory.pop();
    }

    if (targetSocketId) {
      // Direct ring to a specific member
      const clients = io.sockets.adapter.rooms.get(code);
      const isTargetInRoom = clients && clients.has(targetSocketId);

      if (isTargetInRoom) {
        io.to(targetSocketId).emit('receive_ring', ringEntry);
        // Delivery acknowledgment back to sender
        socket.emit('ring_ack', {
          status: 'delivered',
          recipientName: targetMemberName || 'Selected Member',
          count: 1,
          isTargeted: true,
          timestamp: ringEntry.timestamp
        });
      } else {
        // Target member not in room
        socket.emit('ring_ack', {
          status: 'not_delivered',
          reason: `Member "${targetMemberName || 'Selected'}" is no longer connected to this room.`,
          recipientName: targetMemberName || 'Selected Member',
          count: 0,
          isTargeted: true,
          timestamp: ringEntry.timestamp
        });
      }
    } else {
      // Broadcast to everyone else in the room
      const clients = io.sockets.adapter.rooms.get(code);
      const recipientCount = clients ? clients.size - 1 : 0;

      if (recipientCount > 0) {
        socket.to(code).emit('receive_ring', ringEntry);
        socket.emit('ring_ack', {
          status: 'delivered',
          recipientName: `${recipientCount} room member(s)`,
          count: recipientCount,
          isTargeted: false,
          timestamp: ringEntry.timestamp
        });
      } else {
        socket.emit('ring_ack', {
          status: 'not_delivered',
          reason: 'No other active members in the room to receive your ring.',
          recipientName: 'Room Devices',
          count: 0,
          isTargeted: false,
          timestamp: ringEntry.timestamp
        });
      }
    }

    // Broadcast updated history log
    if (room) {
      io.to(code).emit('ring_history_sync', room.ringHistory);
    }
  });

  socket.on('disconnecting', () => {
    const userObj = socketUsers.get(socket.id);
    for (const roomCode of socket.rooms) {
      if (roomCode !== socket.id) {
        socket.to(roomCode).emit('user_left', socket.id);
      }
    }
    if (userObj && userObj.roomCode) {
      // Broadcast roster update after disconnection
      setTimeout(() => {
        broadcastRoomUpdate(userObj.roomCode);
      }, 100);
    }
  });

  socket.on('disconnect', () => {
    console.log(`User disconnected: ${socket.id}`);
    socketUsers.delete(socket.id);
  });
});

const PORT = process.env.PORT || 3001;

server.listen(PORT, () => {
  console.log(`\n==================================================`);
  console.log(`🔔 Calling Bell Server running on port ${PORT}`);
  console.log(`==================================================`);
  console.log(`💻 Localhost: http://localhost:${PORT}`);
  const localIps = getLocalIpAddresses();
  if (localIps.length > 0) {
    console.log(`📡 Wi-Fi / Local Network IP(s):`);
    localIps.forEach(ip => {
      console.log(`   👉 http://${ip}:${PORT}`);
    });
  }
  console.log(`==================================================\n`);
});
