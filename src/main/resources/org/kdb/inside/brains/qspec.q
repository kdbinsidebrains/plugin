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
    dict,:`name`id`nodeId`parentNodeId`locationHint`metainfo!(name;id;id;suite`nodeId;location;"");
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
.tst.app.runSuite:{[spec]
    suite:.tst.app.msgSuite["testSuiteStarted"; spec];
    res:.tst.app.runSpec[suite; spec];
    .tst.app.msgSuite["testSuiteFinished"; spec];
    :res;
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

    pot:raze {[x;y]
                 a:select from x;
                 if[not ()~y 0; a:select from a where title like y 0];
                 if[not ()~y 1; a:select from a where expectations[; `desc] like y 1];
                 :a;
             }[update string title from ungroup update `$title from .tst.app.specs;] each filters;
    :0!?[pot; (); c!c:cols[pot] except `expectations; (enlist `expectations)!(enlist (distinct;`expectations))];
 };

/ As we print result per script, we iterate over each rather than collect descriptions for all
.tst.app.runScript:{[qSpecPath;rootFolder;scriptsWithFilters]
    / store params for debugging
    .tst.app.params:(qSpecPath;rootFolder;scriptsWithFilters);

    qSpecPath:.tst.app.params 0; rootFolder:.tst.app.params 1; scriptsWithFilters:.tst.app.params 2;
    @[.tst.app.init; qSpecPath; {-2 "QSpec can't be loaded from ",x,":\n\t",y; exit -1}[qSpecPath;]];

    specs:raze {
                   .[.tst.app.loadSpecs; (x 0;x 1); {-2 "Testing script '",x,"' can't be loaded:\n\t",y; exit -1}[x 0;]]
               } each scriptsWithFilters;

    .tst.app.msgMatrix specs;

    .tst.app.msgRootName rootFolder;

    {@[.tst.app.runSuite; x; {-2 "Test ",x[`title]," can't be executed:\n\t",y; exit -1}[x;]];} each specs;

    exit 0;
 };