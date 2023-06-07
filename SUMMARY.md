# Summary

File-API is the microservice application written in Kotlin/Spring Boot with basic authentication.

It allows:
- upload file (using local drive storage at the moment) and receive its unique identifier (token)
- download file using token
- get meta info about the files
- delete the file by its token

Please refer to API doc before using this application: http://localhost:6011/docs

To run this application you need to download it and execute CLI command
``./do.sh start
``
or use your preferable IDE to import this project as Spring app and run it from there.

## Comments

// A short comment about the assignment in general

- Spring requires paid version of Intellij IDEA. Wouldn't pure Kotlin or Java assignment be enough?
- Gradle seems to be more convenient to use than Maven nowadays
- In general assignment is good. Not very complicated, but also not boring. The volume of work seems to be
  just fine. It was interesting and fun.

// Was the Acceptance Criteria easy to understand?

- > "When an invalid request is made, 400 Bad Request is returned with message containing information about the error"
  >
  What is considered as invalid? Can we return 404 if token is invalid? Should we validate anything? - not clear

## Which part of the assignment took the most time and why?

Testing, writing tests, bug-fixing. Why? It's kinda always like this, trying to cover possible test cases,
discovering bugs in the process, fixing them, more tests following these bugfixes etc.

## What You learned

// Example: Learned the basics about Kotlin

A bit about mongoDB, remembered maven, overall refreshed some other bits and pieces in my memory.

## TODOs

// TODO: Write what features should/could be added to this API
// Example: Add file type validation to file upload endpoint

- File type/size validation
- Other parameters validation
- Same files handling
- Other implementations of FileStorage: GridFS, S3 etc
- Improved security and authentication: eg JWT, hashes etc
