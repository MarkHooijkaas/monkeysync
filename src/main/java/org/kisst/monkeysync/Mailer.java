package org.kisst.monkeysync;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kisst.script.BasicLanguage;
import org.kisst.script.Config;
import org.kisst.script.Context;
import org.kisst.script.Script;

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


public class Mailer extends BasicLanguage.BasicStep {
    private static final Logger logger= LogManager.getLogger();
    private final String name;
    private final String template;
    private final Props mailprops;

    public Mailer(Config cfg, String[] args) {
        super(cfg);
        if (args.length != 2)
            throw new IllegalArgumentException(args[0]+" should have 1 parameter <mailcfg>");
        this.name = args[1];
        mailprops=cfg.props.getProps(name);
        try {
            template = new String(Files.readAllBytes(Paths.get(mailprops.getString("bodyTemplate"))));
        }
        catch (IOException e) { throw new RuntimeException(e); }
    }

    @Override public String toString() { return "send "+name;}

    @Override public void run(Context ctx) {
        String body=ctx.substitute(template);
        if (! getConfig().props.getBoolean("dryRun", false))
            sendFromGMail(mailprops, body);
        logger.debug("sending mail\n{}", body);
    }


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

}