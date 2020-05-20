username=postgres
createuser --createrole -createdb --echo $username
createdb --username=$username world