load('vertx.js');

function hndl1(msg) {
    console.log("jenkins-vertx -- " + JSON.stringify(msg));
}

function hndl2(msg) {
    console.log("jenkins.item -- " + JSON.stringify(msg));
}

function hndl3(msg) {
    // console.log("jenkins.run -- " + JSON.stringify(msg));
    var r = msg;

    if (r.action === "completed") {
        console.log(r.run.fullDisplayName + " has " + r.action + "; result: " + r.run.build.result.name);
    }
}

var hndl1_id = vertx.eventBus.registerHandler("jenkins-vertx", hndl1);
var hndl2_id = vertx.eventBus.registerHandler("jenkins.item", hndl2);
var hndl3_id = vertx.eventBus.registerHandler("jenkins.run", hndl3);

function vertxStop() {
    console.log("stopping");

    vertx.eventBus.unregisterHandler(hndl1_id, hndl1);
    vertx.eventBus.unregisterHandler(hndl2_id, hndl2);
    vertx.eventBus.unregisterHandler(hndl3_id, hndl3);
}

console.log("ready");

vertx.eventBus.send(
    "jenkins",
    {
        "action": "scheduleBuild",
        "data": {
            "projectName": "parameterized",
            "params": {
                "key":"vert.x value",
                "foo":"bar"
            }
        }
    },
    function(r) {
        console.log(JSON.stringify(r));
    }
);

