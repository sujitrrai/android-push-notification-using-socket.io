// Setup basic express server
var express = require('express');
var app = express();
var server = require('http').createServer(app);
var io = require('socket.io')(server);
var port = process.env.PORT || 3000;

server.listen(port, function () {
  console.log('Server listening at port %d', port);
});

var clients = 0;
// Routing
app.use(express.static(__dirname + '/public'));

var button = {};
var buttons = {};
var buttonspressed = [];
//var clients = [];

var connections = [];
for(var i=1;i<=9;i++){
  buttons[i]= {};
  buttonspressed[i] = 0;
}



// Chatroom

// usernames which are currently connected to the chat


io.on('connection', function (socket) {
  if(clients<9){
    
    occupants(socket);
    console.log("got 1 connection");
    clients++;
    console.log('number of clients : '+clients);
    socket.on("type",function(data){
      console.log("connections before");
      console.log(connections);
      console.log("data");
      console.log(data);
      // for(var i=0;i<connections.length;i++){
      //   if(connections[i].socketId===socket.id){
      //     connections[i].socket.disconnect();
      //   }
      // }
      connections.push({socketId:socket.id,type:data,socket:socket});
      console.log("connections after");
      console.log(connections);
    });
    socket.on('pressedButton',function(data){
      var length = buttonspressed.length;
      var occupied = false;
      console.log("button : "+data);
      var num = data*1;
      if(buttonspressed[num]===1){
          socket.emit('ack',{ack:0});
          occupied = true;
      }
      else {
        buttonspressed[num]=1;
        buttons[num] = {};
        buttons[num].num = num;
        buttons[num].socketId = socket.id;
        socket.emit('ack',{ack:1,num:num});
        socket.broadcast.emit('occupied',{num:num});
        socket.broadcast.emit('oc',{num:num});
        //socket.broadcast.emit('nm',{title:'new button',message:''+data+' pressed'});
        console.log('after reserving buttons');
        console.log(buttons);
        console.log(buttonspressed);
      }

    });
    socket.on('disconnect',function(){
      for(var i=0;i<connections.length;i++){
        if(connections[i].socketId===socket.id){
          connections.splice(i,1);
        }
      }
      console.log('connections');
      console.log(connections);
      console.log('socket disconnected');
      clients--;
      console.log('number of clients : '+clients);
      for(var i=1;i<buttonspressed.length;i++){
        if(buttons[i].socketId === socket.id){
          buttonspressed[buttons[i].num] = 0;
          buttons[i] = {};
          socket.broadcast.emit("available",{av:i});
          socket.broadcast.emit("av",{av:i})
        }
      }
      console.log("after disconnection");
      console.log(buttons);
      console.log(buttonspressed);
    });
  }
  else {
    socket.disconnect();
  }
});

function occupants(socket){
  if(clients>0){
      console.log('clients > 0');
      var contains = false;
      var jsonarr = [];
      for(var i=1;i<=9;i++){
        if(buttonspressed[i] === 1){
          contains = true;
          jsonarr.push({num:i});
          console.log(jsonarr);
        }
      }
      if(contains){
        console.log('sending existing data');
        socket.emit('preOccupied',jsonarr);
      } 
    }
}