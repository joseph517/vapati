#!/bin/bash
# Wait for SQL Server to start
sleep 15s

/opt/mssql-tools18/bin/sqlcmd \
  -S localhost \
  -U sa \
  -P "TuContrasenaSegura!" \
  -i /init-db.sql \
  -C