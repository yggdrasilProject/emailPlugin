package ru.linachan.email;

import ru.linachan.yggdrasil.YggdrasilCore;
import ru.linachan.yggdrasil.plugin.YggdrasilPlugin;
import ru.linachan.yggdrasil.plugin.helpers.Plugin;
import ru.linachan.yggdrasil.scheduler.YggdrasilTask;

import javax.mail.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Plugin(name = "email", description = "Provides ability to send and receive E-Mails.")
public class EMailPlugin implements YggdrasilPlugin {

    private String smtpServer;

    private String emailUser;
    private String emailPassword;

    private Map<String, EMailHandler> messageHandlers = new HashMap<>();

    @Override
    public void onInit() {
        smtpServer = core.getConfig().getString("email.smtp.host", "smtp.gmail.com");

        emailUser = core.getConfig().getString("email.user", "user@gmail.com");
        emailPassword = core.getConfig().getString("email.password", "password");

        Integer checkDelay = core.getConfig().getInt("email.check.delay", 10);
        Integer checkInterval = core.getConfig().getInt("email.check.interval", 120);

        core.getScheduler().scheduleTask(new YggdrasilTask(
            "EMailWatch", new EMailListener(), checkDelay, checkInterval, TimeUnit.SECONDS
        ));
    }

    @Override
    public void onShutdown() {
        core.getScheduler().getTask("EMailWatch").cancelTask();
    }

    public void registerHandler(String routingKey, EMailHandler handler) {
        messageHandlers.put(routingKey, handler);
    }

    public void sendMessage(Message message) throws MessagingException {
        sendMessage(message, message.getAllRecipients());
    }

    public void sendMessage(Message message, Address[] recipients) throws MessagingException {
        Session session = Session.getDefaultInstance(new Properties());

        Transport transport = session.getTransport("smtp");
        transport.connect(smtpServer, emailUser, emailPassword);

        transport.sendMessage(message, recipients);

        transport.close();
    }

    public void handleMessage(Message message) throws MessagingException {
        String subject = message.getSubject();

        Matcher subjectMatcher = Pattern.compile(
            "^((RE|re|Re):?\\s*)*(\\[-(?<rk>[A-Z0-9]+)-\\])\\s*(?<title>.*?)$"
        ).matcher(subject);

        if (subjectMatcher.matches()) {
            String routingKey = subjectMatcher.group("rk");
            String title = subjectMatcher.group("title");

            if (messageHandlers.containsKey(routingKey)) {
                messageHandlers.get(routingKey).handle(title, message);
            } else {
                logger.warn("No handler registered for routing key: {}", routingKey);
            }
        } else {
            logger.warn("No routing key detected for message: {}", subject);
        }
    }
}
