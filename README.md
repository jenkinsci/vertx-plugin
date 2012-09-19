jenkins-vertx-plugin
====================

You got Vert.x in my Jenkins!

The plugin starts a clustered instance and deploys no real verticles on its own.
To build applications based on these events, start another clustered instance
and deploy your verticles there.

Right now, based on my development environment, I need to tweak `cluster.xml` to
set the interface to use.  The plugin uses `0.0.0.0:25000` for the cluster host
and port.  So you just need to make sure the cluster config works for your
environment and that you don't have conflicting ports.

sample application
------------------

There's a really quick-and-dirty "application" that will trigger a new build of
the job named "parameterized", and output a message whenever a job is completed.

Run it like so:

    vertx run app.js -cluster -cluster-host 0.0.0.0 -cluster-port 25001

or maybe even just:

    vertx run app.js -cluster

published messages
------------------

### address: `jenkins-vertx`

#### when jenkins starts

    {"action":"started"}

#### when jenkins stops

    {"action":"stopped"}

### address: `jenkins.run`

Notifications of Runs.  A Run is serialized like:

    {
        "id":"2012-09-06_16-41-53",
        "num":12,
        "fullDisplayName":"parameterized #12",
        "externalizableId":"parameterized#12",
        "scheduledTimestamp":1346971313525,
        "parent":{
            "name":"parameterized",
            "fullName":"parameterized",
            "url":"job/parameterized/"
            "@class":"hudson.model.FreeStyleProject",
            …
        },
        "build":{
            "@class":"hudson.model.FreeStyleBuild",
            "actions":[
                {
                    "parameters":[
                        {"name":"key","value":"vert.x value"},
                        {"name":"foo","value":"bar"}
                    ]
                },
                {
                    "causes":[
                        {"type":"vert.x"}
                    ]
                }
            ],
            "number":12,
            "result":{
                "name":"SUCCESS",
                "ordinal":0,
                "color":"BLUE"
            },
            "duration":61,
            "charset":"UTF-8",
            "keepLog":false,
            "builtOn":"",
            "workspace":"…",
            "hudsonVersion":"1.466",
            "scm":{"@class":"hudson.scm.NullChangeLogParser"},
            "culprits":[]
        },
        "artifacts":[],
        "url":"job/parameterized/12/",
        "previousBuild":{
            "@class":"hudson.model.FreeStyleBuild",
            …
        }
    }


#### when a Run is started

    {
        "action":"started",
        "run": …
    }

#### when a Run is completed

    {
        "action":"completed",
        "run": …
    }

#### when a Run is finalized

"finalized" means written to disk

    {
        "action":"finalized",
        "run": …
    }

#### when a Run is deleted

    {
        "action":"deleted",
        "run": …
    }


### address: `jenkins.item`

An Item is serialized like

    {
        "name":"parameterized",
        "fullName":"parameterized",
        "url":"job/parameterized/"
        "hudson.model.FreeStyleProject":{ … },
    }

#### when all items have been loaded

Basically when Jenkins is ready to roll.

    {"action":"allloaded"}


#### when a new item is created

    {
        "action":"created",
        "item": { … }
    }

####  when an item is updated

    {
        "action":"updated",
        "item": { … }
    }

####  when an item is renamed

    {
        "action":"renamed",
        "item": { … },
        "oldName": "…",
        "newName": "…"
    }

####  when an item is deleted

    {
        "action":"deleted",
        "item": { … }
    }


registered handlers
-------------------

### address: `jenkins`

#### schedule a build

    {
       "action":"scheduleBuild",
       "data":{
           "projectName":"someProject"
       }
    }

#### schedule a *parameterized* build

    {
       "action":"scheduleBuild",
       "data":{
           "projectName":"someParameterizedProject",
           "params": {
               "param1":"value1",
               "param2":"value2"
           }
       }
    }

Response is
    
    {"status":"ok"}

or
    
    {
        "status":"error",
        "message":"failed to schedule"
    }
 
All parameters are treated as strings.

extension points
----------------

### [QueueTaskDispatcher](http://javadoc.jenkins-ci.org/hudson/model/queue/QueueTaskDispatcher.html)

Allows a queue item to be blocked from building by calling an EventBus handler.

#### register the handler

Send a message to `jenkins.queueTaskDispatcher` with the payload

    {
        "action": "register",
        "handlerAddress" : "<address-of-your-handler>"
    }

A reply will be sent; either

    {"status":"ok"}

or 

    {"status":"error", "message":"reason"}

#### handle events

A message with the action `canRun` will be sent to ask if the queue item can be scheduled to run.

    {
        "action" : "canRun",
        "item" : {…}
    }

A reply is expected in the form of 

    {"canRun":true}

or

    {
        "canRun": false,
        "reason": "don't wanna"
    }

If no reply is received within 5 seconds (*way* too long…) or `canRun` is true, the job will be scheduled (if there is no other reason the job can't be scheduled).
