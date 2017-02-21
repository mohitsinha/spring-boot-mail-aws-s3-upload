package foo.bar;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.activation.DataHandler;
import javax.mail.*;
import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MailService {

    @Autowired
    private AmazonS3Client amazonS3Client;

    @Value("${mail.folder.name}")
    private String mailFolderName;

    @Value("${aws.bucket.name}")
    private String awsBucketName;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public void handleMail(MimeMessage message) throws Exception {
        Folder folder = message.getFolder();
        folder.open(Folder.READ_WRITE);
        String messageId = message.getMessageID();
        Message[] messages = folder.getMessages();
        FetchProfile contentsProfile = new FetchProfile();
        contentsProfile.add(FetchProfile.Item.ENVELOPE);
        contentsProfile.add(FetchProfile.Item.CONTENT_INFO);
        contentsProfile.add(FetchProfile.Item.FLAGS);
        folder.fetch(messages, contentsProfile);
        for (int i = 0; i < messages.length; i++) {

            if (((MimeMessage) messages[i]).getMessageID().equals(messageId)) {
                messages[i].setFlag(Flags.Flag.DELETED, true);

                MimeMessage mimeMessage = (MimeMessage) messages[i];

                mimeMessage.setFlag(Flags.Flag.DELETED, true);
                logger.info("SUBJECT: " + mimeMessage.getSubject());
                Address senderAddress = mimeMessage.getFrom()[0];
                logger.info("SENDER " + senderAddress.toString());
                extractDetailsAndDownload(message, mimeMessage);

                break;
            }
        }

        Store store = folder.getStore();
        Folder fooFolder = store.getFolder(mailFolderName);
        fooFolder.open(Folder.READ_WRITE);
        fooFolder.appendMessages(new MimeMessage[]{message});
        folder.expunge();
        folder.close(true);
        fooFolder.close(false);
    }

    private void extractDetailsAndDownload(Message message, MimeMessage mimeMessage) throws MessagingException, IOException {
        logger.info("SUBJECT: " + mimeMessage.getSubject());

        Multipart multipart = (Multipart) message.getContent();
        for (int j = 0; j < multipart.getCount(); j++) {

            BodyPart bodyPart = multipart.getBodyPart(j);

            String disposition = bodyPart.getDisposition();

            if (disposition != null && Part.ATTACHMENT.equalsIgnoreCase(disposition)) { // BodyPart.ATTACHMENT doesn't work for gmail
//                Upload mail attachments
                logger.info("Mail has some attachments");
                DataHandler handler = bodyPart.getDataHandler();
                logger.info("file name : " + handler.getName());
                upload(bodyPart.getInputStream(), bodyPart.getFileName());
            } else {
//                Log mail contents
                logger.info("Body: " + bodyPart.getContent());
            }

        }
    }

    public List<PutObjectResult> upload(MultipartFile[] multipartFiles) {
        List<PutObjectResult> putObjectResults = new ArrayList<>();

        Arrays.stream(multipartFiles)
                .filter(multipartFile -> !StringUtils.isEmpty(multipartFile.getOriginalFilename()))
                .forEach(multipartFile -> {
                    try {
                        putObjectResults.add(upload(multipartFile.getInputStream(), multipartFile.getOriginalFilename()));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });

        return putObjectResults;
    }

    private PutObjectResult upload(InputStream inputStream, String uploadKey) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(awsBucketName, uploadKey, inputStream, new ObjectMetadata());

        putObjectRequest.setCannedAcl(CannedAccessControlList.PublicRead);

        PutObjectResult putObjectResult = amazonS3Client.putObject(putObjectRequest);

        IOUtils.closeQuietly(inputStream);

        return putObjectResult;
    }
}
