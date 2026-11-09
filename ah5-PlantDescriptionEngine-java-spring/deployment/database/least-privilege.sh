# Sourced by the mysql image entrypoint after it has created MYSQL_USER and run 10-schema.sql.
# The entrypoint grants that application account ALL on MYSQL_DATABASE. At run time the system
# only reads and changes rows (JPA ddl-auto=validate, no migrations, Log4j2 inserts into logs),
# so the account keeps SELECT, INSERT, UPDATE and DELETE and loses DDL, DROP and the rest.
# Schema changes are made by root through the initialisation scripts.
if [ -n "${MYSQL_USER:-}" ] && [ -n "${MYSQL_DATABASE:-}" ]; then
	mysql_note "Restricting '$MYSQL_USER' to data-manipulation privileges on $MYSQL_DATABASE"
	# same escaped schema pattern as the entrypoint's GRANT, otherwise the REVOKE does not match it
	docker_process_sql --database=mysql <<-EOSQL
		REVOKE ALL ON \`${MYSQL_DATABASE//_/\\_}\`.* FROM '$MYSQL_USER'@'%' ;
		GRANT SELECT, INSERT, UPDATE, DELETE ON \`${MYSQL_DATABASE//_/\\_}\`.* TO '$MYSQL_USER'@'%' ;
	EOSQL
fi
