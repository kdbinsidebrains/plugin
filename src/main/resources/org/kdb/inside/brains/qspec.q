/ Utility script to run QSpec (https://github.com/nugend/qspec) inside the plugin.
/ The code is based on https://github.com/nugend/qspec/blob/master/app/spec.q but formats the output in correct fashion

/ Required for QSpec somewhere inside. No ideas what's that.
.tst.halt:0b;

.tst.app.params:();

/ Workaround for .utl package - we need only two things but many pain with downloading
.utl.require:{x:$[-11h=type x; string x; x]; system "l ",x;};

.tst.app.init:{[qspecPath]
    .utl.require (.utl.PKGLOADING:qspecPath,"/lib"),"/init.q";
 };

/ https://www.jetbrains.com/help/teamcity/service-messages.html#Escaped+Values
.tst.app.teamcityMaskChars:{
    raze {$[x in "|'[]{}\n\r"; "|",x; x]} each x
 };

.tst.app.msg:{[name;dict]
    msg:$[()~dict;
          "##teamcity[",name,"]\n";
          "##teamcity[",name," ",(" " sv {[k;v] k,"=","'",.tst.app.teamcityMaskChars[v],"'"}'[string key dict; value dict]),"]\n"
         ];
    -1 msg;
 };

.tst.app.msgSuite:{[tag;spec]
    name:spec`title;
    path:$[":"=first s:string spec`tstPath; 1_s; s];
    id:"[engine:qspec]/[file:",path,"]/[suite:",name,"]";
    parentId:"[engine:qspec]/[file:",path,"]";
    location:"qspec:suite://",path,"?[",name,"]";
    dict:`name`id`nodeId`parentNodeId`locationHint!(name;id;id;parentId;location);
    .tst.app.msg[tag; dict];
    :dict;
 };

.tst.app.msgTestCase:{[tag;suite;expect;dict]
    name:expect`desc;
    id:suite[`id],"/[expect:",name,"]";
    location:ssr[suite[`locationHint]; ":suite:"; ":test:"],"/[",name,"]";
    dict,:`name`id`nodeId`parentNodeId`locationHint`metainfo!(name;id;id;suite`nodeId;location;"asdfasdf");
    .tst.app.msg[tag; dict];
    :dict;
 };

.tst.app.msgRootName:{
    parts:"/" vs x;
    path:"/" sv -1_parts;
    base:$[1=count fileParts:"." vs fileName:last parts; fileName; "." sv -1_fileParts];
    .tst.app.msg["rootName"; `name`comment`location!(base;path;"qspec:script://",x)];
 };

/ callback implementation: https://github.com/nugend/qspec/wiki/Callbacks
/ We do nothing here, just collecting descriptions
.tst.callbacks.descLoaded:{[specObj]
    .tst.app.specs,:enlist specObj;
 };

/ Real callback for execute tests: https://github.com/nugend/qspec/wiki/Callbacks
/ We print result of each test here
.tst.callbacks.expecRan:{[s;e]
 };

/ this code is based on original app/qspec.q file
.tst.app.loadSpecs:{[script;specifications;expectations]
    .tst.app.specs:();

    / .utl.FILELOADING required to set tstPath in the spec
    .tst.loadTests hsym .utl.FILELOADING:hsym `$":",script;

    if[0<>count specifications;
       .tst.app.specs:.tst.app.specs where (or) over .tst.app.specs[; `title] like/:specifications
      ];

    if[0<>count expectations;
       .tst.app.specs[`expectations]:{x where (or) over x[; `desc] like/:y}[; expectations] each .tst.app.specs`expectations;
       .tst.app.specs:.tst.app.specs where 0<count each .tst.app.specs[; `expectations];
      ];

    {[spec]
        suite:.tst.app.msgSuite["suiteTreeStarted"; spec];
        {[suite;x]
            .tst.app.msgTestCase["suiteTreeNode"; suite; x; ()];
        }[suite;] each spec`expectations;
        .tst.app.msgSuite["suiteTreeEnded"; spec];
    } each .tst.app.specs;
    :.tst.app.specs;
 };

/ This is modified version of qspec/lib/tests/spec.q
.tst.app.runSpec:{[suite;spec]
    oldContext:.tst.context;
    .tst.context:spec[`context];
    .tst.tstPath:spec[`tstPath];
    {[suite;spec;e]
        .tst.app.msgTestCase["testStarted"; suite; e; ()];
        res:.tst.runExpec[spec; e];
        dict:enlist[`duration]!(enlist string `long$(`long$res`time)%1000000);
        if[not `pass~res`result;
           errMsg:$[not ()~m:res`errorText; m; ()~m:res`failures; ""; "\n" sv m];
           .tst.app.msgTestCase["testFailed"; suite; e; dict,`error`message!("true";errMsg)];
          ];
        .tst.app.msgTestCase["testFinished"; suite; e; dict];
        :res;
    }[suite; spec;] each spec`expectations;
    .tst.restoreDir[];
    .tst.context:oldContext;
    .tst.tstPath:`;
    spec[`result]:$[all `pass=spec[`expectations; ; `result]; `pass; `fail];
    :spec;
 };

/ We need an ability to run each text
.tst.app.runSpecs:{[specs]
    result:{[spec]
        suite:.tst.app.msgSuite["testSuiteStarted"; spec];
        res:.tst.app.runSpec[suite; spec];
        .tst.app.msgSuite["testSuiteFinished"; spec];
        :res;
    } each specs;
    :result;
 };

/ As we print result per script, we iterate over each rather than collect descriptions for all
.tst.app.runScript:{[filename;qSpecPath;scripts;specs;expects]
    / store params for debugging
    .tst.app.params:(filename;qSpecPath;scripts;specs;expects);

    @[.tst.app.init; qSpecPath; {-2 "QSpec can't be loaded:\n",x; exit -1}];

    .tst.app.msg["enteredTheMatrix"; ()];
    specs:raze {
                   .[.tst.app.loadSpecs; (x;y;z); {-2 "Testing script can't be loaded:\n",x; exit -1}]
               }[; specs; expects] each scripts;
    .tst.app.msg["treeEnded"; ()];

    .tst.app.msgRootName[filename];

    .tst.app.result:@[.tst.app.runSpecs; specs; {-2 "Tests can't be executed:\n",x; exit -1}];

    exit 0;
 };