psql -c 'create user postgres createdb' postgres
psql -c 'create database world;' -U postgres
psql -c '\i world.psql' -d world -U postgres
psql -d world -c "create type myenum as enum ('foo', 'bar')" -U postgres
# psql -d world -c "create extension postgis" -U postgres # doen't work for user 'postgres'
psql -d world -c "create extension postgis" -U postgres
psql -d world -U postgres -c "select name, continent, population from country where name like 'U%';"