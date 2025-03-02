/ Utility script to run QSpec (https://github.com/nugend/qspec) inside the plugin.
/ The code is based on https://github.com/nugend/qspec/blob/master/app/spec.q but formats the output in correct fashion

/ Required for QSpec somewhere inside. No ideas what's that.
.tst.halt:0b;

/ Empty namespace
.tst.app.wrap:enlist[`]!(enlist (::));

/ Workaround for .utl package - we need only two things but many pain with downloading
.utl.require:{x:$[-11h=type x; string x; x]; system "l ",x;};

.tst.app.init:{[qspecPath]
    .utl.require (.utl.PKGLOADING:qspecPath,"/lib"),"/init.q";

    if[not `runSpec in .tst.app.wrap;
       .tst.app.wrap.runSpec:.tst.runSpec;
       .tst.runSpec:{.tst.app.runSpec x};
      ];

    if[not `runExpec in .tst.app.wrap;
       .tst.app.wrap.runExpec:.tst.runExpec;
       .tst.runExpec:{.tst.app.runExpec[x; y]};
      ];
 };

/ https://www.jetbrains.com/help/teamcity/service-messages.html#Escaped+Values
.tst.app.teamcityMaskChars:{
    raze {$[x in "|'[]{}\n\r"; "|",x; x]} each x
 };

.tst.app.msg:{[name;dict]
    -1 "##teamcity[",name,$[()~dict; ""; " ",(" " sv {[k;v] k,"=","'",.tst.app.teamcityMaskChars[v],"'"}'[string key dict; value dict])],"]";
 };

.tst.app.msgSuite:{[tag;spec]
    name:spec`title;
    path:$[":"=first s:string spec`tstPath; 1_s; s];
    id:"qspec:suite://",path,"?[",name,"]";
    parentId:"qspec:script://",path;
    dict:`name`id`nodeId`parentNodeId`locationHint!(name;id;id;parentId;id);
    .tst.app.msg[tag; dict];
    :dict;
 };

.tst.app.msgTestCase:{[tag;spec;expect;dict]
    name:expect`desc;
    id:ssr[spec[`id]; ":suite:"; ":test:"],"/[",name,"]";
    dict,:`name`id`nodeId`parentNodeId`locationHint`metainfo!(name;id;id;spec`nodeId;id;"");
    .tst.app.msg[tag; dict];
    :dict;
 };

.tst.app.msgMatrix:{[specs]
    .tst.app.msg["enteredTheMatrix"; ()];
    {[spec]
        suite:.tst.app.msgSuite["suiteTreeStarted"; spec];
        {[suite;x]
            .tst.app.msgTestCase["suiteTreeNode"; suite; x; ()];
        }[suite;] each spec`expectations;
        .tst.app.msgSuite["suiteTreeEnded"; spec];
    } each specs;
    .tst.app.msg["treeEnded"; ()];
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

/ this code is based on original app/qspec.q file but fully redesigned
/ The idea here - additionally to each file we get a list of liters in format (specification;expectation).
/ Each filter is compared to all loaded specs.
/ To do that, we ugroup specs to filter each row and group back with distinct expectations
.tst.app.loadSpecs:{[script;filters]
    .tst.app.specs:();

    / .utl.FILELOADING required to set tstPath in the spec
    .tst.loadTests hsym .utl.FILELOADING:hsym `$":",script;

    if[()~.tst.app.specs; :()];

    / ungroup all expectations into plain table for filtering
    expectations:update string title from ungroup update `$title from .tst.app.specs;
    / Filter out broken tests
    expectations:select from expectations where 10h=abs type each expectations[; `desc];
    / fix one char names
    expectations:update {(),x} each title, {x[`desc]:(),x[`desc]; x} each expectations from expectations;

    pot:raze {[x;y]
                 a:select from x;
                 if[not ()~y 0; a:select from a where title like ((),y 0)];
                 if[not ()~y 1; a:select from a where expectations[; `desc] like ((),y 1)];
                 :a;
             }[expectations;] each filters;
    :0!?[pot; (); c!c:cols[pot] except `expectations; (enlist `expectations)!(enlist (distinct;`expectations))];
 };

.tst.app.runSpec:{[spec]
    suite:.tst.app.msgSuite["testSuiteStarted"; spec];
    res:.tst.app.wrap.runSpec spec,suite;
    .tst.app.msgSuite["testSuiteFinished"; spec];
    :res;
 };

.tst.app.runExpec:{[spec;expec]
    .tst.app.msgTestCase["testStarted"; spec; expec; ()];
    / run original code
    res:.tst.app.wrap.runExpec[spec; expec];
    / process result
    dict:enlist[`duration]!(enlist string `long$(`long$res`time)%1000000);
    if[not `pass~res`result;
       errMsg:$[not ()~m:res`errorText; m; ()~m:res`failures; ""; "\n" sv m];
       .tst.app.msgTestCase["testFailed"; spec; expec; dict,`error`message!("true";errMsg)];
      ];
    .tst.app.msgTestCase["testFinished"; spec; expec; dict];
    :res;
 };

/ As we print result per script, we iterate over each rather than collect descriptions for all
.tst.app.runScript:{[qSpecPath;rootFolder;keepAlive;scriptsWithFilters]
    / store params for debugging
    .tst.app.params:(qSpecPath;rootFolder;keepAlive;scriptsWithFilters);
    qSpecPath:.tst.app.params 0; rootFolder:.tst.app.params 1; keepAlive:.tst.app.params 2; scriptsWithFilters:.tst.app.params 3;

    res:.tst.app.runScriptSafe[qSpecPath; rootFolder; scriptsWithFilters];

    if[not keepAlive;
       exit $[res<0; -1; 0];
      ];
    if[res=0; exit 0];
 };

.tst.app.runScriptSafe:{[qSpecPath;rootFolder;scriptsWithFilters]
    @[.tst.app.init; qSpecPath; {-2 "QSpec can't be loaded from '",x,"':\n\t",y; exit -1}[qSpecPath;]];

    .tst.app.failed:0b;

    specs:raze {
                   .[.tst.app.loadSpecs; (x 0;x 1); {-2 "Testing script '",x,"' can't be loaded:\n\t",y; .tst.app.failed:1b; ()}[x 0;]]
               } each scriptsWithFilters;

    if[()~specs;
       .tst.app.msg["enteredTheMatrix"; ()];
       .tst.app.msg["treeEnded"; ()];
       exit 0;
      ];

    .tst.app.msgMatrix specs;

    .tst.app.msgRootName rootFolder;

    res:{
        @[.tst.runSpec; x; {-2 "Test ",x[`title]," can't be executed:\n\t",y; .tst.app.failed:1b;}[x;]]
    } each specs;

    // failed in any case
    if[.tst.app.failed; :-1];

    :sum not `pass=res`result;
 };