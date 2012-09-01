load('vertx.js');

function handler(msg) {
    console.log(JSON.stringify(msg));
}

vertx.eventBus.registerHandler("jenkins-vertx", handler);
vertx.eventBus.registerHandler("jenkins.items", handler);
vertx.eventBus.registerHandler("jenkins.run", handler);

console.log("ready");
