# FileProcessor

Before running the application, run `docker run --name fileprocessor-postgres -e POSTGRES_USER=user -e POSTGRES_PASSWORD=pass -e POSTGRES_DB=fileprocessor -d -p 5433:5432 postgres:14.2` to initialize the database. The FileProcessor application can then be started by either running FileProcessorApplication in IntelliJ or running the command `mvn spring-boot:run` in this directory.

An example request to this service will be `curl -X POST -F file=@EntryFile.txt -F validate=true -o OutcomeFile.json http://127.0.0.1:8080/v0/process` (if the service is running on `localhost`) where the EntryFile.txt is in the directory you are running from.

