$(function () {
    console.log("HI");

    var r = jsRoutes.controllers.Application.socket();
    var ws = new WebSocket(r.webSocketURL());
    ws.onmessage = function(msg) {
        console.log("onmessage", msg);
        $(".messages pre").prepend(msg.data + "\n");
    };

    $(".start").click(function() {
        ws.send("start");
    });
});