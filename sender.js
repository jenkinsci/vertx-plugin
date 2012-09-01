load('vertx.js');

function handler(msg) {
    console.log(JSON.stringify(msg));
}

vertx.eventBus.registerHandler("jenkins-vertx", handler);
vertx.eventBus.registerHandler("jenkins.items", handler);
vertx.eventBus.registerHandler("jenkins.run", handler);

vertx.setPeriodic(3000, function() {
    console.log("sending");
    vertx.eventBus.publish("jenkins-vertx", {"addr":"jenkins-vertx"});
    vertx.eventBus.publish("jenkins.items", {"addr":"jenkins.items"});
    vertx.eventBus.publish("jenkins.run", {"addr":"jenkins.run"});
    console.log("sent");
});
