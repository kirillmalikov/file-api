# Test assignment: File API

For assignment description see the [assignment.md](assignment.md) file.

## Notes

There is no Java version of this. As on a daily basis we work with Kotlin projects here, the test-assignment is a short introduction into the technologies that we use here.

The aim of the test-assignment is not to only test Your development skills, but to give You an overview of how and what we do here.

### Tips

Kotlin and Java classes work very well together (https://kotlinlang.org/docs/mixing-java-kotlin-intellij.html#adding-kotlin-source-code-to-an-existing-java-project).

## Start-up

### Starting the database
    docker-compose up -d

### Configuration

See `variables.env` file

## Usage
In development add

    127.0.0.1    filedb
to your `/etc/hosts` file

For basic auth, username is `admin` and password is `hunter2`

### Start from CLI

    ./do.sh start

For API documentation go to http://localhost:6011/docs
