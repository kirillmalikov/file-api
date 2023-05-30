# Task overview

At the moment every API has its own upload/download endpoints implemented, but maintaining them all is difficult.
To improve the situation, an API is needed that allows uploading files and accessing them with a token.
The API is responsible for saving the file on disk and storing metadata about it.
For each uploaded file, a unique token (UUID) should be generated.

Boilerplate for this service is already provided with a MongoDB database set up.

**Acceptance**
- Code follows basic coding conventions (see for Java and Kotlin standards below)
- User can upload files and a token for this file is returned (see documentation below)
- User can request metadata for the token, multiple tokens metadata can be queried for in a single request
- User can request a file with token, file is returned with valid headers set
- [openapi.yml](src/main/resources/public/docs/openapi.yml) file describes all the endpoints
- When an invalid request is made, 400 Bad Request is returned with message containing information about the error
- Internal exceptions that happen are handled and `503 Service Unavailable` is returned
- Critical exceptions are logged
  - Should use [Logger.kt](src/main/kotlin/com/hrblizz/fileapi/library/log/Logger.kt) component
- [SUMMARY.md](SUMMARY.md) file is filled with Your commments

# Submission

You can submit your code whichever way you’re most comfortable with. A zip archive is fine.
But if you are familiar with version control tools, you probably know the benefits of setting up a repository, keeping your commit history and providing us with a link where we can leave feedback right in your code.

As for any additional scripts, documents or your draft-and-test files (yes, please!), include them in a subdirectory.

Also please include an informative [SUMMARY.md] where you at the very least describe what your app (“basically”) does and how to start it.

# Coding standards

For most of the new code Kotlin is preferred, but for this task Java can be used.

For Kotlin - https://kotlinlang.org/docs/reference/coding-conventions.html
For Java - https://google.github.io/styleguide/javaguide.html

## Golden coding rules
- ALL files MUST end with a newline symbol
- NO trailing whitespaces are allowed (GIT hates them)
- Use 4 spaces for indentation (also for continuation indentation). Do not use tabs.
- Just 1 newline between different code parts is enough (unless required by linter)
- Line lengths soft limit is 80 characters and hard limit is 120 characters
- Always use hard line wrap, because GIT does not work with soft wraps
- No unused code is allowed, Including commented code for most cases

## API spec
*File upload*

    POST /files

It's a multipart/form-data request with the following form fields:

    - name
    - contentType
    - meta # JSON of additional meta. Example: {"creatorEmployeeId": 1}
    - source # timesheet, mss, hrb, ...
    - expireTime # optional
    - content # file content

And the response is JSON:

    > HTTP 201
    {
        "token": "file-token-1"
    }

*GET file metdata endpoint*

    POST /files/metas
    {
        "tokens": [
            "file-token-1",
            ...
        ]
    }

Response;

    >HTTP 200
    {
        "files": {
            "file-token-1": {
                "token": "file-token-1",
                "filename": "Example.pdf",
                "size": 525,
                "contentType": "application/pdf",
                "createTime": "2019-11-21T15:42:22Z", // file upload time
                "meta": {
                    "creatorEmployeeId": 1
                }
            },
            ...
        }
    }

*File download endpoint*

    GET /file/{token}

returns the file in the body + additional headers:

    > HTTP 200
    X-Filename: "example.pdf"
    X-Filesize: "525"
    X-CreateTime: "2019-11-21T15:42:22Z"
    Content-Type: "application/pdf"

*File delete*

    DELETE /file/{token}

*GET file metadata endpoint*
