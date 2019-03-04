package org.kisst.monkeysync;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;


public class Mail {

    public static void sendFromGMail(Props props, String body) {
        String host = props.getString("host");
        String from= props.getString("user");
        String pass= props.getString("password");
        String[] to= props.getStrings("to");
        String subject= props.getString("subject");

        Properties mailProps = new Properties();
        mailProps.put("mail.smtp.port", props.getString("port","587"));
        mailProps.put("mail.smtp.auth", props.getString("auth","true"));
        mailProps.put("mail.smtp.starttls.enable", props.getString("starttls.enable","true"));

        Session session = Session.getDefaultInstance(mailProps);
        MimeMessage message = new MimeMessage(session);

        try {
            message.setFrom(new InternetAddress(from));

            for( int i = 0; i < to.length; i++)
                message.addRecipient(Message.RecipientType.TO, new InternetAddress(to[i]));

            message.setSubject(subject);
            message.setText(body);
            Transport transport = session.getTransport("smtp");
            transport.connect(host, from, pass);
            transport.sendMessage(message, message.getAllRecipients());
            transport.close();
        }
        catch (AddressException e) {throw new RuntimeException(e);}
        catch (MessagingException e) {throw new RuntimeException(e);}
    }

    public static void send(Props props) {
        String template;
        try {
            template = new String(Files.readAllBytes(Paths.get(props.getString("bodyTemplate"))));
        }
        catch (IOException e) { throw new RuntimeException(e); }
        String body=Env.substitute(template);
        sendFromGMail(props, body);
    }
}