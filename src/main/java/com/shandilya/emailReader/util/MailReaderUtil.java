package com.shandilya.emailReader.util;

import com.shandilya.emailReader.constants.EmailConstants;
import com.shandilya.emailReader.vo.EmailMessageDetails;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;

import javax.mail.*;
import javax.mail.internet.MimeMultipart;
import javax.mail.search.FlagTerm;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;


public class MailReaderUtil {


    public static EmailMessageDetails probe() {
        Properties properties = new Properties();
        Message[] messages = new Message[100];
        Folder inbox = null;
        try {
            properties.load(new FileInputStream(new File(EmailConstants.GMAIL_PROPS)));
            // Java Mail Session is the Context of how we are going to interact with mail host
            // Create the session
            Session session = Session.getDefaultInstance(properties, null);
            // Store is an abstract class which models the message store and the access protocol
            // Used to store and retrieve messages
            // Create a Store from the Session
            // Protocol used is imaps
            // TIP : SMTP is used to send message (Handles the outgoing messages)
            // IMAP & POP3 are two popular protocols to receive message
            Store store = session.getStore(EmailConstants.PROTOCOL);

            // Connect to the store using smtp host of Gmail, gmail UID & Pass
            store.connect(EmailConstants.HOST, EmailConstants.USER, EmailConstants.PASS);
            // Get Handle to the Inbox Folder
            inbox = store.getFolder(EmailConstants.INBOX_FOLDER);
            // Open Inbox in RW mode
            inbox.open(Folder.READ_WRITE);
            // Retrieve all the messages
            messages = inbox.getMessages();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return EmailMessageDetails.builder()
                .messages(messages)
                .inbox(inbox)
                .build();
    }

    @SneakyThrows
    public static int unseen() {
        final EmailMessageDetails probe = probe();
        Flags seen = new Flags(Flags.Flag.SEEN);
        FlagTerm unseenFlag = new FlagTerm(seen, Boolean.FALSE);
        Message[] unseenMessages = probe.getInbox().search(unseenFlag);
        return Math.max(unseenMessages.length, 0);
    }

    @SneakyThrows
    public static void readUnseen() {
        final int unseen = unseen();
        if (unseen > 0) {
            int readCount = 0;
            final EmailMessageDetails probe = probe();
            final Message[] messages = probe.getMessages();
            // Mark as read once message processed
            probe.getInbox().setFlags(messages, new Flags(Flags.Flag.SEEN), Boolean.TRUE);
            for (Message m : messages) {
                System.out.println(getTextFromMessage(m));
                readCount++;
                if (readCount >= unseen) break;
            }
        } else {
            System.out.println("No New Messages!");
        }
    }

    private static String getTextFromMessage(Message message) throws MessagingException, IOException {
        String result = "";
        // MIME - Multipurpose Internet Mail Extension
        if (message.isMimeType("text/plain")) {
            result = message.getContent().toString();
        } else if (message.isMimeType("multipart/*")) {
            MimeMultipart mimeMultipart = (MimeMultipart) message.getContent();
            result = getTextFromMimeMultipart(mimeMultipart);
        }
        return result;
    }

    private static String getTextFromMimeMultipart(
            MimeMultipart mimeMultipart) throws MessagingException, IOException{
        StringBuilder result = new StringBuilder();
        int count = mimeMultipart.getCount();
        for (int i = 0; i < count; i++) {
            BodyPart bodyPart = mimeMultipart.getBodyPart(i);
            if (bodyPart.isMimeType("text/plain")) {
                result.append("\n").append(bodyPart.getContent());
                break;
            } else if (bodyPart.isMimeType("text/html")) {
                String html = (String) bodyPart.getContent();
                result.append("\n").append(org.jsoup.Jsoup.parse(html).text());
            } else if (bodyPart.getContent() instanceof MimeMultipart){
                result.append(getTextFromMimeMultipart((MimeMultipart) bodyPart.getContent()));
            }
        }
        return result.toString();
    }
}