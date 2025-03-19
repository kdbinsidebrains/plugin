/ Utility script to run QSpec (https://github.com/nugend/qspec) inside the plugin.
/ The code is based on https://github.com/nugend/qspec/blob/master/app/spec.q but formats the output in correct fashion

/ Required for QSpec somewhere inside. No ideas what's that.
.tst.halt:0b;

/ Empty namespace
.tst.kib.wrap:enlist[`]!(enlist (::));

/ Ligth implementation of .utl.required if one is not defined
.tst.kib.require:{x:$[-11h=type x; string x; x]; system "l ",x;};

.tst.kib.init:{[qspecPath]
    / We need own implementation to load QSpec from random place
    / but we should restore original .utl.required if it's defined: issue #19
    orig_require:@[value; `.utl.require; `];
    .utl.require:.tst.kib.require;
    .utl.require (.utl.PKGLOADING:qspecPath,"/lib"),"/init.q";
    if[not null orig_require; .utl.require:orig_require];

    if[not `runSpec in .tst.kib.wrap;
       .tst.kib.wrap.runSpec:.tst.runSpec;
       .tst.runSpec:{.tst.kib.runSpec x};
      ];

    if[not `runExpec in .tst.kib.wrap;
       .tst.kib.wrap.runExpec:.tst.runExpec;
       .tst.runExpec:{.tst.kib.runExpec[x; y]};
      ];
 };

/ https://www.jetbrains.com/help/teamcity/service-messages.html#Escaped+Values
.tst.kib.teamcityMaskChars:{
    raze {$[x in "|'[]{}\n\r"; "|",x; x]} each x
 };

.tst.kib.msg:{[name;dict]
    -1 "##teamcity[",name,$[()~dict; ""; " ",(" " sv {[k;v] k,"=","'",.tst.kib.teamcityMaskChars[v],"'"}'[string key dict; value dict])],"]";
 };

.tst.kib.msgSuite:{[tag;spec]
    name:spec`title;
    path:$[":"=first s:string spec`tstPath; 1_s; s];
    id:"qspec:suite://",path,"?[",name,"]";
    parentId:"qspec:script://",path;
    dict:`name`id`nodeId`parentNodeId`locationHint!(name;id;id;parentId;id);
    .tst.kib.msg[tag; dict];
    :dict;
 };

.tst.kib.msgTestCase:{[tag;spec;expect;dict]
    name:expect`desc;
    id:ssr[spec[`id]; ":suite:"; ":test:"],"/[",name,"]";
    dict,:`name`id`nodeId`parentNodeId`locationHint`metainfo!(name;id;id;spec`nodeId;id;"");
    .tst.kib.msg[tag; dict];
    :dict;
 };

.tst.kib.msgMatrix:{[specs]
    .tst.kib.msg["enteredTheMatrix"; ()];
    {[spec]
        suite:.tst.kib.msgSuite["suiteTreeStarted"; spec];
        {[suite;x]
            .tst.kib.msgTestCase["suiteTreeNode"; suite; x; ()];
        }[suite;] each spec`expectations;
        .tst.kib.msgSuite["suiteTreeEnded"; spec];
    } each specs;
    .tst.kib.msg["treeEnded"; ()];
 };

.tst.kib.msgRootName:{
    parts:"/" vs x;
    path:"/" sv -1_parts;
    base:$[1=count fileParts:"." vs fileName:last parts; fileName; "." sv -1_fileParts];
    .tst.kib.msg["rootName"; `name`comment`location!(base;path;"qspec:script://",x)];
 };

/ callback implementation: https://github.com/nugend/qspec/wiki/Callbacks
/ We do nothing here, just collecting descriptions
.tst.callbacks.descLoaded:{[specObj]
    .tst.kib.specs,:enlist specObj;
 };

/ Real callback for execute tests: https://github.com/nugend/qspec/wiki/Callbacks
/ We print result of each test here
.tst.callbacks.expecRan:{[s;e]
 };

/ this code is based on original app/qspec.q file but fully redesigned
/ The idea here - additionally to each file we get a list of filters in format (specification;expectation).
/ Each filter is compared to all loaded specs.
/ To do that, we ugroup specs to filter each row and group back with distinct expectations
.tst.kib.loadSpecs:{[script;filters]
    .tst.kib.specs:();

    / .utl.FILELOADING required to set tstPath in the spec
    .tst.loadTests hsym .utl.FILELOADING:hsym `$":",script;

    if[()~.tst.kib.specs; :()];

    / ungroup all expectations into plain table for filtering
    expectations:update string title from ungroup update `$title from .tst.kib.specs;
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

.tst.kib.runSpec:{[spec]
    suite:.tst.kib.msgSuite["testSuiteStarted"; spec];
    res:.tst.kib.wrap.runSpec spec,suite;
    .tst.kib.msgSuite["testSuiteFinished"; spec];
    :res;
 };

.tst.kib.runExpec:{[spec;expec]
    .tst.kib.msgTestCase["testStarted"; spec; expec; ()];
    / run original code
    res:.tst.kib.wrap.runExpec[spec; expec];
    / process result
    dict:enlist[`duration]!(enlist string `long$(`long$res`time)%1000000);
    if[not `pass~res`result;
       errMsg:$[not ()~m:res`errorText; m; ()~m:res`failures; ""; "\n" sv m];
       .tst.kib.msgTestCase["testFailed"; spec; expec; dict,`error`message!("true";errMsg)];
      ];
    .tst.kib.msgTestCase["testFinished"; spec; expec; dict];
    :res;
 };

/ As we print result per script, we iterate over each rather than collect descriptions for all
.tst.kib.runScript:{[qSpecPath;rootFolder;keepAlive;scriptsWithFilters]
    / store params for debugging
    .tst.kib.params:(qSpecPath;rootFolder;keepAlive;scriptsWithFilters);
    / Testing only
    / qSpecPath:.tst.kib.params 0; rootFolder:.tst.kib.params 1; keepAlive:.tst.kib.params 2; scriptsWithFilters:.tst.kib.params 3;

    res:.tst.kib.runScriptSafe[qSpecPath; rootFolder; scriptsWithFilters];

    if[not keepAlive;
       exit $[res<0; -1; 0];
      ];
    if[res=0; exit 0];
 };

.tst.kib.runScriptSafe:{[qSpecPath;rootFolder;scriptsWithFilters]
    @[.tst.kib.init; qSpecPath; {-2 "QSpec can't be loaded from '",x,"':\n\t",y; exit -1}[qSpecPath;]];

    .tst.kib.failed:0b;

    specs:raze {
                   .[.tst.kib.loadSpecs; (x 0;x 1); {-2 "Testing script '",x,"' can't be loaded:\n\t",y; .tst.kib.failed:1b; ()}[x 0;]]
               } each scriptsWithFilters;

    if[()~specs;
       .tst.kib.msg["enteredTheMatrix"; ()];
       .tst.kib.msg["treeEnded"; ()];
       exit 0;
      ];

    .tst.kib.msgMatrix specs;

    .tst.kib.msgRootName rootFolder;

    res:{
        @[.tst.runSpec; x; {-2 "Test ",x[`title]," can't be executed:\n\t",y; .tst.kib.failed:1b;}[x;]]
    } each specs;

    // failed in any case
    if[.tst.kib.failed; :-1];

    :sum not `pass=res`result;
 };