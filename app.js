load('vertx.js');

var registeredHandlers = [];
var queueTaskDispatcherId = java.util.UUID.randomUUID().toString();

var blockedJobs = {};

function queueTaskDispatcherHandler(msg, replier) {
    /*
    MUST return null if ok or not null otherwise.
    
    msg: {
        "action" : "canRun",
        "item" : {…}
    }
    
    -- FUTURE: --
    msg: {
        "action" : "canTake",
        "node" : {…}
        "item" : {…}
    }
    */
    
    var queueItem = msg.item;

    if (blockedJobs[queueItem.task.name] === undefined) {
        blockedJobs[queueItem.task.name] = 0;
    }

    if (msg.action == "canRun") {
        var canRun = (blockedJobs[queueItem.task.name]++ >= 4);

        replier({"canRun": canRun, "reason": "don't wanna"});

        if (canRun) {
            console.log("queueTaskDispatcher -- NOT blocked " + queueItem.task.name + "[" + blockedJobs[queueItem.task.name] + "]");
            // delete blockedJobs[queueItem.task.name];
        } else {
            console.log("queueTaskDispatcher -- blocked " + queueItem.task.name + "[" + blockedJobs[queueItem.task.name] + "]");
        }
    } else {
        replier({});
    }
}

function doRegister() {
    console.log("registering handlers");

    registeredHandlers.push([vertx.eventBus.registerHandler(queueTaskDispatcherId, queueTaskDispatcherHandler), queueTaskDispatcherHandler]);

    vertx.eventBus.send(
        "jenkins.queueTaskDispatcher",
        {
            "action": "register",
            "handlerAddress" : queueTaskDispatcherId
        },
        function(r) {
            console.log("registered queueTaskDispatcher: " + JSON.stringify(r));
        }
    );
}

function doUnregister() {
    console.log("unregistering handlers");

    vertx.eventBus.send(
        "jenkins.queueTaskDispatcher",
        {
            "action": "unregister",
            "handlerAddress" : queueTaskDispatcherId
        },
        function(r) {
            console.log("unregistered queueTaskDispatcher: " + JSON.stringify(r));
        }
    );

    for (var i = 0; i < registeredHandlers.length; i++) {
        vertx.eventBus.unregisterHandler(registeredHandlers[i][0], registeredHandlers[i][1]);
    }
}

function vertxStop() {
    console.log("stopping");

    doUnregister();
}

vertx.eventBus.registerHandler("jenkins-vertx", function(msg) {
    console.log("jenkins-vertx -- " + JSON.stringify(msg));

    if (msg.action == "started") {
        doRegister();
    } else if (msg.action == "stopped") {
        doUnregister();
    }
});

vertx.eventBus.registerHandler("jenkins.item", function(msg) {
    console.log("jenkins.item -- " + JSON.stringify(msg));
});

vertx.eventBus.registerHandler("jenkins.run", function(msg) {
    // console.log("jenkins.run -- " + JSON.stringify(msg));
    var r = msg;

    if (r.action === "completed") {
        console.log(r.run.parent.name + " has " + r.action + "; result: " + r.run.build.result);
        console.log("jenkins.run -- " + JSON.stringify(msg));
    }
});

vertx.eventBus.send("jenkins", {"action": "getQueue"}, function(r) {
    doRegister();
});

console.log("ready");

// vertx.eventBus.send(
//     "jenkins",
//     {
//         "action": "scheduleBuild",
//         "data": {
//             "projectName": "parameterized",
//             "quietPeriod": 10,
//             "params": {
//                 "key":"vert.x value",
//                 "foo":"bar"
//             },
//             "cause": {
//                 "because": "I said so"
//             }
//         }
//     },
//     function(r) {
//         console.log("build scheduled -- " + JSON.stringify(r));
        
//         vertx.eventBus.send("jenkins", {"action": "getQueue"}, function(r) {
//             console.log("queue -- " + JSON.stringify(r));
//         });
//     }
// );

// // vertx.eventBus.send("jenkins", {"action": "getAllItems"}, function(r) {
// //     console.log("all items -- " + JSON.stringify(r));
// // });

