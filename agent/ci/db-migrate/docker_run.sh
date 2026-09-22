#!/bin/bash
set -eo pipefail
echo "excuting migrate sql ..."

FLYWAY_HOME="/flyway"
SQL_HOME="db/migration"
DB_HOST="${MYSQL_HOST}"
DB_PORT="${MYSQL_PORT}"
DB_USER="${MYSQL_USER}"
DB_PASS="${MYSQL_PASSWORD}"
DB_NAME_FILE="${FLYWAY_HOME}/${SQL_HOME}/database.txt"

function flyway_cmd()
{
    local db=$1
    local cmd=$2
    local sql_path="${SQL_HOME}/${db}"
    local init_sql="CREATE DATABASE IF NOT EXISTS ${db} DEFAULT charset UTF8MB4 COLLATE utf8mb4_general_ci;use ${db}"
    local db_url="jdbc:mysql://${DB_HOST}:${DB_PORT}"
    flyway -user="${DB_USER}" -password="${DB_PASS}" -initSql="${init_sql}" -locations="${sql_path}" -url="${db_url}" ${cmd}
}

function check_result()
{
    echo "check execute result ..."
    local result="$1"
    local failed_count
    failed_count=$(echo "${result}"|grep -c -w -i "failed")
    return "${failed_count}"
}

echo "excuting migrate sql start ..."
grep -Ev '^$|#' "${DB_NAME_FILE}" | while read -r db;
do
    echo "migrating database:${db}"
    flyway_cmd "${db}" migrate 2>&1
    flyway_cmd "${db}" info 2>&1
done

echo "excuting migrate sql end."