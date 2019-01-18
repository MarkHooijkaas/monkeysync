# monkeysync
Synchornise between various formats, sql database, mailchimp, json file

## Introduction
monkeysync is designed to synchronize any changes in a source "table" to a destination "table".
- A table is basically a set of rows, consisting of String fields.
- A source table can be a SQL query, a json file, or a mailchimp list
- A destination table can be a mailchimp list, or a json file
- In the future other formats may be supported

monkeysync is a commandline tool with much flexibilty to test synchronizations interactively.
One can run simple commands on the command line, or load them from a file.

monkeysync loads all tables in memory.
The maximum size of tables is thus limited by memory.

## Running 
```
Usage: java -jar monkeysync-all-<version>.jar <command> [<arg>|<option>]*
  -c, --config=<configFile>
                       The config file.
  -h, --help           Show this help message and exit.
  -a, --ask            if set will ask before each update.
  -v, --verbose        if set, will output details.
  -V, --version        Print version information and exit.
  echo ....            echoes text to the console
  run <file>           run a script from a file
  load <name> <file>   load a table from <file> and name it <name>
  save <name> <file>   save a table with <name> to <file> 
  sync <name1> <name2> sync table <name1> to table <name2>
  syncCreate ...       sync only new records
  syncUpdate ...       sync only existing records
  syncDelete ...       sync only records that are marked as deleted in src
  syncDeleteMissing ...sync only records that are missing in src
```

On the commandline you can specify one or more commands, separated by a comma.
A command may have 1 or more arguments.
A command may also have different kinds of options, always starting with - or --
The options are parsed (and removed) before the command is parsed.

## Examples
```
monkeysync save mailchimp file.json
```
Will load/retrieve the "table", defined in the configuration file as mailchimp,
and save it to a json file

```
monkeysync sync database mailchimp
```
Will load/retrieve the "table"s, defined in the configuration file as mailchimp and database
and the sync all created, changed and deleted records from database into mailchimp.
This sync will use API calls to mailchimp.

## Tables
Tables each have a symbolic name.
The properties of a table are defined in a configuration file, by default monkeysync.props
```
users.type=SqlTable
users.user = sa
users.password = <secret>
users.url = jdbc:sqlserver://sqlserver.example.com;databaseName=PRODDB
users.class = com.microsoft.sqlserver.jdbc.SQLServerDriver
users.sqlfile=config/users.sql

mailchimp.type=MailchimpTable
mailchimp.url=https://us12.api.mailchimp.com/3.0/
mailchimp.apikey=<secret>
mailchimp.listid=<nunmericalid>
```
The above configuration defines a table named users, that is retrieved using a sql query in a separate file.
It also defines a table named mailchimp that retrieves all the members from a specific list.

Whenever the name of a table is referenced for te first time that table is retrieved from source as defined. 
Once a table is loaded/retrieved, it will stay in memory for use in other commands


## Configuration

## Complex scripts

```
monkeysync run script
```

## TODO
- logging
- more efficient memory handling
- new table types (e.g, CSV file, SQL destination, ...)
- tables with other fields than Strings (e.g. nested strutures)
- ...
