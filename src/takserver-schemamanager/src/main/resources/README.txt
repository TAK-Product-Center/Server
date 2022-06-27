README for db-utils/schemaManager/resource directory.

Files in this directory will be bundled into the SchemaManager
JAR.

Please *do not* check in SQL files that are still in development.
You can commit those files in db-utils/pending. So if you're
working on schema version 16, please don't check in the V16__.sql
file here until schema version 16 is tested and frozen. The
release manager will take care of
migrating scripts from db-utils/pending to here when the time 
is right.

Note that, if it's helpful, we can now support "point releases"
such as V16.1, etc.

For testing purposes, copy your .SQL file to this directory
(schemaManager/resources) before you run
"ant clean deploy."  All .sql files in this directory will get
bundled into the jar.

Please see 
https://dsl-external.bbn.com/tracsvr/marti/wiki/MartiDatabase
for instructions on the naming convention for your file.

It's a good idea to clone your DB and do your testing on the
clone. 

java -jar SchemaManager.jar clone -name testing_db
java -jar SchemaManager.jar -dbname testing_db validate
java -jar SchemaManager.jar -dbname testing_db upgrade

If you prefer to do your testing with the flyway command-line
utility, you can instead copy all the SQL files (both all
past releases and your working copy of the next version) to the
flyway-4.0/sql directory and the command-line client will find
them.

