E-mail poller gmail that uses Spring AWS Cloud and Spring boot mail support 
=======



```
Spring Boot + Spring Mail Integration + Spring Cloud AWS
```

###Description
This project demonstrates how to periodically read emails from gmail, process them and upload attachments to Amazon S3.

###How to Open
```
./gradlew idea
IntelliJ -> Open -> .ipr
```

###Configuration
```
  application.properties
- cloud.aws.credentials.accessKey=
- cloud.aws.credentials.secretKey=
- cloud.aws.s3.bucket=
- cloud.aws.region=
- aws.bucket.name=
- mail.folder.name=

  resources/hello/integration.xml
- correctly configure store-uri="imaps://username:password@imap.gmail.com/INBOX"

- Give write access to current user for /var/log/email-poller-aws for creating logs
```

###How to Run
```
- ./gradlew bootRun
- Run main() on Application Class
```



