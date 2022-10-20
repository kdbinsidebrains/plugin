{
    / fix for root namespace
    v:$[x=y; x; ` sv x,y];
    / list of all inner namespaces - we get all keys and if the first value is (::) - that's namespace
    ns:l where (::)~'(first')(value') ` sv'v,'l:key[v] except `;
    / get all functions, tables, variables
    r:{system y," ",x}[string v;] each "fav";
    / return a dict with: table `name`size`meta
    tbs:{v:$[x=`; y; ` sv x,y]; (y;count value v;0!meta v)}[v;] each r[1];
    / variables are anything except namespaces and tables. We get `name`type for each
    vrs:{(y;type get $[x=`; y; ` sv x,y])}[v;] each r[2] except ns,r[1];
    / return final result
    (y;r[0];tbs;vrs;.z.s[v;] each ns)
 }[`; `]