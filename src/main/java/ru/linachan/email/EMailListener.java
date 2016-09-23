package ru.linachan.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.linachan.yggdrasil.YggdrasilCore;
import ru.linachan.yggdrasil.plugin.YggdrasilPluginManager;
import ru.linachan.yggdrasil.scheduler.YggdrasilRunnable;

import javax.mail.*;
import javax.mail.search.FlagTerm;
import java.util.Properties;

public class EMailListener implements YggdrasilRunnable {

    private String emailServer;
    private String emailProtocol;

    private String emailUser;
    private String emailPassword;

    private static Logger logger = LoggerFactory.getLogger(EMailListener.class);

    public EMailListener() {
        emailServer = core.getConfig().getString("email.inbox.host", "imap.gmail.com");
        emailProtocol = core.getConfig().getString("email.inbox.protocol", "imaps");

        emailUser = core.getConfig().getString("email.user", "user@gmail.com");
        emailPassword = core.getConfig().getString("email.password", "password");
    }

    private void receiveMessages() throws MessagingException {
        Session session = Session.getInstance(new Properties());

        Store store = session.getStore(emailProtocol);
        store.connect(emailServer, emailUser, emailPassword);

        Folder inbox = store.getFolder("INBOX");

        if (!inbox.exists()) {
            logger.warn("Given mailbox doesn't contain inbox folder.");
            return;
        }

        inbox.open(Folder.READ_WRITE);

        Message[] messages = inbox.search(new FlagTerm(new Flags(Flags.Flag.SEEN), false));
        if (messages.length != 0) {
            for (Message message: messages) {
                YggdrasilCore.INSTANCE
                    .getManager(YggdrasilPluginManager.class)
                    .get(EMailPlugin.class)
                    .handleMessage(message);

                inbox.setFlags(new Message[] { message }, new Flags(Flags.Flag.SEEN), true);
            }
        }

        inbox.close(false);
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
