{
    / fix for root namespace
    v:$[x=y; x; ` sv x,y];
    / list of all inner namespaces - we get all keys that has ` as the first key and it's value is (::) - that's namespace
    ns:l where {$[`~first key x; (::)~x[`]; 0b]} each ` sv'v,'l:key[v] except `;
    / get all functions, tables, variables in current namespace
    r:(system') ("fav",\:" "),\:string v;
    / Functions
    fns:{v:$[x=`; y; ` sv x,y]; t:type value v; a:$[112h=t; enlist `; 4h=type first s:2 value/v; s 1; $[t in 101 103h; enlist `x; t in 102 106 107 108 109 110 111h; `x`y; enlist `]]; (y;t;a)}[v;] each r[0];
    / return the table details in format (name;size;meta;keys)
    tbs:{v:$[x=`; y; ` sv x,y]; (y;count value v;0!meta v;keys v)}[v;] each r[1];
    / variables are anything except namespaces and tables. We get (name;type;description) for each
    /   for dictionary - (size;keys type; values type)
    / Description depends on the type and is:
    vrs:{v:$[x=`; get y; x[y]]; t:type v; (y;t;$[99h=t; (count v;type key v;type value v); ()])}[v;] each r[2] except ns,r[1];
    / return final result; (namespace;function;tables;variables;list of inner namespaces or an error)
    (y;fns;tbs;vrs;.z.s[v;] each ns)
 }[`; `]