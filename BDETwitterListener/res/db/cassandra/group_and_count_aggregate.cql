CREATE OR REPLACE FUNCTION state_group_and_count( state map<text, int>, type text )
CALLED ON NULL INPUT
RETURNS map<text, int>
LANGUAGE java AS '
Integer count = (Integer) state.get(type);  if (count == null) count = 1; else count++; state.put(type, count); return state; ' ;

CREATE OR REPLACE AGGREGATE group_and_count(text) 
SFUNC state_group_and_count 
STYPE map<text, int> 
INITCOND {};