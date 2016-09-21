package ru.linachan.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.linachan.yggdrasil.YggdrasilCore;
import ru.linachan.yggdrasil.plugin.YggdrasilPluginManager;
import ru.linachan.yggdrasil.scheduler.YggdrasilRunnable;

import javax.mail.*;
import java.util.Properties;

public class EMailListener implements YggdrasilRunnable {

    private String pop3Server;

    private String emailUser;
    private String emailPassword;

    private static Logger logger = LoggerFactory.getLogger(EMailListener.class);

    public EMailListener() {
        pop3Server = YggdrasilCore.INSTANCE.getConfig().getString("email.pop3.host", "pop.gmail.com");

        emailUser = YggdrasilCore.INSTANCE.getConfig().getString("email.user", "user@gmail.com");
        emailPassword = YggdrasilCore.INSTANCE.getConfig().getString("email.password", "password");
    }

    private void receiveMessages() throws MessagingException {
        Session session = Session.getDefaultInstance(new Properties());

        Store store = session.getStore("pop3s");
        store.connect(pop3Server, emailUser, emailPassword);

        Folder folder = store.getFolder("inbox");

        if (!folder.exists()) {
            logger.warn("Given mailbox doesn't contain inbox folder.");
            return;
        }

        folder.open(Folder.READ_ONLY);

        Message[] messages = folder.getMessages();
        if (messages.length != 0) {
            for (Message message: messages) {
                YggdrasilCore.INSTANCE
                    .getManager(YggdrasilPluginManager.class)
                    .get(EMailPlugin.class)
                    .handleMessage(message);
            }
        }

        folder.close(false);
        store.close();
    }

    @Override
    public void run() {
        try {
            receiveMessages();
        } catch (MessagingException e) {
            logger.error("Unable to receive messages: {}", e.getMessage());
        }
    }

    @Override
    public void onCancel() {

    }
}
