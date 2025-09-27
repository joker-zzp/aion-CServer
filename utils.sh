#!/bin/sh

write_db_properties() {
	local config_dir=$1
	local config_file=$2

	local db_url="database.url = jdbc:mysql://mysql:3306/${AION_DB_NAME}?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true"
	local db_user="database.user = ${AION_DB_USER}"
	local db_password="database.password = ${AION_DB_PASSWORD}"

	echo $db_url > $config_dir/$config_file
	echo $db_user >> $config_dir/$config_file
	echo $db_password >> $config_dir/$config_file
}

write_custom_properties() {
	local config_dir=$1
	local config_file=$2
	local key=$3
	local value=$4
	echo "$key = $value" >> $config_dir/$config_file
}
